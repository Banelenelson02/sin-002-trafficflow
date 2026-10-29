
package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class IntersectionClient {

    private static final String DEFAULT_INGESTION_URL =
            "http://localhost:7020/intersections";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String ingestionUrl;

    public IntersectionClient() {
        this(DEFAULT_INGESTION_URL);
    }

    public IntersectionClient(String ingestionUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.ingestionUrl = ingestionUrl;
    }

    public List<Intersection> fetchIntersections()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ingestionUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Ingestion Service returned HTTP "
                            + response.statusCode()
            );
        }

        return objectMapper.readValue(
                response.body(),
                new TypeReference<List<Intersection>>() {}
        );
    }
}

