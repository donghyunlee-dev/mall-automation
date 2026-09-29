package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.TopItem;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 잠봉 슬라이스 판매 비중(R19). {@link GetDashboardTopItemsUseCase}는 잠봉 3개 SKU를 하나로 롤업하지만,
 * 이 UseCase는 반대로 롤업하지 않고 개별 SKU 매출/수량을 그대로 반환한다("한정" 상품 제외 규칙은 동일).
 */
@Service
public class GetDashboardJambongBreakdownUseCase {

    private static final Pattern JAMBONG_PATTERN = Pattern.compile("잠봉 슬라이스 500g|잠봉 슬라이스 400g");
    private static final String JAMBONG_EXCLUDE_KEYWORD = "한정";

    private final DashboardQueryRepository dashboardQueryRepository;
    private final DashboardPeriodResolver periodResolver;

    public GetDashboardJambongBreakdownUseCase(DashboardQueryRepository dashboardQueryRepository, DashboardPeriodResolver periodResolver) {
        this.dashboardQueryRepository = dashboardQueryRepository;
        this.periodResolver = periodResolver;
    }

    public List<TopItem> execute(DashboardPeriod period, LocalDate date, YearMonth yearMonth, Integer year, Integer quarter) {
        DashboardPeriodResolver.DateRange range = periodResolver.resolve(period, date, yearMonth, year, quarter);
        List<TopItem> raw = switch (period) {
            case DAILY, WEEKLY -> dashboardQueryRepository.topItemsByOrderDate(range.start(), range.end());
            case MONTHLY, QUARTERLY -> dashboardQueryRepository.topItemsByShippedDate(range.start(), range.end());
        };

        return raw.stream()
            .filter(this::isJambongSlice)
            .sorted(Comparator.comparing(TopItem::revenue).reversed())
            .toList();
    }

    private boolean isJambongSlice(TopItem item) {
        return JAMBONG_PATTERN.matcher(item.productName()).find() && !item.productName().contains(JAMBONG_EXCLUDE_KEYWORD);
    }
}
