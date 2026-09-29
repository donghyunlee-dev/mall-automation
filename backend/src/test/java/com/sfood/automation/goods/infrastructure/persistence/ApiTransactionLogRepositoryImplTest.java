package com.sfood.automation.goods.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ApiTransactionLogRepositoryImpl.class)
class ApiTransactionLogRepositoryImplTest {

    @Autowired
    private ApiTransactionLogJpaRepository apiTransactionLogJpaRepository;

    @Autowired
    private ApiTransactionLogRepositoryImpl apiTransactionLogRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @AfterEach
    void tearDown() {
        apiTransactionLogJpaRepository.deleteAll();
    }

    @Test
    void should_save_request_log_before_response() {
        ApiTransactionLog saved = apiTransactionLogRepository.save(ApiTransactionLog.pending(
            "GOODS_SEARCH",
            "10000001",
            "https://example.test/Goods_Search.php",
            "goodsNo=10000001",
            Instant.parse("2026-03-31T10:00:00Z")
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.resultCode()).isNull();
        assertThat(saved.isSuccess()).isFalse();
    }

    @Test
    void should_update_response_fields_after_call() {
        ApiTransactionLog saved = apiTransactionLogRepository.save(ApiTransactionLog.pending(
            "GOODS_SEARCH",
            "10000001",
            "https://example.test/Goods_Search.php",
            "goodsNo=10000001",
            Instant.parse("2026-03-31T10:00:00Z")
        ));

        ApiTransactionLog updated = apiTransactionLogRepository.updateResponse(
            saved.id(),
            "<response/>",
            "000",
            "success",
            true,
            Instant.parse("2026-03-31T10:00:01Z")
        );

        assertThat(updated.resultCode()).isEqualTo("000");
        assertThat(updated.resultMessage()).isEqualTo("success");
        assertThat(updated.isSuccess()).isTrue();
        assertThat(updated.respondedAt()).isEqualTo(Instant.parse("2026-03-31T10:00:01Z"));
    }
}
