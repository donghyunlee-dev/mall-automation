package com.sfood.automation.goods.infrastructure.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sfood-admin")
public class SfoodAdminProperties {

    private String baseUrl;
    private String managerId;
    private String managerPw;
    private String loginPath;
    private String goodsFormPath;
    private String goodsSavePath;
    private String testGoodsCode;
    private String memberListPath;
    private String memberDetailPath;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public String getManagerPw() { return managerPw; }
    public void setManagerPw(String managerPw) { this.managerPw = managerPw; }
    public String getLoginPath() { return loginPath; }
    public void setLoginPath(String loginPath) { this.loginPath = loginPath; }
    public String getGoodsFormPath() { return goodsFormPath; }
    public void setGoodsFormPath(String goodsFormPath) { this.goodsFormPath = goodsFormPath; }
    public String getGoodsSavePath() { return goodsSavePath; }
    public void setGoodsSavePath(String goodsSavePath) { this.goodsSavePath = goodsSavePath; }
    public String getTestGoodsCode() { return testGoodsCode; }
    public void setTestGoodsCode(String testGoodsCode) { this.testGoodsCode = testGoodsCode; }
    public String getMemberListPath() { return memberListPath; }
    public void setMemberListPath(String memberListPath) { this.memberListPath = memberListPath; }
    public String getMemberDetailPath() { return memberDetailPath; }
    public void setMemberDetailPath(String memberDetailPath) { this.memberDetailPath = memberDetailPath; }
}
