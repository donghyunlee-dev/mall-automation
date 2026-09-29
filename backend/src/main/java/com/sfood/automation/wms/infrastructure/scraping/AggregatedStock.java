package com.sfood.automation.wms.infrastructure.scraping;

public record AggregatedStock(
    String itemCd,
    int totalInvnQty,
    Integer validPercent,
    Integer remainValidDays,
    boolean lotExists,
    boolean allowedZone,
    boolean oa110Lot
) {
    public AggregatedStock(
        String itemCd,
        int totalInvnQty,
        Integer validPercent,
        boolean lotExists,
        boolean allowedZone,
        boolean oa110Lot
    ) {
        this(itemCd, totalInvnQty, validPercent, null, lotExists, allowedZone, oa110Lot);
    }

    public AggregatedStock(String itemCd, int totalInvnQty, Integer validPercent, Integer remainValidDays, boolean lotExists) {
        this(itemCd, totalInvnQty, validPercent, remainValidDays, lotExists, true, true);
    }

    public AggregatedStock(String itemCd, int totalInvnQty, Integer validPercent, boolean lotExists) {
        this(itemCd, totalInvnQty, validPercent, null, lotExists, true, true);
    }

    public Integer minValidPercent() {
        return validPercent;
    }
}
