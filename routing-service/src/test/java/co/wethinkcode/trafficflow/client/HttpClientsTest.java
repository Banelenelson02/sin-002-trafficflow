package co.wethinkcode.trafficflow.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpClientsTest {

    private HttpServer intersectionServer;
    private HttpServer congestionServer;

    @AfterEach
    void stopServers() {
        if (intersectionServer != null) {
            intersectionServer.stop(0);
        }
        if (congestionServer != null) {
            congestionServer.stop(0);
        }
    }

    @Test
    void returnsDeserializedIntersectionForSuccessfulResponse() throws Exception {
        startIntersectionServer(200, "{\"id\":\"INT-1001\",\"district\":\"Downtown\","
                + "\"signalType\":\"4-way\",\"active\":true}");

        var result = new IntersectionClient().getIntersection("INT-1001");

        assertEquals("INT-1001", result.id());
        assertEquals(true, result.active());
    }

    @Test
    void returnsNullWhenIntersectionDoesNotExist() throws Exception {
        startIntersectionServer(404, "{\"error\":\"Intersection not found\"}");

        assertNull(new IntersectionClient().getIntersection("UNKNOWN"));
    }

    @Test
    void rejectsUnexpectedIntersectionServiceResponses() throws Exception {
        startIntersectionServer(500, "error");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> new IntersectionClient().getIntersection("INT-1001")
        );

        assertEquals("Intersection Service returned HTTP 500", exception.getMessage());
    }

    @Test
    void returnsDeserializedCongestionForSuccessfulResponse() throws Exception {
        startCongestionServer(200, "{\"level\":6}");

        assertEquals(6, new CongestionClient().getCongestion().level());
    }

    @Test
    void rejectsUnexpectedCongestionServiceResponses() throws Exception {
        startCongestionServer(503, "unavailable");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> new CongestionClient().getCongestion()
        );

        assertEquals("Congestion Service returned HTTP 503", exception.getMessage());
    }

    private void startIntersectionServer(int status, String body) throws IOException {
        intersectionServer = startServer(7021, status, body);
    }

    private void startCongestionServer(int status, String body) throws IOException {
        congestionServer = startServer(7022, status, body);
    }

    private HttpServer startServer(int port, int status, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/", exchange -> respond(exchange, status, body));
        server.start();
        return server;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
