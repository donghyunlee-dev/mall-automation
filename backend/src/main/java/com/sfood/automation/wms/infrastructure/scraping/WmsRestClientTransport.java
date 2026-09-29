package com.sfood.automation.wms.infrastructure.scraping;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WmsRestClientTransport implements WmsTransport {

    private final RestClient restClient;

    public WmsRestClientTransport(@Qualifier("wmsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public WmsTransportResponse get(String url, String query) {
        return restClient.get()
            .uri(url + "?" + query)
            .exchange((request, response) -> new WmsTransportResponse(
                new String(response.getBody().readAllBytes()),
                headers(response.getHeaders()),
                headerValues(response.getHeaders())
            ));
    }

    @Override
    public WmsTransportResponse post(String url, String body, HttpCookie cookie) {
        return restClient.post()
            .uri(url)
            .header("Content-Type", "application/json")
            .header("Cookie", cookie.getName() + "=" + cookie.getValue())
            .body(body)
            .exchange((request, response) -> new WmsTransportResponse(
                new String(response.getBody().readAllBytes()),
                headers(response.getHeaders()),
                headerValues(response.getHeaders())
            ));
    }

    private Map<String, String> headers(org.springframework.http.HttpHeaders httpHeaders) {
        Map<String, String> headers = new HashMap<>();
        httpHeaders.forEach((key, values) -> headers.put(key, firstValue(values)));
        return headers;
    }

    private String firstValue(List<String> values) {
        return values.isEmpty() ? "" : values.get(0);
    }

    private Map<String, List<String>> headerValues(org.springframework.http.HttpHeaders httpHeaders) {
        Map<String, List<String>> headers = new HashMap<>();
        httpHeaders.forEach((key, values) -> headers.put(key, List.copyOf(values)));
        return headers;
    }
}
