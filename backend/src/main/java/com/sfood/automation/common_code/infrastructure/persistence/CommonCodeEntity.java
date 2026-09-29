package com.sfood.automation.common_code.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "common_code")
public class CommonCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "group_code", nullable = false)
    private String groupCode;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "code_name", nullable = false)
    private String codeName;

    private String note;

    @Column(name = "use_yn", nullable = false)
    private String useYn = "Y";

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCodeName() { return codeName; }
    public void setCodeName(String codeName) { this.codeName = codeName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
