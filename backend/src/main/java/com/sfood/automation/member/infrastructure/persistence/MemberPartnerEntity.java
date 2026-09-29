package com.sfood.automation.member.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_partner")
public class MemberPartnerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mem_no", nullable = false, unique = true)
    private Long memNo;

    @Column(name = "partner_code")
    private String partnerCode;

    @Column(name = "partner_name")
    private String partnerName;

    @Column(name = "tax_invoice_fl", nullable = false)
    private String taxInvoiceFl = "n";

    @Column(name = "member_status", nullable = false)
    private String memberStatus = "NORMAL";

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Long getMemNo() { return memNo; }
    public void setMemNo(Long memNo) { this.memNo = memNo; }
    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    public String getTaxInvoiceFl() { return taxInvoiceFl; }
    public void setTaxInvoiceFl(String taxInvoiceFl) { this.taxInvoiceFl = taxInvoiceFl; }
    public String getMemberStatus() { return memberStatus; }
    public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
