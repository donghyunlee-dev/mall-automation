package com.sfood.automation.goods.infrastructure.external;

import java.util.Map;

public record GodoTransportResponse(
    String body,
    Map<String, String> headers
) {
}
