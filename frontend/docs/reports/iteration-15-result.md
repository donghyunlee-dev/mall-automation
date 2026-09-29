# Iteration 15 결과 보고서 — 대시보드 화면 (분기 탭)

> 완료일: 2026-09-04
> 담당: Sohee Kang
> 브랜치: feat/dashboard-quarterly-tab

---

## 완료 범위

분기 탭을 구현해 **4개 탭(일간/주간/월간/분기) 전체를 완료**했다. 원본 화면(`app.py` L1129~1436, `23169c8` 커밋)과 동일하게 구현했으며, MONTH/QUARTER 버킷은 이미 출고일 기준으로 자동 처리되므로 **신규 백엔드 작업 없이** 진행했다(Iteration 13의 `dateBasis` 확장은 DAY 버킷 한정 이슈였음).

### 분기 탭 구성
- 분기(YYYY-QQ) 선택 드롭다운
- 핵심 지표 카드 8개(ARPPU 포함)
- 분기 내 월별 출고 추이(3개월 콤보 차트) + 등급별 매출 비중(도넛)
- 등급별 상세 지표(AOV/ARPPU) + 분기 인기 품목 Top 6
- 월별 실적 상세 피벗 테이블(분기 내 3개월, 3개 항목 전환)
- 전년 동분기 비교 콤보 차트 2종
- 최근 8분기 추이 콤보 차트
- 분기별 실적 상세 피벗 테이블(8분기, 7개 항목 전환 + 등급별 라인 차트)

---

## 주요 결정 사항

| 결정 | 이유 |
|------|------|
| 신규 UI 컴포넌트 없음 | Iteration 11~14에서 만든 `MetricCards`/`RevenueTrendChart`/`GradeRevenuePie`/`GradeDetailTable`/`TopItemsChart`/`GradePivotTable`/`YoyComparisonChart`를 그대로 재사용 — 월간 탭과 구성이 거의 동일 |
| Phase 분리 없이 한 번에 구현 | 월간 탭과 달리 신규 백엔드 작업이 없고 신규 컴포넌트도 없어 작업량이 작음(주간 탭 수준의 재사용) |
| 분기 목록/분기 말일 계산을 순수 함수로 분리 | `quarterOptions.ts`/`quarterEndDate.ts`를 TDD로 작성해 날짜 경계(연도 전환 등) 검증 |

---

## 신규 파일

| 파일 | 역할 |
|------|------|
| `src/utils/quarterOptions.ts`(+test) | 분기 선택 드롭다운 옵션 목록 생성 |
| `src/utils/quarterEndDate.ts`(+test) | 선택 분기의 마지막 날짜 계산 |
| `src/hooks/useQuarterlyDashboard.ts` | 분기 탭 데이터 병렬 조회 훅 |
| `docs/plan/iteration/iteration-15-plan.md` | 이터레이션 계획 |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `src/pages/DashboardPage.tsx` | `QuarterlyTab` 구현, 플레이스홀더 완전 제거(4개 탭 모두 구현 완료) |
| `src/pages/DashboardPage.module.scss` | 더 이상 쓰이지 않는 `.placeholder` 클래스 제거 |
| `mock-server.js` | `revenue-trend`/`grade-pivot`의 `QUARTER` 버킷 처리 추가(`buildBucketEnds` 공통 헬퍼로 통합) |

---

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 35 파일 / 170 테스트 전체 통과
- 브라우저 수동 확인 — ✅ 완료(Playwright, 분기 탭 전 구간 렌더링 및 피벗 조회 항목 전환 동작 확인, 콘솔 에러 없음). 상세: `docs/test/results/iteration-15-test-result.md`

---

## Open Gaps / 다음 단계

- 4개 탭 전체 구현 완료 — 이제 기존 Streamlit 대시보드 정리(retire) 단계로 진행 가능
- 성능 검증(대량 데이터 기준 응답 속도, 특히 분기별 8회 반복 `grade-pivot` 호출) — 진행하면서 확인 예정
- Godo 주문 API 실제 연동 검증(`order-sync` 배치, 현재 `enabled=0`으로 비활성 유지) — 실데이터 기준 전체 탭 재검증 필요
