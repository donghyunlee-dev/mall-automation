package com.sfood.automation.goods.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "common_status_config")
public class CommonStatusConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String scope;

    @Column(name = "out_of_stock_qty", nullable = false)
    private int outOfStockQty;

    @Column(name = "on_sale_qty", nullable = false)
    private int onSaleQty;

    @Column(name = "safety_rate", nullable = false)
    private int safetyRate;

    @Column(name = "remain_valid_days", nullable = false)
    private int remainValidDays;

    private String description;

    @Column(name = "updated_id")
    private String updatedId;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public int getOutOfStockQty() { return outOfStockQty; }
    public void setOutOfStockQty(int outOfStockQty) { this.outOfStockQty = outOfStockQty; }
    public int getOnSaleQty() { return onSaleQty; }
    public void setOnSaleQty(int onSaleQty) { this.onSaleQty = onSaleQty; }
    public int getSafetyRate() { return safetyRate; }
    public void setSafetyRate(int safetyRate) { this.safetyRate = safetyRate; }
    public int getRemainValidDays() { return remainValidDays; }
    public void setRemainValidDays(int remainValidDays) { this.remainValidDays = remainValidDays; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUpdatedId() { return updatedId; }
    public void setUpdatedId(String updatedId) { this.updatedId = updatedId; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
