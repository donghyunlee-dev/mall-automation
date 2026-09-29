package com.sfood.automation.member.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "member_partner_log")
public class MemberPartnerLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mem_no", nullable = false)
    private Long memNo;

    @Column(name = "partner_code")
    private String partnerCode;

    @Column(name = "partner_name")
    private String partnerName;

    @Column(name = "tax_invoice_fl")
    private String taxInvoiceFl;

    @Column(name = "member_status")
    private String memberStatus;

    @Column(name = "logged_at", nullable = false)
    private String loggedAt;

    @Column(name = "logged_by")
    private String loggedBy;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

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
    public String getLoggedAt() { return loggedAt; }
    public void setLoggedAt(String loggedAt) { this.loggedAt = loggedAt; }
    public String getLoggedBy() { return loggedBy; }
    public void setLoggedBy(String loggedBy) { this.loggedBy = loggedBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
