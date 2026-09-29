package com.sfood.automation.goods.infrastructure.persistence;

import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApiTransactionLogRepositoryImpl implements ApiTransactionLogRepository {

    private final ApiTransactionLogJpaRepository apiTransactionLogJpaRepository;

    public ApiTransactionLogRepositoryImpl(ApiTransactionLogJpaRepository apiTransactionLogJpaRepository) {
        this.apiTransactionLogJpaRepository = apiTransactionLogJpaRepository;
    }

    @Override
    public ApiTransactionLog save(ApiTransactionLog log) {
        return toDomain(apiTransactionLogJpaRepository.save(toEntity(log)));
    }

    @Override
    public ApiTransactionLog updateResponse(
        Integer id,
        String responseBody,
        String resultCode,
        String resultMessage,
        boolean isSuccess,
        Instant respondedAt
    ) {
        ApiTransactionLogEntity entity = apiTransactionLogJpaRepository.findById(id).orElseThrow();
        entity.setResponseBody(responseBody);
        entity.setResultCode(resultCode);
        entity.setResultMessage(resultMessage);
        entity.setIsSuccess(isSuccess ? 1 : 0);
        entity.setRespondedAt(respondedAt.toString());
        return toDomain(apiTransactionLogJpaRepository.save(entity));
    }

    @Override
    public Optional<ApiTransactionLog> findById(Integer id) {
        return apiTransactionLogJpaRepository.findById(id).map(this::toDomain);
    }

    private ApiTransactionLogEntity toEntity(ApiTransactionLog log) {
        ApiTransactionLogEntity entity = new ApiTransactionLogEntity();
        entity.setId(log.id());
        entity.setApiType(log.apiType());
        entity.setGodoGoodsCode(log.godoGoodsCode());
        entity.setRequestUrl(log.requestUrl());
        entity.setRequestBody(log.requestBody());
        entity.setResponseBody(log.responseBody());
        entity.setResultCode(log.resultCode());
        entity.setResultMessage(log.resultMessage());
        entity.setIsSuccess(log.isSuccess() ? 1 : 0);
        entity.setRequestedAt(log.requestedAt().toString());
        entity.setRespondedAt(log.respondedAt() == null ? null : log.respondedAt().toString());
        entity.setCreatedAt(log.createdAt().toString());
        return entity;
    }

    private ApiTransactionLog toDomain(ApiTransactionLogEntity entity) {
        return new ApiTransactionLog(
            entity.getId(),
            entity.getApiType(),
            entity.getGodoGoodsCode(),
            entity.getRequestUrl(),
            entity.getRequestBody(),
            entity.getResponseBody(),
            entity.getResultCode(),
            entity.getResultMessage(),
            entity.getIsSuccess() == 1,
            SqliteInstantParser.parse(entity.getRequestedAt()),
            SqliteInstantParser.parse(entity.getRespondedAt()),
            SqliteInstantParser.parse(entity.getCreatedAt())
        );
    }
}
