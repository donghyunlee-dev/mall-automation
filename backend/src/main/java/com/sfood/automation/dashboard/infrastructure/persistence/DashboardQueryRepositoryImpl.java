package com.sfood.automation.dashboard.infrastructure.persistence;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.model.GradeDetail;
import com.sfood.automation.dashboard.domain.model.GradeRevenue;
import com.sfood.automation.dashboard.domain.model.HourlyRevenue;
import com.sfood.automation.dashboard.domain.model.TopItem;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardQueryRepositoryImpl implements DashboardQueryRepository {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final QGodoOrderEntity ORDER = QGodoOrderEntity.godoOrderEntity;
    private static final QGodoOrderItemEntity ITEM = QGodoOrderItemEntity.godoOrderItemEntity;

    private final JPAQueryFactory queryFactory;

    public DashboardQueryRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public DashboardSummary summaryByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
        String start = startInclusive.atStartOfDay().format(DATE_TIME);
        String end = endInclusive.atTime(23, 59, 59).format(DATE_TIME);

        List<Long> orderNos = queryFactory
            .select(ORDER.orderNo)
            .from(ORDER)
            .where(ORDER.orderedAt.between(start, end))
            .fetch();

        if (orderNos.isEmpty()) {
            return DashboardSummary.empty();
        }
        return summarize(orderNos, () -> queryFactory
            .select(ITEM.orderNo, ITEM.productName.countDistinct(), ITEM.productQty.sum())
            .from(ITEM)
            .where(ITEM.orderNo.in(orderNos))
            .groupBy(ITEM.orderNo)
            .fetch());
    }

    @Override
    public DashboardSummary summaryByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
        String start = startInclusive.toString();
        String end = endInclusive.toString();

        List<Long> orderNos = queryFactory
            .select(ITEM.orderNo).distinct()
            .from(ITEM)
            .where(ITEM.omsShippedAt.between(start, end))
            .fetch();

        if (orderNos.isEmpty()) {
            return DashboardSummary.empty();
        }
        return summarize(orderNos, () -> queryFactory
            .select(ITEM.orderNo, ITEM.productName.countDistinct(), ITEM.productQty.sum())
            .from(ITEM)
            .where(ITEM.orderNo.in(orderNos).and(ITEM.omsShippedAt.between(start, end)))
            .groupBy(ITEM.orderNo)
            .fetch());
    }

    @Override
    public List<HourlyRevenue> hourlyRevenue(LocalDate date) {
        String start = date.atStartOfDay().format(DATE_TIME);
        String end = date.atTime(23, 59, 59).format(DATE_TIME);

        List<Tuple> rows = queryFactory
            .select(ORDER.orderedAt.substring(11, 13), ORDER.paymentFinalAmount.sum())
            .from(ORDER)
            .where(ORDER.orderedAt.between(start, end))
            .groupBy(ORDER.orderedAt.substring(11, 13))
            .fetch();

        Map<Integer, BigDecimal> byHour = new HashMap<>();
        for (Tuple row : rows) {
            byHour.put(Integer.parseInt(row.get(0, String.class)), row.get(1, BigDecimal.class));
        }

        List<HourlyRevenue> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            result.add(new HourlyRevenue(hour, byHour.getOrDefault(hour, BigDecimal.ZERO)));
        }
        return result;
    }

    @Override
    public List<GradeRevenue> gradeRevenueByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
        String start = startInclusive.atStartOfDay().format(DATE_TIME);
        String end = endInclusive.atTime(23, 59, 59).format(DATE_TIME);
        return rollUpGradeRevenue(queryFactory
            .select(ORDER.memberGrade, ORDER.paymentFinalAmount.sum())
            .from(ORDER)
            .where(ORDER.orderedAt.between(start, end))
            .groupBy(ORDER.memberGrade)
            .fetch());
    }

    @Override
    public List<GradeRevenue> gradeRevenueByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
        List<Long> orderNos = shippedOrderNos(startInclusive, endInclusive);
        if (orderNos.isEmpty()) {
            return List.of();
        }
        return rollUpGradeRevenue(queryFactory
            .select(ORDER.memberGrade, ORDER.paymentFinalAmount.sum())
            .from(ORDER)
            .where(ORDER.orderNo.in(orderNos))
            .groupBy(ORDER.memberGrade)
            .fetch());
    }

    @Override
    public List<GradeDetail> gradeDetailByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
        String start = startInclusive.atStartOfDay().format(DATE_TIME);
        String end = endInclusive.atTime(23, 59, 59).format(DATE_TIME);
        List<Long> orderNos = queryFactory.select(ORDER.orderNo).from(ORDER).where(ORDER.orderedAt.between(start, end)).fetch();
        return buildGradeDetail(orderNos, () -> queryFactory
            .select(ITEM.orderNo, ITEM.productName.countDistinct(), ITEM.productQty.sum())
            .from(ITEM)
            .where(ITEM.orderNo.in(orderNos))
            .groupBy(ITEM.orderNo)
            .fetch());
    }

    @Override
    public List<GradeDetail> gradeDetailByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
        List<Long> orderNos = shippedOrderNos(startInclusive, endInclusive);
        String start = startInclusive.toString();
        String end = endInclusive.toString();
        return buildGradeDetail(orderNos, () -> queryFactory
            .select(ITEM.orderNo, ITEM.productName.countDistinct(), ITEM.productQty.sum())
            .from(ITEM)
            .where(ITEM.orderNo.in(orderNos).and(ITEM.omsShippedAt.between(start, end)))
            .groupBy(ITEM.orderNo)
            .fetch());
    }

    @Override
    public List<TopItem> topItemsByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
        String start = startInclusive.atStartOfDay().format(DATE_TIME);
        String end = endInclusive.atTime(23, 59, 59).format(DATE_TIME);
        List<Long> orderNos = queryFactory.select(ORDER.orderNo).from(ORDER).where(ORDER.orderedAt.between(start, end)).fetch();
        if (orderNos.isEmpty()) {
            return List.of();
        }
        return toTopItems(queryFactory
            .select(ITEM.productName, ITEM.productLineRevenue.sum(), ITEM.productQty.sum())
            .from(ITEM)
            .where(ITEM.orderNo.in(orderNos))
            .groupBy(ITEM.productName)
            .orderBy(ITEM.productLineRevenue.sum().desc())
            .fetch());
    }

    @Override
    public List<TopItem> topItemsByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
        String start = startInclusive.toString();
        String end = endInclusive.toString();
        return toTopItems(queryFactory
            .select(ITEM.productName, ITEM.productLineRevenue.sum(), ITEM.productQty.sum())
            .from(ITEM)
            .where(ITEM.omsShippedAt.between(start, end))
            .groupBy(ITEM.productName)
            .orderBy(ITEM.productLineRevenue.sum().desc())
            .fetch());
    }

    private List<Long> shippedOrderNos(LocalDate startInclusive, LocalDate endInclusive) {
        String start = startInclusive.toString();
        String end = endInclusive.toString();
        return queryFactory
            .select(ITEM.orderNo).distinct()
            .from(ITEM)
            .where(ITEM.omsShippedAt.between(start, end))
            .fetch();
    }

    /**
     * SQL 레벨에서는 원시 등급(member_grade)으로만 GROUP BY하고, "사업자회원_*" 롤업은 여기서 처리한다.
     * PostgreSQL은 SELECT의 CASE 표현식과 GROUP BY의 CASE 표현식을 동일하게 인식하지 못해
     * "column must appear in the GROUP BY clause" 오류를 내므로(SQLite는 이 문제가 없어 로컬 테스트로는
     * 잡히지 않았다), CASE를 SQL GROUP BY 대상에서 제외하고 원시 컬럼으로만 그룹핑한다.
     */
    private List<GradeRevenue> rollUpGradeRevenue(List<Tuple> rows) {
        Map<String, BigDecimal> revenueByGrade = new LinkedHashMap<>();
        for (Tuple row : rows) {
            String grade = rollUpGradeName(row.get(ORDER.memberGrade));
            revenueByGrade.merge(grade, row.get(ORDER.paymentFinalAmount.sum()), BigDecimal::add);
        }
        return revenueByGrade.entrySet().stream()
            .map(entry -> new GradeRevenue(entry.getKey(), entry.getValue()))
            .toList();
    }

    private String rollUpGradeName(String rawGrade) {
        return rawGrade != null && rawGrade.contains("사업자") ? "사업자회원" : rawGrade;
    }

    /** 원본 화면의 "회원등급명_요약" 로직: "사업자"가 포함된 세부 등급은 전부 "사업자회원"으로 합산한다. */
    private StringExpression rolledUpGrade() {
        return new CaseBuilder()
            .when(ORDER.memberGrade.contains("사업자")).then("사업자회원")
            .otherwise(ORDER.memberGrade);
    }

    /**
     * 구매자 구분 키. member_id가 있으면 그대로 쓰고, 없으면(비회원) 주문번호를 대신 써서
     * 서로 다른 비회원 주문을 별개의 구매자로 센다(원본의 "구분자_비회원포함"을 이름/전화번호 없이 근사).
     */
    private StringExpression buyerKey() {
        return new CaseBuilder()
            .when(ORDER.memberId.eq(""))
            .then(ORDER.orderNo.stringValue())
            .otherwise(ORDER.memberId);
    }

    private List<TopItem> toTopItems(List<Tuple> rows) {
        return rows.stream()
            .map(row -> new TopItem(
                row.get(ITEM.productName),
                row.get(ITEM.productLineRevenue.sum()),
                row.get(ITEM.productQty.sum()).longValue()
            ))
            .toList();
    }

    private List<GradeDetail> buildGradeDetail(List<Long> orderNos, java.util.function.Supplier<List<Tuple>> perOrderItemStatsQuery) {
        if (orderNos.isEmpty()) {
            return List.of();
        }

        StringExpression gradeExpr = rolledUpGrade();
        List<Tuple> orderRows = queryFactory
            .select(ORDER.orderNo, gradeExpr, ORDER.memberId, ORDER.paymentFinalAmount)
            .from(ORDER)
            .where(ORDER.orderNo.in(orderNos))
            .fetch();

        Map<Long, String> orderGrade = new HashMap<>();
        Map<String, BigDecimal> revenueByGrade = new LinkedHashMap<>();
        Map<String, Long> orderCountByGrade = new HashMap<>();
        Map<String, Set<String>> buyersByGrade = new HashMap<>();

        for (Tuple row : orderRows) {
            long orderNo = row.get(ORDER.orderNo);
            String gradeValue = row.get(gradeExpr);
            String memberId = row.get(ORDER.memberId);
            String buyerKey = memberId == null || memberId.isEmpty() ? String.valueOf(orderNo) : memberId;
            BigDecimal revenue = row.get(ORDER.paymentFinalAmount);
            orderGrade.put(orderNo, gradeValue);
            revenueByGrade.merge(gradeValue, revenue, BigDecimal::add);
            orderCountByGrade.merge(gradeValue, 1L, Long::sum);
            buyersByGrade.computeIfAbsent(gradeValue, g -> new HashSet<>()).add(buyerKey);
        }

        Map<String, List<Long>> skuByGrade = new HashMap<>();
        Map<String, List<Integer>> qtyByGrade = new HashMap<>();
        for (Tuple row : perOrderItemStatsQuery.get()) {
            long orderNo = row.get(ITEM.orderNo);
            String grade = orderGrade.get(orderNo);
            if (grade == null) {
                continue;
            }
            skuByGrade.computeIfAbsent(grade, g -> new ArrayList<>()).add(row.get(ITEM.productName.countDistinct()));
            qtyByGrade.computeIfAbsent(grade, g -> new ArrayList<>()).add(row.get(ITEM.productQty.sum()));
        }

        List<GradeDetail> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : revenueByGrade.entrySet()) {
            String grade = entry.getKey();
            double avgSku = skuByGrade.getOrDefault(grade, List.of()).stream().mapToLong(Long::longValue).average().orElse(0);
            double avgQty = qtyByGrade.getOrDefault(grade, List.of()).stream().mapToInt(Integer::intValue).average().orElse(0);
            result.add(new GradeDetail(
                grade,
                entry.getValue(),
                orderCountByGrade.get(grade),
                buyersByGrade.get(grade).size(),
                avgSku,
                avgQty
            ));
        }
        return result;
    }

    private DashboardSummary summarize(List<Long> orderNos, java.util.function.Supplier<List<Tuple>> perOrderItemStatsQuery) {
        StringExpression buyerKey = buyerKey();
        Tuple headerAgg = queryFactory
            .select(ORDER.paymentFinalAmount.sum(), ORDER.orderNo.count(), buyerKey.countDistinct())
            .from(ORDER)
            .where(ORDER.orderNo.in(orderNos))
            .fetchOne();

        BigDecimal revenue = headerAgg.get(ORDER.paymentFinalAmount.sum());
        long orderCount = headerAgg.get(ORDER.orderNo.count());
        long buyerCount = headerAgg.get(buyerKey.countDistinct());

        List<Tuple> perOrderItemStats = perOrderItemStatsQuery.get();
        double avgSku = perOrderItemStats.stream()
            .mapToLong(row -> row.get(ITEM.productName.countDistinct()))
            .average()
            .orElse(0);
        double avgQty = perOrderItemStats.stream()
            .mapToInt(row -> row.get(ITEM.productQty.sum()))
            .average()
            .orElse(0);

        return new DashboardSummary(revenue == null ? BigDecimal.ZERO : revenue, orderCount, buyerCount, avgSku, avgQty);
    }
}
