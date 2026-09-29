package com.sfood.automation.goods.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GodoApiClientTest {

    @Test
    void should_append_goods_path_when_base_url_has_only_host() {
        CapturingTransport transport = new CapturingTransport("""
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
            """);
        GodoApiClient client = new GodoApiClient(transport, new FakeApiTransactionLogRepository(), properties("https://openhub.godo.co.kr"));

        client.searchGoods("10000001");

        assertThat(transport.lastUrl).isEqualTo("https://openhub.godo.co.kr/godomall5/goods/Goods_Search.php");
    }

    @Test
    void should_not_duplicate_goods_path_when_base_url_already_has_path() {
        CapturingTransport transport = new CapturingTransport("""
            <response>
              <header>
                <code>000</code>
                <message>success</message>
              </header>
            </response>
            """);
        GodoApiClient client = new GodoApiClient(
            transport,
            new FakeApiTransactionLogRepository(),
            properties("https://openhub.godo.co.kr/godomall5/goods")
        );

        client.updateSoldOutStatus("10000001", "y");

        assertThat(transport.lastUrl).isEqualTo("https://openhub.godo.co.kr/godomall5/goods/Goods_Soldout_Status.php");
    }

    @Test
    void should_use_documented_soldout_parameter_name_when_updating_status() {
        CapturingTransport transport = new CapturingTransport("""
            <data>
              <header>
                <code>000</code>
                <msg>success</msg>
              </header>
            </data>
            """);
        GodoApiClient client = new GodoApiClient(
            transport,
            new FakeApiTransactionLogRepository(),
            properties("https://openhub.godo.co.kr")
        );

        client.updateSoldOutStatus("10000001", "y");

        assertThat(transport.lastRequestBody).contains("goodsNo=10000001");
        assertThat(transport.lastRequestBody).contains("soldoutFl=y");
        assertThat(transport.lastRequestBody).doesNotContain("soldOutFl=");
    }

    private GodoApiProperties properties(String baseUrl) {
        GodoApiProperties properties = new GodoApiProperties();
        properties.setBaseUrl(baseUrl);
        properties.setPartnerKey("partner");
        properties.setKey("secret");
        return properties;
    }

    private static final class CapturingTransport implements GodoTransport {
        private final String responseBody;
        private String lastUrl;
        private String lastRequestBody;

        private CapturingTransport(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public GodoTransportResponse postForm(String url, String requestBody) {
            this.lastUrl = url;
            this.lastRequestBody = requestBody;
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
