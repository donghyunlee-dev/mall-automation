package com.sfood.automation.goods.domain.model;

public record GodoApiResult(
    boolean success,
    String resultCode,
    String resultMessage
) {
}
