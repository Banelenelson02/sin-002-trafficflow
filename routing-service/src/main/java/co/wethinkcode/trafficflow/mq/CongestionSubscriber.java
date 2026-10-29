
package co.wethinkcode.trafficflow.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import co.wethinkcode.trafficflow.Congestion;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.concurrent.atomic.AtomicInteger;

public class CongestionSubscriber {

    private final AtomicInteger currentLevel = new AtomicInteger(0);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean receivedUpdate;

    public int getCurrentLevel() {
        return currentLevel.get();
    }

    public synchronized void initializeCurrentLevel(int level) {
        validateLevel(level);
        if (!receivedUpdate) {
            currentLevel.set(level);
            System.out.println(
                    "Initialized congestion level from snapshot: " + level);
        }
    }

    synchronized void recordUpdate(int level) {
        validateLevel(level);
        currentLevel.set(level);
        receivedUpdate = true;
    }

    private void validateLevel(int level) {
        if (level < 0 || level > 8) {
            throw new IllegalArgumentException(
                    "Congestion level must be between 0 and 8");
        }
    }

    public void start() throws Exception {
        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(MqConfig.BROKER_URL);

        Connection connection = factory.createConnection();
        connection.setExceptionListener(exception ->
                System.err.println("ActiveMQ connection error: "
                        + exception.getMessage()));

        Session session = connection.createSession(
                false, Session.AUTO_ACKNOWLEDGE);

        Topic topic = session.createTopic(MqConfig.TOPIC);
        MessageConsumer consumer = session.createConsumer(topic);

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if (message instanceof TextMessage textMessage) {
                    try {
                        Congestion congestion = objectMapper.readValue(
                                textMessage.getText(), Congestion.class);

                        if (congestion.level() >= 0
                                && congestion.level() <= 8) {
                            recordUpdate(congestion.level());
                            System.out.println(
                                    "Received congestion update: "
                                            + congestion.level());
                        }
                    } catch (Exception e) {
                        System.err.println(
                                "Could not process congestion message: "
                                        + e.getMessage());
                    }
                }
            }
        });

        connection.start();
        System.out.println("Subscribed to ActiveMQ topic: " + MqConfig.TOPIC);
    }
}
