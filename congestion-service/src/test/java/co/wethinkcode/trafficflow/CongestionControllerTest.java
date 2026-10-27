package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CongestionControllerTest {

    @Test
    void publishesWhenCongestionLevelChanges() throws Exception {
        List<Integer> publishedLevels = new ArrayList<>();
        CongestionController controller = new CongestionController(publishedLevels::add);

        assertEquals(5, controller.updateLevel(5));
        assertEquals(5, controller.getLevel());
        assertEquals(List.of(5), publishedLevels);
    }

    @Test
    void doesNotPublishWhenCongestionLevelStaysTheSame() throws Exception {
        List<Integer> publishedLevels = new ArrayList<>();
        CongestionController controller = new CongestionController(publishedLevels::add);

        controller.updateLevel(5);
        controller.updateLevel(5);

        assertEquals(List.of(5), publishedLevels);
        assertEquals(5, controller.getLevel());
    }

    @Test
    void keepsPreviousLevelWhenPublishingFails() {
        CongestionController controller = new CongestionController(level -> {
            throw new Exception("Broker unavailable");
        });

        assertThrows(Exception.class, () -> controller.updateLevel(5));

        assertEquals(0, controller.getLevel());
    }

    @Test
    void rejectsInvalidLevelWithoutPublishing() {
        List<Integer> publishedLevels = new ArrayList<>();
        CongestionController controller = new CongestionController(publishedLevels::add);

        assertThrows(IllegalArgumentException.class, () -> controller.updateLevel(9));

        assertEquals(0, controller.getLevel());
        assertTrue(publishedLevels.isEmpty());
    }
}
