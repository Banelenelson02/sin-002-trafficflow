
package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.client.CongestionClient;
import co.wethinkcode.trafficflow.client.IntersectionClient;
import io.javalin.Javalin;

import java.util.Map;

public class RoutingServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7023);

        IntersectionClient intersectionClient = new IntersectionClient();
        CongestionClient congestionClient = new CongestionClient();

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/route", ctx -> {
            String fromId = ctx.queryParam("from");
            String toId = ctx.queryParam("to");

            if (fromId == null || fromId.isBlank()
                    || toId == null || toId.isBlank()) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error", "Both from and to intersection IDs are required"
                ));
                return;
            }

            try {
                Intersection from = intersectionClient.getIntersection(fromId);
                Intersection to = intersectionClient.getIntersection(toId);

                if (from == null || to == null) {
                    ctx.status(404);
                    ctx.json(Map.of("error", "Intersection not found"));
                    return;
                }

                if (Boolean.FALSE.equals(from.active())
                        || Boolean.FALSE.equals(to.active())) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "Both intersections must be active"));
                    return;
                }

                Congestion congestion = congestionClient.getCongestion();

                int estimatedMinutes = 10 + (congestion.level() * 5);

                ctx.json(Map.of(
                        "from", from.id(),
                        "to", to.id(),
                        "congestionLevel", congestion.level(),
                        "estimatedTravelTimeMinutes", estimatedMinutes
                ));

            } catch (Exception e) {
                ctx.status(502);
                ctx.json(Map.of(
                        "error", "A dependent service could not be reached"
                ));
            }
        });
    }
}
