# Iteration 12 결과 보고서 — 대시보드 화면 (주간 탭)

> 완료일: 2026-09-04
> 담당: Sohee Kang
> 브랜치: feat/dashboard-weekly-tab

---

## 완료 범위

Iteration 11(일간 탭)에 이어 주간 탭을 원본 화면(`sfood-mall-dashboard/app.py` L562~757, `23169c8` 커밋)과 동일하게 구현했다. 백엔드는 이미 `DashboardPeriod.WEEKLY`, `TrendBucketUnit.WEEK`를 지원하고 있어 신규 백엔드 작업 없이 프론트 구현만으로 완료했다.

### 주간 탭 구성
- 핵심 지표 카드 7개(일간 탭과 동일 세트)
- 이번 주 매출/주문 추이(월요일~기준일, 요일별 콤보 차트)
- 등급별 매출 비중(도넛 차트)
- 등급별 상세 지표 테이블(합계 행 포함)
- 주간 인기 품목 Top 6(가로 막대 차트)
- 최근 5주 매출/주문 추이(콤보 차트)
- 날짜 선택 + "데이터 최신화"

---

## 주요 결정 사항

| 결정 | 이유 |
|------|------|
| 잠봉 슬라이스 전용 차트 미생성 | 원본에는 인기 품목 아래 별도 차트가 있으나, Iteration 11에서 이미 "top-items API가 롤업 처리하므로 생략" 결정 — 일관성 유지 |
| 신규 컴포넌트 없이 기존 컴포넌트 재사용 | `MetricCards`/`GradeRevenuePie`/`GradeDetailTable`/`TopItemsChart`/`RevenueTrendChart`가 이미 필요한 데이터 형태를 그대로 받을 수 있어 신규 UI 컴포넌트 불필요. `RevenueTrendChart`를 요일별 추이(DAY 버킷)와 5주 추이(WEEK 버킷) 두 곳에 재사용 |
| 시간대별 매출 차트 없음 | 원본에도 주간 탭에는 시간대별 차트가 없음(일간 탭 전용) |
| "이번 주" 일수 계산을 순수 함수로 분리(`weekTrend.ts`) | TDD로 작성(RED: 모듈 없음 → GREEN) — `revenue-trend` API의 `count` 파라미터 계산 로직을 테스트 가능하게 격리 |

---

## 신규 파일

| 파일 | 역할 |
|------|------|
| `src/utils/weekTrend.ts`(+test) | 기준일이 속한 주의 월요일부터 기준일까지 일수 계산 |
| `src/hooks/useWeeklyDashboard.ts` | 주간 탭 데이터 병렬 조회 훅 |
| `docs/plan/iteration/iteration-12-plan.md` | 이터레이션 계획 |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `src/pages/DashboardPage.tsx` | `WeeklyTab` 컴포넌트 구현, `WEEKLY` 탭 플레이스홀더 대체 |

---

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 30 파일 / 154 테스트 전체 통과
- 브라우저 수동 확인 — ✅ 완료(Playwright, 주간 탭 클릭 → 전 구간 렌더링 확인, 콘솔 에러 없음). 상세 내역은 `docs/test/results/iteration-12-test-result.md` 참조.

---

## Open Gaps / 다음 단계

- 월간 탭 구현(다음 이터레이션) — ARPPU 카드 추가, 전년 동월 비교, 등급별 라인 차트 등 신규 UI 필요 (`GetDashboardYoyComparisonUseCase`, `GetDashboardGradePivotUseCase` API 활용)
- 분기 탭 구현
- 기존 Streamlit 대시보드 정리(retire) — 신규 탭 전체 구현 이후 진행
