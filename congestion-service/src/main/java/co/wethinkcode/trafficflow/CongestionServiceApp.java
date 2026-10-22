
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

            currentLevel.set(level);

            ctx.json(new Congestion(currentLevel.get()));
        });
    }
}

