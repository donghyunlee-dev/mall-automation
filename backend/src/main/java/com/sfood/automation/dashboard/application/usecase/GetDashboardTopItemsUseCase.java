package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.TopItem;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 인기 품목 Top N. 8번 섹션 5항 결정에 따라 원본 Streamlit의 하드코딩 로직을 그대로 유지한다.
 *  - "선물"이 포함된 상품명은 집계에서 제외
 *  - "잠봉 슬라이스 500g/400g"(단, "한정" 포함 상품 제외)은 하나의 롤업 행으로 합산
 */
@Service
public class GetDashboardTopItemsUseCase {

    private static final String GIFT_KEYWORD = "선물";
    private static final Pattern JAMBONG_PATTERN = Pattern.compile("잠봉 슬라이스 500g|잠봉 슬라이스 400g");
    private static final String JAMBONG_EXCLUDE_KEYWORD = "한정";
    private static final String JAMBONG_ROLLUP_LABEL = "잠봉 슬라이스 (3종 총합)";

    private final DashboardQueryRepository dashboardQueryRepository;
    private final DashboardPeriodResolver periodResolver;

    public GetDashboardTopItemsUseCase(DashboardQueryRepository dashboardQueryRepository, DashboardPeriodResolver periodResolver) {
        this.dashboardQueryRepository = dashboardQueryRepository;
        this.periodResolver = periodResolver;
    }

    public List<TopItem> execute(DashboardPeriod period, LocalDate date, YearMonth yearMonth, Integer year, Integer quarter, int limit) {
        DashboardPeriodResolver.DateRange range = periodResolver.resolve(period, date, yearMonth, year, quarter);
        List<TopItem> raw = switch (period) {
            case DAILY, WEEKLY -> dashboardQueryRepository.topItemsByOrderDate(range.start(), range.end());
            case MONTHLY, QUARTERLY -> dashboardQueryRepository.topItemsByShippedDate(range.start(), range.end());
        };

        List<TopItem> withoutGifts = raw.stream()
            .filter(item -> !item.productName().contains(GIFT_KEYWORD))
            .toList();

        List<TopItem> jambong = withoutGifts.stream()
            .filter(this::isJambongRollupTarget)
            .toList();
        List<TopItem> others = withoutGifts.stream()
            .filter(item -> !isJambongRollupTarget(item))
            .toList();

        List<TopItem> result = new ArrayList<>(others);
        if (!jambong.isEmpty()) {
            BigDecimal revenue = jambong.stream().map(TopItem::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
            long qty = jambong.stream().mapToLong(TopItem::qty).sum();
            result.add(new TopItem(JAMBONG_ROLLUP_LABEL, revenue, qty));
        }

        return result.stream()
            .sorted(Comparator.comparing(TopItem::revenue).reversed())
            .limit(limit)
            .toList();
    }

    private boolean isJambongRollupTarget(TopItem item) {
        return JAMBONG_PATTERN.matcher(item.productName()).find() && !item.productName().contains(JAMBONG_EXCLUDE_KEYWORD);
    }
}
