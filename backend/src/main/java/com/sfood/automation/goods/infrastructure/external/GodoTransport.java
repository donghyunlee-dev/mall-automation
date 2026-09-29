package com.sfood.automation.goods.infrastructure.external;

public interface GodoTransport {
    GodoTransportResponse postForm(String url, String requestBody);
}
