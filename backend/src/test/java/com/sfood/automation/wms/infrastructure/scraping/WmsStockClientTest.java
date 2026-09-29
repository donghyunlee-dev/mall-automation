package com.sfood.automation.wms.infrastructure.scraping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.exception.WmsScrapingException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WmsStockClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void should_return_stock_items_when_error_code_is_zero() {
        RecordingTransport transport = new RecordingTransport("""
            {
              "DS_TOTALCOUNT": 1,
              "ErrorMsg": "",
              "ErrorCode": 0,
              "DS_OUT": [
                {
                  "EXCHG_LALOC_QTY": 10,
                  "ITEM_CD": "ERP-0001",
                  "ITEM_NM": "Sample",
                  "INVN_QTY": 30,
                  "LALOC_QTY": 12,
                  "VALID_PERCENT": 80,
                  "LOT_NO": "LOT-1",
                  "LOT_ATTR_1": "OA110",
                  "ZONE_CD": "104",
                  "LOT_HLD_SCD": "",
                  "WH_CD": "SLP001",
                  "VALID_DATETIME": "2026-04-01T00:00:00"
                }
              ]
            }
            """);
        WmsStockClient client = new WmsStockClient(properties(), transport);

        List<WmsStockItem> items = client.fetchAll(new HttpCookie("WMSSID", "test-session"));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).itemCd()).isEqualTo("ERP-0001");
        assertThat(items.get(0).lalocQty()).isEqualTo(12);
    }

    @Test
    void should_throw_when_error_code_is_not_zero() {
        WmsStockClient client = new WmsStockClient(
            properties(),
            new RecordingTransport("""
                {"DS_TOTALCOUNT":0,"ErrorMsg":"fail","ErrorCode":100,"DS_OUT":[]}
                """)
        );

        assertThatThrownBy(() -> client.fetchAll(new HttpCookie("WMSSID", "test-session")))
            .isInstanceOf(WmsScrapingException.class);
    }

    @Test
    void should_throw_wms_error_when_error_code_is_session_timeout_string() {
        WmsStockClient client = new WmsStockClient(
            properties(),
            new RecordingTransport("""
                {"ErrorMsg":"세션이 종료되었습니다. 다시 로그인 하십시오.","ErrorCode":"SESSION_TIMEOUT"}
                """)
        );

        assertThatThrownBy(() -> client.fetchAll(new HttpCookie("SCOUTER", "not-auth-session")))
            .isInstanceOf(WmsScrapingException.class)
            .hasMessageContaining("SESSION_TIMEOUT")
            .hasMessageContaining("세션이 종료되었습니다");
    }

    @Test
    void should_treat_missing_error_code_as_success() {
        WmsStockClient client = new WmsStockClient(
            properties(),
            new RecordingTransport("""
                {"DS_TOTALCOUNT":0,"ErrorMsg":"OK","DS_OUT":[]}
                """)
        );

        List<WmsStockItem> items = client.fetchAll(new HttpCookie("JSESSIONID", "test-session"));

        assertThat(items).isEmpty();
    }

    @Test
    void should_treat_string_zero_error_code_as_success() {
        WmsStockClient client = new WmsStockClient(
            properties(),
            new RecordingTransport("""
                {"DS_TOTALCOUNT":0,"ErrorMsg":"OK","ErrorCode":"0","DS_OUT":[]}
                """)
        );

        List<WmsStockItem> items = client.fetchAll(new HttpCookie("JSESSIONID", "test-session"));

        assertThat(items).isEmpty();
    }

    @Test
    void should_include_search_condition_in_request_body() {
        RecordingTransport transport = new RecordingTransport("""
            {"DS_TOTALCOUNT":0,"ErrorMsg":"","ErrorCode":0,"DS_OUT":[]}
            """);
        WmsStockClient client = new WmsStockClient(properties(), transport);

        client.fetchAll(new HttpCookie("WMSSID", "test-session"));

        assertThat(transport.lastUrl).contains("CURRENT_MENUCODE=WMS141123");
        assertThat(transport.lastUrl).contains("PAGE_SIZE=10000");
        assertThat(transport.lastUrl).contains("CURRENT_PAGE=1");
        assertThat(transport.lastUrl).contains("SES_LANG=KO");
        assertThat(transport.lastUrl).contains("SES_USERGROUP=Observer");
        assertThat(transport.lastUrl).contains("SES_WHSE=SLP001");
        JsonNode searchCondition = readPayload(transport.lastBody).get("DS_SEARCHCONDITION");
        assertThat(searchCondition).hasSize(2);
        assertThat(searchCondition.get(0).get("val0").asText()).isEqualTo("WH_CD");
        assertThat(searchCondition.get(0).get("val3").asText()).isEqualTo("SLP001");
        assertThat(searchCondition.get(1).get("val0").asText()).isEqualTo("LOT_ATTR_1");
        assertThat(searchCondition.get(1).get("val3").asText()).isEqualTo("CENTER1");
    }

    private WmsProperties properties() {
        WmsProperties properties = new WmsProperties();
        properties.setStockUrl("https://wms.example.test/stock");
        properties.setCenterCode("CENTER1");
        properties.setWarehouseCode("SLP001");
        return properties;
    }

    private JsonNode readPayload(String body) {
        try {
            return OBJECT_MAPPER.readTree(body);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class RecordingTransport implements WmsTransport {
        private final String responseBody;
        private String lastUrl;
        private String lastBody;

        private RecordingTransport(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public WmsTransportResponse get(String url, String query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WmsTransportResponse post(String url, String body, HttpCookie cookie) {
            this.lastUrl = url;
            this.lastBody = body;
            return new WmsTransportResponse(responseBody, Map.of());
        }
    }
}
