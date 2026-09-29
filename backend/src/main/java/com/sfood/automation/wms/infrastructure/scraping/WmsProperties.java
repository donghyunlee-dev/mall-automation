package com.sfood.automation.wms.infrastructure.scraping;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wms")
public class WmsProperties {

    private String loginUrl;
    private String stockUrl;
    private String userId;
    private String password;
    private String centerCode;
    private String warehouseCode;

    public String getLoginUrl() { return loginUrl; }
    public void setLoginUrl(String loginUrl) { this.loginUrl = loginUrl; }
    public String getStockUrl() { return stockUrl; }
    public void setStockUrl(String stockUrl) { this.stockUrl = stockUrl; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCenterCode() { return centerCode; }
    public void setCenterCode(String centerCode) { this.centerCode = centerCode; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
}
