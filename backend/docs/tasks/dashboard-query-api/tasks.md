# dashboard-query-api Tasks

## Implementation Tasks

### 공통 뼈대
- [x] `DashboardPeriod` enum, `DashboardSummary` 도메인 모델 작성
- [x] `DashboardQueryRepository` 포트 작성(최종 9개 메서드)
- [x] `DashboardPeriodResolver` — period 해석 + `TrendBucketUnit`(DAY/WEEK/MONTH/QUARTER) 버킷 범위 계산
- [x] `DashboardQueryException` + `ErrorCode.DASHBOARD_QUERY_INVALID` 추가

### API별 구현 (7/7)
- [x] `GET /api/dashboard/summary` — `GetDashboardSummaryUseCase`
- [x] `GET /api/dashboard/hourly-revenue` — `GetDashboardHourlyRevenueUseCase`
- [x] `GET /api/dashboard/grade-revenue` — `GetDashboardGradeRevenueUseCase`
- [x] `GET /api/dashboard/grade-detail` — `GetDashboardGradeDetailUseCase` + `GradeDetailResponse`
- [x] `GET /api/dashboard/top-items` — `GetDashboardTopItemsUseCase`(선물 제외 + 잠봉 롤업)
- [x] `GET /api/dashboard/revenue-trend` — `GetDashboardRevenueTrendUseCase` + `DashboardTrendPointResponse`
- [x] `GET /api/dashboard/grade-pivot` — `GetDashboardGradePivotUseCase` + `GradePivotResponse`
- [x] `GET /api/dashboard/yoy-comparison` — `GetDashboardYoyComparisonUseCase` + `YoyComparisonResponse`
- [x] `DashboardController`에 7개 엔드포인트 전부 연결

## Test Tasks

- [x] `DashboardQueryRepositoryImplTest` — 9개 케이스(요약 2종 + 등급 3종 + 인기품목 1종 + 시간대별 1종, 기존 2종 포함)
  - [x] 날짜 범위 필터 뮤테이션 점검(RED 확인 후 원복)
  - [x] 등급 매핑(`orderGrade.get(orderNo)`) 뮤테이션 점검(RED 확인 후 원복)
- [x] `DashboardPeriodResolverTest` — 11개 케이스(기존 7개 + 버킷 계산 4종: 일/주/월/분기 경계)
- [x] `GetDashboardSummaryUseCaseTest` — 3개 케이스
- [x] `GetDashboardTopItemsUseCaseTest` — 4개 케이스(선물 제외, 잠봉 롤업, 한정판 제외, 정렬+limit)
- [x] `GetDashboardRevenueTrendUseCaseTest` — 3개 케이스(basis 분기, 버킷 순서)
- [x] `GetDashboardGradePivotUseCaseTest` — 2개 케이스(매트릭스 0채움, metric 추출)
- [x] `GetDashboardYoyComparisonUseCaseTest` — 4개 케이스(월/분기 범위 계산, 일/주 거부, 응답 매핑)
- [x] `DashboardControllerTest` — 8개 케이스(기존 3개 + 신규 4개 엔드포인트 + 기존 것 유지)
- [x] 타깃 테스트 스코프(`dashboard.*` + `batch.*`) 총 102 tests, 0 failed 확인 — 새 testing-strategy 지침에 따름(전체 스위트는 main 반영 시)

## Documentation Tasks

- [x] spec/plan/tasks 문서를 7개 API 전체 범위로 갱신
- [x] test-result/result는 최종 테스트 결과 확인 후 마무리

## Progress Notes

- `summary`를 먼저 완성해 패턴(Repository 포트 분리, QueryDSL, PeriodResolver 재사용)을 확립한 뒤, 나머지 6개는 같은 패턴을 그대로 복제해 빠르게 구현했다.
- Repository 계층은 뮤테이션 점검으로 실효성을 2차례 확인했다(날짜 필터, 등급 매핑). UseCase/Controller 계층은 클래스·메서드 미존재로 인한 컴파일 실패(RED)를 실제로 관찰한 뒤 구현했다.
- `revenue-trend`/`grade-pivot`/`yoy-comparison`은 신규 Repository 메서드를 추가하지 않고 기존 `summary*`/`gradeDetail*` 메서드를 버킷 단위로 반복 호출하는 방식으로 구현해 Repository 계층의 복잡도를 늘리지 않았다.

## Review Notes

- "선물" 제외 로직은 `top-items`에만 적용되어 있고 주문 헤더 매출 집계에는 미반영 — spec.md Assumptions/plan.md Risk 3에 기록.
- `revenue-trend`/`grade-pivot`의 버킷별 반복 쿼리 방식은 실제 데이터 볼륨에서 성능 재검증이 필요할 수 있음(plan.md Risk 4).
