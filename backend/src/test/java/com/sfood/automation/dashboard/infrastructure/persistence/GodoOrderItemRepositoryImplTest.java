package com.sfood.automation.dashboard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.dashboard.domain.model.GodoOrderItem;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.math.BigDecimal;
import java.util.List;
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
@Import(GodoOrderItemRepositoryImpl.class)
class GodoOrderItemRepositoryImplTest {

    @Autowired
    private GodoOrderItemJpaRepository godoOrderItemJpaRepository;

    @Autowired
    private GodoOrderItemRepositoryImpl godoOrderItemRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @AfterEach
    void tearDown() {
        godoOrderItemJpaRepository.deleteAll();
    }

    @Test
    void should_insert_new_item_when_type_and_line_no_not_found() {
        godoOrderItemRepository.upsert(newItem("GOODS", 449051L, 1001L, 1));

        List<GodoOrderItem> found = godoOrderItemRepository.findByOrderNo(1001L);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).productQty()).isEqualTo(1);
    }

    @Test
    void should_update_existing_row_instead_of_inserting_duplicate_when_type_and_line_no_already_exist() {
        godoOrderItemRepository.upsert(newItem("GOODS", 449051L, 1001L, 1));

        godoOrderItemRepository.upsert(newItem("GOODS", 449051L, 1001L, 3));

        assertThat(godoOrderItemJpaRepository.findAll()).hasSize(1);
        List<GodoOrderItem> found = godoOrderItemRepository.findByOrderNo(1001L);
        assertThat(found.get(0).productQty()).isEqualTo(3);
    }

    @Test
    void should_allow_same_line_no_for_different_item_types() {
        godoOrderItemRepository.upsert(newItem("GOODS", 1L, 1001L, 1));
        godoOrderItemRepository.upsert(newItem("ADD_GOODS", 1L, 1001L, 2));

        List<GodoOrderItem> found = godoOrderItemRepository.findByOrderNo(1001L);

        assertThat(found).hasSize(2);
    }

    private GodoOrderItem newItem(String itemType, long godoLineNo, long orderNo, int productQty) {
        return new GodoOrderItem(
            null,
            itemType,
            godoLineNo,
            orderNo,
            "리얼 스모크햄",
            productQty,
            new BigDecimal("11050"),
            new BigDecimal("11050").multiply(BigDecimal.valueOf(productQty)),
            "2026-03-25T09:38:10",
            "2026-03-26",
            "2026-03-24T17:52:56",
            "2026-03-24T17:52:56"
        );
    }
}
