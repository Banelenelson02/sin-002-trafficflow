
package co.wethinkcode.trafficflow.client;

import co.wethinkcode.trafficflow.Intersection;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class IntersectionClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:7021";

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntersectionClient() {
        this(DEFAULT_BASE_URL);
    }

    public IntersectionClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Intersection getIntersection(String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/intersections/" + id))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 404) {
            return null;
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Intersection Service returned HTTP "
                            + response.statusCode()
            );
        }

        return objectMapper.readValue(response.body(), Intersection.class);
    }
}
