# Iteration 25 계획 — 일간/주간 탭 핵심 지표 카드 라벨 접두어 (R22)

> 작성일: 2026-09-09
> 관련: `docs/dashboard-integration/dashboard-prd.md` R22(Iteration 24 조사 중 신규 발견)

## 배경

Iteration 24에서 원본 서비스를 직접 확인한 결과, 원본은 일간/주간 탭의 핵심 지표 첫 카드에도 "일간 총 매출 (VAT-)"/"주간 총 매출 (VAT-)"처럼 탭별 접두어를 쓴다. R12(Iteration 22)는 월간/분기만 반영해 완료 처리했었다.

## 범위

- `MetricCards`는 이미 `revenueCardLabel` prop(Iteration 22에서 추가)이 있어 컴포넌트 변경 불필요
- `DailyTab`/`WeeklyTab`의 `MetricCards` 호출에 각각 `revenueCardLabel="일간 총 매출 (VAT-)"`, `revenueCardLabel="주간 총 매출 (VAT-)"` 전달

## 완료 기준

- [x] 일간 탭 카드 라벨 "일간 총 매출 (VAT-)" 표시 확인
- [x] 주간 탭 카드 라벨 "주간 총 매출 (VAT-)" 표시 확인
- [x] `npm run build`/`lint`/`test` 통과, Playwright로 시각 확인
