package com.sfood.automation.wms.infrastructure.scraping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WmsStockItem(
    @JsonProperty("ITEM_CD") String itemCd,
    @JsonProperty("ITEM_NM") String itemName,
    @JsonProperty("LALOC_QTY") int lalocQty,
    @JsonProperty("VALID_PERCENT") Integer validPercent,
    @JsonProperty("REMAIN_VALID_DAYS") Integer remainValidDays,
    @JsonProperty("LOT_NO") String lotNo,
    @JsonProperty("LOT_ATTR_1") String lotAttr1,
    @JsonProperty("ZONE_CD") String zoneCd,
    @JsonProperty("TEMP_TCD") String tempTcd,
    @JsonProperty("LOT_HLD_SCD") String lotHoldScd,
    @JsonProperty("WH_CD") String warehouseCode,
    @JsonProperty("VALID_DATETIME") String validDatetime
) {
    public WmsStockItem(
        String itemCd,
        String itemName,
        int lalocQty,
        Integer validPercent,
        String lotNo,
        String lotAttr1,
        String zoneCd,
        String lotHoldScd,
        String warehouseCode,
        String validDatetime
    ) {
        this(itemCd, itemName, lalocQty, validPercent, null, lotNo, lotAttr1, zoneCd, null, lotHoldScd, warehouseCode, validDatetime);
    }
}
