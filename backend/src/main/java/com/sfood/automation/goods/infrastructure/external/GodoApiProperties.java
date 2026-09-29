package com.sfood.automation.goods.infrastructure.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.godo")
public class GodoApiProperties {

    private String baseUrl;
    private String partnerKey;
    private String key;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getPartnerKey() { return partnerKey; }
    public void setPartnerKey(String partnerKey) { this.partnerKey = partnerKey; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
