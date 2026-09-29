# dashboard-query-api Test Result

## Scope

- 7개 대시보드 조회 API 전체(`summary`, `hourly-revenue`, `grade-revenue`, `grade-detail`, `top-items`, `revenue-trend`, `grade-pivot`, `yoy-comparison`)의 Repository/UseCase/Controller 단위·통합 테스트.
- `docs/guide/testing-strategy.md`의 "Command Scope By Target Branch" 지침에 따라, 이번 develop 대상 작업은 **타깃 테스트만 실행**했다(전체 스위트는 main 반영 시점에 별도 실행 예정).

## Environment

- Profile: `local` (기본 프로파일)
- Database: SQLite (`DashboardQueryRepositoryImplTest`는 `@DataJpaTest` + 실제 SQLite에 데이터 저장 후 QueryDSL 쿼리 검증)
- External dependencies: 없음

## Commands

- `./gradlew test --tests "com.sfood.automation.dashboard.infrastructure.persistence.DashboardQueryRepositoryImplTest"`
- `./gradlew test --tests "com.sfood.automation.dashboard.application.support.DashboardPeriodResolverTest"`
- `./gradlew test --tests "com.sfood.automation.dashboard.application.usecase.*"` (8개 UseCase 테스트 클래스)
- `./gradlew test --tests "com.sfood.automation.dashboard.presentation.DashboardControllerTest"`
- `./gradlew test --tests "com.sfood.automation.dashboard.*" --tests "com.sfood.automation.batch.*"` (최종 통합 확인)

## Results

- `DashboardQueryRepositoryImplTest` (9 tests): 신규 5개 케이스(시간대별/등급매출/등급상세/인기품목) 추가 후 전체 GREEN.
  - 날짜 범위 필터(`ORDER.orderedAt.between`)를 무력화 → 정확히 1건(`should_summarize_by_order_date_within_range_only`) FAILED 확인 → 원복 → GREEN.
  - 등급 매핑(`orderGrade.get(orderNo)`)을 `null`로 무력화 → 정확히 1건(`should_compute_grade_detail_with_basket_metrics_for_order_date_range`) FAILED 확인 → 원복 → GREEN.
- `DashboardPeriodResolverTest` (11 tests): 버킷 계산(DAY/WEEK/MONTH/QUARTER, 연도 경계 포함) 4개 케이스 추가 — 컴파일 실패(RED) 확인 후 구현 → GREEN.
- `GetDashboardSummaryUseCaseTest` (3 tests), `GetDashboardTopItemsUseCaseTest` (4 tests), `GetDashboardRevenueTrendUseCaseTest` (3 tests), `GetDashboardGradePivotUseCaseTest` (2 tests), `GetDashboardYoyComparisonUseCaseTest` (4 tests): 전부 클래스 미존재로 컴파일 실패(RED) 확인 → 구현 → GREEN.
- `DashboardControllerTest` (8 tests): 신규 엔드포인트 4개 케이스 추가, 새 UseCase 8개 `@MockBean` 등록 — 전체 GREEN.
- 최종 통합 실행(`dashboard.*` + `batch.*`): **102 tests, 0 failed, 0 errors** → `BUILD SUCCESSFUL` (~30초).
- PASS: 신규/기존 포함 102건 전부, 기존 `batch.*` 회귀 없음.
- FAIL: 없음.

## Failure Notes

- Repository 계층에서 2회의 뮤테이션 점검을 통해 핵심 로직(날짜 필터, 등급 매핑)이 실제로 검증되고 있음을 확인했다. 나머지 UseCase/Controller 계층은 클래스·메서드가 없어 컴파일 실패하는 것을 RED로 관찰한 뒤 구현하는 정석적인 TDD 사이클을 따랐다.

## Retest Results

- 두 차례의 뮤테이션 점검 모두 원복 후 재실행 PASS.
- `dashboard.*` + `batch.*` 최종 통합 실행 PASS(102/102).
- **전체 스위트는 아직 실행하지 않음** — main 반영 시점에 실행 예정(정책 반영).

## Acceptance Criteria Coverage

- AC1 (DAILY/WEEKLY는 ordered_at, MONTHLY/QUARTERLY는 oms_shipped_at, trend/pivot도 동일 원칙): 충족 — Repository/UseCase 테스트로 직접 검증.
- AC2 (월간/분기 주문 단위 dedup 1회 합산): 충족(summary 태스크에서 이미 검증, 이번에도 유지).
- AC3 (필수 파라미터 누락 시 400): 충족.
- AC4 (미인증 401): 충족.
- AC5 (빈 기간 0으로 응답): 충족.
- AC6 (top-items 선물 제외 + 잠봉 롤업): 충족 — `GetDashboardTopItemsUseCaseTest` 4건.
- AC7 (grade-pivot 0 채움): 충족 — `should_build_matrix_with_zero_for_grade_missing_in_a_bucket`.
- AC8 (yoy-comparison은 DAILY/WEEKLY 거부): 충족 — `should_reject_daily_and_weekly_periods`.
