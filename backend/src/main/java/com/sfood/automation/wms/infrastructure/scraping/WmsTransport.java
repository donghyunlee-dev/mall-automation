package com.sfood.automation.wms.infrastructure.scraping;

import java.net.HttpCookie;

public interface WmsTransport {
    WmsTransportResponse get(String url, String query);
    WmsTransportResponse post(String url, String body, HttpCookie cookie);
}
