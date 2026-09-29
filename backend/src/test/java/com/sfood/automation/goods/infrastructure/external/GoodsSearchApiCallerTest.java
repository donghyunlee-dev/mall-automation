package com.sfood.automation.goods.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class GoodsSearchApiCallerTest {

    @Test
    void should_parse_goods_info_when_code_is_000() {
        FakeApiTransactionLogRepository logRepository = new FakeApiTransactionLogRepository();
        GoodsSearchApiCaller caller = new GoodsSearchApiCaller(
            new FakeGodoTransport("""
                <response>
                  <header>
                    <code>000</code>
                    <message>success</message>
                  </header>
                  <data>
                    <goodsNo>10000001</goodsNo>
                    <goodsNm>Sample Goods</goodsNm>
                    <soldOutFl>n</soldOutFl>
                  </data>
                </response>
                """),
            new XmlMapper(),
            logRepository
        );

        var response = caller.call("https://example.test/Goods_Search.php", "goodsNo=10000001", "10000001");

        assertThat(response.godoGoodsCode()).isEqualTo("10000001");
        assertThat(response.goodsName()).isEqualTo("Sample Goods");
        assertThat(response.soldOutYn()).isEqualTo("N");
        assertThat(logRepository.savedLogs).hasSize(1);
        assertThat(logRepository.savedLogs.get(0).resultCode()).isEqualTo("000");
        assertThat(logRepository.savedLogs.get(0).isSuccess()).isTrue();
    }

    @Test
    void should_parse_goods_info_from_live_godo_xml_shape() {
        FakeApiTransactionLogRepository logRepository = new FakeApiTransactionLogRepository();
        GoodsSearchApiCaller caller = new GoodsSearchApiCaller(
            new FakeGodoTransport("""
                <?xml version='1.0' encoding='UTF-8'?>
                <data>
                  <header>
                    <code>000</code>
                    <msg><![CDATA[성공]]></msg>
                    <total>1</total>
                  </header>
                  <return>
                    <goods_data idx="1">
                      <goodsNo>1000000062</goodsNo>
                      <goodsNm><![CDATA[Sample Live Goods]]></goodsNm>
                      <goodsSellFl>y</goodsSellFl>
                      <soldOutFl>y</soldOutFl>
                    </goods_data>
                  </return>
                </data>
                """),
            new XmlMapper(),
            logRepository
        );

        var response = caller.call("https://example.test/Goods_Search.php", "goodsNo=1000000062", "1000000062");

        assertThat(response.godoGoodsCode()).isEqualTo("1000000062");
        assertThat(response.goodsName()).isEqualTo("Sample Live Goods");
        assertThat(response.soldOutYn()).isEqualTo("Y");
        assertThat(logRepository.savedLogs.get(0).resultCode()).isEqualTo("000");
        assertThat(logRepository.savedLogs.get(0).isSuccess()).isTrue();
    }

    @Test
    void should_return_blank_goods_info_when_live_response_has_empty_return() {
        FakeApiTransactionLogRepository logRepository = new FakeApiTransactionLogRepository();
        GoodsSearchApiCaller caller = new GoodsSearchApiCaller(
            new FakeGodoTransport("""
                <?xml version='1.0' encoding='UTF-8'?>
                <data>
                  <header>
                    <code>000</code>
                    <msg><![CDATA[성공]]></msg>
                    <total>0</total>
                  </header>
                  <return/>
                </data>
                """),
            new XmlMapper(),
            logRepository
        );

        var response = caller.call("https://example.test/Goods_Search.php", "goodsNo=9900000733", "9900000733");

        assertThat(response.godoGoodsCode()).isBlank();
        assertThat(response.goodsName()).isBlank();
        assertThat(response.soldOutYn()).isEqualTo("N");
        assertThat(logRepository.savedLogs.get(0).resultCode()).isEqualTo("000");
        assertThat(logRepository.savedLogs.get(0).isSuccess()).isTrue();
    }

    @Test
    void should_throw_when_code_is_119() {
        GoodsSearchApiCaller caller = new GoodsSearchApiCaller(
            new FakeGodoTransport("""
                <response>
                  <header>
                    <code>119</code>
                    <message>not found</message>
                  </header>
                </response>
                """),
            new XmlMapper(),
            new FakeApiTransactionLogRepository()
        );

        assertThatThrownBy(() -> caller.call("https://example.test/Goods_Search.php", "goodsNo=10000001", "10000001"))
            .isInstanceOf(GodoApiException.class);
    }

    @Test
    void should_throw_when_code_is_999() {
        GoodsSearchApiCaller caller = new GoodsSearchApiCaller(
            new FakeGodoTransport("""
                <response>
                  <header>
                    <code>999</code>
                    <message>auth error</message>
                  </header>
                </response>
                """),
            new XmlMapper(),
            new FakeApiTransactionLogRepository()
        );

        assertThatThrownBy(() -> caller.call("https://example.test/Goods_Search.php", "goodsNo=10000001", "10000001"))
            .isInstanceOf(GodoApiException.class);
    }

    private static final class FakeGodoTransport implements GodoTransport {
        private final String responseBody;

        private FakeGodoTransport(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public GodoTransportResponse postForm(String url, String requestBody) {
            return new GodoTransportResponse(responseBody, Map.of());
        }
    }

    private static final class FakeApiTransactionLogRepository implements ApiTransactionLogRepository {
        private final List<ApiTransactionLog> savedLogs = new ArrayList<>();

        @Override
        public ApiTransactionLog save(ApiTransactionLog log) {
            ApiTransactionLog saved = log.withId(savedLogs.size() + 1);
            savedLogs.add(saved);
            return saved;
        }

        @Override
        public ApiTransactionLog updateResponse(Integer id, String responseBody, String resultCode, String resultMessage, boolean isSuccess, Instant respondedAt) {
            ApiTransactionLog current = savedLogs.stream()
                .filter(log -> log.id().equals(id))
                .findFirst()
                .orElseThrow();
            ApiTransactionLog updated = current.complete(responseBody, resultCode, resultMessage, isSuccess, respondedAt);
            savedLogs.set(id - 1, updated);
            return updated;
        }

        @Override
        public Optional<ApiTransactionLog> findById(Integer id) {
            return savedLogs.stream().filter(log -> log.id().equals(id)).findFirst();
        }
    }
}
