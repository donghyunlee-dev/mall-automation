package com.sfood.automation.goods.domain.model;

import java.time.Instant;

public record ApiTransactionLog(
    Integer id,
    String apiType,
    String godoGoodsCode,
    String requestUrl,
    String requestBody,
    String responseBody,
    String resultCode,
    String resultMessage,
    boolean isSuccess,
    Instant requestedAt,
    Instant respondedAt,
    Instant createdAt
) {

    public static ApiTransactionLog pending(
        String apiType,
        String godoGoodsCode,
        String requestUrl,
        String requestBody,
        Instant requestedAt
    ) {
        return new ApiTransactionLog(
            null,
            apiType,
            godoGoodsCode,
            requestUrl,
            requestBody,
            null,
            null,
            null,
            false,
            requestedAt,
            null,
            requestedAt
        );
    }

    public ApiTransactionLog withId(Integer id) {
        return new ApiTransactionLog(
            id,
            apiType,
            godoGoodsCode,
            requestUrl,
            requestBody,
            responseBody,
            resultCode,
            resultMessage,
            isSuccess,
            requestedAt,
            respondedAt,
            createdAt
        );
    }

    public ApiTransactionLog complete(
        String responseBody,
        String resultCode,
        String resultMessage,
        boolean isSuccess,
        Instant respondedAt
    ) {
        return new ApiTransactionLog(
            id,
            apiType,
            godoGoodsCode,
            requestUrl,
            requestBody,
            responseBody,
            resultCode,
            resultMessage,
            isSuccess,
            requestedAt,
            respondedAt,
            createdAt
        );
    }
}
