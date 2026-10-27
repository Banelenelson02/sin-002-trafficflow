
package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CongestionServiceApp {

    private static final int PORT = 7022;
    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 8;

    public static void main(String[] args) {
        AtomicInteger currentLevel = new AtomicInteger(0);
        CongestionPublisher publisher = new CongestionPublisher();

        Javalin app = Javalin.create().start(PORT);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/congestion", ctx -> {
            Congestion congestion = new Congestion(currentLevel.get());
            ctx.json(congestion);
        });

        app.put("/congestion", ctx -> {
            Map<String, Integer> requestBody =
                    ctx.bodyAsClass(Map.class);

            Integer level = requestBody.get("level");

            if (level == null || level < MIN_LEVEL || level > MAX_LEVEL) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error",
                        "Congestion level must be between 0 and 8"
                ));
                return;
            }

            int previousLevel = currentLevel.getAndSet(level);

            if (previousLevel != level) {
                try {
                    publisher.publish(level);
                } catch (Exception e) {
                    System.err.println(
                            "Failed to publish congestion update: "
                                    + e.getMessage()
                    );
                    ctx.status(502);
                    ctx.json(Map.of(
                            "error",
                            "Could not publish congestion update"
                    ));
                    return;
                }
            }

            ctx.json(new Congestion(currentLevel.get()));
        });
    }
}
