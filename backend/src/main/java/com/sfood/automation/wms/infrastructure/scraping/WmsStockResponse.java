package com.sfood.automation.wms.infrastructure.scraping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WmsStockResponse(
    @JsonProperty("DS_TOTALCOUNT") int totalCount,
    @JsonProperty("ErrorMsg") String errorMessage,
    @JsonProperty("ErrorCode") String errorCode,
    @JsonProperty("DS_OUT") List<WmsStockItem> items
) {
}
