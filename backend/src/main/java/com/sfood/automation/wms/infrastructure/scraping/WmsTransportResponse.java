package com.sfood.automation.wms.infrastructure.scraping;

import java.util.List;
import java.util.Map;

public record WmsTransportResponse(
    String body,
    Map<String, String> headers,
    Map<String, List<String>> headerValues
) {
    public WmsTransportResponse(String body, Map<String, String> headers) {
        this(body, headers, Map.of());
    }

    public String header(String name) {
        String exact = headers.get(name);
        if (exact != null) {
            return exact;
        }
        return headers.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    public List<String> headers(String name) {
        List<String> exact = headerValues.get(name);
        if (exact != null) {
            return exact;
        }
        return headerValues.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseGet(List::of);
    }
}
