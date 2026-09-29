package com.sfood.automation.goods.domain.model;

import com.sfood.automation.goods.domain.enums.GoodsStatus;
import java.time.Instant;

public class Goods {

    private final Integer id;
    private final String godoGoodsCode;
    private String erpGoodsCode;
    private String goodsName;
    private String onlyMallYn;
    private String soldOutYn;
    private int availableQty;
    private int safetyRate;
    private int remainValidDays;
    private String godoSyncYn;
    private String erpSyncYn;
    private String godoSyncBlockedYn;
    private String description;
    private String message;
    private GoodsStatus status;
    private final String createdId;
    private final Instant createdAt;
    private String updatedId;
    private Instant updatedAt;

    public Goods(
        Integer id,
        String godoGoodsCode,
        String erpGoodsCode,
        String goodsName,
        String onlyMallYn,
        String soldOutYn,
        int availableQty,
        int safetyRate,
        int remainValidDays,
        String godoSyncYn,
        String erpSyncYn,
        String godoSyncBlockedYn,
        String description,
        String message,
        GoodsStatus status,
        String createdId,
        Instant createdAt,
        String updatedId,
        Instant updatedAt
    ) {
        this.id = id;
        this.godoGoodsCode = godoGoodsCode;
        this.erpGoodsCode = erpGoodsCode;
        this.goodsName = goodsName;
        this.onlyMallYn = onlyMallYn;
        this.soldOutYn = soldOutYn;
        this.availableQty = availableQty;
        this.safetyRate = safetyRate;
        this.remainValidDays = remainValidDays;
        this.godoSyncYn = godoSyncYn;
        this.erpSyncYn = erpSyncYn;
        this.godoSyncBlockedYn = godoSyncBlockedYn;
        this.description = description;
        this.message = message;
        this.status = status;
        this.createdId = createdId;
        this.createdAt = createdAt;
        this.updatedId = updatedId;
        this.updatedAt = updatedAt;
    }

    public Goods(
        Integer id,
        String godoGoodsCode,
        String erpGoodsCode,
        String goodsName,
        String onlyMallYn,
        String soldOutYn,
        int availableQty,
        int safetyRate,
        int remainValidDays,
        String godoSyncYn,
        String erpSyncYn,
        String description,
        String message,
        GoodsStatus status,
        String createdId,
        Instant createdAt,
        String updatedId,
        Instant updatedAt
    ) {
        this(
            id,
            godoGoodsCode,
            erpGoodsCode,
            goodsName,
            onlyMallYn,
            soldOutYn,
            availableQty,
            safetyRate,
            remainValidDays,
            godoSyncYn,
            erpSyncYn,
            "N",
            description,
            message,
            status,
            createdId,
            createdAt,
            updatedId,
            updatedAt
        );
    }

    public static Goods create(String godoGoodsCode, String erpGoodsCode, String goodsName, String onlyMallYn, String createdId) {
        Instant now = Instant.now();
        return new Goods(
            null, godoGoodsCode, erpGoodsCode, goodsName, onlyMallYn, "N", 0, 0, 0,
            "N", "N", "N", null, null, GoodsStatus.UNVERIFIED, createdId, now, null, null
        );
    }

    public Goods withId(Integer id) {
        return new Goods(id, godoGoodsCode, erpGoodsCode, goodsName, onlyMallYn, soldOutYn, availableQty, safetyRate, remainValidDays,
            godoSyncYn, erpSyncYn, godoSyncBlockedYn, description, message, status, createdId, createdAt, updatedId, updatedAt);
    }

    public void restore() {
        this.status = GoodsStatus.NORMAL;
    }

    public void hide() {
        this.status = GoodsStatus.HIDE;
    }

    public void applyGodoSyncResult(String soldOutFl) {
        this.godoSyncYn = "Y";
        this.soldOutYn = soldOutFl;
    }

    public void clearGodoSyncResult() {
        this.godoSyncYn = "N";
        this.soldOutYn = "N";
    }

    public void initializeCriteria(int availableQty, int safetyRate, int remainValidDays) {
        this.availableQty = availableQty;
        this.safetyRate = safetyRate;
        this.remainValidDays = remainValidDays;
    }

    public void changeScope(String onlyMallYn, int availableQty, int safetyRate, int remainValidDays) {
        this.onlyMallYn = onlyMallYn;
        initializeCriteria(availableQty, safetyRate, remainValidDays);
    }

    public void markErpError(String message) {
        this.erpSyncYn = "N";
        this.status = GoodsStatus.ERROR;
        this.message = message;
    }

    public void recoverErpSync() {
        this.erpSyncYn = "Y";
        this.status = GoodsStatus.NORMAL;
        this.message = null;
    }

    public void updateSoldOutStatus(String soldOutYn, String updatedId) {
        this.soldOutYn = soldOutYn;
        this.updatedId = updatedId;
        this.updatedAt = Instant.now();
    }

    public void enableGodoSyncBlock(String updatedId) {
        this.godoSyncBlockedYn = "Y";
        this.updatedId = updatedId;
        this.updatedAt = Instant.now();
    }

    public void disableGodoSyncBlock(String updatedId) {
        this.godoSyncBlockedYn = "N";
        this.updatedId = updatedId;
        this.updatedAt = Instant.now();
    }

    public boolean isNormalStatus() {
        return GoodsStatus.NORMAL.equals(status);
    }

    public void update(String goodsName, String erpGoodsCode, String onlyMallYn, int availableQty, int safetyRate, int remainValidDays, String updatedId) {
        this.goodsName = goodsName;
        this.erpGoodsCode = erpGoodsCode;
        this.changeScope(onlyMallYn, availableQty, safetyRate, remainValidDays);
        this.updatedId = updatedId;
        this.updatedAt = Instant.now();
    }

    public Integer getId() { return id; }
    public String getGodoGoodsCode() { return godoGoodsCode; }
    public String getErpGoodsCode() { return erpGoodsCode; }
    public String getGoodsName() { return goodsName; }
    public String getOnlyMallYn() { return onlyMallYn; }
    public String getSoldOutYn() { return soldOutYn; }
    public int getAvailableQty() { return availableQty; }
    public int getSafetyRate() { return safetyRate; }
    public int getRemainValidDays() { return remainValidDays; }
    public String getGodoSyncYn() { return godoSyncYn; }
    public String getErpSyncYn() { return erpSyncYn; }
    public String getGodoSyncBlockedYn() { return godoSyncBlockedYn; }
    public String getDescription() { return description; }
    public String getMessage() { return message; }
    public GoodsStatus getStatus() { return status; }
    public String getCreatedId() { return createdId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedId() { return updatedId; }
    public Instant getUpdatedAt() { return updatedAt; }
}
