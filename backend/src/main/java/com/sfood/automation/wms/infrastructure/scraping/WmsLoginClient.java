package com.sfood.automation.wms.infrastructure.scraping;

import com.sfood.automation.common.exception.WmsScrapingException;
import java.net.HttpCookie;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(WmsProperties.class)
public class WmsLoginClient {

    private final WmsProperties properties;
    private final WmsTransport transport;

    public WmsLoginClient(WmsProperties properties, WmsTransport transport) {
        this.properties = properties;
        this.transport = transport;
    }

    public HttpCookie login() {
        String query = "CURRENT_MENUCODE=LOGIN"
            + "&SELLANGBEFORELOGIN=KO"
            + "&USERID=" + properties.getUserId()
            + "&LOGIN_USERID=" + properties.getUserId()
            + "&PASSWORD=" + properties.getPassword();
        WmsTransportResponse response = transport.get(properties.getLoginUrl(), query);
        List<String> setCookies = response.headers("Set-Cookie");
        if (setCookies.isEmpty()) {
            String setCookie = response.header("Set-Cookie");
            if (setCookie != null && !setCookie.isBlank()) {
                setCookies = List.of(setCookie);
            }
        }
        if (setCookies.isEmpty()) {
            throw new WmsScrapingException("WMS login response did not include Set-Cookie header");
        }
        List<HttpCookie> cookies = setCookies.stream()
            .flatMap(setCookie -> HttpCookie.parse(setCookie).stream())
            .toList();
        if (cookies.isEmpty()) {
            throw new WmsScrapingException("WMS login response returned an unparsable cookie");
        }
        return cookies.stream()
            .filter(cookie -> "JSESSIONID".equalsIgnoreCase(cookie.getName()))
            .findFirst()
            .orElse(cookies.get(0));
    }
}
