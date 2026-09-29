# Iteration 22 결과 보고서 — 안내 문구/카드 라벨 접두어 통일 (R12)

> 완료일: 2026-09-09
> 관련: `docs/dashboard-integration/dashboard-prd.md` R9, R12

## 스코프 결정 (사용자 확인, 2026-09-09)

- **R9(등급별 상세 테이블 CSV/검색/컬럼 표시숨김)**: 실제 업무에 불필요 — 이번 Iteration 범위에서 제외, 향후 재요청 시 별도 착수
- **R12(안내 문구/카드 라벨 접두어)**: 원본과 통일 — 이번 Iteration에서 구현

## 완료 범위

원본과 동일하게 월간/분기 탭 상단에 안내 문구를 추가하고, 핵심 지표 첫 카드("총 매출 (VAT-)") 라벨에 탭별 접두어를 붙였다. 백엔드 변경 없음.

| 탭 | 카드 라벨 | 안내 문구 |
|---|---|---|
| 일간/주간 | "총 매출 (VAT-)"(변경 없음) | 없음(변경 없음) |
| 월간 | "월간 총 매출 (VAT-)" | "이 대시보드는 월 단위로 확인 가능합니다" |
| 분기 | "분기 총 매출 (VAT-)" | "이 대시보드는 분기 단위로 확인 가능합니다" |

**참고**: 원본 Streamlit 앱(`app.py`)의 정확한 `gray_memo` 문구 소스가 이 저장소에 없어(`update_data.py`만 존재), `comparison-report.md`의 요약 문구("이 대시보드는 월/분기 단위로 확인 가능합니다")를 근거로 탭별 문구를 유추해 적용했다.

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/components/salesDashboard/MetricCards.tsx` | `revenueCardLabel?: string`(기본 `'총 매출 (VAT-)'`) prop 추가 |
| `frontend/src/pages/DashboardPage.tsx` | MonthlyTab/QuarterlyTab에 `revenueCardLabel` 전달, 상단에 안내 문구(`.memo`) 요소 추가 |
| `frontend/src/pages/DashboardPage.module.scss` | `.memo`(회색, 작은 글씨) 스타일 추가 |

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 38 파일 / 191 테스트 전체 통과
- 브라우저 수동 확인(Playwright, mock-server) — ✅ 4개 탭 전체 확인. 상세: `docs/test/results/iteration-22-test-result.md`
