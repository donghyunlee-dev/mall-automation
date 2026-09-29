# Iteration 22 계획 — 안내 문구/카드 라벨 접두어 통일 (R12)

> 작성일: 2026-09-09
> 관련: `docs/dashboard-integration/dashboard-prd.md` R12, `comparison-report.md` 2.2 마지막 표

## 범위 결정 (사용자 확인 완료, 2026-09-09)

- **R9(등급별 상세 테이블 CSV/검색/컬럼 표시숨김)**: 실제 업무에 불필요 — 이번 Iteration 범위에서 제외
- **R12(안내 문구/카드 라벨 접두어)**: 원본과 통일 — 이번 Iteration에서 진행

## R12 상세

`comparison-report.md`에서 확인된 차이:

| 항목 | 원본 | 현재 |
|---|---|---|
| 핵심 지표 카드 첫 라벨(월간/분기) | "월간 총 매출 (VAT-)" / "분기 총 매출 (VAT-)" | "총 매출 (VAT-)"(4개 탭 공통) |
| 페이지 상단 안내문(월간/분기) | "이 대시보드는 월/분기 단위로 확인 가능합니다"(`gray_memo`) | 없음 |

**참고**: 원본 Streamlit 앱(`app.py`)의 정확한 문구 소스가 이 저장소에 없어(`update_data.py`만 존재), comparison-report.md의 요약 문구를 근거로 탭별 문구를 유추한다 — 월간 탭 "이 대시보드는 월 단위로 확인 가능합니다", 분기 탭 "이 대시보드는 분기 단위로 확인 가능합니다".

## 구현 방향

| 파일 | 변경 |
|---|---|
| `frontend/src/components/salesDashboard/MetricCards.tsx` | `revenueCardLabel?: string`(기본 `'총 매출 (VAT-)'`) prop 추가, 첫 카드 라벨에 사용 |
| `frontend/src/pages/DashboardPage.tsx` | MonthlyTab에 `revenueCardLabel="월간 총 매출 (VAT-)"`, QuarterlyTab에 `revenueCardLabel="분기 총 매출 (VAT-)"` 전달. 두 탭 상단에 안내 문구(`.memo`) 요소 추가 |
| `frontend/src/pages/DashboardPage.module.scss` | `.memo`(회색, 작은 글씨) 스타일 추가 |

## 완료 기준

- [ ] 일간/주간 탭 첫 카드 라벨 "총 매출 (VAT-)" 유지 확인
- [ ] 월간 탭 첫 카드 라벨 "월간 총 매출 (VAT-)", 안내 문구 표시 확인
- [ ] 분기 탭 첫 카드 라벨 "분기 총 매출 (VAT-)", 안내 문구 표시 확인
- [ ] `npm run build`/`lint`/`test` 통과, Playwright로 4개 탭 시각 확인
