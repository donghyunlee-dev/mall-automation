# Iteration 17 Plan — UI 컴포넌트/차트 렌더링 버그 수정 + 잠봉 슬라이스 차트

> 작성일: 2026-09-07
> 상태: 진행
> 참조: `docs/dashboard-integration/dashboard-prd.md` R13~R20 (2026-09-07 사용자 실사용 피드백)

---

## 목표

실사용 중 발견된 UI 문제 8건을 수정한다. 그중 2건(R15, R20)은 원인이 코드로 명확히 확인된 버그, 1건(R19)은 신규 기능(결정 완료), 나머지는 스타일 개선이다.

## 범위

| # | 항목 | 유형 |
|---|---|---|
| R13 | 탭별 날짜 선택 컴포넌트를 커스텀 스타일로 교체 | 스타일 개선 |
| R14 | "데이터 최신화" 버튼 스타일 적용 | 스타일 개선 |
| R15 | 콤보 차트 꺾은선이 막대에 가려지는 렌더링 순서 버그 | 버그 수정 |
| R16 | 가로 막대 차트(인기 품목)에 수치 라벨 표시 | 스타일 개선 |
| R17 | 파이/도넛 차트 내부 수치 라벨 표시 | 스타일 개선 |
| R18 | 콤보 차트 범례 아이콘을 시리즈 타입에 맞게 통일(선=line 아이콘) | 스타일 개선 |
| R19 | 잠봉 슬라이스 판매 비중 차트 신규 구현(4개 탭) | 신규 기능(백엔드+프론트) |
| R20 | 월간/분기 탭 "등급별 {항목} 추이" 타이틀 미출력 버그 | 버그 수정 |

## 원인 분석 (착수 전 확인 완료)

- **R15**: `RevenueTrendChart`/`YoyComparisonChart`의 bar/line 데이터셋에 `order`를 지정하지 않아 Chart.js 기본 렌더링 순서상 막대가 위로 그려짐.
- **R20**: `GradePivotTable`이 `plugins.title`을 설정하지만 `chartSetup.ts`에 Chart.js `Title` 플러그인이 등록되어 있지 않아 조용히 무시됨.
- **R19**: 코드베이스 전체에 "잠봉" 문자열이 없음(완전 미구현 확인). 기존 `top-items` API는 잠봉 3개 SKU를 이미 합산 롤업한 결과만 반환하므로, 개별 SKU 매출/수량을 반환하는 별도 조회 로직이 필요하다.

## 구현 방향

### 백엔드 (R19)

- `GetDashboardTopItemsUseCase`가 이미 갖고 있는 `JAMBONG_PATTERN`(잠봉 슬라이스 500g|400g, "한정" 제외) 매칭 로직을 재사용해, 롤업하지 않고 개별 상품명 그대로 반환하는 신규 UseCase(`GetDashboardJambongBreakdownUseCase`) + 엔드포인트(`GET /api/dashboard/jambong-breakdown`) 추가. `DashboardQueryRepository`에 새 메서드를 추가하지 않고 기존 `topItemsByOrderDate`/`topItemsByShippedDate`의 원시 데이터(품목별 매출/수량, 롤업 전)를 재사용할 수 있는지 우선 검토 — 안 되면 Repository에 최소 메서드 추가.
- TDD로 진행.

### 프론트

- `RevenueTrendChart`/`YoyComparisonChart`: bar 데이터셋 `order: 2`, line 데이터셋 `order: 1` 추가(R15). `plugins.legend.labels.usePointStyle: true` + line 데이터셋 `pointStyle: 'line'`(R18).
- `chartSetup.ts`: `Title` 플러그인 등록(R20).
- `TopItemsChart`: 막대 끝 수량 라벨 표시(R16) — Chart.js 커스텀 플러그인(추가 의존성 없이 `afterDatasetsDraw` 훅으로 직접 그리기) 또는 CDN 허용 목록에 없는 `chartjs-plugin-datalabels`는 npm 패키지로 설치 검토.
- `GradeRevenuePie`: 조각별 비율(%) 라벨 표시(R17) — 동일 플러그인 방식.
- `JambongRatioChart`(신규): `TopItemsChart`와 유사한 가로 막대 차트, 4개 탭의 인기 품목 차트 아래에 배치.
- `DashboardPage.module.scss`: `.refreshBtn`(R14), 날짜 입력 스타일링(R13, 목업 없어 최소 스타일링으로 진행).

## 검증 계획

- `./gradlew test`(R19 백엔드), `npm run build`/`lint`/`test`
- Playwright로 4개 탭 전체 브라우저 재검증(실데이터 기준) — 특히 R15(선이 보이는지)/R20(타이틀 노출)/R19(잠봉 차트 렌더링) 확인
