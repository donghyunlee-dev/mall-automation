package com.sfood.automation.wms.infrastructure.scraping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.WmsScrapingException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WmsLoginClientTest {

    @Test
    void should_return_cookie_when_login_succeeds() {
        CapturingLoginTransport transport = new CapturingLoginTransport(
            new WmsTransportResponse("OK", Map.of("Set-Cookie", "WMSSID=test-session; Path=/"))
        );
        WmsLoginClient client = new WmsLoginClient(
            properties(),
            transport
        );

        HttpCookie cookie = client.login();

        assertThat(cookie.getName()).isEqualTo("WMSSID");
        assertThat(cookie.getValue()).isEqualTo("test-session");
        assertThat(transport.url).isEqualTo("https://wms.example.test/login");
        assertThat(transport.query)
            .isEqualTo("CURRENT_MENUCODE=LOGIN"
                + "&SELLANGBEFORELOGIN=KO"
                + "&USERID=tester"
                + "&LOGIN_USERID=tester"
                + "&PASSWORD=secret");
    }

    @Test
    void should_return_cookie_when_login_header_is_lowercase() {
        WmsLoginClient client = new WmsLoginClient(
            properties(),
            new CapturingLoginTransport(new WmsTransportResponse("OK", Map.of("set-cookie", "JSESSIONID=test-session; Path=/")))
        );

        HttpCookie cookie = client.login();

        assertThat(cookie.getName()).isEqualTo("JSESSIONID");
        assertThat(cookie.getValue()).isEqualTo("test-session");
    }

    @Test
    void should_prefer_jsessionid_when_login_returns_multiple_cookies() {
        WmsLoginClient client = new WmsLoginClient(
            properties(),
            new CapturingLoginTransport(new WmsTransportResponse(
                "OK",
                Map.of("Set-Cookie", "SCOUTER=scouter-session; Path=/"),
                Map.of("Set-Cookie", List.of(
                    "SCOUTER=scouter-session; Path=/",
                    "JSESSIONID=wms-session; Path=/; HttpOnly"
                ))
            ))
        );

        HttpCookie cookie = client.login();

        assertThat(cookie.getName()).isEqualTo("JSESSIONID");
        assertThat(cookie.getValue()).isEqualTo("wms-session");
    }

    @Test
    void should_throw_when_login_response_has_no_cookie() {
        WmsLoginClient client = new WmsLoginClient(
            properties(),
            new CapturingLoginTransport(new WmsTransportResponse("OK", Map.of()))
        );

        assertThatThrownBy(client::login)
            .isInstanceOf(WmsScrapingException.class);
    }

    private WmsProperties properties() {
        WmsProperties properties = new WmsProperties();
        properties.setLoginUrl("https://wms.example.test/login");
        properties.setUserId("tester");
        properties.setPassword("secret");
        return properties;
    }

    private static final class CapturingLoginTransport implements WmsTransport {
        private final WmsTransportResponse response;
        private String url;
        private String query;

        private CapturingLoginTransport(WmsTransportResponse response) {
            this.response = response;
        }

        @Override
        public WmsTransportResponse get(String url, String query) {
            this.url = url;
            this.query = query;
            return response;
        }

        @Override
        public WmsTransportResponse post(String url, String body, HttpCookie cookie) {
            throw new UnsupportedOperationException();
        }
    }
}
