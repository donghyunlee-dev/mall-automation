package com.sfood.automation.dashboard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.dashboard.domain.model.GodoOrder;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.math.BigDecimal;
import java.util.Optional;
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
@Import(GodoOrderRepositoryImpl.class)
class GodoOrderRepositoryImplTest {

    @Autowired
    private GodoOrderJpaRepository godoOrderJpaRepository;

    @Autowired
    private GodoOrderRepositoryImpl godoOrderRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @AfterEach
    void tearDown() {
        godoOrderJpaRepository.deleteAll();
    }

    @Test
    void should_insert_new_order_when_order_no_not_found() {
        GodoOrder order = newOrder(1001L, "결제완료", new BigDecimal("10000"));

        godoOrderRepository.upsert(order);

        Optional<GodoOrder> found = godoOrderRepository.findByOrderNo(1001L);
        assertThat(found).isPresent();
        assertThat(found.get().orderStatus()).isEqualTo("결제완료");
        assertThat(found.get().paymentFinalAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void should_update_existing_row_instead_of_inserting_duplicate_when_order_no_already_exists() {
        godoOrderRepository.upsert(newOrder(1001L, "결제완료", new BigDecimal("10000")));

        godoOrderRepository.upsert(newOrder(1001L, "배송완료", new BigDecimal("10000")));

        assertThat(godoOrderJpaRepository.findAll()).hasSize(1);
        Optional<GodoOrder> found = godoOrderRepository.findByOrderNo(1001L);
        assertThat(found).isPresent();
        assertThat(found.get().orderStatus()).isEqualTo("배송완료");
    }

    @Test
    void should_return_empty_when_order_no_not_found() {
        Optional<GodoOrder> found = godoOrderRepository.findByOrderNo(999L);

        assertThat(found).isEmpty();
    }

    private GodoOrder newOrder(long orderNo, String orderStatus, BigDecimal paymentFinalAmount) {
        return new GodoOrder(
            null,
            orderNo,
            "member01",
            "일반회원",
            orderStatus,
            "2026-03-24T17:52:56",
            new BigDecimal("9000"),
            new BigDecimal("1000"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            paymentFinalAmount,
            "2026-03-24T17:52:56",
            "2026-03-24T17:52:56"
        );
    }
}
