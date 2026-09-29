# Iteration 13 Plan — 대시보드 화면 (월간 탭 Phase A)

> 작성일: 2026-09-04
> 상태: 진행
> 의존: 백엔드 `GET /api/dashboard/*` + `dateBasis` 파라미터(PR #209, develop 반영 완료)
> 참조: `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 6.4절, 원본 `sfood-mall-dashboard/app.py` L758~1128(월간 탭, `23169c8` 커밋)

---

## 목표

월간 탭을 2단계로 나눠 구현한다. 이번 이터레이션은 **Phase A**(일간/주간 탭과 동일한 패턴으로 재사용 가능한 부분)만 구현하고, Phase B(피벗 테이블 2종 + 전년 동월 비교 + 최근 2개년 추이)는 다음 이터레이션으로 넘긴다. 사용자 확정 사항.

## 원본 화면 구성 중 Phase A 범위

- 조회 기준: 월(YYYY-MM) 선택 드롭다운, `OMS출고일` 기준
- 핵심 지표 카드 8개(일간/주간 대비 ARPPU 추가)
- 월간 출고 추이(일별 콤보 차트, 월초~기준일 또는 월말)
- 등급별 매출 비중(도넛) + 등급별 상세 지표(AOV/ARPPU 포함)
- 월간 인기 품목 Top 6

## 백엔드 재사용 매핑

| 원본 섹션 | API | 파라미터 |
|---|---|---|
| 핵심 지표 카드 | `GET /summary` | `period=MONTHLY&yearMonth=` |
| 월간 출고 추이(일별) | `GET /revenue-trend` | `bucket=DAY&count=<월 일수 또는 경과일수>&endDate=<월말 또는 기준일>&dateBasis=SHIPPED`(PR #209로 신규 지원) |
| 등급별 매출 비중 | `GET /grade-revenue` | `period=MONTHLY&yearMonth=` |
| 등급별 상세 지표 | `GET /grade-detail` | `period=MONTHLY&yearMonth=` (ARPPU는 응답에 이미 포함, 프론트에서만 컬럼 추가) |
| 인기 품목 | `GET /top-items` | `period=MONTHLY&yearMonth=&limit=6` |

시간대별 매출 차트는 월간 탭에 없음(원본에 없음).

---

## 범위

### 신규 파일

| 파일 | 역할 |
|------|------|
| `src/utils/monthlyTrendRange.ts`(+test) | 선택 월의 일별 출고 추이 조회 범위(시작/끝/일수) 계산 — 당월이면 기준일(어제)까지로 제한 |
| `src/utils/monthOptions.ts`(+test) | 2025-01 ~ 기준월까지 내림차순 월 목록 생성(드롭다운용) |
| `src/hooks/useMonthlyDashboard.ts` | 월간 탭 데이터 병렬 조회 훅 |

### 기존 파일 수정

| 파일 | 변경 내용 |
|------|----------|
| `src/types/dashboard.ts` | `DateBasis` 타입 추가 |
| `src/api/dashboard.ts` | `getRevenueTrend`에 선택적 `dateBasis` 파라미터 추가 |
| `src/components/salesDashboard/MetricCards.tsx`(+scss) | `showArppu` prop 추가(8번째 카드), 그리드를 `auto-fit`으로 변경(7/8개 모두 대응) |
| `src/components/salesDashboard/GradeDetailTable.tsx` | `showArppu` prop 추가(ARPPU 컬럼) |
| `src/pages/DashboardPage.tsx` | `MonthlyTab` 구현(월 선택 드롭다운), `MONTHLY` 탭 플레이스홀더 대체 |

---

## 검증 계획

- `npm run build`, `npm run lint`, `npm run test`
- 브라우저 수동 확인(Playwright): 월간 탭 클릭 → 전 구간 렌더링 확인, 당월 선택 시 일별 추이가 기준일까지만 표시되는지 확인
