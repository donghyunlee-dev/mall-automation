package com.sfood.automation.goods.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "goods_status_log")
public class GoodsStatusLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "godo_goods_code", nullable = false)
    private String godoGoodsCode;

    @Column(name = "erp_goods_code", nullable = false)
    private String erpGoodsCode;

    @Column(name = "wms_available_qty", nullable = false)
    private int wmsAvailableQty;

    @Column(name = "criteria_qty", nullable = false)
    private int criteriaQty;

    @Column(name = "safety_rate", nullable = false)
    private int safetyRate;

    @Column(name = "valid_percent")
    private Integer validPercent;

    @Column(name = "remain_valid_days")
    private Integer remainValidDays;

    @Column(name = "before_status", nullable = false)
    private String beforeStatus;

    @Column(name = "after_status", nullable = false)
    private String afterStatus;

    @Column(name = "changed_at", nullable = false)
    private String changedAt;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getGodoGoodsCode() { return godoGoodsCode; }
    public void setGodoGoodsCode(String godoGoodsCode) { this.godoGoodsCode = godoGoodsCode; }
    public String getErpGoodsCode() { return erpGoodsCode; }
    public void setErpGoodsCode(String erpGoodsCode) { this.erpGoodsCode = erpGoodsCode; }
    public int getWmsAvailableQty() { return wmsAvailableQty; }
    public void setWmsAvailableQty(int wmsAvailableQty) { this.wmsAvailableQty = wmsAvailableQty; }
    public int getCriteriaQty() { return criteriaQty; }
    public void setCriteriaQty(int criteriaQty) { this.criteriaQty = criteriaQty; }
    public int getSafetyRate() { return safetyRate; }
    public void setSafetyRate(int safetyRate) { this.safetyRate = safetyRate; }
    public Integer getValidPercent() { return validPercent; }
    public void setValidPercent(Integer validPercent) { this.validPercent = validPercent; }
    public Integer getRemainValidDays() { return remainValidDays; }
    public void setRemainValidDays(Integer remainValidDays) { this.remainValidDays = remainValidDays; }
    public String getBeforeStatus() { return beforeStatus; }
    public void setBeforeStatus(String beforeStatus) { this.beforeStatus = beforeStatus; }
    public String getAfterStatus() { return afterStatus; }
    public void setAfterStatus(String afterStatus) { this.afterStatus = afterStatus; }
    public String getChangedAt() { return changedAt; }
    public void setChangedAt(String changedAt) { this.changedAt = changedAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
