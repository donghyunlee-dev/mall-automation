package com.sfood.automation.dashboard.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "godo_order")
public class GodoOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_no", nullable = false, unique = true)
    private Long orderNo;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "member_grade", nullable = false)
    private String memberGrade;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "ordered_at", nullable = false)
    private String orderedAt;

    @Column(name = "total_goods_price", nullable = false)
    private BigDecimal totalGoodsPrice;

    @Column(name = "total_delivery_charge", nullable = false)
    private BigDecimal totalDeliveryCharge;

    @Column(name = "use_deposit", nullable = false)
    private BigDecimal useDeposit;

    @Column(name = "use_mileage", nullable = false)
    private BigDecimal useMileage;

    @Column(name = "total_goods_dc_price", nullable = false)
    private BigDecimal totalGoodsDcPrice;

    @Column(name = "total_member_dc_price", nullable = false)
    private BigDecimal totalMemberDcPrice;

    @Column(name = "total_coupon_dc_price", nullable = false)
    private BigDecimal totalCouponDcPrice;

    @Column(name = "payment_final_amount", nullable = false)
    private BigDecimal paymentFinalAmount;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Long getOrderNo() { return orderNo; }
    public void setOrderNo(Long orderNo) { this.orderNo = orderNo; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberGrade() { return memberGrade; }
    public void setMemberGrade(String memberGrade) { this.memberGrade = memberGrade; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getOrderedAt() { return orderedAt; }
    public void setOrderedAt(String orderedAt) { this.orderedAt = orderedAt; }
    public BigDecimal getTotalGoodsPrice() { return totalGoodsPrice; }
    public void setTotalGoodsPrice(BigDecimal totalGoodsPrice) { this.totalGoodsPrice = totalGoodsPrice; }
    public BigDecimal getTotalDeliveryCharge() { return totalDeliveryCharge; }
    public void setTotalDeliveryCharge(BigDecimal totalDeliveryCharge) { this.totalDeliveryCharge = totalDeliveryCharge; }
    public BigDecimal getUseDeposit() { return useDeposit; }
    public void setUseDeposit(BigDecimal useDeposit) { this.useDeposit = useDeposit; }
    public BigDecimal getUseMileage() { return useMileage; }
    public void setUseMileage(BigDecimal useMileage) { this.useMileage = useMileage; }
    public BigDecimal getTotalGoodsDcPrice() { return totalGoodsDcPrice; }
    public void setTotalGoodsDcPrice(BigDecimal totalGoodsDcPrice) { this.totalGoodsDcPrice = totalGoodsDcPrice; }
    public BigDecimal getTotalMemberDcPrice() { return totalMemberDcPrice; }
    public void setTotalMemberDcPrice(BigDecimal totalMemberDcPrice) { this.totalMemberDcPrice = totalMemberDcPrice; }
    public BigDecimal getTotalCouponDcPrice() { return totalCouponDcPrice; }
    public void setTotalCouponDcPrice(BigDecimal totalCouponDcPrice) { this.totalCouponDcPrice = totalCouponDcPrice; }
    public BigDecimal getPaymentFinalAmount() { return paymentFinalAmount; }
    public void setPaymentFinalAmount(BigDecimal paymentFinalAmount) { this.paymentFinalAmount = paymentFinalAmount; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
