
package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TextMessage;

public class CongestionPublisher {

    public void publish(int level) throws Exception {
        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(MqConfig.BROKER_URL);

        try (Connection connection = factory.createConnection()) {
            connection.start();

            Session session = connection.createSession(
                    false, Session.AUTO_ACKNOWLEDGE);

            try {
                Topic topic = session.createTopic(MqConfig.TOPIC);

                try (MessageProducer producer = session.createProducer(topic)) {
                    TextMessage message = session.createTextMessage(
                            "{\"level\":" + level + "}");

                    producer.send(message);
                }
            } finally {
                session.close();
            }
        }
    }
}
