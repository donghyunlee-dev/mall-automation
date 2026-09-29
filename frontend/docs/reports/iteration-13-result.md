# Iteration 13 결과 보고서 — 대시보드 화면 (월간 탭 Phase A)

> 완료일: 2026-09-04
> 담당: Sohee Kang
> 브랜치: feat/dashboard-monthly-tab

---

## 완료 범위

월간 탭을 Phase A/B로 나눠 진행하기로 사용자와 합의(일간/주간 탭과 달리 피벗 테이블·전년 비교 등 신규 UI 패턴이 커서 단계적 진행). 이번 이터레이션은 **Phase A**만 구현했다.

### 월간 탭 Phase A 구성
- 월(YYYY-MM) 선택 드롭다운(2025-01 ~ 기준월, 기본값 최신월)
- 핵심 지표 카드 8개(ARPPU 추가)
- 월간 출고 추이(일별 콤보 차트, 당월 선택 시 기준일까지만)
- 등급별 매출 비중(도넛 차트)
- 등급별 상세 지표 테이블(ARPPU 컬럼 포함)
- 월간 인기 품목 Top 6

### 선행 작업: 백엔드 dateBasis 확장 (PR #209, 별도 병합 완료)

월간 출고 추이는 일(day) 단위 × 출고일 기준 집계가 필요한데, 기존 `revenue-trend` API는 DAY 버킷일 때 항상 주문일자 기준으로 고정돼 있어 이 이터레이션 착수 전 백엔드를 먼저 확장했다(`DateBasis` 파라미터 추가, 하위 호환). 상세: `backend/docs/tasks/dashboard-query-api/result.md` "갱신 (2026-09-04)".

---

## 주요 결정 사항

| 결정 | 이유 |
|------|------|
| Phase A/B 분리 | 사용자 확정 — 피벗 테이블(조회 항목 전환) 2종 + 전년 동월 비교 2차트 + 최근 2개년 추이는 완전히 새로운 UI 패턴이라 별도 이터레이션으로 분리해 리뷰/검증 단위를 작게 유지 |
| `MetricCards`/`GradeDetailTable`에 `showArppu` prop 추가 | 새 컴포넌트를 만들지 않고 기존 컴포넌트를 확장 — 일간/주간 탭은 기본값(false)으로 기존 동작 그대로 유지 |
| `MetricCards` 그리드를 `repeat(7, 1fr)` → `repeat(auto-fit, minmax(140px, 1fr))`로 변경 | 7개(일간/주간)와 8개(월간) 카드 모두 동일 컴포넌트로 대응 |
| 당월 선택 시 일별 추이를 기준일(어제)까지로 제한 | 원본 Streamlit도 데이터 로드 시점(사실상 어제)까지만 표시 — `monthlyTrendRange.ts`로 순수 함수 분리, TDD로 검증 |
| 월 목록 시작을 2025-01로 고정 | 원본 `app.py`의 `pd.date_range(start='2025-01-01', ...)`과 동일 |

---

## 신규 파일

| 파일 | 역할 |
|------|------|
| `src/utils/monthlyTrendRange.ts`(+test) | 선택 월의 일별 출고 추이 조회 범위 계산 |
| `src/utils/monthOptions.ts`(+test) | 월 선택 드롭다운 옵션 목록 생성 |
| `src/hooks/useMonthlyDashboard.ts` | 월간 탭 데이터 병렬 조회 훅 |
| `docs/plan/iteration/iteration-13-plan.md` | 이터레이션 계획 |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `src/types/dashboard.ts` | `DateBasis` 타입 추가 |
| `src/api/dashboard.ts` | `getRevenueTrend`에 선택적 `dateBasis` 파라미터 추가 |
| `src/components/salesDashboard/MetricCards.tsx`(+scss) | `showArppu` prop, 그리드 `auto-fit` |
| `src/components/salesDashboard/GradeDetailTable.tsx` | `showArppu` prop(ARPPU 컬럼) |
| `src/pages/DashboardPage.tsx` | `MonthlyTab` 구현 |

---

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 32 파일 / 160 테스트 전체 통과
- 브라우저 수동 확인 — ✅ 완료(Playwright, 월간 탭 클릭 → 전 구간 렌더링 확인, 당월 일별 추이가 기준일까지만 표시되는 것 확인, 콘솔 에러 없음). 상세: `docs/test/results/iteration-13-test-result.md`

---

## Open Gaps / 다음 단계 (Phase B)

- 일별 실적 상세 피벗 테이블(매출액/주문건수/주문자수 조회 항목 전환) — `grade-pivot` API(`bucket=DAY&dateBasis=SHIPPED`) 활용
- 전년 동월 비교 콤보 차트 2종(매출/주문건수, 구매자수/AOV) — `yoy-comparison` API 활용, 신규 비교 차트 컴포넌트 필요
- 최근 2개년 추이 콤보 차트 — `revenue-trend`(`bucket=MONTH`) 재사용
- 월별 실적 상세 피벗 테이블(7개 항목 전환) — `grade-pivot` API(`bucket=MONTH`) 활용
- 분기 탭 구현
- 기존 Streamlit 대시보드 정리(retire)
