
package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IntersectionServiceApp {

    private static final int PORT = 7021;

    public static void main(String[] args) {
        IntersectionClient client = new IntersectionClient();
        Map<String, Intersection> intersections = new HashMap<>();

        try {
            List<Intersection> loaded = client.fetchIntersections();

            for (Intersection intersection : loaded) {
                intersections.put(
                        intersection.id().toUpperCase(Locale.ROOT),
                        intersection
                );
            }

            System.out.println(
                    "Loaded " + intersections.size() + " intersections."
            );

        } catch (IOException e) {
            System.err.println(
                    "Could not load intersections from Ingestion Service: "
                            + e.getMessage()
            );
            return;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Loading intersections was interrupted.");
            return;
        }

        Javalin app = Javalin.create().start(PORT);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/intersections/{id}", ctx -> {
            String id = ctx.pathParam("id").toUpperCase(Locale.ROOT);
            Intersection intersection = intersections.get(id);

            if (intersection == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "Intersection not found"));
                return;
            }

            ctx.json(intersection);
        });
    }
}

