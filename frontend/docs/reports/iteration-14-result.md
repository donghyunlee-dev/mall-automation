# Iteration 14 결과 보고서 — 대시보드 화면 (월간 탭 Phase B)

> 완료일: 2026-09-04
> 담당: Sohee Kang
> 브랜치: feat/dashboard-monthly-phase-b

---

## 완료 범위

Iteration 13(Phase A)에 이어 월간 탭의 나머지를 구현해 **월간 탭을 완전히 완료**했다.

### Phase B 구성
- 일별 실적 상세 피벗 테이블(매출액/주문 건수/주문자 수 조회 항목 전환, 등급×일 매트릭스 + 합계 행)
- 전년 동월 비교 콤보 차트 2종(매출·주문건수 / 구매자수·AOV)
- 최근 2개년 추이 콤보 차트(월별)
- 월별 실적 상세 피벗 테이블(7개 항목 전환, 등급×월 매트릭스 + 합계 행 + 선택 항목의 등급별 라인 차트)

---

## 주요 결정 사항

| 결정 | 이유 |
|------|------|
| `GradePivotTable` 컴포넌트 하나로 일별(3항목)·월별(7항목) 양쪽 재사용 | 조회 항목 전환 UI(원본 `segmented_control`)와 합계 행 로직이 동일 — `showTrendChart` prop으로 월별 전용 라인 차트만 추가 표시 |
| `YoyComparisonChart` 컴포넌트를 범용 콤보 차트로 설계 | 매출/주문건수 비교와 구매자수/AOV 비교가 축·라벨만 다른 동일 구조라 하나의 컴포넌트로 재사용 |
| "최근 2개년 추이"를 별도 API 호출 없이 월별 실적 상세(`REVENUE`/`ORDER_COUNT` 피벗)에서 등급 합산해 클라이언트에서 구성 | 원본도 동일 소스 데이터에서 파생 — 중복 API 호출 방지 |
| `dateBasis`를 `getGradePivot`에도 추가 | Phase A에서 `getRevenueTrend`에만 추가했던 것을 동일 패턴으로 확장(일별 실적 상세 피벗도 출고일 기준 필요) |
| YoY 차트 축 눈금 등 세부 스타일은 기존 콤보 차트 스타일 그대로 사용 | 원본의 Y축 눈금 숨김·막대 폭 조정 등은 생략(계획서에 명시한 의도적 단순화) |

---

## 신규 파일

| 파일 | 역할 |
|------|------|
| `src/components/salesDashboard/GradePivotTable.tsx`(+scss) | 등급×기간 피벗 테이블 + 조회 항목 전환 + (옵션) 등급별 라인 차트 |
| `src/components/salesDashboard/YoyComparisonChart.tsx`(+scss) | 전년/올해 비교 범용 콤보 차트 |
| `src/utils/twoYearRange.ts`(+test) | "전년 1월 ~ 선택월" 개월 수 계산 |
| `docs/plan/iteration/iteration-14-plan.md` | 이터레이션 계획 |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `src/api/dashboard.ts` | `getGradePivot`에 선택적 `dateBasis` 파라미터 추가 |
| `src/utils/monthlyTrendRange.ts`(+test) | `monthEndDate` 헬퍼 추출(선택월 말일 계산, 연간 피벗/YoY의 `endDate`에 재사용) |
| `src/hooks/useMonthlyDashboard.ts` | 일별/월별 피벗, YoY 비교, 2개년 추이 데이터 추가 조회 |
| `src/pages/DashboardPage.tsx` | `MonthlyTab`에 Phase B 섹션 4개 추가 |
| `mock-server.js` | `grade-pivot`, `yoy-comparison` 엔드포인트 추가(로컬 검증용) |

---

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 33 파일 / 164 테스트 전체 통과
- 브라우저 수동 확인 — ✅ 완료(Playwright, 월간 탭 Phase B 전 구간 렌더링 및 조회 항목 전환 동작 확인, 콘솔 에러 없음). 상세: `docs/test/results/iteration-14-test-result.md`

---

## Open Gaps / 다음 단계

- 분기 탭 구현(월간 탭과 거의 동일한 구성 — `GradePivotTable`/`YoyComparisonChart` 재사용 가능할 것으로 예상)
- 기존 Streamlit 대시보드 정리(retire) — 4개 탭 전체 구현 이후 진행
- 성능 검증(대량 데이터 기준 응답 속도, 특히 피벗 테이블의 다중 `grade-pivot` 호출) — 진행하면서 확인 예정
