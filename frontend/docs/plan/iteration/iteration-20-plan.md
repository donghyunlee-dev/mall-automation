# Iteration 20 계획 — 날짜/기간 라벨 포맷 통일 (R5)

> 작성일: 2026-09-09
> 관련: `docs/dashboard-integration/dashboard-prd.md` R5, `comparison-report.md` 2.2(b)
> 번호 조정: dashboard-prd.md 로드맵에는 "Iteration 19"로 예정되어 있었으나, 실제 프론트엔드 Iteration 번호는 19까지 이미 사용됨(회원 관리 수동 수집 버튼) — 다음 실제 번호인 20으로 진행

## 배경

원본 Streamlit 대시보드는 트렌드 차트 x축과 피벗 테이블 컬럼 헤더에 사람이 읽기 쉬운 축약 날짜 포맷을 쓰는 반면, 재구현판은 API가 내려주는 원본 ISO 날짜 문자열을 그대로 노출한다. `comparison-report.md` 2.2(b)에서 확인된 위치별 목표 포맷은 다음과 같다.

| 그래뉼래리티 | 원본 포맷 | 대상 |
|---|---|---|
| DAY | `26-08-15(토)` (요일 포함) | 일간 "최근 20일", 주간 "이번 주 추이", 월간 "월간 출고 추이", 월간 "일별 실적 상세" 컬럼 |
| WEEK | `26/08/03` (주 시작 월요일 기준) | 주간 "최근 5주" |
| MONTH | `25-01` (2자리 연도-월) | 월간 "최근 2개년 추이"/"월별 실적 상세", 분기 "분기 내 월별 출고 추이"/"월별 실적 상세" |
| QUARTER | `25-1Q` | 분기 "최근 8분기 추이"/"분기별 실적 상세" |

**WEEK 포맷은 단순 표기 변경이 아니라 기준일 자체가 다르다** — 현재 `trend5Weeks`는 주 종료일(일요일, `point.end`)을 라벨로 쓰는데, 원본은 주 시작일(월요일, `point.start`)을 쓴다. `DashboardTrendPoint`가 이미 `start`/`end`를 모두 갖고 있어 백엔드 변경 없이 프론트에서 참조 필드만 바꾸면 된다.

**범위 제외**: comparison-report.md 2.2(c)에서 별도로 "결정 필요" 항목으로 분리된 월간 출고 추이의 오프바이원(배송일자 vs OMS출고일) 이슈는 R5와 무관하므로 손대지 않는다. 전년동월/동분기 비교 차트(YoY)와 월/분기 선택 드롭다운 라벨(`YYYY-MM`, `YYYY-NQ`)도 comparison-report.md 목록에 없으므로 이번 범위에서 제외한다.

## 구현 방향

| 파일 | 내용 |
|---|---|
| `frontend/src/utils/dashboardLabel.ts`(신규, TDD) | `formatShortDayLabel(dateStr)`, `formatShortWeekLabel(weekStartDateStr)`, `formatShortMonthLabel(dateStr)`, `formatShortQuarterLabel(dateStr)` + 그래뉼래리티 기반 트렌드 포인트 라벨 헬퍼 |
| `frontend/src/components/salesDashboard/RevenueTrendChart.tsx` | `granularity: 'DAY' \| 'WEEK' \| 'MONTH' \| 'QUARTER'` prop 추가, `labels`를 그래뉼래리티에 맞는 포맷터로 생성(WEEK만 `point.start`, 나머지는 `point.end`) |
| `frontend/src/pages/DashboardPage.tsx` | 각 `RevenueTrendChart` 사용처에 `granularity` prop 전달, `GradePivotTable`에 넘기던 수동 `.slice(0, 7)` 제거하고 포맷터로 대체 |
| `frontend/src/components/salesDashboard/GradePivotTable.tsx` | 변경 없음(포맷된 문자열을 그대로 `columnLabels`로 받으므로 컴포넌트 내부 수정 불필요 — `DashboardPage`에서 포맷 적용) |

## 완료 기준

- [ ] `dashboardLabel.ts` 유틸 4종 TDD로 작성(RED→GREEN)
- [ ] 4개 탭 전체 트렌드 차트 x축 라벨이 목표 포맷대로 표시
- [ ] 월간/분기 피벗 테이블 컬럼 헤더가 `25-01`/`25-1Q` 형태로 표시
- [ ] 주간 "최근 5주" 라벨이 주 시작(월요일) 기준 `YY/MM/DD`로 표시
- [ ] `npm run build` + `npm run lint` + `npm run test` 통과
