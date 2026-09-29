package com.sfood.automation.wms.infrastructure.scraping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.exception.WmsScrapingException;
import java.net.HttpCookie;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(WmsProperties.class)
public class WmsStockClient {

    private static final Logger log = LoggerFactory.getLogger(WmsStockClient.class);

    private final WmsProperties properties;
    private final WmsTransport transport;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WmsStockClient(WmsProperties properties, WmsTransport transport) {
        this.properties = properties;
        this.transport = transport;
    }

    public List<WmsStockItem> fetchAll(HttpCookie cookie) {
        WmsTransportResponse response = transport.post(properties.getStockUrl() + "?" + requestQuery(), requestBody(), cookie);
        try {
            WmsStockResponse parsed = objectMapper.readValue(response.body(), WmsStockResponse.class);
            if (hasError(parsed.errorCode())) {
                throw new WmsScrapingException("WMS stock response errorCode=%s, message=%s"
                    .formatted(parsed.errorCode(), parsed.errorMessage()));
            }
            return parsed.items() == null ? List.of() : parsed.items();
        } catch (WmsScrapingException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to parse WMS stock response", exception);
            throw new WmsScrapingException("Failed to parse WMS stock response", exception);
        }
    }

    private String requestQuery() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("CURRENT_MENUCODE", "WMS141123");
        query.put("PAGE_SIZE", "10000");
        query.put("CURRENT_PAGE", "1");
        query.put("SES_LANG", "KO");
        query.put("SES_USERGROUP", "Observer");
        query.put("SES_WHSE", properties.getWarehouseCode());
        query.put("SES_MULTI_LANG_YN", "N");
        return formEncodedQuery(query);
    }

    private String requestBody() {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("DS_SEARCHCONDITION", List.of(
                Map.of(
                    "ROW_STATUS", "normal",
                    "val0", "WH_CD",
                    "val1", "WH_CD",
                    "val2", "=",
                    "val3", properties.getWarehouseCode(),
                    "val4", "COMBO"
                ),
                Map.of(
                    "ROW_STATUS", "normal",
                    "val0", "LOT_ATTR_1",
                    "val1", "WAREHOUSE_LABEL",
                    "val2", "IN",
                    "val3", properties.getCenterCode(),
                    "val4", "POP"
                )
            ));
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new WmsScrapingException("Failed to serialize WMS search payload", exception);
        }
    }

    private String formEncodedQuery(Map<String, String> values) {
        return values.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));
    }

    private boolean hasError(String errorCode) {
        return errorCode != null && !errorCode.isBlank() && !"0".equals(errorCode);
    }
}
