package co.wethinkcode.trafficflow;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntersectionClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesAndDeserializesIntersections() throws Exception {
        startServer(200, "[{\"id\":\"INT-1001\",\"district\":\"Downtown\","
                + "\"signalType\":\"4-way\",\"active\":true}]");

        assertEquals(
                new Intersection("INT-1001", "Downtown", "4-way", true),
                new IntersectionClient().fetchIntersections().get(0)
        );
    }

    @Test
    void reportsNonSuccessfulIngestionResponses() throws Exception {
        startServer(503, "unavailable");

        IOException exception = assertThrows(
                IOException.class,
                () -> new IntersectionClient().fetchIntersections()
        );

        assertEquals("Ingestion Service returned HTTP 503", exception.getMessage());
    }

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 7020), 0);
        server.createContext("/intersections", exchange -> respond(exchange, status, body));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
