package com.sfood.automation.goods.domain.port;

import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import java.time.Instant;
import java.util.Optional;

public interface ApiTransactionLogRepository {
    ApiTransactionLog save(ApiTransactionLog log);
    ApiTransactionLog updateResponse(
        Integer id,
        String responseBody,
        String resultCode,
        String resultMessage,
        boolean isSuccess,
        Instant respondedAt
    );
    Optional<ApiTransactionLog> findById(Integer id);
}
