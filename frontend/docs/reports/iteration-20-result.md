# Iteration 20 결과 보고서 — 날짜/기간 라벨 포맷 통일 (R5)

> 완료일: 2026-09-09
> 관련: `docs/dashboard-integration/dashboard-prd.md` R5, `comparison-report.md` 2.2(b)

## 완료 범위

원본 Streamlit 대시보드와 동일한 축약 날짜 라벨 포맷을 4개 탭의 트렌드 차트/피벗 테이블 헤더에 적용했다. 백엔드 변경 없음(WEEK 그래뉼래리티는 이미 응답에 있던 `start` 필드만 참조 필드로 바꿔 사용).

| 그래뉼래리티 | 포맷 | 참조 필드 |
|---|---|---|
| DAY | `26-08-15(토)` | `point.end` |
| WEEK | `26/08/03` | `point.start`(주 시작 월요일) |
| MONTH | `25-01` | `point.end` |
| QUARTER | `25-1Q` | `point.end` |

## 신규 파일

| 파일 | 역할 |
|------|------|
| `frontend/src/utils/dashboardLabel.ts`(+test, TDD) | `formatShortDayLabel`/`formatShortWeekLabel`/`formatShortMonthLabel`/`formatShortQuarterLabel`, 그래뉼래리티 기반 `formatTrendPointLabel` |
| `frontend/docs/plan/iteration/iteration-20-plan.md` | 이터레이션 계획 |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/components/salesDashboard/RevenueTrendChart.tsx` | `granularity` prop 추가, x축 라벨을 `formatTrendPointLabel`로 생성 |
| `frontend/src/pages/DashboardPage.tsx` | 6곳의 `RevenueTrendChart`에 `granularity` 전달, 4곳의 `GradePivotTable` `columnLabels`를 수동 `.slice(0,7)` 대신 공용 포맷터로 대체 |

## 범위 제외

- 월간 출고 추이의 오프바이원(배송일자 vs OMS출고일) 이슈 — `comparison-report.md` 2.2(c)에서 별도 결정 필요 항목으로 분리되어 있어 손대지 않음
- 전년동월/동분기 비교(YoY) 차트, 월/분기 선택 드롭다운 라벨 — comparison-report.md의 R5 대상 목록에 없어 제외

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 38 파일 / 187 테스트 전체 통과
- 브라우저 수동 확인(Playwright, mock-server) — ✅ 4개 탭 전체 스크린샷으로 라벨 포맷 확인. 상세: `docs/test/results/iteration-20-test-result.md`

## 번호 조정 안내

`dashboard-prd.md` 로드맵에는 R5가 "Iteration 19"로 예정되어 있었으나, 프론트엔드 Iteration 번호가 이 로드맵과 별개로 계속 이어져(회원 관리 등) 19까지 이미 소진된 상태였다. 다음 실제 번호인 20으로 진행했고, `dashboard-prd.md`의 로드맵 표(Iteration 20~23)도 함께 갱신했다.
