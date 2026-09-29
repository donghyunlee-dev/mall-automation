package com.sfood.automation.dashboard.infrastructure.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchQuery;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchResult;
import com.sfood.automation.dashboard.domain.port.GodoOrderSearchPort;
import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import com.sfood.automation.goods.infrastructure.external.GodoApiProperties;
import com.sfood.automation.goods.infrastructure.external.GodoTransport;
import com.sfood.automation.goods.infrastructure.external.GodoTransportResponse;
import java.time.Instant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(GodoApiProperties.class)
public class GodoOrderApiClient implements GodoOrderSearchPort {

    private final GodoTransport godoTransport;
    private final ApiTransactionLogRepository apiTransactionLogRepository;
    private final GodoApiProperties properties;
    private final GodoOrderResponseParser parser;
    private final XmlMapper xmlMapper = new XmlMapper();

    public GodoOrderApiClient(
        GodoTransport godoTransport,
        ApiTransactionLogRepository apiTransactionLogRepository,
        GodoApiProperties properties
    ) {
        this.godoTransport = godoTransport;
        this.apiTransactionLogRepository = apiTransactionLogRepository;
        this.properties = properties;
        this.parser = new GodoOrderResponseParser(xmlMapper);
    }

    @Override
    public GodoOrderSearchResult search(GodoOrderSearchQuery query) {
        String url = orderBaseUrl() + "/Order_Search.php";
        String requestBody = buildRequestBody(query);

        ApiTransactionLog saved = apiTransactionLogRepository.save(
            ApiTransactionLog.pending("ORDER_SEARCH", query.startDate() + "~" + query.endDate(), url, requestBody, Instant.now())
        );
        GodoTransportResponse response = godoTransport.postForm(url, requestBody);

        String code = readHeaderField(response.body(), "code");
        String message = readHeaderField(response.body(), "msg");
        boolean isSuccess = "000".equals(code);
        apiTransactionLogRepository.updateResponse(saved.id(), response.body(), code, message, isSuccess, Instant.now());

        if (!isSuccess) {
            throw new GodoApiException();
        }
        return parser.parse(response.body());
    }

    private String orderBaseUrl() {
        String configured = properties.getBaseUrl();
        if (configured.endsWith("/godomall5/order")) {
            return configured;
        }
        if (configured.endsWith("/")) {
            return configured + "godomall5/order";
        }
        return configured + "/godomall5/order";
    }

    private String buildRequestBody(GodoOrderSearchQuery query) {
        StringBuilder body = new StringBuilder()
            .append("partner_key=").append(properties.getPartnerKey())
            .append("&key=").append(properties.getKey())
            .append("&dateType=").append(query.dateType())
            .append("&startDate=").append(query.startDate())
            .append("&endDate=").append(query.endDate());
        if (query.size() != null) {
            body.append("&size=").append(query.size());
        }
        if (query.lastOrder() != null) {
            body.append("&lastOrder=").append(query.lastOrder());
        }
        return body.toString();
    }

    private String readHeaderField(String responseBody, String fieldName) {
        try {
            JsonNode root = xmlMapper.readTree(responseBody);
            return root.path("header").path(fieldName).asText("");
        } catch (Exception exception) {
            throw new GodoOrderResponseParseException(exception);
        }
    }
}
