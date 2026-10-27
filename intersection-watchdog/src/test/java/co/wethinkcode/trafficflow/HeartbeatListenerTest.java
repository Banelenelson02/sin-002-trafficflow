package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbeatListenerTest {

    @Test
    void reportsMissingHeartbeatBeforeAnyMessageIsReceived() {
        assertTrue(new HeartbeatListener().isHeartbeatMissing());
    }
}
