# Iteration 11 결과 보고서 — 대시보드 화면 (일간 탭)

> 완료일: 2026-09-04
> 담당: Sohee Kang
> 브랜치: feat/dashboard-frontend

---

## 완료 범위

에쓰푸드몰 판매 대시보드(Streamlit)를 React로 재구현하는 첫 단계로, 상단 메뉴에 "대시보드"를 추가하고 로그인 후 기본 진입 화면으로 만들었다. 원본 화면(`docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 6.2절)을 기준으로 최대한 동일하게 구현했으며, 목업은 별도로 만들지 않았다(사용자 확정).

이번 이터레이션은 **일간 탭만** 완전히 구현했다. 주간/월간/분기 탭은 플레이스홀더만 표시하고 다음 이터레이션으로 넘긴다.

### 일간 탭 구성
- 핵심 지표 카드 7개(총 매출/주문 건수/주문자 수/주문 빈도/건당 SKU/건당 수량/AOV)
- 시간대별 매출 추이(영역 차트)
- 등급별 매출 비중(도넛 차트)
- 등급별 상세 지표 테이블(합계 행 포함)
- 일간 인기 품목 Top 6(가로 막대 차트)
- 최근 20일 매출/주문 콤보 차트(막대+라인, 듀얼 Y축)
- 날짜 선택 + "데이터 최신화"(캐시 없는 설계이므로 단순 재조회)

### 네비게이션/라우팅
- 상단 메뉴 최좌측에 "대시보드" 탭 추가
- 로그인 후 기본 진입 경로(`/`, 알 수 없는 경로)를 `/dashboard`로 변경

---

## 주요 결정 사항

| 결정 | 이유 |
|------|------|
| 일간 탭만 우선 구현 | 사용자 확정 — 나머지 3개 탭은 다음 이터레이션 |
| 목업 생성 생략, 원본 화면 기준으로 직접 구현 | 사용자 확정 — "원본 화면 기반으로 최대한 동일하게 체크하면서 개발" |
| 잠봉 슬라이스 전용 차트 미생성 | 백엔드 `top-items` API가 이미 롤업된 항목을 순위 리스트로 내려주므로 인기 품목 차트 하나로 통합 |
| Chart.js + react-chartjs-2 | 이전 세션에서 확정. v4는 컨트롤러(`BarController`/`LineController`/`DoughnutController`)를 요소와 별도로 명시 등록해야 함 — 코드 리뷰 중 누락 발견해 추가 |
| "대시보드" 명칭 충돌 해소 | `src/api/teams.ts`의 Teams 알림 버튼 문구를 "대시보드 바로가기" → "상품 관리 바로가기"로 변경 |
| `src/components/salesDashboard/` 별도 디렉토리 사용 | 기존 `src/components/dashboard/`는 GoodsPage 소속이라 이름 충돌 방지 |

---

## 신규 파일

| 파일 | 역할 |
|------|------|
| `src/types/dashboard.ts` | 7개 API 요청/응답 타입 |
| `src/api/dashboard.ts` | 7개 API 호출 함수 |
| `src/hooks/useDailyDashboard.ts` | 일간 탭 데이터 병렬 조회 훅 |
| `src/utils/formatNumber.ts`(+test) | 원화/건수/소수점 포맷 유틸 |
| `src/utils/gradeDetailTotal.ts`(+test) | 등급별 상세 테이블 합계 행 계산 |
| `src/components/salesDashboard/chartSetup.ts` | Chart.js 컨트롤러/엘리먼트 등록 |
| `src/components/salesDashboard/MetricCards.tsx` | 핵심 지표 카드 7개 |
| `src/components/salesDashboard/HourlyRevenueChart.tsx` | 시간대별 매출 영역 차트 |
| `src/components/salesDashboard/GradeRevenuePie.tsx` | 등급별 매출 비중 도넛 차트 |
| `src/components/salesDashboard/GradeDetailTable.tsx`(+scss) | 등급별 상세 지표 테이블 |
| `src/components/salesDashboard/TopItemsChart.tsx` | 인기 품목 가로 막대 차트 |
| `src/components/salesDashboard/RevenueTrendChart.tsx` | 매출/주문 콤보(막대+라인) 차트 |
| `src/pages/DashboardPage.tsx`(+scss) | 탭 컨테이너 + 일간 탭 레이아웃 |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `src/App.tsx` | `/dashboard` 라우트 추가, 루트·알 수 없는 경로 기본값을 `/dashboard`로 변경 |
| `src/components/layout/Header.tsx` | "대시보드" 네비게이션 탭 추가(최좌측) |
| `src/api/teams.ts` | 명칭 충돌 해소 |
| `package.json` | `chart.js`, `react-chartjs-2` 의존성 추가; `express`, `cors` devDependency 추가(브라우저 수동 확인용 mock 서버 실행) |

## 로컬 개발 도구 (Git 미추적 아님 — 리포에 포함된 mock 서버)

| 파일 | 변경 내용 |
|------|----------|
| `mock-server.js` | 대시보드 6개 엔드포인트 mock 추가; 세션 쿠키 파싱 버그 수정(아래 참조) |

---

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 29 파일 / 151 테스트 전체 통과
- 브라우저 수동 확인 — ✅ 완료(Playwright, 이메일 OTP 로그인 → `/dashboard` 일간 탭 전 구간 렌더링 확인, 콘솔 에러 없음). 상세 내역은 `docs/test/results/iteration-11-test-result.md` 참조.

### 확인 중 발견한 이슈(수정함)

`mock-server.js`의 `checkAuth`가 `req.headers.cookie?.split('=')[1]`로 세션 쿠키를 파싱하고 있었는데, 브라우저에 다른 쿠키(예: 이전 세션의 잔여 쿠키)가 함께 존재하면 첫 `=` 기준 분할이 깨져 정상 세션도 401 처리되는 버그였다. 대시보드 기능과는 무관한 기존 결함이지만, 로그인 자체가 막혀 이번 브라우저 검증의 전제조건이었으므로 `getCookie` 헬퍼를 추가해 쿠키 이름 기준으로 정확히 파싱하도록 최소 범위로 수정했다.

### 갱신 (2026-09-04): 차트 크기 조정

브라우저 확인 후 사용자 피드백 — 원형 차트와 아래 차트들이 컨테이너 높이 제약 없이 `responsive: true`만 켜져 있어 기본 종횡비대로 커지고, 스크롤이 과도하게 발생하는 문제. `HourlyRevenueChart`/`GradeRevenuePie`/`TopItemsChart`/`RevenueTrendChart` 각각에 고정 높이 wrapper(`*.module.scss`, 220px/280px)를 추가하고 `maintainAspectRatio: false`를 적용해 해결. 재검증 결과 build/lint/test(151개) 통과, Playwright로 1440×900 뷰포트 기준 스크롤 없이 일간 탭 전체가 한 화면에 들어오는 것 확인.

---

## Open Gaps / 다음 단계

- 주간/월간/분기 탭 구현(다음 이터레이션)
- 성능 검증(대량 데이터 기준 응답 속도) — 진행하면서 확인 예정
- Godo 주문 API 실제 연동 검증(`order-sync` 배치, 현재 `enabled=0`으로 비활성 유지)
- 기존 Streamlit 대시보드 정리(retire) — 신규 탭 전체 구현 이후 진행
