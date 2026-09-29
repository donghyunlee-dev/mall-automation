package com.sfood.automation.member.infrastructure.persistence;

import com.sfood.automation.common.security.EncryptedFieldConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "godo_member_business_log")
public class GodoMemberBusinessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mem_no", nullable = false)
    private Long memNo;

    private String company;

    @Column(name = "business_no")
    private String businessNo;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "ceo_name")
    private String ceoName;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "item_code")
    private String itemCode;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "com_address")
    private String comAddress;

    @Column(name = "biz_license_file")
    private String bizLicenseFile;

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
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public String getCeoName() { return ceoName; }
    public void setCeoName(String ceoName) { this.ceoName = ceoName; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getComAddress() { return comAddress; }
    public void setComAddress(String comAddress) { this.comAddress = comAddress; }
    public String getBizLicenseFile() { return bizLicenseFile; }
    public void setBizLicenseFile(String bizLicenseFile) { this.bizLicenseFile = bizLicenseFile; }
    public String getLoggedAt() { return loggedAt; }
    public void setLoggedAt(String loggedAt) { this.loggedAt = loggedAt; }
    public String getLoggedBy() { return loggedBy; }
    public void setLoggedBy(String loggedBy) { this.loggedBy = loggedBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
}
