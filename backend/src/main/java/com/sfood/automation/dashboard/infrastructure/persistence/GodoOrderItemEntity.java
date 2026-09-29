package com.sfood.automation.dashboard.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "godo_order_item")
public class GodoOrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "godo_line_no", nullable = false)
    private Long godoLineNo;

    @Column(name = "order_no", nullable = false)
    private Long orderNo;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_qty", nullable = false)
    private Integer productQty;

    @Column(name = "product_unit_price", nullable = false)
    private BigDecimal productUnitPrice;

    @Column(name = "product_line_revenue", nullable = false)
    private BigDecimal productLineRevenue;

    @Column(name = "delivered_at")
    private String deliveredAt;

    @Column(name = "oms_shipped_at")
    private String omsShippedAt;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public Long getGodoLineNo() { return godoLineNo; }
    public void setGodoLineNo(Long godoLineNo) { this.godoLineNo = godoLineNo; }
    public Long getOrderNo() { return orderNo; }
    public void setOrderNo(Long orderNo) { this.orderNo = orderNo; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getProductQty() { return productQty; }
    public void setProductQty(Integer productQty) { this.productQty = productQty; }
    public BigDecimal getProductUnitPrice() { return productUnitPrice; }
    public void setProductUnitPrice(BigDecimal productUnitPrice) { this.productUnitPrice = productUnitPrice; }
    public BigDecimal getProductLineRevenue() { return productLineRevenue; }
    public void setProductLineRevenue(BigDecimal productLineRevenue) { this.productLineRevenue = productLineRevenue; }
    public String getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(String deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getOmsShippedAt() { return omsShippedAt; }
    public void setOmsShippedAt(String omsShippedAt) { this.omsShippedAt = omsShippedAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
