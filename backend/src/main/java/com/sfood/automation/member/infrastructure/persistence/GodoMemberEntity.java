package com.sfood.automation.member.infrastructure.persistence;

import com.sfood.automation.common.security.EncryptedFieldConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "godo_member")
public class GodoMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mem_no", nullable = false, unique = true)
    private Long memNo;

    @Column(name = "mem_id", nullable = false, unique = true)
    private String memId;

    @Column(name = "group_sno", nullable = false)
    private int groupSno = 1;

    @Column(name = "member_fl", nullable = false)
    private String memberFl = "personal";

    @Column(name = "app_fl", nullable = false)
    private String appFl = "n";

    @Column(name = "app_dt")
    private String appDt;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "mem_name")
    private String memName;

    @Column(name = "mem_name_hash")
    private String memNameHash;

    @Column(name = "nick_name")
    private String nickName;

    @Column(name = "sex_fl")
    private String sexFl;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "birth_dt")
    private String birthDt;

    @Column(name = "marri_fl")
    private String marriFl;

    @Column(name = "marri_date")
    private String marriDate;

    private String job;

    private String interest;

    @Convert(converter = EncryptedFieldConverter.class)
    private String email;

    @Column(name = "email_hash")
    private String emailHash;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "cell_phone")
    private String cellPhone;

    @Column(name = "cell_phone_hash")
    private String cellPhoneHash;

    @Convert(converter = EncryptedFieldConverter.class)
    private String phone;

    private String fax;

    @Column(name = "mailling_fl", nullable = false)
    private String maillingFl = "y";

    @Column(name = "sms_fl", nullable = false)
    private String smsFl = "y";

    @Convert(converter = EncryptedFieldConverter.class)
    private String address;

    @Column(name = "entry_dt")
    private String entryDt;

    @Column(name = "entry_path", nullable = false)
    private String entryPath = "pc";

    @Column(name = "last_login_dt")
    private String lastLoginDt;

    @Column(name = "login_count", nullable = false)
    private int loginCount;

    @Column(name = "sale_amount", nullable = false)
    private double saleAmount;

    @Column(nullable = false)
    private double mileage;

    @Column(nullable = false)
    private double deposit;

    @Column(name = "recomm_id")
    private String recommId;

    private String ex1;
    private String ex2;
    private String ex3;
    private String ex4;
    private String ex5;
    private String ex6;

    @Column(name = "expiration_fl", nullable = false)
    private int expirationFl = 1;

    private String memo;

    @Column(name = "deleted_fl", nullable = false)
    private String deletedFl = "n";

    @Column(name = "synced_at", nullable = false)
    private String syncedAt;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Long getMemNo() { return memNo; }
    public void setMemNo(Long memNo) { this.memNo = memNo; }
    public String getMemId() { return memId; }
    public void setMemId(String memId) { this.memId = memId; }
    public int getGroupSno() { return groupSno; }
    public void setGroupSno(int groupSno) { this.groupSno = groupSno; }
    public String getMemberFl() { return memberFl; }
    public void setMemberFl(String memberFl) { this.memberFl = memberFl; }
    public String getAppFl() { return appFl; }
    public void setAppFl(String appFl) { this.appFl = appFl; }
    public String getAppDt() { return appDt; }
    public void setAppDt(String appDt) { this.appDt = appDt; }
    public String getMemName() { return memName; }
    public void setMemName(String memName) { this.memName = memName; }
    public String getMemNameHash() { return memNameHash; }
    public void setMemNameHash(String memNameHash) { this.memNameHash = memNameHash; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getSexFl() { return sexFl; }
    public void setSexFl(String sexFl) { this.sexFl = sexFl; }
    public String getBirthDt() { return birthDt; }
    public void setBirthDt(String birthDt) { this.birthDt = birthDt; }
    public String getMarriFl() { return marriFl; }
    public void setMarriFl(String marriFl) { this.marriFl = marriFl; }
    public String getMarriDate() { return marriDate; }
    public void setMarriDate(String marriDate) { this.marriDate = marriDate; }
    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }
    public String getInterest() { return interest; }
    public void setInterest(String interest) { this.interest = interest; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailHash() { return emailHash; }
    public void setEmailHash(String emailHash) { this.emailHash = emailHash; }
    public String getCellPhone() { return cellPhone; }
    public void setCellPhone(String cellPhone) { this.cellPhone = cellPhone; }
    public String getCellPhoneHash() { return cellPhoneHash; }
    public void setCellPhoneHash(String cellPhoneHash) { this.cellPhoneHash = cellPhoneHash; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public String getMaillingFl() { return maillingFl; }
    public void setMaillingFl(String maillingFl) { this.maillingFl = maillingFl; }
    public String getSmsFl() { return smsFl; }
    public void setSmsFl(String smsFl) { this.smsFl = smsFl; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEntryDt() { return entryDt; }
    public void setEntryDt(String entryDt) { this.entryDt = entryDt; }
    public String getEntryPath() { return entryPath; }
    public void setEntryPath(String entryPath) { this.entryPath = entryPath; }
    public String getLastLoginDt() { return lastLoginDt; }
    public void setLastLoginDt(String lastLoginDt) { this.lastLoginDt = lastLoginDt; }
    public int getLoginCount() { return loginCount; }
    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }
    public double getSaleAmount() { return saleAmount; }
    public void setSaleAmount(double saleAmount) { this.saleAmount = saleAmount; }
    public double getMileage() { return mileage; }
    public void setMileage(double mileage) { this.mileage = mileage; }
    public double getDeposit() { return deposit; }
    public void setDeposit(double deposit) { this.deposit = deposit; }
    public String getRecommId() { return recommId; }
    public void setRecommId(String recommId) { this.recommId = recommId; }
    public String getEx1() { return ex1; }
    public void setEx1(String ex1) { this.ex1 = ex1; }
    public String getEx2() { return ex2; }
    public void setEx2(String ex2) { this.ex2 = ex2; }
    public String getEx3() { return ex3; }
    public void setEx3(String ex3) { this.ex3 = ex3; }
    public String getEx4() { return ex4; }
    public void setEx4(String ex4) { this.ex4 = ex4; }
    public String getEx5() { return ex5; }
    public void setEx5(String ex5) { this.ex5 = ex5; }
    public String getEx6() { return ex6; }
    public void setEx6(String ex6) { this.ex6 = ex6; }
    public int getExpirationFl() { return expirationFl; }
    public void setExpirationFl(int expirationFl) { this.expirationFl = expirationFl; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getDeletedFl() { return deletedFl; }
    public void setDeletedFl(String deletedFl) { this.deletedFl = deletedFl; }
    public String getSyncedAt() { return syncedAt; }
    public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
