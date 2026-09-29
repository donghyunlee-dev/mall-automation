# dashboard-query-api Plan

## Summary

- 먼저 `summary` 1개를 수직 슬라이스로 완성해 뼈대(period 해석, 조회 전용 Repository 분리, QueryDSL 패턴)를 확립한 뒤, 같은 패턴으로 나머지 6개 API를 이어서 완성했다.

## Impacted Modules

- `com.sfood.automation.dashboard.domain.model`: `DashboardPeriod`, `DashboardSummary`, `GradeRevenue`, `GradeDetail`, `TopItem`, `HourlyRevenue`, `GradePivotMetric`(전부 신규)
- `com.sfood.automation.dashboard.domain.port`: `DashboardQueryRepository`(신규, 조회 전용 — 기존 `GodoOrderRepository`/`GodoOrderItemRepository`와 별도, 총 9개 메서드)
- `com.sfood.automation.dashboard.application.support`: `DashboardPeriodResolver`(신규, period 해석 + 버킷 범위 계산 재사용 컴포넌트), `DashboardQueryException`(신규)
- `com.sfood.automation.dashboard.application.usecase`: `GetDashboardSummaryUseCase`, `GetDashboardHourlyRevenueUseCase`, `GetDashboardGradeRevenueUseCase`, `GetDashboardGradeDetailUseCase`, `GetDashboardTopItemsUseCase`, `GetDashboardRevenueTrendUseCase`, `GetDashboardGradePivotUseCase`, `GetDashboardYoyComparisonUseCase`(전부 신규, 7개 API 1:1)
- `com.sfood.automation.dashboard.application.dto`: `DashboardSummaryResponse`, `GradeDetailResponse`, `DashboardTrendPointResponse`, `GradePivotResponse`, `YoyComparisonResponse`(전부 신규)
- `com.sfood.automation.dashboard.infrastructure.persistence`: `DashboardQueryRepositoryImpl`(신규, QueryDSL, 9개 메서드 구현)
- `com.sfood.automation.dashboard.presentation`: `DashboardController`(신규, 엔드포인트 7개)
- `com.sfood.automation.common.exception`: `ErrorCode`에 `DASHBOARD_QUERY_INVALID` 추가

## Architecture Approach

- 기존 `GodoOrderRepository`(command/upsert 용도)와 신규 `DashboardQueryRepository`(조회 전용)를 분리 — CQRS-lite. 이 코드베이스에 처음 도입하는 패턴이라 향후 다른 도메인에도 참고가 될 수 있음(별도 논의 없이 우선 dashboard 모듈에만 적용).
- `DashboardPeriodResolver`를 `@Component`로 만들어 UseCase가 주입받아 쓰도록 — 7개 API의 UseCase가 전부 이 컴포넌트를 재사용.
- 파라미터 검증 실패는 `DashboardQueryException`(→ `BusinessException` → 기존 `GlobalExceptionHandler.handleBusinessException`)으로 통일 — 신규 글로벌 핸들러 불필요.

## Data And API Impact

- `GET /api/dashboard/summary?period={DAILY|WEEKLY|MONTHLY|QUARTERLY}&date=...&yearMonth=...&year=...&quarter=...`
- `GET /api/dashboard/hourly-revenue?date=...`
- `GET /api/dashboard/grade-revenue?period=...&date/yearMonth/year+quarter=...`
- `GET /api/dashboard/grade-detail?period=...&...`
- `GET /api/dashboard/top-items?period=...&...&limit=6`
- `GET /api/dashboard/revenue-trend?bucket={DAY|WEEK|MONTH|QUARTER}&count=20&endDate=...`
- `GET /api/dashboard/grade-pivot?metric={REVENUE|ORDER_COUNT|BUYER_COUNT|AVG_SKU|AVG_QTY|AOV|ARPPU}&bucket=...&count=...&endDate=...`
- `GET /api/dashboard/yoy-comparison?period={MONTHLY|QUARTERLY}&yearMonth=...&year=...&quarter=...`
- DB 스키마 변경 없음(기존 `godo_order`/`godo_order_item` 그대로 사용).
- QueryDSL Q-클래스는 이미 `build.gradle`에 의존성이 있었고(다른 도메인에서 이미 사용 중), `godo_order`/`godo_order_item` 엔티티의 Q-클래스도 자동 생성되어 있어 별도 빌드 설정 변경 불필요.

## Validation Plan

- TDD: `summary`(Repository → Resolver → UseCase → Controller)를 먼저 완성해 패턴을 확립한 뒤, 나머지 6개 API도 각각 Repository 메서드 → UseCase → Controller 순서로 실패 테스트 작성 후 구현.
- Repository 테스트는 실제 SQLite에 데이터를 저장한 뒤 QueryDSL 쿼리 결과를 검증(통합 테스트에 가까움).
- 핵심 로직(날짜 범위 필터, 등급 매핑, 잠봉 롤업)은 의도적으로 깨뜨려 테스트가 실제로 실패하는지 확인(뮤테이션 점검) — 총 3회(요약 필터, 등급 매핑, 상단에서 이미 적용) 수행.
- **테스트 실행 범위**: `docs/guide/testing-strategy.md`의 "Command Scope By Target Branch"에 따라, 이번 develop 대상 작업에서는 `--tests "com.sfood.automation.dashboard.*"` + 영향받은 `com.sfood.automation.batch.*`만 실행한다(총 102 tests). 전체 스위트는 main 반영 시점에 실행.

## Risks

- Risk 1: `godo_order`/`godo_order_item`에 실제 데이터가 없어(아직 `order-sync`가 `enabled=0`) 운영 환경에서 이 API들이 항상 빈 응답만 반환한다 — 3번 단계(라이브 검증 + 활성화)가 선행되지 않으면 실사용 불가.
- Risk 2: 월간/분기 집계의 "주문 단위 dedup" 규칙(AC2)은 성능과 정확성 사이의 트레이드오프가 있어, 실제 데이터 볼륨에서 성능 재검증이 필요할 수 있음(9.6 열린 질문과 동일).
- Risk 3: "선물" 제외 로직이 `top-items`에만 적용되고 주문 헤더 매출(summary 등)에는 반영되지 않아, 원본 Streamlit과 완전히 동일한 숫자가 나오지 않을 수 있음(spec.md Assumptions 참고, 현업 확인 필요).
- Risk 4: `revenue-trend`/`grade-pivot`은 버킷마다 별도 쿼리를 호출하는 방식이라(예: 8분기 조회 시 쿼리 8회), 매우 많은 버킷을 요청하면 응답 시간이 늘어날 수 있음 — 실제 데이터 볼륨 확인 전까지는 최적화하지 않기로 함(Out of Scope).

## Rollback Considerations

- 신규 파일만 추가되었고, `ErrorCode`만 기존 파일에 상수 1개 추가(다른 코드에 영향 없음). 되돌리려면 `com.sfood.automation.dashboard` 패키지의 신규 파일들과 `ErrorCode`의 추가된 라인만 제거하면 됨.
