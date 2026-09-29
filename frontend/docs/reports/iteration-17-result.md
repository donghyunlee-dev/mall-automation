# Iteration 17 결과 보고서 — UI 개선 (R13~R20)

> 완료일: 2026-09-07
> 담당: Sohee Kang (프론트) / Donghyun Kim (백엔드)
> 브랜치: (dev-branch-pr 플로우로 생성)

---

## 완료 범위

사용자가 전달한 8개 UI 수정 사항(R13~R20, `docs/dashboard-integration/dashboard-prd.md` 5장)을 모두 구현했다.

| 요구사항 | 내용 | 처리 |
|---|---|---|
| R13 | 탭별 달력 컴포넌트 UI 개선 | 네이티브 `<input type="date">`/`<select>`를 디자인 토큰으로 스타일링 (사용자 확인에 따라 커스텀 캘린더 신규 구현은 범위 제외) |
| R14 | 데이터 최신화가 버튼 스타일이 아님 | 전용 버튼 클래스(`styles.refreshBtn`) 적용 |
| R15 | 막대+꺾은선 콤보 차트에서 선이 뒤로 가려짐 | Chart.js `order` 속성으로 그리기 순서 조정 |
| R16 | 가로형 막대 차트에 수치 미표현 | `chartjs-plugin-datalabels`로 값 라벨 추가 |
| R17 | 원형 차트 안에 수치 미표현 | 동일 플러그인으로 비중(%) 라벨 추가 |
| R18 | 막대 차트 범례의 꺾은선 항목이 네모로 표시 | `pointStyle: 'line'` + `usePointStyle: true` |
| R19 | 잠봉 슬라이스 판매 비중 차트 미노출 | 신규 백엔드 엔드포인트 + 프론트 조건부 렌더링으로 구현 |
| R20 | 월간/분기 탭 등급별 매출액 추이 타이틀 누락 | `chartSetup.ts`에 `Title` 플러그인 누락 등록 |

---

## 주요 결정 사항

| 결정 | 이유 |
|------|------|
| R13은 네이티브 input 스타일링으로 한정 | 커스텀 캘린더 컴포넌트는 별도 라이브러리 도입과 상당한 작업량이 필요해 사용자에게 확인 후 범위를 좁힘 |
| R19는 별도 유스케이스(`GetDashboardJambongBreakdownUseCase`) 신설 | 기존 `GetDashboardTopItemsUseCase`는 잠봉 3종(500g 냉동/400g 냉동/500g 냉장)을 하나로 롤업해서 보여주므로, 원본과 동일하게 개별 SKU 단위로 보여주려면 롤업하지 않는 별도 조회 경로가 필요했음. 기존 `DashboardQueryRepository.topItemsByOrderDate/ShippedDate`를 그대로 재사용해 리포지토리 변경 없이 구현 |
| R19 차트는 데이터가 있을 때만 조건부 렌더링 | 잠봉이 판매되지 않은 기간(예: 일부 주간 구간)에는 빈 차트 대신 아예 미노출 |
| datalabels 전역 기본값 `display: false` | `chartSetup.ts`에서 전역 비활성화 후 필요한 차트(TopItemsChart, GradeRevenuePie)에서만 개별 활성화 — 기존 다른 차트에 의도치 않은 라벨 노출 방지 |

---

## 신규 파일

| 파일 | 역할 |
|------|------|
| `backend/.../GetDashboardJambongBreakdownUseCase.java`(+test) | 잠봉 슬라이스 개별 SKU 매출 비중 조회 (R19) |
| `frontend/docs/plan/iteration/iteration-17-plan.md` | 이터레이션 계획 (R13~R20 근본 원인 분석 포함) |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `backend/.../DashboardController.java` | `GET /api/dashboard/jambong-breakdown` 엔드포인트 추가 |
| `backend/.../DashboardControllerTest.java` | 신규 엔드포인트 테스트 추가 |
| `frontend/src/components/salesDashboard/chartSetup.ts` | `Title` 플러그인 등록(R20), `chartjs-plugin-datalabels` 등록 및 전역 기본값 비활성화 |
| `frontend/src/components/salesDashboard/RevenueTrendChart.tsx` | 막대/선 `order`·`pointStyle` 조정, 범례 `usePointStyle`(R15/R18), 선 `fill: false`(회귀 방지) |
| `frontend/src/components/salesDashboard/YoyComparisonChart.tsx` | 동일 (R15/R18), 회귀 발견 및 `fill: false` 수정 |
| `frontend/src/components/salesDashboard/TopItemsChart.tsx` | 막대 위 수량 라벨 추가(R16) |
| `frontend/src/components/salesDashboard/GradeRevenuePie.tsx` | 도넛 안 비중(%) 라벨 추가(R17) |
| `frontend/src/api/dashboard.ts` | `getJambongBreakdown` API 함수 추가 |
| `frontend/src/hooks/use{Daily,Weekly,Monthly,Quarterly}Dashboard.ts` | `jambongBreakdown` 병렬 조회 및 반환 추가 |
| `frontend/src/pages/DashboardPage.tsx` | 4개 탭에 날짜/셀렉트/버튼 스타일 클래스 적용(R13/R14), 잠봉 차트 조건부 렌더링(R19) |
| `frontend/src/pages/DashboardPage.module.scss` | `.dateInput`/`.selectInput`/`.refreshBtn` 클래스 추가 |
| `frontend/package.json` | `chartjs-plugin-datalabels` 의존성 추가 |
| `docs/dashboard-integration/dashboard-prd.md` | Iteration 17 항목 완료 체크 |

---

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 35 파일 / 170 테스트 전체 통과
- 백엔드 `./gradlew test` (dashboard/batch 범위) — ✅ 전체 통과, R19 신규 테스트 3개 TDD로 작성
- 브라우저 수동 확인 — ✅ 완료(Playwright, 실 데이터 기준 4개 탭 전체 재검증, R15 관련 회귀 발견·수정 포함). 상세: `docs/test/results/iteration-17-test-result.md`

---

## Open Gaps / 다음 단계

- 사용자가 명시한 다음 작업: **데이터 갱신(동기화) 방법 검증** — 아직 미착수. Iteration 17 병합 후 진행 예정
- Iteration 18(2년치 백필), 19(날짜 라벨 포맷), 20(AOV 헤더/축 제목/도넛 hole 비율), 21(CSV/검색 테이블 — 결정 보류), 22(Streamlit 정리) — `dashboard-prd.md` 로드맵 참고, 미착수
