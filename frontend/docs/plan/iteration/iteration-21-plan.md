# Iteration 21 계획 — 차트 스타일 보정 (R7/R8/R11)

> 작성일: 2026-09-09
> 관련: `docs/dashboard-integration/dashboard-prd.md` R7, R8, R11

## 범위

### R7 — AOV 컬럼 헤더 탭별 구분
- `GradeDetailTable`이 4개 탭 공통으로 `<th>객단가</th>`를 하드코딩 중. 일간/주간은 그대로 "객단가", 월간/분기는 "AOV"로 표시해야 한다.
- `GradePivotTable`은 월간/분기 탭에서만 쓰이고 이미 `AOV` 라벨을 사용 중이라 변경 불필요.

### R8 — 차트 축 제목 추가
- `HourlyRevenueChart`: X축 "주문시간", Y축 "매출액"
- `RevenueTrendChart`: 이미 받는 `granularity` prop을 이용해 X축 제목을 그래뉼래리티별로 표시(DAY="일자", WEEK="주차 (Week)", MONTH="월", QUARTER="분기"), Y축(좌) "매출액", Y축(우) "주문건수"
- `YoyComparisonChart`: 이미 받는 `barLabel`/`lineLabel`을 그대로 Y축(좌/우) 제목으로 재사용(신규 prop 불필요)

### R11 — 도넛 차트 hole 비율
- `GradeRevenuePie`의 Doughnut 옵션에 `cutout: '40%'` 추가

## 구현 방향

| 파일 | 변경 |
|---|---|
| `frontend/src/components/salesDashboard/GradeDetailTable.tsx` | `aovLabel?: string`(기본값 `'객단가'`) prop 추가, 헤더/합계행에 사용 |
| `frontend/src/pages/DashboardPage.tsx` | Monthly/QuarterlyTab의 `GradeDetailTable`에 `aovLabel="AOV"` 전달 |
| `frontend/src/utils/dashboardLabel.ts`(+test, TDD) | `getTrendAxisTitle(granularity)` 순수 함수 추가 |
| `frontend/src/components/salesDashboard/HourlyRevenueChart.tsx` | `scales.x/y.title` 추가 |
| `frontend/src/components/salesDashboard/RevenueTrendChart.tsx` | `scales.y/y1.title` 고정값 + `scales.x.title`을 `getTrendAxisTitle(granularity)`로 |
| `frontend/src/components/salesDashboard/YoyComparisonChart.tsx` | `scales.y/y1.title`을 `barLabel`/`lineLabel`로 |
| `frontend/src/components/salesDashboard/GradeRevenuePie.tsx` | `cutout: '40%'` 추가 |

## 완료 기준

- [ ] `getTrendAxisTitle` TDD로 작성(RED→GREEN)
- [ ] 일간/주간 GradeDetailTable = "객단가", 월간/분기 = "AOV" 확인
- [ ] 4개 탭 트렌드/시간대별/YoY 차트에 축 제목 표시 확인
- [ ] 도넛 차트 hole이 40%로 보임 확인
- [ ] `npm run build`/`lint`/`test` 통과, Playwright로 4개 탭 시각 확인
