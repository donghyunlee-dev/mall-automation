package com.sfood.automation.goods.infrastructure.external;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class SoldoutStatusApiCaller {

    private static final Logger log = LoggerFactory.getLogger(SoldoutStatusApiCaller.class);
    private final GodoTransport godoTransport;
    private final XmlMapper xmlMapper;
    private final ApiTransactionLogRepository apiTransactionLogRepository;
    private final Sleeper sleeper;

    public SoldoutStatusApiCaller(
        GodoTransport godoTransport,
        XmlMapper xmlMapper,
        ApiTransactionLogRepository apiTransactionLogRepository,
        Sleeper sleeper
    ) {
        this.godoTransport = godoTransport;
        this.xmlMapper = xmlMapper;
        this.apiTransactionLogRepository = apiTransactionLogRepository;
        this.sleeper = sleeper;
    }

    public GodoApiResult call(String url, String requestBody, String godoGoodsCode) {
        log.info("Godo sold-out API call started: godoGoodsCode={} url={}", godoGoodsCode, url);
        ApiTransactionLog saved = apiTransactionLogRepository.save(
            ApiTransactionLog.pending("SOLDOUT_STATUS", godoGoodsCode, url, requestBody, Instant.now())
        );

        GodoTransportResponse response = executeWithRetry(url, requestBody);
        if ("EXHAUSTED".equalsIgnoreCase(response.headers().getOrDefault("ratelimit-available-level", ""))) {
            apiTransactionLogRepository.updateResponse(saved.id(), response.body(), null, "EXHAUSTED", false, Instant.now());
            log.warn("Godo sold-out API rate limit exhausted: godoGoodsCode={} url={}", godoGoodsCode, url);
            throw new GodoApiException();
        }

        GodoParsedResponse parsed = parse(response.body());
        boolean isSuccess = "000".equals(parsed.code());
        apiTransactionLogRepository.updateResponse(
            saved.id(),
            response.body(),
            parsed.code(),
            parsed.message(),
            isSuccess,
            Instant.now()
        );
        if (!isSuccess) {
            log.warn(
                "Godo sold-out API returned non-success: godoGoodsCode={} code={} message={}",
                godoGoodsCode,
                parsed.code(),
                parsed.message()
            );
            throw new GodoApiException();
        }
        log.info("Godo sold-out API call succeeded: godoGoodsCode={} code={}", godoGoodsCode, parsed.code());
        return new GodoApiResult(true, parsed.code(), parsed.message());
    }

    private GodoTransportResponse executeWithRetry(String url, String requestBody) {
        try {
            return godoTransport.postForm(url, requestBody);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() != HttpStatus.TOO_MANY_REQUESTS) {
                log.error("Godo sold-out API transport failed: url={} status={}", url, exception.getStatusCode(), exception);
                throw new GodoApiException();
            }
            log.warn("Godo sold-out API rate limited: url={} status={}", url, exception.getStatusCode());
        } catch (RestClientException exception) {
            log.error("Godo sold-out API transport exception: url={}", url, exception);
            throw new GodoApiException();
        }

        sleeper.sleep(Duration.ofSeconds(1));
        try {
            return godoTransport.postForm(url, requestBody);
        } catch (RestClientResponseException exception) {
            log.error("Godo sold-out API retry failed: url={} status={}", url, exception.getStatusCode(), exception);
            throw new GodoApiException();
        } catch (RestClientException exception) {
            log.error("Godo sold-out API retry transport exception: url={}", url, exception);
            throw new GodoApiException();
        }
    }

    private GodoParsedResponse parse(String responseBody) {
        try {
            var root = xmlMapper.readTree(responseBody);
            return new GodoParsedResponse(
                firstText(root, "code"),
                firstNonEmpty(firstText(root, "message"), firstText(root, "msg")),
                "",
                "",
                ""
            );
        } catch (Exception exception) {
            log.error("Failed to parse Godo sold-out API response", exception);
            throw new GodoApiException();
        }
    }

    private String firstText(com.fasterxml.jackson.databind.JsonNode root, String fieldName) {
        return root.findPath(fieldName).asText("");
    }

    private String firstNonEmpty(String primary, String fallback) {
        return primary.isEmpty() ? fallback : primary;
    }
}
