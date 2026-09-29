package com.sfood.automation.goods.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class SoldoutStatusApiCallerTest {

    @Test
    void should_return_success_when_code_is_000() {
        SoldoutStatusApiCaller caller = new SoldoutStatusApiCaller(
            new SequenceTransport().addSuccess("""
                <data>
                  <header>
                    <code>000</code>
                    <msg>success</msg>
                  </header>
                </data>
                """, Map.of()),
            new XmlMapper(),
            new FakeApiTransactionLogRepository(),
            duration -> { }
        );

        var response = caller.call("https://example.test/Goods_Soldout_Status.php", "goodsNo=10000001&soldOutFl=y", "10000001");

        assertThat(response.success()).isTrue();
        assertThat(response.resultCode()).isEqualTo("000");
        assertThat(response.resultMessage()).isEqualTo("success");
    }

    @Test
    void should_retry_once_when_429_then_succeed() {
        SequenceTransport transport = new SequenceTransport()
            .addFailure(HttpStatus.TOO_MANY_REQUESTS, "rate limit")
            .addSuccess("""
                <response>
                  <header>
                    <code>000</code>
                    <message>success</message>
                  </header>
                </response>
                """, Map.of());
        SoldoutStatusApiCaller caller = new SoldoutStatusApiCaller(
            transport,
            new XmlMapper(),
            new FakeApiTransactionLogRepository(),
            duration -> { }
        );

        var response = caller.call("https://example.test/Goods_Soldout_Status.php", "goodsNo=10000001&soldOutFl=y", "10000001");

        assertThat(response.success()).isTrue();
        assertThat(transport.calls).isEqualTo(2);
    }

    @Test
    void should_throw_when_429_happens_twice() {
        SequenceTransport transport = new SequenceTransport()
            .addFailure(HttpStatus.TOO_MANY_REQUESTS, "rate limit")
            .addFailure(HttpStatus.TOO_MANY_REQUESTS, "rate limit");
        SoldoutStatusApiCaller caller = new SoldoutStatusApiCaller(
            transport,
            new XmlMapper(),
            new FakeApiTransactionLogRepository(),
            duration -> { }
        );

        assertThatThrownBy(() -> caller.call("https://example.test/Goods_Soldout_Status.php", "goodsNo=10000001&soldOutFl=y", "10000001"))
            .isInstanceOf(GodoApiException.class);
    }

    @Test
    void should_throw_immediately_when_rate_limit_header_is_exhausted() {
        SequenceTransport transport = new SequenceTransport()
            .addSuccess("""
                <response>
                  <header>
                    <code>000</code>
                    <message>success</message>
                  </header>
                </response>
                """, Map.of("ratelimit-available-level", "EXHAUSTED"));
        SoldoutStatusApiCaller caller = new SoldoutStatusApiCaller(
            transport,
            new XmlMapper(),
            new FakeApiTransactionLogRepository(),
            duration -> { }
        );

        assertThatThrownBy(() -> caller.call("https://example.test/Goods_Soldout_Status.php", "goodsNo=10000001&soldOutFl=y", "10000001"))
            .isInstanceOf(GodoApiException.class);
    }

    @Test
    void should_throw_godo_api_exception_when_transport_fails_before_response_is_available() {
        SoldoutStatusApiCaller caller = new SoldoutStatusApiCaller(
            new SequenceTransport().addRuntimeFailure(new ResourceAccessException("connection failed")),
            new XmlMapper(),
            new FakeApiTransactionLogRepository(),
            duration -> { }
        );

        assertThatThrownBy(() -> caller.call("https://example.test/Goods_Soldout_Status.php", "goodsNo=10000001&soldOutFl=y", "10000001"))
            .isInstanceOf(GodoApiException.class);
    }

    private static final class SequenceTransport implements GodoTransport {
        private final Deque<Step> steps = new ArrayDeque<>();
        private int calls;

        private SequenceTransport addSuccess(String body, Map<String, String> headers) {
            steps.add(new Step(body, headers, null));
            return this;
        }

        private SequenceTransport addFailure(HttpStatus status, String body) {
            steps.add(new Step(body, Map.of(), new RestClientResponseException(
                "error", status.value(), status.getReasonPhrase(), null, body.getBytes(), null
            )));
            return this;
        }

        private SequenceTransport addRuntimeFailure(RuntimeException exception) {
            steps.add(new Step("", Map.of(), exception));
            return this;
        }

        @Override
        public GodoTransportResponse postForm(String url, String requestBody) {
            calls++;
            Step step = steps.removeFirst();
            if (step.exception != null) {
                throw step.exception;
            }
            return new GodoTransportResponse(step.body, step.headers);
        }
    }

    private record Step(String body, Map<String, String> headers, RuntimeException exception) {
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
