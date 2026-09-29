package com.sfood.automation.goods.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "goods")
public class GoodsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "godo_goods_code", nullable = false, unique = true)
    private String godoGoodsCode;

    @Column(name = "erp_goods_code", nullable = false)
    private String erpGoodsCode;

    @Column(name = "goods_name", nullable = false)
    private String goodsName;

    @Column(name = "only_mall_yn", nullable = false)
    private String onlyMallYn;

    @Column(name = "sold_out_yn", nullable = false)
    private String soldOutYn;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "safety_rate", nullable = false)
    private int safetyRate;

    @Column(name = "remain_valid_days", nullable = false)
    private int remainValidDays;

    @Column(name = "godo_sync_yn", nullable = false)
    private String godoSyncYn;

    @Column(name = "erp_sync_yn", nullable = false)
    private String erpSyncYn;

    @Column(name = "godo_sync_blocked_yn", nullable = false)
    private String godoSyncBlockedYn;

    private String description;
    private String message;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_id", nullable = false)
    private String createdId;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_id")
    private String updatedId;

    @Column(name = "updated_at")
    private String updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getGodoGoodsCode() { return godoGoodsCode; }
    public void setGodoGoodsCode(String godoGoodsCode) { this.godoGoodsCode = godoGoodsCode; }
    public String getErpGoodsCode() { return erpGoodsCode; }
    public void setErpGoodsCode(String erpGoodsCode) { this.erpGoodsCode = erpGoodsCode; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getOnlyMallYn() { return onlyMallYn; }
    public void setOnlyMallYn(String onlyMallYn) { this.onlyMallYn = onlyMallYn; }
    public String getSoldOutYn() { return soldOutYn; }
    public void setSoldOutYn(String soldOutYn) { this.soldOutYn = soldOutYn; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }
    public int getSafetyRate() { return safetyRate; }
    public void setSafetyRate(int safetyRate) { this.safetyRate = safetyRate; }
    public int getRemainValidDays() { return remainValidDays; }
    public void setRemainValidDays(int remainValidDays) { this.remainValidDays = remainValidDays; }
    public String getGodoSyncYn() { return godoSyncYn; }
    public void setGodoSyncYn(String godoSyncYn) { this.godoSyncYn = godoSyncYn; }
    public String getErpSyncYn() { return erpSyncYn; }
    public void setErpSyncYn(String erpSyncYn) { this.erpSyncYn = erpSyncYn; }
    public String getGodoSyncBlockedYn() { return godoSyncBlockedYn; }
    public void setGodoSyncBlockedYn(String godoSyncBlockedYn) { this.godoSyncBlockedYn = godoSyncBlockedYn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedId() { return createdId; }
    public void setCreatedId(String createdId) { this.createdId = createdId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedId() { return updatedId; }
    public void setUpdatedId(String updatedId) { this.updatedId = updatedId; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
