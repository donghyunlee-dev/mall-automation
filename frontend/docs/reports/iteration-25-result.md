# Iteration 25 결과 보고서 — 일간/주간 탭 카드 라벨 접두어 (R22)

> 완료일: 2026-09-09
> 관련: `docs/dashboard-integration/dashboard-prd.md` R22

## 완료 범위

원본과 동일하게 일간/주간 탭의 핵심 지표 첫 카드 라벨에도 탭별 접두어를 적용했다. `MetricCards`는 이미 Iteration 22에서 `revenueCardLabel` prop을 지원하고 있어 `DashboardPage.tsx`의 호출부만 수정하면 됐다.

| 탭 | 카드 라벨 |
|---|---|
| 일간 | "일간 총 매출 (VAT-)" |
| 주간 | "주간 총 매출 (VAT-)" |
| 월간 | "월간 총 매출 (VAT-)"(Iteration 22에서 기완료) |
| 분기 | "분기 총 매출 (VAT-)"(Iteration 22에서 기완료) |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/pages/DashboardPage.tsx` | `DailyTab`/`WeeklyTab`의 `MetricCards`에 `revenueCardLabel` 전달 |

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 38 파일 / 191 테스트 전체 통과
- 브라우저 수동 확인(Playwright, mock-server) — ✅ 일간/주간 탭 카드 라벨 확인. 상세: `docs/test/results/iteration-25-test-result.md`
