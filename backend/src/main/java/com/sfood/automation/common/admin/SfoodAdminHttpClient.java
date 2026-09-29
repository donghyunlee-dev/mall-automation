package com.sfood.automation.common.admin;

public interface SfoodAdminHttpClient {
    boolean login();
    String fetchPage(String url);
}
