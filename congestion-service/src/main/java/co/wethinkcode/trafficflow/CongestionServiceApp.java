
package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.util.Map;

public class CongestionServiceApp {
    private static final int PORT = 7022;

    public static void main(String[] args) {
        CongestionController controller =
                new CongestionController(new CongestionPublisher());

        Javalin app = Javalin.create().start(PORT);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/congestion", ctx -> {
            Congestion congestion = new Congestion(controller.getLevel());
            ctx.json(congestion);
        });

        app.put("/congestion", ctx -> {
            Map<String, Integer> requestBody = ctx.bodyAsClass(Map.class);
            Integer level = requestBody.get("level");

            if (level == null || level < 0 || level > 8) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error", "Congestion level must be between 0 and 8"
                ));
                return;
            }

            try {
                int updatedLevel = controller.updateLevel(level);
                ctx.json(new Congestion(updatedLevel));
            } catch (Exception e) {
                System.err.println(
                        "Failed to publish congestion update: " + e.getMessage()
                );
                ctx.status(502);
                ctx.json(Map.of(
                        "error", "Could not publish congestion update"
                ));
            }
        });
    }
}

