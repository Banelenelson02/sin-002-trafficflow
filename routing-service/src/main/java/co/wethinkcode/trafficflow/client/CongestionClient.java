
package co.wethinkcode.trafficflow.client;

import co.wethinkcode.trafficflow.Congestion;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CongestionClient {

    private static final String DEFAULT_URL =
            "http://localhost:7022/congestion";

    private final String url;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CongestionClient() {
        this(DEFAULT_URL);
    }

    public CongestionClient(String url) {
        this.url = url;
    }

    public Congestion getCongestion() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Congestion Service returned HTTP "
                            + response.statusCode()
            );
        }

        return objectMapper.readValue(response.body(), Congestion.class);
    }
}
