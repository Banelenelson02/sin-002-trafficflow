
package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

public class IngestionServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7020);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/intersections", ctx ->
                ctx.json(new IntersectionCsvReader().readIntersections())
        );
    }
}

