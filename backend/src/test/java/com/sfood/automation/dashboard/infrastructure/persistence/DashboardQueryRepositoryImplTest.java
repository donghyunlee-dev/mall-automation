package com.sfood.automation.dashboard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.model.GradeDetail;
import com.sfood.automation.dashboard.domain.model.GradeRevenue;
import com.sfood.automation.dashboard.domain.model.HourlyRevenue;
import com.sfood.automation.dashboard.domain.model.TopItem;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
@Import(DashboardQueryRepositoryImpl.class)
class DashboardQueryRepositoryImplTest {

    @Autowired
    private GodoOrderJpaRepository godoOrderJpaRepository;

    @Autowired
    private GodoOrderItemJpaRepository godoOrderItemJpaRepository;

    @Autowired
    private DashboardQueryRepositoryImpl dashboardQueryRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @AfterEach
    void tearDown() {
        godoOrderItemJpaRepository.deleteAll();
        godoOrderJpaRepository.deleteAll();
    }

    @Test
    void should_summarize_by_order_date_within_range_only() {
        saveOrder(1L, "member01", "2026-03-24 10:00:00", "10000.00");
        saveItem(1L, 1L, "상품A", 1);
        saveItem(2L, 1L, "상품B", 2);
        saveOrder(2L, "member02", "2026-03-25 10:00:00", "20000.00"); // 범위 밖

        DashboardSummary summary = dashboardQueryRepository.summaryByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        assertThat(summary.revenue()).isEqualByComparingTo("10000.00");
        assertThat(summary.orderCount()).isEqualTo(1);
        assertThat(summary.buyerCount()).isEqualTo(1);
        assertThat(summary.avgSku()).isEqualTo(2.0); // 상품A, 상품B 2종
        assertThat(summary.avgQty()).isEqualTo(3.0); // 1 + 2
    }

    @Test
    void should_count_distinct_buyers_when_same_member_orders_multiple_times() {
        saveOrder(1L, "member01", "2026-03-24 09:00:00", "10000.00");
        saveOrder(2L, "member01", "2026-03-24 15:00:00", "5000.00");

        DashboardSummary summary = dashboardQueryRepository.summaryByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        assertThat(summary.orderCount()).isEqualTo(2);
        assertThat(summary.buyerCount()).isEqualTo(1);
        assertThat(summary.revenue()).isEqualByComparingTo("15000.00");
    }

    @Test
    void should_count_each_guest_order_as_a_distinct_buyer() {
        // member_id가 빈 문자열인 비회원 주문들은 서로 다른 구매자로 취급해야 한다(원본 update_data.py의
        // 구분자_비회원포함 로직과 동일한 취지 — 여기서는 회원명/전화번호 대신 order_no로 근사한다).
        saveOrder(1L, "", "2026-03-24 09:00:00", "10000.00");
        saveOrder(2L, "", "2026-03-24 10:00:00", "5000.00");

        DashboardSummary summary = dashboardQueryRepository.summaryByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        assertThat(summary.orderCount()).isEqualTo(2);
        assertThat(summary.buyerCount()).isEqualTo(2);
    }

    @Test
    void should_return_empty_summary_when_no_orders_in_range() {
        DashboardSummary summary = dashboardQueryRepository.summaryByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        assertThat(summary.orderCount()).isEqualTo(0);
        assertThat(summary.revenue()).isEqualByComparingTo("0");
    }

    @Test
    void should_summarize_by_shipped_date_counting_full_order_revenue_once_even_if_item_ships_in_range() {
        saveOrder(1L, "member01", "2026-03-24 10:00:00", "10000.00");
        saveItem(1L, 1L, "상품A", 1, "2026-03-31"); // 이 아이템만 3월에 출고

        DashboardSummary summary = dashboardQueryRepository.summaryByShippedDate(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)
        );

