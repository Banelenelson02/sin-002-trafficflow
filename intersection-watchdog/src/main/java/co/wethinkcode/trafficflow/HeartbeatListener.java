package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatListener implements AutoCloseable {

    private static final String EXPECTED_HEARTBEAT =
            "Intersection service is alive";

    private final String heartbeatQueue;
    private final String heartbeatMessageType;

    public HeartbeatListener() {
        this(MqConfig.HEARTBEAT_QUEUE, MqConfig.HEARTBEAT_MESSAGE_TYPE);
    }

    HeartbeatListener(String heartbeatQueue, String heartbeatMessageType) {
        this.heartbeatQueue = heartbeatQueue;
        this.heartbeatMessageType = heartbeatMessageType;
    }

    private final AtomicLong lastHeartbeat = new AtomicLong(0);
    private final AtomicLong lastDeadLetter = new AtomicLong(0);
    private final AtomicInteger invalidHeartbeatAttempts = new AtomicInteger(0);

    private Connection connection;
    private Session heartbeatSession;
    private Session deadLetterSession;

    public synchronized void start() throws Exception {
        if (connection != null) {
            return;
        }

        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        factory.getRedeliveryPolicy().setMaximumRedeliveries(
                MqConfig.MAX_REDELIVERIES
        );

        connection = factory.createConnection();

        heartbeatSession = connection.createSession(
                true,
                Session.SESSION_TRANSACTED
        );

        Queue queue = heartbeatSession.createQueue(heartbeatQueue);
        MessageConsumer consumer = heartbeatSession.createConsumer(
                queue,
                heartbeatSelector()
        );
        consumer.setMessageListener(
                message -> handleHeartbeat(heartbeatSession, message)
        );

        deadLetterSession = connection.createSession(
                false,
                Session.AUTO_ACKNOWLEDGE
        );
        Queue deadLetterQueue = deadLetterSession.createQueue(
                MqConfig.DEAD_LETTER_QUEUE
        );
        MessageConsumer deadLetterConsumer = deadLetterSession.createConsumer(
                deadLetterQueue,
                heartbeatSelector()
        );
        deadLetterConsumer.setMessageListener(this::handleDeadLetter);

        connection.start();
        System.out.println("Watchdog listening for heartbeats.");
    }

    public boolean isHeartbeatMissing() {
        long lastReceived = lastHeartbeat.get();

        if (lastReceived == 0) {
            return true;
        }

        return System.currentTimeMillis() - lastReceived > 15000;
    }

    public boolean hasDeadLetteredHeartbeat() {
        return lastDeadLetter.get() != 0;
    }

    int invalidHeartbeatAttempts() {
        return invalidHeartbeatAttempts.get();
    }

    boolean isValidHeartbeat(Message message) {
        try {
            return message instanceof TextMessage textMessage
                    && EXPECTED_HEARTBEAT.equals(textMessage.getText());
        } catch (Exception ignored) {
            return false;
        }
    }

    private void handleHeartbeat(Session session, Message message) {
        try {
            if (!isValidHeartbeat(message)) {
                invalidHeartbeatAttempts.incrementAndGet();
                session.rollback();
                System.err.println("Rejected invalid heartbeat for redelivery.");
                return;
            }

            session.commit();
            recordHeartbeat();
            System.out.println("Watchdog received a heartbeat.");
        } catch (Exception e) {
            System.err.println("Could not process heartbeat: " + e.getMessage());
            try {
                session.rollback();
            } catch (Exception rollbackError) {
                System.err.println(
                        "Could not roll back heartbeat: "
                                + rollbackError.getMessage()
                );
            }
        }
    }

    private void handleDeadLetter(Message message) {
        recordDeadLetter();
        System.err.println("Watchdog received a dead-lettered heartbeat.");
    }

    void recordHeartbeat() {
        lastHeartbeat.set(System.currentTimeMillis());
        lastDeadLetter.set(0);
    }

    void recordDeadLetter() {
        lastDeadLetter.set(System.currentTimeMillis());
    }

    private String heartbeatSelector() {
        return MqConfig.MESSAGE_TYPE_PROPERTY + " = '"
                + heartbeatMessageType + "'";
    }

    @Override
    public synchronized void close() throws Exception {
        if (connection != null) {
            connection.close();
            connection = null;
            heartbeatSession = null;
            deadLetterSession = null;
        }
    }
}
