package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbeatDlqIntegrationTest {

    private HeartbeatListener listener;

    @AfterEach
    void stopListener() throws Exception {
        if (listener != null) {
            listener.close();
        }
    }

    @Test
    @Tag("integration")
    void retriesMalformedHeartbeatOnceThenDetectsItsDeadLetter() throws Exception {
        Assumptions.assumeTrue(
                Boolean.getBoolean("trafficflow.integrationTests"),
                "Requires an ActiveMQ broker on " + MqConfig.BROKER_URL
        );

        // Use unique destinations/markers so running services and other
        // watchdog consumers cannot steal or acknowledge this test's messages.
        String testId = UUID.randomUUID().toString().replace("-", "");
        String queueName = MqConfig.HEARTBEAT_QUEUE + "-test-" + testId;
        String messageType = MqConfig.HEARTBEAT_MESSAGE_TYPE + "-" + testId;

        listener = new HeartbeatListener(queueName, messageType);
        listener.start();
        sendMalformedHeartbeat(queueName, messageType);

        await(() -> listener.hasDeadLetteredHeartbeat(), Duration.ofSeconds(10));

        assertEquals(2, listener.invalidHeartbeatAttempts());
        assertTrue(listener.hasDeadLetteredHeartbeat());
    }

    private void sendMalformedHeartbeat(String queueName, String messageType) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
                MqConfig.BROKER_URL
        );
        try (Connection connection = factory.createConnection()) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);
            try (MessageProducer producer = session.createProducer(queue)) {
                TextMessage message = session.createTextMessage("malformed heartbeat");
                message.setStringProperty(
                        MqConfig.MESSAGE_TYPE_PROPERTY,
                        messageType
                );
                producer.send(message);
            } finally {
                session.close();
            }
        }
    }

    private void await(Check check, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (check.satisfied()) {
                return;
            }
            Thread.sleep(100);
        }
        assertTrue(check.satisfied(), "Timed out waiting for dead-letter detection");
    }

    @FunctionalInterface
    private interface Check {
        boolean satisfied() throws Exception;
    }
}
