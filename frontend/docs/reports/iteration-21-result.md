# Iteration 21 결과 보고서 — 차트 스타일 보정 (R7/R8/R11)

> 완료일: 2026-09-09
> 관련: `docs/dashboard-integration/dashboard-prd.md` R7, R8, R11

## 완료 범위

원본 Streamlit 대시보드와 동일하게 AOV 컬럼 헤더를 탭별로 구분하고, 트렌드/시간대별/YoY 차트에 축 제목을 추가하고, 도넛 차트 hole 비율을 40%로 맞췄다. 백엔드 변경 없음.

| 요구사항 | 내용 |
|---|---|
| R7 | `GradeDetailTable`에 `aovLabel` prop 추가 — 일간/주간은 기본값 "객단가", 월간/분기는 "AOV" 전달 |
| R8 | `HourlyRevenueChart`/`RevenueTrendChart`/`YoyComparisonChart`에 `scales.*.title` 추가 |
| R11 | `GradeRevenuePie`(Doughnut)에 `cutout: '40%'` 추가 |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/utils/dashboardLabel.ts`(+test, TDD) | `getTrendAxisTitle(granularity)` 추가 — DAY="일자", WEEK="주차 (Week)", MONTH="월", QUARTER="분기" |
| `frontend/src/components/salesDashboard/GradeDetailTable.tsx` | `aovLabel?: string`(기본 `'객단가'`) prop 추가 |
| `frontend/src/pages/DashboardPage.tsx` | Monthly/QuarterlyTab의 `GradeDetailTable`에 `aovLabel="AOV"` 전달 |
| `frontend/src/components/salesDashboard/HourlyRevenueChart.tsx` | X축 "주문시간", Y축 "매출액" 제목 추가 |
| `frontend/src/components/salesDashboard/RevenueTrendChart.tsx` | X축 제목을 `getTrendAxisTitle(granularity)`로, Y축(좌) "매출액", Y축(우) "주문건수" 추가 |
| `frontend/src/components/salesDashboard/YoyComparisonChart.tsx` | Y축(좌/우) 제목을 기존 `barLabel`/`lineLabel` prop 값으로 재사용 |
| `frontend/src/components/salesDashboard/GradeRevenuePie.tsx` | `cutout: '40%'` 추가 |

## 범위 제외

- `GradePivotTable`은 월간/분기 탭 전용이라 이미 "AOV" 라벨을 쓰고 있어 변경 불필요(R7)
- `GradePivotTable` 하단의 등급별 라인 차트, `TopItemsChart`/`GradeRevenuePie`는 축 개념이 다르거나(파이) comparison-report.md 예시에 없어 R8 범위에서 제외

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 38 파일 / 191 테스트 전체 통과
- 브라우저 수동 확인(Playwright, mock-server) — ✅ 4개 탭 전체 스크린샷으로 AOV 헤더/축 제목/도넛 hole 확인. 상세: `docs/test/results/iteration-21-test-result.md`
