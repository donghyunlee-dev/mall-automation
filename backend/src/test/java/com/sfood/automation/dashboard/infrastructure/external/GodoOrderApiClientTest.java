package com.sfood.automation.dashboard.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchQuery;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchResult;
import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import com.sfood.automation.goods.infrastructure.external.GodoApiProperties;
import com.sfood.automation.goods.infrastructure.external.GodoTransport;
import com.sfood.automation.goods.infrastructure.external.GodoTransportResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GodoOrderApiClientTest {

    private static final String SUCCESS_BODY = """
        <data>
            <header><code>000</code><msg>성공</msg></header>
            <return>
                <order_data idx="1">
                    <orderNo>1</orderNo>
                    <orderStatus>s1</orderStatus>
                    <memId>member01</memId>
                    <memGroupNm>일반회원</memGroupNm>
                    <orderDate>2026-03-24 17:52:56</orderDate>
                    <totalGoodsPrice>10000.00</totalGoodsPrice>
                    <totalDeliveryCharge>0.00</totalDeliveryCharge>
                    <useDeposit>0.00</useDeposit>
                    <useMileage>0.00</useMileage>
                    <totalGoodsDcPrice>0.00</totalGoodsDcPrice>
                    <totalMemberDcPrice>0.00</totalMemberDcPrice>
                    <totalCouponGoodsDcPrice>0.00</totalCouponGoodsDcPrice>
                    <totalCouponOrderDcPrice>0.00</totalCouponOrderDcPrice>
                    <totalCouponDeliveryDcPrice>0.00</totalCouponDeliveryDcPrice>
                    <orderGoodsData idx="1">
                        <sno>1</sno>
                        <goodsNm>상품A</goodsNm>
                        <goodsCnt>1</goodsCnt>
                        <goodsPrice>10000.00</goodsPrice>
                        <deliveryDt>2026-03-25 09:38:10</deliveryDt>
                    </orderGoodsData>
                </order_data>
            </return>
        </data>
        """;

    @Test
    void should_call_order_search_endpoint_with_partner_credentials_and_date_range() {
        CapturingTransport transport = new CapturingTransport(SUCCESS_BODY);
        GodoOrderApiClient client = new GodoOrderApiClient(transport, new FakeApiTransactionLogRepository(), properties("https://openhub.godo.co.kr"));

        client.search(GodoOrderSearchQuery.byOrderDateRange("2026-03-24", "2026-03-24"));

        assertThat(transport.lastUrl).isEqualTo("https://openhub.godo.co.kr/godomall5/order/Order_Search.php");
        assertThat(transport.lastRequestBody).contains("partner_key=partner");
        assertThat(transport.lastRequestBody).contains("key=secret");
        assertThat(transport.lastRequestBody).contains("dateType=order");
        assertThat(transport.lastRequestBody).contains("startDate=2026-03-24");
        assertThat(transport.lastRequestBody).contains("endDate=2026-03-24");
    }

    @Test
    void should_return_parsed_orders_when_response_succeeds() {
        CapturingTransport transport = new CapturingTransport(SUCCESS_BODY);
        GodoOrderApiClient client = new GodoOrderApiClient(transport, new FakeApiTransactionLogRepository(), properties("https://openhub.godo.co.kr"));

        GodoOrderSearchResult result = client.search(GodoOrderSearchQuery.byOrderDateRange("2026-03-24", "2026-03-24"));

        assertThat(result.orders()).hasSize(1);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void should_throw_when_response_code_is_not_success() {
        CapturingTransport transport = new CapturingTransport("""
            <data>
                <header><code>999</code><msg>invalid partner_key</msg></header>
            </data>
            """);
        GodoOrderApiClient client = new GodoOrderApiClient(transport, new FakeApiTransactionLogRepository(), properties("https://openhub.godo.co.kr"));

        assertThatThrownBy(() -> client.search(GodoOrderSearchQuery.byOrderDateRange("2026-03-24", "2026-03-24")))
            .isInstanceOf(GodoApiException.class);
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
