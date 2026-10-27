package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class IntersectionCsvReaderTest {

    private final IntersectionCsvReader reader = new IntersectionCsvReader();

    @Test
    void cleansNormalizesAndDeduplicatesLegacyRecords() {
        List<Intersection> intersections = reader.readIntersections();
        Map<String, Intersection> byId = intersections.stream().collect(
                Collectors.toMap(Intersection::id, Function.identity())
        );

        assertEquals(17, intersections.size());
        assertEquals(17, byId.size());
        assertFalse(byId.containsKey("int-1002"));

        assertEquals(
                new Intersection("INT-1002", "Midtown", "pedestrian", true),
                byId.get("INT-1002")
        );
        assertEquals(
                new Intersection("INT-1005", "Downtown", "roundabout", true),
                byId.get("INT-1005")
        );
    }

    @Test
    void retainsRecordsWithExplicitMissingAndUnknownValues() {
        Map<String, Intersection> byId = reader.readIntersections().stream()
                .collect(Collectors.toMap(Intersection::id, Function.identity()));

        Intersection missingSignal = byId.get("INT-1007");
        assertNull(missingSignal.signalType());
        assertEquals(true, missingSignal.active());

        Intersection unknownValues = byId.get("INT-1013");
        assertNull(unknownValues.signalType());
        assertNull(unknownValues.active());

        assertNull(byId.get("INT-1015").district());
    }

    @Test
    void normalizesEverySupportedBooleanRepresentationInTheDataset() {
        Map<String, Intersection> byId = reader.readIntersections().stream()
                .collect(Collectors.toMap(Intersection::id, Function.identity()));

        assertEquals(true, byId.get("INT-1001").active());
        assertEquals(false, byId.get("INT-1003").active());
        assertEquals(false, byId.get("INT-1006").active());
        assertEquals(false, byId.get("INT-1008").active());
        assertEquals(false, byId.get("INT-1009").active());
        assertEquals(false, byId.get("INT-1012").active());
        assertEquals(false, byId.get("INT-1017").active());
    }
}
