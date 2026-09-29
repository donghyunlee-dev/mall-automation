package com.sfood.automation.goods.infrastructure.external;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GodoRestClientTransport implements GodoTransport {

    private final RestClient restClient;

    public GodoRestClientTransport(@Qualifier("godoRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public GodoTransportResponse postForm(String url, String requestBody) {
        return restClient.post()
            .uri(url)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(requestBody)
            .exchange((request, response) -> {
                Map<String, String> headers = new HashMap<>();
                response.getHeaders().forEach((key, values) -> headers.put(key, firstValue(values)));
                return new GodoTransportResponse(new String(response.getBody().readAllBytes()), headers);
            });
    }

    private String firstValue(List<String> values) {
        return values.isEmpty() ? "" : values.get(0);
    }
}
