package co.wethinkcode.trafficflow;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.concurrent.atomic.AtomicLong;

public class HeartbeatListener {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String QUEUE_NAME = "intersection-heartbeat-queue";

    private final AtomicLong lastHeartbeat = new AtomicLong(0);

    public void start() throws Exception {
        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(BROKER_URL);

        Connection connection = factory.createConnection();
        connection.start();

        Session session = connection.createSession(
                false,
                Session.AUTO_ACKNOWLEDGE
        );

        Queue queue = session.createQueue(QUEUE_NAME);
        MessageConsumer consumer = session.createConsumer(queue);

        consumer.setMessageListener((Message message) -> {
            lastHeartbeat.set(System.currentTimeMillis());
            System.out.println("Watchdog received a heartbeat.");
        });

        System.out.println("Watchdog listening for heartbeats.");
    }

    public boolean isHeartbeatMissing() {
        long lastReceived = lastHeartbeat.get();

        if (lastReceived == 0) {
            return true;
        }

        return System.currentTimeMillis() - lastReceived > 15000;
    }
}