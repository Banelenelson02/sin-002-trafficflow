
package co.wethinkcode.trafficflow;

import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class IntersectionCsvReader {

    private static final Set<String> MISSING_VALUES = Set.of(
            "", "n/a", "tbd", "unknown", "-", "nan"
    );

    public List<Intersection> readIntersections() {
        Map<String, Intersection> intersections = new LinkedHashMap<>();

        try (
                InputStream inputStream = Objects.requireNonNull(
                        getClass().getClassLoader()
                                .getResourceAsStream("intersections-legacy.csv"),
                        "intersections-legacy.csv was not found"
                );
                CSVReader reader = new CSVReader(
                        new InputStreamReader(
                                inputStream,
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            reader.readNext(); // Skip the CSV header.

            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row.length < 4) {
                    continue;
                }

                String id = normalizeId(row[0]);

                if (id == null) {
                    continue;
                }

                String district = normalizeDistrict(row[1]);
                String signalType = normalizeSignalType(row[2]);
                Boolean active = normalizeBoolean(row[3]);

                Intersection cleaned = new Intersection(
                        id,
                        district,
                        signalType,
                        active
                );

                // Keep one record per normalized ID.
                // If a duplicate has missing fields, fill them
                // from the other version of that record.
                intersections.merge(id, cleaned, (existing, duplicate) ->
                        new Intersection(
                                id,
                                existing.district() != null
                                        ? existing.district()
                                        : duplicate.district(),
                                existing.signalType() != null
                                        ? existing.signalType()
                                        : duplicate.signalType(),
                                existing.active() != null
                                        ? existing.active()
                                        : duplicate.active()
                        )
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read intersections CSV", e
            );
        }

        return new ArrayList<>(intersections.values());
    }

    private String normalizeId(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned.toUpperCase(Locale.ROOT);
    }

    private String normalizeDistrict(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return null;
        }

        String lowerCase = cleaned.toLowerCase(Locale.ROOT);

        return Character.toUpperCase(lowerCase.charAt(0))
                + lowerCase.substring(1);
    }

    private String normalizeSignalType(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned.toLowerCase(Locale.ROOT);
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim().replaceAll("\\s+", " ");

        if (MISSING_VALUES.contains(cleaned.toLowerCase(Locale.ROOT))) {
            return null;
        }

        return cleaned;
    }

    private Boolean normalizeBoolean(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return null;
        }

        return switch (cleaned.toLowerCase(Locale.ROOT)) {
            case "y", "yes", "1", "true" -> true;
            case "n", "no", "0", "false" -> false;
            default -> null;
        };
    }
}

