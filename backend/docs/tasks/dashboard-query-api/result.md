# dashboard-query-api Result

## Delivered Scope

- 계획했던 9.7의 4번 단계 대시보드 조회 API **7개 전부** 완성: `summary`, `hourly-revenue`, `grade-revenue`, `grade-detail`, `top-items`, `revenue-trend`, `grade-pivot`, `yoy-comparison`.
- 7개 API가 전부 공유하는 뼈대 확립: `DashboardPeriod`/`DashboardPeriodResolver`(period + 버킷 범위 계산), `DashboardQueryRepository`(조회 전용 포트, QueryDSL 구현), `DashboardQueryException`(공통 에러 처리).

## Changed Files

**도메인 모델/포트**
- `GodoOrder*`(변경 없음) 외 신규: `DashboardPeriod`, `DashboardSummary`, `GradeRevenue`, `GradeDetail`, `TopItem`, `HourlyRevenue`, `GradePivotMetric`
- `DashboardQueryRepository`(포트, 9개 메서드)

**애플리케이션 계층**
- `DashboardPeriodResolver`, `DashboardQueryException`
- UseCase 8개: `GetDashboardSummaryUseCase`, `GetDashboardHourlyRevenueUseCase`, `GetDashboardGradeRevenueUseCase`, `GetDashboardGradeDetailUseCase`, `GetDashboardTopItemsUseCase`, `GetDashboardRevenueTrendUseCase`, `GetDashboardGradePivotUseCase`, `GetDashboardYoyComparisonUseCase`
- DTO 5개: `DashboardSummaryResponse`, `GradeDetailResponse`, `DashboardTrendPointResponse`, `GradePivotResponse`, `YoyComparisonResponse`

**인프라/프레젠테이션**
- `DashboardQueryRepositoryImpl`(QueryDSL, 9개 메서드)
- `DashboardController`(엔드포인트 7개)
- `ErrorCode.DASHBOARD_QUERY_INVALID` 추가

**테스트**: 위 각 클래스에 대응하는 테스트 8개 파일(총 신규/누적 케이스 다수, 통합 실행 시 102 tests).

## Key Decisions

- **수직 슬라이스 우선 → 수평 확장**: `summary` 하나를 먼저 끝까지 완성해 아키텍처 패턴(포트 분리, QueryDSL, 날짜 범위 재사용)을 검증한 뒤, 나머지 6개는 같은 패턴을 복제해 빠르게 구현.
- **revenue-trend/grade-pivot/yoy-comparison은 신규 Repository 메서드 없이 기존 메서드 재사용**: 버킷/기간별로 기존 `summaryByOrderDate`/`summaryByShippedDate`/`gradeDetailBy*`를 반복 호출하는 방식으로 구현해, Repository 계층에 복잡한 신규 쿼리를 추가하지 않고도 7개 API를 전부 커버.
- **top-items의 잠봉/선물 로직은 UseCase(애플리케이션 계층)에서 처리**: SQL에 하드코딩하지 않고 Java 코드로 처리해 향후 규칙 변경 시 재배포만으로 대응 가능(SQL 마이그레이션 불필요).
- **"선물" 제외를 top-items에만 한정**: 원본처럼 데이터 로드 전체에 적용하지 않고 품목 랭킹에만 적용 — 헤더/라인 분리 스키마 구조상 완전히 동일한 재현은 별도 결정이 필요해 범위를 좁힘(Open Gaps 참고).
- **grade-pivot의 metric은 enum으로 고정된 7종만 지원**: REVENUE/ORDER_COUNT/BUYER_COUNT/AVG_SKU/AVG_QTY/AOV/ARPPU — 원본 화면의 "조회 항목 전환" segmented_control과 1:1 대응.

## Open Gaps

- "선물" 제외가 주문 헤더 매출(summary 등)에는 반영되지 않음 — 현업 확인 후 결정 필요(spec.md Assumptions).
- `revenue-trend`/`grade-pivot`은 버킷마다 별도 쿼리(N회 왕복) — 실제 데이터 볼륨에서 성능 재검증 필요.
- ~~`godo_order`/`godo_order_item`에 실제 데이터가 없어 실사용 검증 못 함~~ → **완료(2026-09-04)**: `order-sync` 라이브 실행 후 실데이터 기준 API 응답을 원시 SQL과 교차 검증 완료 — `dashboard-order-sync/result.md` "갱신" 참고.
- 전체 스위트(`./gradlew test`, 필터 없음)는 아직 실행하지 않음 — main 반영 전 반드시 실행 필요.
- ~~React 화면 연동 미착수~~ → **완료**: 4개 탭 전부(Iteration 11~15).

## Next Steps

- main 반영 전 전체 스위트 실행.
- 3번 단계(라이브 검증 + `order-sync` 활성화) 완료 후 실데이터로 7개 API 동작 확인.
- React 화면에서 이 7개 API를 사용해 4개 탭(9.7의 5번 단계) 구현.
- "선물" 제외 로직의 매출 집계 반영 여부를 현업에 확인 후 필요 시 추가 작업.

## 갱신 (2026-09-04): revenue-trend / grade-pivot에 dateBasis 파라미터 추가

월간 탭(Iteration 13 예정) 구현 중 발견: 원본 화면의 "월간 출고 추이"·"일별 실적 상세"는 **일(day) 단위 × 출고일(OMS출고일) 기준** 집계가 필요한데, 기존 `revenue-trend`/`grade-pivot`은 `TrendBucketUnit.DAY`일 때 항상 주문일자 기준으로 고정되어 있어 그대로 쓸 수 없었다.

- `GetDashboardRevenueTrendUseCase`/`GetDashboardGradePivotUseCase`에 `execute(..., DateBasis dateBasis)` 오버로드 추가. `dateBasis`가 `null`이면 기존 규칙(버킷 단위 기반 자동 판단)을 그대로 유지하고, `ORDER`/`SHIPPED`를 명시하면 버킷 단위와 무관하게 그 기준을 강제한다.
- `DashboardController`의 `/revenue-trend`, `/grade-pivot`에 선택적 `dateBasis` 쿼리 파라미터 추가(생략 시 기존 동작 그대로).
- 기존 3-arg 메서드 시그니처·기존 테스트는 그대로 유지(하위 호환) — TDD로 신규 오버로드/컨트롤러 파라미터에 대한 실패 테스트를 먼저 작성 후 구현.
- 검증: `./gradlew test --tests "com.sfood.automation.dashboard.*"` 전체 통과.
