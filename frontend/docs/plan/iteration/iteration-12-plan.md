# Iteration 12 Plan — 대시보드 화면 (주간 탭)

> 작성일: 2026-09-04
> 상태: 진행
> 의존: 백엔드 `GET /api/dashboard/*` (Iteration 11에서 이미 구현·머지 완료). 신규 백엔드 작업 없음 — `DashboardPeriod.WEEKLY`, `TrendBucketUnit.WEEK`가 이미 지원됨.
> 참조: `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 6.3절, 원본 `sfood-mall-dashboard/app.py` L562~757(주간 탭, `23169c8` 커밋에서 확인)

---

## 목표

일간 탭에 이어 주간 탭을 원본 화면과 동일하게 구현한다.

---

## 원본 화면 구성 (6.3절 + app.py L562~757)

- 조회 기준: 이번 주 월요일 ~ 기준 일자(주문일자 기준)
- 핵심 지표 카드 7개(일간 탭과 동일 세트)
- Row1: 이번 주 매출/주문 추이(요일별 콤보 차트, 2fr) + 등급별 매출 비중(도넛, 1fr)
- Row2: 등급별 상세 지표 테이블 + 인기 품목 Top 6(가로 막대)
  - 원본에는 인기 품목 아래 "잠봉 슬라이스 판매 비중" 전용 차트가 별도로 있으나, Iteration 11에서 이미 "top-items API가 롤업 처리하므로 별도 차트 생략" 결정을 내렸음 — 일관성을 위해 주간 탭도 동일하게 생략.
- 최근 5주 매출/주문 추이(콤보 차트, 전체 폭)

## 백엔드 재사용 매핑

| 원본 섹션 | API | 파라미터 |
|---|---|---|
| 핵심 지표 카드 | `GET /summary` | `period=WEEKLY&date=` |
| 이번 주 요일별 추이 | `GET /revenue-trend` | `bucket=DAY&count=<월요일부터 date까지 일수>&endDate=date` |
| 등급별 매출 비중 | `GET /grade-revenue` | `period=WEEKLY&date=` |
| 등급별 상세 지표 | `GET /grade-detail` | `period=WEEKLY&date=` |
| 인기 품목 | `GET /top-items` | `period=WEEKLY&date=&limit=6` |
| 최근 5주 추이 | `GET /revenue-trend` | `bucket=WEEK&count=5&endDate=date` |

시간대별 매출 차트(`hourly-revenue`)는 주간 탭에 없음(원본에 없음).

---

## 범위

### 신규 파일

| 파일 | 역할 |
|------|------|
| `src/utils/weekTrend.ts`(+test) | 기준일이 속한 주의 월요일부터 기준일까지 일수 계산 (`revenue-trend`의 `count` 파라미터로 사용) |
| `src/hooks/useWeeklyDashboard.ts` | 주간 탭 데이터 병렬 조회 훅 |

### 기존 파일 수정

| 파일 | 변경 내용 |
|------|----------|
| `src/pages/DashboardPage.tsx` | `WeeklyTab` 컴포넌트 구현, `WEEKLY` 탭 플레이스홀더 대체 |

### 컴포넌트 재사용 (신규 컴포넌트 없음)

`MetricCards`, `GradeRevenuePie`, `GradeDetailTable`, `TopItemsChart`, `RevenueTrendChart`(요일별 추이 + 5주 추이 두 곳에 재사용) — 모두 Iteration 11에서 만든 컴포넌트를 그대로 사용.

---

## 검증 계획

- `npm run build`, `npm run lint`, `npm run test`
- 브라우저 수동 확인(Playwright): 이메일 OTP 로그인 → `/dashboard` → 주간 탭 클릭 → 전 구간 렌더링 확인
