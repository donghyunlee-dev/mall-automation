# Iteration 15 Plan — 대시보드 화면 (분기 탭)

> 작성일: 2026-09-04
> 상태: 진행
> 참조: `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 6.5절, 원본 `sfood-mall-dashboard/app.py` L1129~1436(분기 탭), `23169c8` 커밋

---

## 목표

분기 탭을 구현해 4개 탭(일간/주간/월간/분기)을 전부 완료한다. 월간 탭과 구성이 거의 동일하고, MONTH/QUARTER 버킷은 이미 출고일 기준으로 자동 처리되므로 **신규 백엔드 작업이 없다**(Iteration 13처럼 `dateBasis` 확장이 필요했던 것은 DAY 버킷 한정).

## 원본 화면 구성

- 조회 기준: 분기(YYYY-QQ) 선택 드롭다운, `OMS출고일` 기준
- 핵심 지표 카드 8개(ARPPU 포함)
- 분기 내 월별 출고 추이(3개월 콤보 차트) + 등급별 매출 비중(도넛)
- 등급별 상세 지표(AOV/ARPPU) + 분기 인기 품목 Top 6
- 월별 실적 상세 피벗 테이블(분기 내 3개월, 매출액/주문건수/주문자수 전환)
- 전년 동분기 비교 콤보 차트 2종
- 최근 8분기 추이 콤보 차트
- 분기별 실적 상세 피벗 테이블(8분기, 7개 항목 전환 + 등급별 라인 차트)

## 백엔드 매핑 (전부 기존 API 재사용, 변경 없음)

| 원본 섹션 | API | 파라미터 |
|---|---|---|
| 핵심 지표 카드 | `GET /summary` | `period=QUARTERLY&year=&quarter=` |
| 분기 내 월별 추이 | `GET /revenue-trend` | `bucket=MONTH&count=3&endDate=<분기 말일>` |
| 등급별 매출 비중/상세 | `GET /grade-revenue`, `GET /grade-detail` | `period=QUARTERLY&year=&quarter=` |
| 인기 품목 | `GET /top-items` | `period=QUARTERLY&year=&quarter=&limit=6` |
| 월별 실적 상세(3개월) | `GET /grade-pivot` ×3(metric) | `bucket=MONTH&count=3&endDate=<분기 말일>` |
| 전년 동분기 비교 | `GET /yoy-comparison` | `period=QUARTERLY&year=&quarter=`(기존에 이미 QUARTERLY 지원) |
| 최근 8분기 추이 | `GET /revenue-trend` | `bucket=QUARTER&count=8&endDate=<분기 말일>` |
| 분기별 실적 상세(8분기) | `GET /grade-pivot` ×7(metric) | `bucket=QUARTER&count=8&endDate=<분기 말일>` |

## 컴포넌트 재사용 (신규 컴포넌트 없음)

`MetricCards`(showArppu), `RevenueTrendChart`(분기 내 월별 추이 + 최근 8분기 추이 두 곳), `GradeRevenuePie`, `GradeDetailTable`(showArppu), `TopItemsChart`, `GradePivotTable`(월별 실적 상세 3항목 + 분기별 실적 상세 7항목+라인차트), `YoyComparisonChart`(2종) — Iteration 11~14에서 만든 것을 그대로 사용.

## 신규 유틸

| 파일 | 역할 |
|---|---|
| `src/utils/quarterOptions.ts`(+test) | 분기 선택 드롭다운 옵션 목록 생성(2025-1Q ~ 기준 분기, 내림차순) |
| `src/utils/quarterEndDate.ts`(+test) | 선택 분기의 마지막 날짜 계산(각 API의 `endDate` 파라미터로 사용) |

## 검증 계획

- `npm run build`, `npm run lint`, `npm run test`
- 브라우저 수동 확인(Playwright): 분기 탭 전 구간 렌더링, 피벗 테이블 조회 항목 전환 동작 확인