        assertThat(summary.orderCount()).isEqualTo(1);
        assertThat(summary.revenue()).isEqualByComparingTo("10000.00");
        assertThat(summary.avgSku()).isEqualTo(1.0);
    }

    @Test
    void should_exclude_order_from_shipped_summary_when_no_item_shipped_in_range() {
        saveOrder(1L, "member01", "2026-03-24 10:00:00", "10000.00");
        saveItem(1L, 1L, "상품A", 1, "2026-04-01"); // 4월 출고

        DashboardSummary summary = dashboardQueryRepository.summaryByShippedDate(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)
        );

        assertThat(summary.orderCount()).isEqualTo(0);
    }

    @Test
    void should_group_revenue_by_hour_of_order_date_and_ignore_other_days() {
        saveOrder(1L, "member01", "2026-03-24 09:15:00", "10000.00");
        saveOrder(2L, "member02", "2026-03-24 09:45:00", "5000.00");
        saveOrder(3L, "member03", "2026-03-24 14:00:00", "3000.00");
        saveOrder(4L, "member04", "2026-03-25 09:00:00", "9999.00"); // 다른 날

        List<HourlyRevenue> hourly = dashboardQueryRepository.hourlyRevenue(LocalDate.of(2026, 3, 24));

        Map<Integer, BigDecimal> byHour = hourly.stream()
            .collect(java.util.stream.Collectors.toMap(HourlyRevenue::hour, HourlyRevenue::revenue));
        assertThat(byHour.get(9)).isEqualByComparingTo("15000.00");
        assertThat(byHour.get(14)).isEqualByComparingTo("3000.00");
        assertThat(byHour.values().stream().noneMatch(v -> v.compareTo(new BigDecimal("9999.00")) == 0)).isTrue();
    }

    @Test
    void should_group_revenue_by_grade_for_order_date_range() {
        saveOrder(1L, "member01", "VIP", "2026-03-24 09:00:00", "10000.00");
        saveOrder(2L, "member02", "일반회원", "2026-03-24 10:00:00", "5000.00");
        saveOrder(3L, "member03", "VIP", "2026-03-24 11:00:00", "7000.00");

        List<GradeRevenue> result = dashboardQueryRepository.gradeRevenueByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        Map<String, BigDecimal> byGrade = result.stream()
            .collect(java.util.stream.Collectors.toMap(GradeRevenue::grade, GradeRevenue::revenue));
        assertThat(byGrade.get("VIP")).isEqualByComparingTo("17000.00");
        assertThat(byGrade.get("일반회원")).isEqualByComparingTo("5000.00");
    }

    @Test
    void should_roll_up_business_member_sub_grades_into_a_single_grade_for_revenue() {
        // 원본 화면의 "회원등급명_요약" 로직: "사업자"가 포함된 세부 등급은 전부 "사업자회원"으로 합산한다.
        saveOrder(1L, "member01", "사업자회원_화이트", "2026-03-24 09:00:00", "10000.00");
        saveOrder(2L, "member02", "사업자회원_로제", "2026-03-24 10:00:00", "7000.00");
        saveOrder(3L, "member03", "일반회원", "2026-03-24 11:00:00", "3000.00");

        List<GradeRevenue> result = dashboardQueryRepository.gradeRevenueByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        Map<String, BigDecimal> byGrade = result.stream()
            .collect(java.util.stream.Collectors.toMap(GradeRevenue::grade, GradeRevenue::revenue));
        assertThat(byGrade).doesNotContainKey("사업자회원_화이트");
        assertThat(byGrade).doesNotContainKey("사업자회원_로제");
        assertThat(byGrade.get("사업자회원")).isEqualByComparingTo("17000.00");
        assertThat(byGrade.get("일반회원")).isEqualByComparingTo("3000.00");
    }

    @Test
    void should_roll_up_business_member_sub_grades_into_a_single_grade_for_grade_detail() {
        saveOrder(1L, "member01", "사업자회원_화이트", "2026-03-24 09:00:00", "10000.00");
        saveOrder(2L, "member02", "사업자회원_로제", "2026-03-24 10:00:00", "7000.00");

        List<GradeDetail> result = dashboardQueryRepository.gradeDetailByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).grade()).isEqualTo("사업자회원");
        assertThat(result.get(0).orderCount()).isEqualTo(2);
        assertThat(result.get(0).buyerCount()).isEqualTo(2);
        assertThat(result.get(0).revenue()).isEqualByComparingTo("17000.00");
    }

    @Test
    void should_compute_grade_detail_with_basket_metrics_for_order_date_range() {
        saveOrder(1L, "member01", "VIP", "2026-03-24 09:00:00", "10000.00");
        saveItem(1L, 1L, "상품A", 2);
        saveItem(2L, 1L, "상품B", 1);

        List<GradeDetail> result = dashboardQueryRepository.gradeDetailByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        assertThat(result).hasSize(1);
        GradeDetail vip = result.get(0);
        assertThat(vip.grade()).isEqualTo("VIP");
        assertThat(vip.orderCount()).isEqualTo(1);
        assertThat(vip.buyerCount()).isEqualTo(1);
        assertThat(vip.avgSku()).isEqualTo(2.0);
        assertThat(vip.avgQty()).isEqualTo(3.0);
        assertThat(vip.revenue()).isEqualByComparingTo("10000.00");
    }

    @Test
    void should_rank_top_items_by_revenue_for_order_date_range() {
        saveOrder(1L, "member01", "2026-03-24 09:00:00", "0.00");
        saveItemWithRevenue(1L, 1L, "상품A", 2, "20000.00");
        saveItemWithRevenue(2L, 1L, "상품B", 1, "5000.00");

        List<TopItem> result = dashboardQueryRepository.topItemsByOrderDate(
            LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).productName()).isEqualTo("상품A");
        assertThat(result.get(0).revenue()).isEqualByComparingTo("20000.00");
        assertThat(result.get(0).qty()).isEqualTo(2);
        assertThat(result.get(1).productName()).isEqualTo("상품B");
    }

    private void saveOrder(long orderNo, String memberId, String orderedAt, String paymentFinalAmount) {
        saveOrder(orderNo, memberId, "일반회원", orderedAt, paymentFinalAmount);
    }

    private void saveOrder(long orderNo, String memberId, String grade, String orderedAt, String paymentFinalAmount) {
        GodoOrderEntity entity = new GodoOrderEntity();
        entity.setOrderNo(orderNo);
        entity.setMemberId(memberId);
        entity.setMemberGrade(grade);
        entity.setOrderStatus("s1");
        entity.setOrderedAt(orderedAt);
        entity.setTotalGoodsPrice(new BigDecimal(paymentFinalAmount));
        entity.setTotalDeliveryCharge(BigDecimal.ZERO);
        entity.setUseDeposit(BigDecimal.ZERO);
        entity.setUseMileage(BigDecimal.ZERO);
        entity.setTotalGoodsDcPrice(BigDecimal.ZERO);
        entity.setTotalMemberDcPrice(BigDecimal.ZERO);
        entity.setTotalCouponDcPrice(BigDecimal.ZERO);
        entity.setPaymentFinalAmount(new BigDecimal(paymentFinalAmount));
        entity.setCreatedAt("2026-03-24T00:00:00Z");
        entity.setUpdatedAt("2026-03-24T00:00:00Z");
        godoOrderJpaRepository.save(entity);
    }

    private void saveItem(long godoLineNo, long orderNo, String productName, int qty) {
        saveItem(godoLineNo, orderNo, productName, qty, "2026-03-25");
    }

    private void saveItem(long godoLineNo, long orderNo, String productName, int qty, String omsShippedAt) {
        GodoOrderItemEntity entity = new GodoOrderItemEntity();
        entity.setItemType("GOODS");
        entity.setGodoLineNo(godoLineNo);
        entity.setOrderNo(orderNo);
        entity.setProductName(productName);
        entity.setProductQty(qty);
        entity.setProductUnitPrice(BigDecimal.TEN);
        entity.setProductLineRevenue(BigDecimal.TEN.multiply(BigDecimal.valueOf(qty)));
        entity.setDeliveredAt("2026-03-24 10:00:00");
        entity.setOmsShippedAt(omsShippedAt);
        entity.setCreatedAt("2026-03-24T00:00:00Z");
        entity.setUpdatedAt("2026-03-24T00:00:00Z");
        godoOrderItemJpaRepository.save(entity);
    }

    private void saveItemWithRevenue(long godoLineNo, long orderNo, String productName, int qty, String lineRevenue) {
        GodoOrderItemEntity entity = new GodoOrderItemEntity();
        entity.setItemType("GOODS");
        entity.setGodoLineNo(godoLineNo);
        entity.setOrderNo(orderNo);
        entity.setProductName(productName);
        entity.setProductQty(qty);
        entity.setProductUnitPrice(new BigDecimal(lineRevenue).divide(BigDecimal.valueOf(qty)));
        entity.setProductLineRevenue(new BigDecimal(lineRevenue));
        entity.setDeliveredAt("2026-03-24 10:00:00");
        entity.setOmsShippedAt("2026-03-25");
        entity.setCreatedAt("2026-03-24T00:00:00Z");
        entity.setUpdatedAt("2026-03-24T00:00:00Z");
        godoOrderItemJpaRepository.save(entity);
    }
}
