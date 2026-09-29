package com.sfood.automation.goods.application.dto;

public record GoodsStatusLogResponse(
    Integer id,
    String godoGoodsCode,
    String erpGoodsCode,
    int wmsAvailableQty,
    int criteriaQty,
    int safetyRate,
    Integer validPercent,
    Integer remainValidDays,
    String beforeStatus,
    String afterStatus,
    String changedAt,
    String createdAt
) {
    public GoodsStatusLogResponse(
        Integer id,
        String godoGoodsCode,
        String erpGoodsCode,
        int wmsAvailableQty,
        int criteriaQty,
        int safetyRate,
        Integer validPercent,
        String beforeStatus,
        String afterStatus,
        String changedAt,
        String createdAt
    ) {
        this(id, godoGoodsCode, erpGoodsCode, wmsAvailableQty, criteriaQty, safetyRate, validPercent, null, beforeStatus, afterStatus, changedAt, createdAt);
    }
}
