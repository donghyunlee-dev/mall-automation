package com.sfood.automation.wms.infrastructure.scraping;

public record ErpVerifyResult(
    Integer goodsId,
    String erpSyncYn,
    String status,
    String message
) {
}
