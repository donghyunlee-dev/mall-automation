# Iteration 17 Test Result — UI 개선 (R13~R20)

> 실행일: 2026-09-07
> 담당: Sohee Kang

---

## 빌드 검증

```
npm run build
```

결과: ✅ TypeScript 오류 없음. 빌드 성공.

---

## 린트 검증

```
npm run lint
```

결과: ✅ 오류·경고 0 (`--max-warnings 0`)

---

## 테스트 실행

```
npm run test
```

결과: ✅ 35 파일 / 170 테스트 전체 통과 (기존 테스트 회귀 없음. R13~R20은 스타일/차트 옵션 변경 위주라 신규 프론트 단위 테스트는 추가하지 않았고, 아래 브라우저 수동 확인으로 검증했다)

---

## 백엔드 테스트 실행 (R19 잠봉 슬라이스 판매 비중)

```
./gradlew test --tests "com.sfood.automation.dashboard.*" --tests "com.sfood.automation.batch.*"
```

결과: ✅ 전체 통과. `GetDashboardJambongBreakdownUseCaseTest`(개별 SKU 매출 미롤업, "한정" 상품 제외, 잠봉 미판매 시 빈 목록 3개 테스트) TDD로 RED → GREEN 확인. `DashboardControllerTest`에 `should_return_jambong_breakdown` 추가.

---

## 브라우저 수동 확인

로컬 백엔드(포트 8090, 실 Godo 동기화 데이터) + Vite dev 서버로 Playwright에서 이메일 OTP 로그인 → `/dashboard`에서 4개 탭(일간/주간/월간/분기) 전체를 확인했다.

| 항목 | 결과 |
|------|------|
| R13: 탭별 날짜 입력(`<input type="date">`/`<select>`)에 디자인 토큰 스타일(`styles.dateInput`/`styles.selectInput`) 적용 | ✅ 확인 (4개 탭) |
| R14: "데이터 최신화" 버튼에 `styles.refreshBtn` 스타일 적용 | ✅ 확인 (4개 탭) |
| R15: 막대+꺾은선 콤보 차트에서 선이 막대 뒤로 가려지던 문제 — `order` 속성으로 그리기 순서 조정 | ✅ 확인 — 모든 콤보 차트(추이, 전년 동기 비교)에서 선이 막대 위에 정상 노출 |
| R15 관련 회귀: `order` 조정 직후 `Filler` 플러그인의 선 아래 영역 채우기가 막대를 완전히 가리는 회귀 발생 → `fill: false` 추가로 수정 | ✅ 확인 — 월간/분기 탭 "전년 동월(동분기) 비교" 차트에서 막대 정상 노출 재검증 완료 |
| R16: 가로형 막대 차트(인기 품목 Top 6, 잠봉 슬라이스 판매 비중)에 수치 라벨(`chartjs-plugin-datalabels`) 표시 | ✅ 확인 (4개 탭) |
| R17: 원형(도넛) 차트 안에 비중(%) 라벨 표시 | ✅ 확인 (4개 탭 — 예: 월간 88.5%, 분기 88.2%, 일간 82.5%) |
| R18: 막대 차트 범례에서 꺾은선 항목(주문건수 등)이 네모가 아닌 선 모양 아이콘으로 표시 | ✅ 확인 — `pointStyle: 'line'` + `usePointStyle: true` 적용 |
| R19: 잠봉 슬라이스 개별 판매 비중 차트(신규 `/api/dashboard/jambong-breakdown`) | ✅ 확인 — 4개 탭 모두 인기 품목 차트 아래 "🥩 잠봉 슬라이스 판매 비중" 차트 노출, 잠봉 미판매 구간에서는 조건부 미노출 로직 정상 |
| R20: 월간/분기 탭 등급별 매출액 추이(피벗 하단 라인 차트) 타이틀 미표시 — `chartSetup.ts`에 `Title` 플러그인 미등록이 원인 | ✅ 확인 — `Title` 플러그인 등록 후 탭별 타이틀 정상 노출 |
| 콘솔 에러 없음 | ✅ 확인 |

### 확인 중 발견·수정한 이슈

`YoyComparisonChart.tsx`에 R15 대응으로 `order`/`pointStyle`만 추가하고 `fill: false`를 빠뜨려, 전역 등록된 `Filler` 플러그인이 선 데이터셋의 영역을 자동으로 채우면서 막대 차트를 완전히 가리는 회귀가 발생했다. 월간 탭 "전년 동월 비교" 스크린샷에서 발견 → `fill: false` 추가로 수정, 동일 구조인 `RevenueTrendChart.tsx`에도 방어적으로 동일 수정 적용 후 재검증했다.

### 참고

- R13은 사용자 확인에 따라 네이티브 `<input>`/`<select>`를 디자인 토큰으로 스타일링하는 범위로 한정했다(커스텀 캘린더 컴포넌트 신규 구현 아님).
- R19는 기존 `topItemsByOrderDate/ShippedDate` 리포지토리 메서드를 재사용하되, 기존 인기 품목 조회(`GetDashboardTopItemsUseCase`)가 잠봉 3종을 하나로 롤업하는 것과 달리 개별 SKU 단위로 필터링하는 별도 유스케이스(`GetDashboardJambongBreakdownUseCase`)로 구현했다.
