package co.wethinkcode.trafficflow.mq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CongestionSubscriberTest {

    @Test
    void initializesCurrentLevelFromSnapshot() {
        CongestionSubscriber subscriber = new CongestionSubscriber();

        subscriber.initializeCurrentLevel(6);

        assertEquals(6, subscriber.getCurrentLevel());
    }

    @Test
    void liveUpdateIsNotOverwrittenByStartupSnapshot() {
        CongestionSubscriber subscriber = new CongestionSubscriber();

        subscriber.recordUpdate(4);
        subscriber.initializeCurrentLevel(2);

        assertEquals(4, subscriber.getCurrentLevel());
    }

    @Test
    void rejectsSnapshotOutsideValidRange() {
        CongestionSubscriber subscriber = new CongestionSubscriber();

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriber.initializeCurrentLevel(9)
        );
    }
}
