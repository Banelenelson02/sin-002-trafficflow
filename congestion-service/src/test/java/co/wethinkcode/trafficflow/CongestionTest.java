package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CongestionTest {

    @Test
    void acceptsMinimumCongestionLevel() {
        Congestion congestion = new Congestion(0);

        assertEquals(0, congestion.level());
    }

    @Test
    void acceptsMaximumCongestionLevel() {
        Congestion congestion = new Congestion(8);

        assertEquals(8, congestion.level());
    }

    @Test
    void acceptsLevelWithinRange() {
        Congestion congestion = new Congestion(5);

        assertEquals(5, congestion.level());
    }

    @Test
    void rejectsNegativeCongestionLevel() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Congestion(-1)
        );
    }

    @Test
    void rejectsCongestionLevelAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Congestion(9)
        );
    }
}
