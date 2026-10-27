package co.wethinkcode.trafficflow;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HeartbeatListenerTest {

    @Test
    void reportsMissingHeartbeatBeforeAnyMessageIsReceived() {
        assertTrue(new HeartbeatListener().isHeartbeatMissing());
    }

    @Test
    void reportsRecentHeartbeatAsPresent() {
        HeartbeatListener listener = new HeartbeatListener();

        listener.recordHeartbeat();

        assertFalse(listener.isHeartbeatMissing());
        assertFalse(listener.hasDeadLetteredHeartbeat());
    }

    @Test
    void recordsDeadLetteredHeartbeatAsAnUnhealthyCondition() {
        HeartbeatListener listener = new HeartbeatListener();

        listener.recordDeadLetter();

        assertTrue(listener.hasDeadLetteredHeartbeat());
    }

    @Test
    void acceptsOnlyTheExpectedTextHeartbeat() throws Exception {
        HeartbeatListener listener = new HeartbeatListener();
        ActiveMQTextMessage valid = new ActiveMQTextMessage();
        valid.setText("Intersection service is alive");

        ActiveMQTextMessage invalidBody = new ActiveMQTextMessage();
        invalidBody.setText("not alive");

        assertTrue(listener.isValidHeartbeat(valid));
        assertFalse(listener.isValidHeartbeat(invalidBody));
        assertFalse(listener.isValidHeartbeat(new ActiveMQMessage()));
    }

    @Test
    void clearsDeadLetterAlarmAfterAValidHeartbeatRecovers() {
        HeartbeatListener listener = new HeartbeatListener();
        listener.recordDeadLetter();

        listener.recordHeartbeat();

        assertFalse(listener.hasDeadLetteredHeartbeat());
        assertFalse(listener.isHeartbeatMissing());
    }
}
