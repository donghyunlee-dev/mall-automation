package com.sfood.automation.dashboard.domain.port;

import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.model.GradeDetail;
import com.sfood.automation.dashboard.domain.model.GradeRevenue;
import com.sfood.automation.dashboard.domain.model.HourlyRevenue;
import com.sfood.automation.dashboard.domain.model.TopItem;
import java.time.LocalDate;
import java.util.List;

public interface DashboardQueryRepository {

    /** 일간/주간 탭 — 주문일자(godo_order.ordered_at) 기준. */
    DashboardSummary summaryByOrderDate(LocalDate startInclusive, LocalDate endInclusive);

    /** 월간/분기 탭 — 출고일(godo_order_item.oms_shipped_at) 기준. */
    DashboardSummary summaryByShippedDate(LocalDate startInclusive, LocalDate endInclusive);

    /** 일간 탭 전용 — 0~23시 시간대별 매출(값이 없는 시간은 호출부에서 0으로 채움). */
    List<HourlyRevenue> hourlyRevenue(LocalDate date);

    List<GradeRevenue> gradeRevenueByOrderDate(LocalDate startInclusive, LocalDate endInclusive);

    List<GradeRevenue> gradeRevenueByShippedDate(LocalDate startInclusive, LocalDate endInclusive);

    List<GradeDetail> gradeDetailByOrderDate(LocalDate startInclusive, LocalDate endInclusive);

    List<GradeDetail> gradeDetailByShippedDate(LocalDate startInclusive, LocalDate endInclusive);

    /** 상품명(product_name) 기준 매출 Top N. 잠봉 슬라이스 롤업/선물 제외 등 상품 하드코딩 로직은 UseCase에서 후처리한다. */
    List<TopItem> topItemsByOrderDate(LocalDate startInclusive, LocalDate endInclusive);

    List<TopItem> topItemsByShippedDate(LocalDate startInclusive, LocalDate endInclusive);
}
