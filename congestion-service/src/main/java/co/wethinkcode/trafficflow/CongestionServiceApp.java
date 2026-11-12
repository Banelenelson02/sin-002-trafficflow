package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import java.util.Map;

public class CongestionServiceApp {
    private static final int PORT = 7022;

    public static void main(String[] args) {
        CongestionController controller =
                new CongestionController(new CongestionPublisher());

        Javalin app = Javalin.create().start(PORT);
        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/congestion", ctx -> ctx.json(new Congestion(controller.getLevel())));

        app.put("/congestion", ctx -> {
            JsonNode requestBody;
            try {
                requestBody = ctx.bodyAsClass(JsonNode.class);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Request body must be valid JSON"));
                return;
            }

            JsonNode levelNode = requestBody == null ? null : requestBody.get("level");
            if (levelNode == null || !levelNode.isIntegralNumber()
                    || !levelNode.canConvertToInt()
                    || levelNode.intValue() < 0 || levelNode.intValue() > 8) {
                ctx.status(400).json(Map.of(
                        "error", "Congestion level must be an integer between 0 and 8"));
                return;
            }

            try {
                int updatedLevel = controller.updateLevel(levelNode.intValue());
                ctx.json(new Congestion(updatedLevel));
            } catch (Exception e) {
                System.err.println("Failed to publish congestion update: " + e.getMessage());
                ctx.status(502).json(Map.of(
                        "error", "Could not publish congestion update"));
            }
        });
    }
}
