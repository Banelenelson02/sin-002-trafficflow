package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.util.Map;

public class IntersectionWatchdogApp {

    public static void main(String[] args) {
        HeartbeatListener heartbeatListener = new HeartbeatListener();

        try {
            heartbeatListener.start();
        } catch (Exception e) {
            System.err.println(
                    "Could not start heartbeat listener: " + e.getMessage()
            );
            return;
        }

        Javalin app = Javalin.create().start(7024);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/status", ctx -> {
            boolean missing = heartbeatListener.isHeartbeatMissing();

            if (missing) {
                ctx.status(503);
                ctx.json(Map.of(
                        "status", "DOWN",
                        "message", "No recent heartbeat from Intersection Service"
                ));
            } else {
                ctx.json(Map.of(
                        "status", "UP",
                        "message", "Intersection Service heartbeat is being received"
                ));
            }
        });
    }
}