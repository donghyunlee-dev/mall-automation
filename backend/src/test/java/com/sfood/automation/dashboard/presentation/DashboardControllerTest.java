package com.sfood.automation.dashboard.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.dashboard.application.dto.DashboardSummaryResponse;
import com.sfood.automation.dashboard.application.dto.DashboardTrendPointResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.application.support.DashboardQueryException;
import com.sfood.automation.dashboard.application.usecase.GetDashboardGradeDetailUseCase;
import com.sfood.automation.dashboard.application.dto.GradePivotResponse;
import com.sfood.automation.dashboard.application.usecase.GetDashboardGradePivotUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardGradeRevenueUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardHourlyRevenueUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardJambongBreakdownUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardRevenueTrendUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardSummaryUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardTopItemsUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardYoyComparisonUseCase;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.DateBasis;
import com.sfood.automation.dashboard.domain.model.GradePivotMetric;
import com.sfood.automation.dashboard.domain.model.GradeRevenue;
import com.sfood.automation.dashboard.domain.model.HourlyRevenue;
import com.sfood.automation.dashboard.domain.model.TopItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetDashboardSummaryUseCase getDashboardSummaryUseCase;

    @MockBean
    private GetDashboardHourlyRevenueUseCase getDashboardHourlyRevenueUseCase;

    @MockBean
    private GetDashboardGradeRevenueUseCase getDashboardGradeRevenueUseCase;

    @MockBean
    private GetDashboardGradeDetailUseCase getDashboardGradeDetailUseCase;

    @MockBean
    private GetDashboardTopItemsUseCase getDashboardTopItemsUseCase;

    @MockBean
    private GetDashboardRevenueTrendUseCase getDashboardRevenueTrendUseCase;

    @MockBean
    private GetDashboardGradePivotUseCase getDashboardGradePivotUseCase;

    @MockBean
    private GetDashboardYoyComparisonUseCase getDashboardYoyComparisonUseCase;

    @MockBean
    private GetDashboardJambongBreakdownUseCase getDashboardJambongBreakdownUseCase;

    @Test
    void should_return_summary_for_daily_period() throws Exception {
        when(getDashboardSummaryUseCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null))
            .thenReturn(new DashboardSummaryResponse(new BigDecimal("10000"), 2, 2, 1.0, 1.5, 3.0, new BigDecimal("5000.00"), new BigDecimal("5000.00")));

        mockMvc.perform(get("/api/dashboard/summary")
                .param("period", "DAILY")
                .param("date", "2026-03-24")
                .session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revenue").value(10000))
            .andExpect(jsonPath("$.orderCount").value(2));
    }

    @Test
    void should_return_bad_request_when_required_param_missing() throws Exception {
        when(getDashboardSummaryUseCase.execute(any(), any(), any(), any(), any()))
            .thenThrow(new DashboardQueryException("DAILY 조회에는 date가 필요합니다."));

        mockMvc.perform(get("/api/dashboard/summary")
                .param("period", "DAILY")
                .session(authenticatedSession()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_reject_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                .param("period", "DAILY")
                .param("date", "2026-03-24"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_hourly_revenue() throws Exception {
        when(getDashboardHourlyRevenueUseCase.execute(LocalDate.of(2026, 3, 24)))
            .thenReturn(List.of(new HourlyRevenue(9, new BigDecimal("10000"))));

        mockMvc.perform(get("/api/dashboard/hourly-revenue")
                .param("date", "2026-03-24")
                .session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hour").value(9));
    }

    @Test
    void should_return_grade_revenue() throws Exception {
        when(getDashboardGradeRevenueUseCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null))
            .thenReturn(List.of(new GradeRevenue("VIP", new BigDecimal("10000"))));

        mockMvc.perform(get("/api/dashboard/grade-revenue")
                .param("period", "DAILY")
                .param("date", "2026-03-24")
                .session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].grade").value("VIP"));
    }

    @Test
    void should_return_top_items_with_limit_param() throws Exception {
        when(getDashboardTopItemsUseCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null, 3))
            .thenReturn(List.of(new TopItem("상품A", new BigDecimal("10000"), 2)));

        mockMvc.perform(get("/api/dashboard/top-items")
                .param("period", "DAILY")
                .param("date", "2026-03-24")
                .param("limit", "3")
                .session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productName").value("상품A"));
    }

    @Test
    void should_return_revenue_trend() throws Exception {
        when(getDashboardRevenueTrendUseCase.execute(DashboardPeriodResolver.TrendBucketUnit.DAY, 20, LocalDate.of(2026, 3, 24)))
            .thenReturn(List.of(new DashboardTrendPointResponse(LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24), new BigDecimal("10000"), 2)));

        mockMvc.perform(get("/api/dashboard/revenue-trend")
                .param("bucket", "DAY")
                .param("count", "20")
                .param("endDate", "2026-03-24")
                .session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderCount").value(2));
    }

    @Test
    void should_pass_date_basis_to_revenue_trend_use_case() throws Exception {
        when(getDashboardRevenueTrendUseCase.execute(DashboardPeriodResolver.TrendBucketUnit.DAY, 30, LocalDate.of(2026, 3, 24), DateBasis.SHIPPED))
            .thenReturn(List.of(new DashboardTrendPointResponse(LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 24), new BigDecimal("10000"), 2)));

        mockMvc.perform(get("/api/dashboard/revenue-trend")
                .param("bucket", "DAY")
                .param("count", "30")
                .param("endDate", "2026-03-24")
                .param("dateBasis", "SHIPPED")
                .session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderCount").value(2));
    }

    @Test
    void should_pass_date_basis_to_grade_pivot_use_case() throws Exception {
        when(getDashboardGradePivotUseCase.execute(
            GradePivotMetric.REVENUE, DashboardPeriodResolver.TrendBucketUnit.DAY, 30, LocalDate.of(2026, 3, 24), DateBasis.SHIPPED
        )).thenReturn(new GradePivotResponse(List.of(LocalDate.of(2026, 3, 24)), Map.of("VIP", List.of(new BigDecimal("10000")))));

        mockMvc.perform(get("/api/dashboard/grade-pivot")
                .param("metric", "REVENUE")
                .param("bucket", "DAY")
                .param("count", "30")
                .param("endDate", "2026-03-24")
                .param("dateBasis", "SHIPPED")
                .session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.byGrade.VIP[0]").value(10000));
    }

    @Test
    void should_return_jambong_breakdown() throws Exception {
        when(getDashboardJambongBreakdownUseCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null))
            .thenReturn(List.of(new TopItem("잠봉 슬라이스 500g (냉동)", new BigDecimal("10000"), 2)));

        mockMvc.perform(get("/api/dashboard/jambong-breakdown")
                .param("period", "DAILY")
                .param("date", "2026-03-24")
                .session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productName").value("잠봉 슬라이스 500g (냉동)"));
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        return session;
    }
}
