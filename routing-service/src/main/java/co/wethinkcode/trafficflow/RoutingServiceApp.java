
package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.client.IntersectionClient;
import co.wethinkcode.trafficflow.mq.CongestionSubscriber;
import io.javalin.Javalin;

import java.util.Map;

public class RoutingServiceApp {

    public static void main(String[] args) {
        CongestionSubscriber congestionSubscriber =
                new CongestionSubscriber();

        try {
            congestionSubscriber.start();
        } catch (Exception e) {
            System.err.println(
                    "Could not start ActiveMQ subscriber: "
                            + e.getMessage());
            System.exit(1);
        }

        IntersectionClient intersectionClient = new IntersectionClient();

        Javalin app = Javalin.create().start(7023);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/route", ctx -> {
            String fromId = ctx.queryParam("from");
            String toId = ctx.queryParam("to");

            if (fromId == null || fromId.isBlank()
                    || toId == null || toId.isBlank()) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error",
                        "Both from and to intersection IDs are required"
                ));
                return;
            }

            try {
                Intersection from =
                        intersectionClient.getIntersection(fromId);
                Intersection to =
                        intersectionClient.getIntersection(toId);

                if (from == null || to == null) {
                    ctx.status(404);
                    ctx.json(Map.of("error", "Intersection not found"));
                    return;
                }

                if (Boolean.FALSE.equals(from.active())
                        || Boolean.FALSE.equals(to.active())) {
                    ctx.status(400);
                    ctx.json(Map.of(
                            "error", "Both intersections must be active"));
                    return;
                }

                int congestionLevel =
                        congestionSubscriber.getCurrentLevel();

                int estimatedMinutes = 10 + (congestionLevel * 5);

                ctx.json(Map.of(
                        "from", from.id(),
                        "to", to.id(),
                        "congestionLevel", congestionLevel,
                        "estimatedTravelTimeMinutes", estimatedMinutes
                ));

            } catch (Exception e) {
                e.printStackTrace();

                ctx.status(502);
                ctx.json(Map.of(
                        "error", "A dependent service could not be reached"
                ));
            }
        });
    }
}

