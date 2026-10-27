package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WatchdogStatusTest {

    @Test
    void reportsDownBeforeAnyHeartbeatIsReceived() {
        WatchdogStatus status = WatchdogStatus.from(new HeartbeatListener());

        assertEquals(503, status.httpStatus());
        assertEquals("No recent heartbeat from Intersection Service", status.message());
    }

    @Test
    void reportsDownWhenAHeartbeatWasDeadLettered() {
        HeartbeatListener listener = new HeartbeatListener();
        listener.recordHeartbeat();
        listener.recordDeadLetter();

        WatchdogStatus status = WatchdogStatus.from(listener);

        assertEquals(503, status.httpStatus());
        assertEquals("A heartbeat was dead-lettered", status.message());
    }

    @Test
    void reportsUpAfterAValidHeartbeatRecoversTheService() {
        HeartbeatListener listener = new HeartbeatListener();
        listener.recordDeadLetter();
        listener.recordHeartbeat();

        WatchdogStatus status = WatchdogStatus.from(listener);

        assertEquals(200, status.httpStatus());
        assertEquals("UP", status.status());
    }
}
