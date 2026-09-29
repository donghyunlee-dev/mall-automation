# Iteration 14 Plan — 대시보드 화면 (월간 탭 Phase B)

> 작성일: 2026-09-04
> 상태: 진행
> 참조: `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 6.4절, 원본 `sfood-mall-dashboard/app.py` L128~332(공통 렌더러), L1019~1128(YoY/2개년/월별 상세), `23169c8` 커밋

---

## 목표

Iteration 13(Phase A)에 이어 월간 탭의 나머지 부분을 구현한다.

## 범위

1. **일별 실적 상세 피벗 테이블** — 조회 항목(매출액/주문 건수/주문자 수) 전환, 등급×일 매트릭스 + 합계 행
2. **전년 동월 비교** — 콤보 차트 2종(매출·주문건수 / 구매자수·AOV)
3. **최근 2개년 추이** — 콤보 차트(월별)
4. **월별 실적 상세 피벗 테이블** — 조회 항목(매출액/주문건수/주문자수/건당수량/건당SKU/AOV/ARPPU) 전환, 등급×월 매트릭스 + 합계 행 + 선택 항목 등급별 라인 차트

## 백엔드 매핑

| 원본 섹션 | API | 파라미터 |
|---|---|---|
| 일별 실적 상세 | `GET /grade-pivot` ×3(metric별) | `bucket=DAY&dateBasis=SHIPPED&count=<Phase A와 동일>&endDate=<Phase A와 동일>` |
| 전년 동월 비교 | `GET /yoy-comparison` | `period=MONTHLY&yearMonth=` |
| 최근 2개년 추이 + 월별 실적 상세 | `GET /grade-pivot` ×7(metric별) | `bucket=MONTH&count=<선택월 month + 12>&endDate=<선택월 말일>` (MONTH 버킷은 기존 규칙상 자동으로 출고일 기준이라 `dateBasis` 불필요) |

- "최근 2개년 추이" 콤보 차트는 별도 API 호출 없이 월별 실적 상세의 `REVENUE`/`ORDER_COUNT` 피벗 응답을 등급 합산해 클라이언트에서 구성한다(원본도 동일 소스 데이터에서 파생되는 값이라 중복 호출 불필요).
- count 계산: 조회월이 `YYYY-MM`일 때 `month + 12`(전년 1월부터 조회월까지의 개월 수). 원본 `start_date_y = Timestamp(start_date_m.year - 1, 1, 1)`과 동일한 범위.

## 신규 컴포넌트

| 컴포넌트 | 역할 |
|---|---|
| `GradePivotTable` | 등급×기간 피벗 테이블 + 조회 항목 전환 버튼(segmented_control 대응) + 합계 행. 평균 지표(건당SKU/건당수량/AOV/ARPPU)는 합계 행을 `-`로 표시. `showTrendChart` 시 선택 항목의 등급별 라인 차트를 하단에 함께 렌더링(월별 실적 상세 전용). 일별(3항목)·월별(7항목) 양쪽에 재사용 |
| `YoyComparisonChart` | 전년/올해 2개 시점 비교용 범용 콤보(막대+라인) 차트 — 라벨/값을 prop으로 받아 매출·주문건수 비교와 구매자수·AOV 비교 양쪽에 재사용 |

## 신규 유틸

| 파일 | 역할 |
|---|---|
| `src/utils/twoYearRange.ts`(+test) | 선택월 기준 "전년 1월 ~ 선택월" 개월 수 계산 |

## 의도적으로 단순화한 부분

- YoY 비교 차트의 Y축 눈금 숨김·막대 폭 조정 등 원본의 세부 스타일링은 생략하고, 기존 콤보 차트 스타일(축 표시)을 그대로 사용한다.
- "매출액 단위: 천 원" 표기는 피벗 테이블 헤더 문구로만 반영하고, 원본처럼 별도 스케일 열 변환 로직은 표시값 계산에만 적용한다(1000으로 나눠 표시).

## 검증 계획

- `npm run build`, `npm run lint`, `npm run test`
- 브라우저 수동 확인(Playwright): 월간 탭에서 Phase B 섹션 전부 렌더링 확인, 피벗 테이블 조회 항목 전환 동작 확인
