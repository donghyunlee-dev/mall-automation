# Iteration 11 Plan — 대시보드 화면 (일간 탭)

> 작성일: 2026-09-04
> 상태: 진행
> 의존: 백엔드 `feat/dashboard-order-schema`(develop 반영 완료) — `GET /api/dashboard/*` 7개 API
> 참조: `docs/dashboard-integration/sfood-mall-dashboard-current-state.md`(저장소 루트) 6장, 9.7 5번 단계

---

## 목표

에쓰푸드몰 판매 대시보드(Streamlit)를 React로 재구현하는 첫 단계로, 상단 메뉴에 "대시보드"를 추가하고 로그인 후 기본 진입 화면으로 만든다. 이번 이터레이션은 **일간 탭만** 완전히 구현하고, 나머지 3개 탭(주간/월간/분기)은 다음 이터레이션으로 넘긴다.

---

## 사전 확인 사항

| 항목 | 확인 방법 |
|------|----------|
| 목업 사용 여부 | 사용자 결정: **목업 생략**, 원본 Streamlit 화면(분석 문서 6.2절)과 최대한 동일하게 구현 |
| 백엔드 API 응답 스키마 | `backend/src/main/java/com/sfood/automation/dashboard/application/dto/*`, `DashboardController` 직접 확인 |
| 차트 라이브러리 | Chart.js + react-chartjs-2 (이전 세션에서 확정) |
| "대시보드" 명칭 충돌 | `src/api/teams.ts`의 Teams 알림 버튼 문구를 "대시보드 바로가기" → "상품 관리 바로가기"로 변경해 해소 |

---

## 범위

### 신규 파일

| 파일 | 역할 |
|------|------|
| `src/types/dashboard.ts` | 7개 API 요청/응답 타입 |
| `src/api/dashboard.ts` | 7개 API 호출 함수 |
| `src/hooks/useDailyDashboard.ts` | 일간 탭 데이터 병렬 조회 훅 |
| `src/utils/formatNumber.ts` | 원화/건수/소수점 포맷 유틸 |
| `src/utils/gradeDetailTotal.ts` | 등급별 상세 테이블 합계 행 계산 |
| `src/components/salesDashboard/chartSetup.ts` | Chart.js 컨트롤러/엘리먼트 등록 |
| `src/components/salesDashboard/MetricCards.tsx` | 핵심 지표 카드 7개 |
| `src/components/salesDashboard/HourlyRevenueChart.tsx` | 시간대별 매출 영역 차트 |
| `src/components/salesDashboard/GradeRevenuePie.tsx` | 등급별 매출 비중 도넛 차트 |
| `src/components/salesDashboard/GradeDetailTable.tsx` | 등급별 상세 지표 테이블 |
| `src/components/salesDashboard/TopItemsChart.tsx` | 인기 품목 가로 막대 차트 |
| `src/components/salesDashboard/RevenueTrendChart.tsx` | 매출/주문 콤보(막대+라인) 차트 |
| `src/pages/DashboardPage.tsx` | 탭 컨테이너 + 일간 탭 레이아웃 |

### 기존 파일 수정

| 파일 | 변경 내용 |
|------|----------|
| `src/App.tsx` | `/dashboard` 라우트 추가, 루트(`/`) 및 알 수 없는 경로 기본값을 `/dashboard`로 변경 |
| `src/components/layout/Header.tsx` | "대시보드" 네비게이션 탭 추가(최좌측) |
| `src/api/teams.ts` | 명칭 충돌 해소(위 참고) |
| `package.json` | `chart.js`, `react-chartjs-2` 의존성 추가 |

### 의도적으로 단순화한 부분 (원본과 완전 동일하지 않은 지점)

- 일간 탭만 구현. 주간/월간/분기는 "다음 이터레이션에서 구현" 플레이스홀더만 표시.
- 잠봉 슬라이스 전용 별도 차트는 만들지 않음 — 백엔드 `top-items` API가 이미 롤업 처리된 항목을 포함해 순위 리스트로 내려주므로, 인기 품목 차트 하나로 통합.
- "데이터 최신화" 버튼은 캐시가 없는 설계(9.5 확정)이므로 단순 재조회만 수행.

---

## 검증 계획

- `npm run build`, `npm run lint`, `npm run test` (프론트 Done Criteria).
- 브라우저 수동 확인: 로컬 mock 서버(`mock-server.js`, `express`/`cors` devDependency 설치) + Playwright로 이메일 OTP 로그인부터 `/dashboard` 일간 탭까지 실제 확인. 결과는 `docs/test/results/iteration-11-test-result.md`, `docs/reports/iteration-11-result.md` 참조.
