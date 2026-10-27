package co.wethinkcode.trafficflow.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatPublisher {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ActiveMQConnectionFactory factory =
                        new ActiveMQConnectionFactory(
                                MqConfig.BROKER_URL
                        );

                try (Connection connection = factory.createConnection()) {
                    connection.start();

                    Session session = connection.createSession(
                            false,
                            Session.AUTO_ACKNOWLEDGE
                    );

                    Queue queue = session.createQueue(
                            MqConfig.HEARTBEAT_QUEUE
                    );

                    MessageProducer producer = session.createProducer(queue);

                    TextMessage message = session.createTextMessage(
                            "Intersection service is alive"
                    );
                    message.setStringProperty(
                            MqConfig.MESSAGE_TYPE_PROPERTY,
                            MqConfig.HEARTBEAT_MESSAGE_TYPE
                    );

                    producer.send(message);
                    producer.close();
                    session.close();

                    System.out.println("Heartbeat sent to ActiveMQ.");
                }

            } catch (Exception e) {
                System.err.println(
                        "Could not send heartbeat: " + e.getMessage()
                );
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
