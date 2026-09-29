# PRD — 에쓰푸드몰 판매 대시보드 통합

> Product Requirements Document
> 참조 문서: `sfood-mall-dashboard-current-state.md`(원본 분석/설계), `comparison-report.md`(원본 vs 재구현 비교 결과)
> 작성일: 2026-09-07
> 상태: 진행 중 — 재구현 완료, 원본 대비 정합성/스타일 보정 단계

---

## 📋 문서 목적

본 문서는 Streamlit 판매 대시보드(`sfood-mall-dashboard`)를 메인 서비스 React 화면으로 재구현하는 프로젝트 전체를 **Iteration 단위**로 추적한다.

- Iteration 11~15: 4개 탭(일간/주간/월간/분기) 최초 재구현 — **완료**
- Iteration 16~ : 원본과의 비교(`comparison-report.md`)에서 발견된 데이터 정합성·스타일 차이를 단계별로 보정 — **이 문서에서 신규 정의**

각 Iteration은 독립적으로 구현·검증·완료 가능해야 하며, 완료 시 `frontend/docs/plan/iteration/iteration-XX-plan.md` + `test-result.md` + `result.md` 3종 문서를 남긴다(백엔드 변경이 포함되면 `backend/docs/tasks/<task-name>/`에도 동일 원칙 적용).

---

## 1. 완료된 작업 (Iteration 11~15 + 백엔드)

| Iteration/작업 | 내용 | 상태 |
|---|---|---|
| 백엔드 스키마/적재 | `godo_order`/`godo_order_item` 테이블, `GodoOrderApiClient`/`GodoOrderResponseParser`, `OrderSyncBatchJob`(21일 롤링+페이지네이션) | ✅ 완료 |
| 백엔드 조회 API | `GET /api/dashboard/*` 7종(summary/hourly-revenue/grade-revenue/grade-detail/top-items/revenue-trend/grade-pivot/yoy-comparison), QueryDSL 기반 | ✅ 완료 |
| 백엔드 `dateBasis` 확장 | `revenue-trend`/`grade-pivot`에 `DateBasis`(ORDER/SHIPPED) 파라미터 추가(월간 탭의 일별 출고 추이용) | ✅ 완료(PR #209) |
| Iteration 11 | 일간 탭 | ✅ 완료(PR #207) |
| Iteration 12 | 주간 탭 | ✅ 완료(PR #208) |
| Iteration 13 | 월간 탭 Phase A(핵심지표8개/일별출고추이/등급별비중·상세/인기품목) | ✅ 완료(PR #210) |
| Iteration 14 | 월간 탭 Phase B(일별·월별 실적상세 피벗/전년동월비교/최근2개년추이) | ✅ 완료(PR #211) |
| Iteration 15 | 분기 탭(월간 탭 컴포넌트 100% 재사용) | ✅ 완료(PR #212) |
| 라이브 검증 | 실제 Godo 주문 API로 `order-sync` 1회 수동 실행, 대시보드 API 응답을 원시 SQL과 교차 검증, `order-sync` 기본 활성화(`enabled=1`) | ✅ 완료(PR #213) |
| **원본 대비 비교** | 원본 Streamlit(`sfood-mall-dashboard-beta.streamlit.app`)과 동일 시점 실데이터로 4개 탭 전수 비교 | ✅ 완료 — `comparison-report.md` |

---

## 2. 비교에서 발견된 문제 → 요구사항 정의

`comparison-report.md`의 발견 사항을 구현 가능한 요구사항으로 변환한다. 우선순위(P0=가장 급함)는 comparison-report.md 6장 기준.

### R1 (P0) — 비회원(게스트) 주문 포함

- **현재**: `GodoOrderResponseParser`가 `memberId`가 빈 주문을 전부 제외 → 비회원 주문이 통째로 누락, 매출/건수/주문자수가 원본 대비 4.7~8.6% 축소 집계
- **요구사항**: 비회원 주문도 수집 대상에 포함하고, 등급을 "비회원"으로 식별해 모든 집계(summary/grade-revenue/grade-detail/grade-pivot/top-items)에 반영
- **영향 범위**: 백엔드(`GodoOrderResponseParser`, 등급 저장/식별 방식), 4개 탭 전체

### R2 (P0) — 회원등급 롤업("회원등급명_요약") 적용

- **현재**: "사업자회원_화이트/로제/VIP/레드"가 개별 등급으로 노출됨. 원본은 이를 "사업자회원" 하나로 합산하고, 최종적으로 "사업자회원/비회원/일반회원/기능장회원/B2B거래처" 5종만 노출
- **요구사항**: 화면 표시(또는 API 응답)에 등급 롤업 규칙 적용 — `사업자회원_*` 패턴은 "사업자회원"으로 합산, 나머지는 원본 그대로
- **영향 범위**: 등급별 매출 비중(도넛)/등급별 상세 테이블/일별·월별·분기별 실적 상세 피벗과 하단 등급별 라인 차트 — 4개 탭 전체

### R3 (P1) — 게스트 주문 오분류 조사·수정

- **현재**: R1 적용 전 상태에서도 "일반회원" 등급 수치가 원본 대비 최대 3배 차이 — 게스트 주문 일부가 `member_grade`를 판정할 수 없어 "일반회원"으로 잘못 떨어지는 것으로 추정
- **요구사항**: 실제 Godo API 응답에서 게스트 주문의 `memId`/등급 필드가 어떻게 내려오는지 확인 후, R1·R2 구현과 함께 정확히 "비회원"으로 분류되도록 파서 로직 보정
- **선행 조건**: R1 구현 중 실제 데이터로 재검증하면서 함께 처리하는 것이 효율적 — R1과 같은 Iteration에서 다룬다.

### R4 (P1) — 2년 백필 기능

- **현재**: 21일 롤링 동기화만 존재 — 월간/분기 탭의 "최근 N개년/N분기 추이", "전년 동월/동분기 비교", "월별/분기별 실적 상세" 피벗이 과거 구간 전부 0으로 표시됨
- **요구사항**: 기간 지정 수동 수집(백필) 기능 추가 — 이미 `backend/docs/tasks/dashboard-order-sync/result.md`에 Open Gap으로 등록되어 있던 항목
- **영향 범위**: 백엔드 전용(`OrderSyncBatchJob`에 날짜 범위 지정 실행 경로 추가), 프론트 변경 없음
- **✅ 부분 해결(2026-09-07, Iteration 18)**: 조사 결과 백엔드는 이미 `POST /api/admin/batch-schedules/order-sync/run?targetDate=`로 임의 날짜 지정 실행을 지원하고 있었으나 화면에 노출되지 않았을 뿐이었다. 이를 대시보드 헤더에 공통 컴포넌트로 노출해 **수동으로 21일 간격씩 반복 실행하는 방식의 백필**이 가능해졌다(상세: `frontend/docs/reports/iteration-18-result.md`). 다만 **여러 기간을 자동으로 순회하는 일괄 백필**은 여전히 미구현.

### R5 (P2) — 날짜/기간 라벨 포맷 통일

- **현재**: 원본은 요일 포함(`26-08-15(토)`), 축약 연월(`25-01`), 축약 분기(`25-1Q`) 등 읽기 쉬운 포맷을 쓰는데, 우리는 대부분 ISO 전체 날짜를 그대로 노출
- **요구사항**: 각 탭의 트렌드 차트·피벗 테이블 컬럼 헤더 라벨을 원본과 동일한 축약 포맷으로 변환하는 공용 포맷터 도입
- **영향 범위**: 프론트(`RevenueTrendChart`, `GradePivotTable` 등 라벨 생성 지점)

### R6 (P2) — 인기 품목 차트 스타일(그라데이션 + 수량 라벨)

- **현재**: 단색 막대, 수량은 툴팁에서만 확인 가능
- **요구사항**: 매출액 크기에 따른 색상 그라데이션 + 막대에 수량 라벨 상시 표시
- **영향 범위**: 프론트(`TopItemsChart`)

### R7 (P2) — AOV 컬럼 헤더 탭별 구분

- **현재**: 4개 탭 전부 "객단가"
- **요구사항**: 원본과 동일하게 일간/주간은 "객단가", 월간/분기는 "AOV"
- **영향 범위**: 프론트(`GradeDetailTable`, `GradePivotTable`에 헤더 라벨 prop 추가)

### R8 (P3) — 차트 축 제목 추가

- **요구사항**: 원본처럼 X/Y축에 제목(예: "주문시간", "매출액") 표시
- **영향 범위**: 프론트(각 Chart.js `options.scales.*.title`)

### R9 (P3) — 등급별 상세 테이블 CSV 다운로드/검색 기능 여부 결정

- **현재**: 정적 HTML 테이블(원본은 Streamlit `st.dataframe`이라 컬럼 표시/숨김·CSV 다운로드·검색·전체화면이 기본 제공)
- **요구사항**: 실제 업무에 필요한 기능인지 사용자 확인 후 범위 확정(구현 여부 자체가 결정 필요 사항)
- **✅ 결정 완료(2026-09-09)**: 실제 업무에 불필요 — 미구현으로 확정, 스코프에서 제외

### R10 (P1) — 잠봉 슬라이스 개별 판매 비중 차트 구현 — ✅ 결정 완료(구현 확정)

- **현재**: Iteration 11에서 "top-items API가 롤업 처리하므로 생략"으로 의도적 결정했으나, 2026-09-07 사용자 확인 결과 **4개 탭 전부에 필요**한 것으로 확정
- **요구사항**: 원본과 동일하게 인기 품목 차트 아래에 "🥩 잠봉 슬라이스 판매 비중" 전용 가로 막대 차트 추가 — 잠봉 슬라이스 400g(냉동)/500g(냉동)/500g(냉장) 3개 SKU의 개별 매출·수량
- **영향 범위**: 백엔드(신규) + 프론트(신규) — 상세는 R19 참고

### R11 (P4) — 도넛 차트 hole 비율

- **현재**: Chart.js 기본 cutout(~50%), 원본은 `hole=0.4`(40%)
- **요구사항**: `cutout: '40%'`로 맞춤

### R12 (P4) — 페이지 상단 안내 문구/카드 라벨 접두어

- **현재**: 월간/분기 탭에 원본에 있는 안내 문구(`gray_memo`)가 없고, 핵심 지표 첫 카드 라벨이 탭 공통("총 매출 (VAT-)")으로 원본의 탭별 접두어("월간 총 매출", "분기 총 매출")와 다름
- **요구사항**: 문구 통일 여부 결정(우선순위 최하 — 기능에 영향 없음)
- **✅ 완료(2026-09-09)**: 원본과 통일 — 월간/분기 탭 상단 안내 문구 추가, 첫 카드 라벨에 탭별 접두어("월간 총 매출 (VAT-)", "분기 총 매출 (VAT-)") 적용

### R13 (P1) — 탭별 날짜 선택 컴포넌트 교체

- **현재**: 일간/주간 탭은 브라우저 기본 `<input type="date">`(무스타일), 월간/분기 탭은 기본 `<select>` — 다른 페이지(`MemberSearchBar`, `HistoryTable`)도 동일하게 무스타일 네이티브 입력을 쓰고 있어 재사용할 기존 디자인 컴포넌트가 없음
- **요구사항**: 디자인 토큰(`_variables.scss`)을 적용한 커스텀 스타일 날짜 선택 컴포넌트로 교체
- **미결 사항**: "다른 디자인된 컴포넌트"의 구체적 참조 디자인(목업)이 없음 — 네이티브 `<input type="date">`를 디자인 토큰으로 스타일링하는 수준으로 진행할지, 완전한 커스텀 캘린더 팝업까지 구현할지 착수 시 확인 필요

### R14 (P1) — "데이터 최신화" 버튼 스타일 적용

- **현재**: `<button>` 엘리먼트이긴 하나 CSS 클래스가 없어 브라우저 기본 버튼 스타일 그대로 노출됨
- **요구사항**: `LoginPage`의 `.primaryBtn` 패턴(디자인 토큰 `var(--teal)`, `var(--r-md)`)을 재사용해 버튼답게 스타일링

### R15 (P0) — 콤보 차트에서 꺾은선이 막대에 가려짐

- **원인 확인됨**: `RevenueTrendChart`/`YoyComparisonChart`가 막대(bar)·꺾은선(line) 두 데이터셋에 `order`를 지정하지 않아 Chart.js 기본 렌더링 순서상 막대가 위로 그려짐
- **요구사항**: 막대 `order`를 꺾은선보다 크게 지정해 꺾은선이 항상 위에 보이도록 수정

### R16 (P1) — 가로 막대 차트(인기 품목)에 수치 라벨 표시

- **현재**: `TopItemsChart`는 막대만 있고 수량/금액 라벨이 없음(툴팁에서만 확인 가능) — comparison-report.md R6와 동일 이슈, 이번에 함께 처리
- **요구사항**: 원본처럼 막대 끝에 수량(개) 라벨 상시 표시

### R17 (P1) — 파이/도넛 차트 내부 수치 라벨 표시

- **현재**: `GradeRevenuePie`는 범례만 있고 조각별 수치(%, 금액)가 차트 위에 표시되지 않음
- **요구사항**: 각 조각에 비율(%) 또는 금액 라벨 표시

### R18 (P1) — 콤보 차트 범례 아이콘을 시리즈 타입에 맞게 통일

- **현재**: `주문건수`(꺾은선) 시리즈의 범례 아이콘이 네모(bar 기본 아이콘)로 표시됨
- **요구사항**: `usePointStyle`+`pointStyle: 'line'`로 꺾은선 시리즈는 선 모양 범례 아이콘 사용

### R19 (P1) — 잠봉 슬라이스 판매 비중 차트 신규 구현 (R10 실행)

- **현재**: 미구현 확인됨(코드베이스 전체에 "잠봉" 문자열 없음) — Iteration 11에서 "top-items API가 이미 롤업 처리"라는 이유로 생략했으나, top-items는 3개 SKU를 **합산 롤업한 결과만** 반환하고 개별 SKU 데이터는 애초에 조회하지 않음
- **요구사항**: 잠봉 슬라이스 3개 SKU(냉동 400g/냉동 500g/냉장 500g)의 개별 매출·수량을 반환하는 백엔드 로직 신규 추가 + 인기 품목 차트 아래 전용 가로 막대 차트 컴포넌트 신규 추가, 4개 탭 전부 적용
- **영향 범위**: 백엔드(`GetDashboardTopItemsUseCase` 또는 신규 UseCase), 프론트(신규 컴포넌트, 4개 탭에 배치)

### R20 (P0) — 월간/분기 탭 "등급별 {항목} 추이" 타이틀 미출력

- **원인 확인됨**: `GradePivotTable`이 Chart.js `plugins.title`을 설정하지만, `chartSetup.ts`에 `Title` 플러그인이 **등록되지 않아** 조용히 무시됨
- **요구사항**: `chartSetup.ts`에 `Title` 임포트·등록 추가

### R21 (신규) — 인기 품목/잠봉 차트 타이틀 접미사 및 X축 제목 누락

- **현재**: `TopItemsChart` 타이틀에 "(주문금액 & 주문수량)" 접미사가 없고, X축 제목("주문금액 (원)")도 없음
- **발견 경위**: 2026-09-09 사용자가 원본 서비스에 직접 접속(제공된 접속 비밀번호)해 실측 비교하여 발견
- **요구사항**: 원본과 동일하게 타이틀 접미사 + X축 제목 추가
- **✅ 완료(2026-09-09)**: `frontend/docs/reports/iteration-24-result.md` 참고. 도넛:트렌드 차트 비율(`.grid2`)도 이번에 실측했으나 이미 원본과 동일(2:1)함을 재확인 — 변경 없음

### R22 — 일간/주간 탭 핵심 지표 카드 라벨 접두어 누락

- **현재**: R12(Iteration 22)는 월간/분기 탭에만 "월간 총 매출 (VAT-)"/"분기 총 매출 (VAT-)" 접두어를 적용
- **발견 경위**: R21 조사 중 원본 일간 탭을 실측한 결과, 원본은 "일간 총 매출 (VAT-)"처럼 **일간/주간 탭에도 탭별 접두어**를 쓴다는 것을 확인 — 당초 R12 결정 시 일간/주간까지는 확인하지 못했던 것으로 보임
- **요구사항**: 일간/주간 탭에도 동일하게 탭별 접두어 적용
- **✅ 완료(2026-09-09)**: `frontend/docs/reports/iteration-25-result.md` 참고

---

## 3. Iteration 로드맵 (R1~R21 반영)

| Iteration | 명칭 | 포함 요구사항 | 영역 | 의존 |
|---|---|---|---|---|
| **Iteration 16** | 회원등급 롤업 + 비회원 포함 | R1, R2, R3 | 백엔드 | ✅ 완료(2026-09-07) — `backend/docs/tasks/dashboard-grade-mapping/result.md` |
| **Iteration 17** | UI 컴포넌트/차트 렌더링 버그 수정 + 잠봉 슬라이스 차트 | R13~R20 | 프론트(+백엔드 R19) | ✅ 완료(2026-09-07) — `frontend/docs/reports/iteration-17-result.md` |
| **Iteration 18** | 주문 동기화 수동 실행 UI (R4 부분 해결) | R4 | 프론트 전용(백엔드 신규 개발 없음) | ✅ 완료(2026-09-07) — `frontend/docs/reports/iteration-18-result.md`. 자동 일괄 백필은 미구현으로 남음 |
| **Iteration 20** | 날짜/기간 라벨 포맷 통일 | R5 | 프론트 | ✅ 완료(2026-09-09) — `frontend/docs/reports/iteration-20-result.md` |
| **Iteration 21** | 차트 스타일 보정(AOV헤더/축제목/도넛 hole) | R7, R8, R11 | 프론트 | ✅ 완료(2026-09-09) — `frontend/docs/reports/iteration-21-result.md` |
| **Iteration 22** | 스코프 확정 필요 항목 처리 | R9, R12 | 결정 후 프론트 | ✅ 완료(2026-09-09) — R9는 제외 결정, R12만 구현. `frontend/docs/reports/iteration-22-result.md` |
| **Iteration 23** | 기존 Streamlit 대시보드 정리(retire) | — | 문서/인프라 | Iteration 16~22 완료 후 |
| **Iteration 24** | 인기 품목/잠봉 차트 타이틀·축 제목 보정 | R21 | 프론트 | ✅ 완료(2026-09-09) — `frontend/docs/reports/iteration-24-result.md` |
| **Iteration 25** | 일간/주간 탭 카드 라벨 접두어 | R22 | 프론트 | ✅ 완료(2026-09-09) — `frontend/docs/reports/iteration-25-result.md` |

> 번호 조정 안내(2026-09-09): 프론트엔드 Iteration 번호가 이 로드맵과 별개로 계속 이어져(회원 관리 등 다른 작업 포함) 19까지 이미 소진되었다. 이 표의 번호를 다음 실제 사용 가능한 번호(20~23)로 한 칸씩 밀어 갱신했다 — 요구사항(R5/R7/R8/R11/R9/R12) 자체는 변경 없음.

**착수 순서 제안**: Iteration 16(R1/R2/R3)이 데이터 정합성에 가장 큰 영향을 주므로 최우선. Iteration 17(R4)은 16과 코드 영역이 겹치지 않아 병행 가능하지만, 순차 진행 시 16 → 17 순서를 권장(등급 롤업이 먼저 반영돼야 백필 데이터도 올바른 등급으로 집계됨).

---

## 4. Iteration 16 상세 설계 — 회원등급 롤업 + 비회원 포함 (R1/R2/R3)

> 아래는 착수 시 바로 구현할 수 있도록 미리 정리한 설계 초안이다. 실제 착수 시 `backend/docs/tasks/dashboard-grade-mapping/` 폴더에 spec.md/plan.md로 재정리한다(레포 컨벤션).

### 4.1 확인된 등급 매핑 규칙 (comparison-report.md 1.2 기준)

| 원시 등급(현재 우리 시스템) | 원본 표시 등급(목표) |
|---|---|
| 사업자회원_화이트 | 사업자회원 |
| 사업자회원_로제 | 사업자회원 |
| 사업자회원_VIP | 사업자회원 |
| 사업자회원_레드 | 사업자회원 |
| 일반회원 | 일반회원 (그대로) |
| 기능장회원 | 기능장회원 (그대로) |
| B2B 거래처 | B2B 거래처 (그대로) |
| (memberId 없음 → 현재 제외됨) | 비회원 (신규) |

패턴 규칙: `member_grade`가 `사업자회원_`로 시작하면 "사업자회원"으로 합산, 그 외는 그대로 유지. 비회원은 별도 처리(4.2 참고).

### 4.2 구현 방향(안 — 착수 시 재검토)

1. `GodoOrderResponseParser`에서 `memberId.isBlank()` 조건으로 주문을 건너뛰지 않고, 대신 `memberGrade`를 "비회원"으로 고정 저장.
   - **선행 조사 필요(R3)**: 실제 Godo API가 게스트 주문에 `memId`를 완전히 빈 문자열로 내려주는지, 아니면 특정 placeholder(예: 공백 아닌 특수값)로 내려주는지 확인 — 이번 라이브 검증에서 "일반회원" 수치 과다 현상의 원인일 가능성이 있으므로 실제 XML 응답을 재확인해야 함.
2. 등급 롤업은 **DB에 저장된 원시 등급은 그대로 두고, 조회 계층(QueryDSL)에서 그룹핑**하는 방식을 우선 검토 — 원시 등급 데이터를 보존해야 추후 세분화된 분석이 필요할 때 재활용 가능. `DashboardQueryRepositoryImpl`의 grade 관련 쿼리(`gradeRevenueBy*`, `gradeDetailBy*`, `gradeDetailByShippedDate` 등)에 `CASE WHEN member_grade LIKE '사업자회원\_%' THEN '사업자회원' ELSE member_grade END` 형태의 그룹핑 표현식 적용.
3. TDD로 진행: 등급 롤업 로직에 대한 단위 테스트(예: `GradeNameMapper` 또는 QueryDSL 표현식 단위 테스트) → RED → GREEN.
4. 라이브 재검증: 백필/재동기화 후 원본과 다시 대조(2026-09-03 등 이미 비교했던 날짜 재사용 가능).

### 4.3 완료 기준 — ✅ 전부 충족 (2026-09-07)

- [x] 비회원 주문이 `godo_order`에 저장되고 "비회원" 등급으로 조회됨
- [x] 등급별 매출 비중/상세 테이블이 원본과 동일한 등급 체계로 표시(사업자회원/비회원/일반회원/기능장회원/[B2B거래처])
- [x] 2026-09-03 기준 재검증 시 총 매출/건수/주문자수가 원본과 오차 1.6% 이내로 수렴
- [x] "일반회원"·"비회원"·"기능장회원" 등급 수치가 원본과 사실상 완전히 일치(R3 우려는 재현되지 않음)
- [x] `./gradlew test` 전체 통과
- [x] 상세 결과는 `backend/docs/tasks/dashboard-grade-mapping/result.md` 참고

**실제 구현은 최초 설계안(4.2)과 세부 사항이 다르다** — 조사 결과 원본 소스코드(`update_data.py`)에서 정확한 규칙(등급명이 아닌 값이 빈 경우만 "비회원", `contains`(포함) 기준 롤업)을 그대로 확인해 이식했다. 프론트엔드 코드 변경은 필요 없었다(등급 목록은 API 응답을 그대로 렌더링하므로 자동 반영).

---

## 5. Iteration 17 상세 — UI 컴포넌트/차트 렌더링 버그 수정 + 잠봉 슬라이스 차트 (R13~R20)

> 2026-09-07 사용자가 실사용 중 직접 발견한 UI 문제 8건. R15(콤보 차트 렌더링 순서)/R20(타이틀 미출력)은 원인이 코드로 명확히 확인된 버그, R19(잠봉 차트)는 결정 완료된 신규 기능, 나머지는 스타일 개선. **✅ 완료(2026-09-07)** — 상세: `frontend/docs/reports/iteration-17-result.md`, `frontend/docs/test/results/iteration-17-test-result.md`

### 5.1 항목별 구현 방향

| 요구사항 | 구현 방향 | 상태 |
|---|---|---|
| R13 캘린더 컴포넌트 | 사용자 확인에 따라 네이티브 `<input type="date">`/`<select>`를 디자인 토큰으로 스타일링 (커스텀 캘린더 컴포넌트는 범위 제외) | ✅ 완료 |
| R14 버튼 스타일 | `DashboardPage.module.scss`에 `.refreshBtn` 추가 | ✅ 완료 |
| R15 콤보 차트 순서 | `RevenueTrendChart`/`YoyComparisonChart`의 bar 데이터셋에 `order: 2`, line 데이터셋에 `order: 1` 추가. 추가로 `Filler` 플러그인 회귀(선 채우기가 막대를 가림) 발견해 `fill: false` 반영 | ✅ 완료 |
| R16 인기품목 라벨 | `TopItemsChart`에 `chartjs-plugin-datalabels`로 수량 라벨 추가 | ✅ 완료 |
| R17 도넛 라벨 | `GradeRevenuePie`에 동일 라벨링 방식으로 비중(%) 표시 | ✅ 완료 |
| R18 범례 아이콘 | `RevenueTrendChart`/`YoyComparisonChart`의 legend 옵션에 `usePointStyle: true`, line 데이터셋에 `pointStyle: 'line'` | ✅ 완료 |
| R19 잠봉 차트 | 백엔드: `GetDashboardJambongBreakdownUseCase` 신규(기존 `topItemsByOrderDate/ShippedDate` 재사용, 롤업 없이 개별 SKU 필터링) → 프론트: 기존 `TopItemsChart` 재사용, 4개 탭에 조건부 배치 | ✅ 완료 |
| R20 타이틀 버그 | `chartSetup.ts`에 `Title` 플러그인 등록 | ✅ 완료 |

### 5.2 완료 기준

- [x] R15/R20은 버그 수정이므로 회귀 확인(기존 차트 렌더링이 깨지지 않는지) 최우선
- [x] R19는 백엔드 신규 로직에 대한 단위 테스트 작성(TDD)
- [x] 4개 탭 전체 Playwright 브라우저 검증(실데이터 기준)
- [x] `npm run build`/`lint`/`test`, `./gradlew test` 통과

---

## 6. 다음 액션

1. Iteration 23(기존 Streamlit 대시보드 정리, retire) — 완료(PR #221 머지됨)
2. Iteration 24(R21, 인기 품목/잠봉 차트 타이틀·축 제목 보정) 완료(2026-09-09) — 원본 서비스 직접 접속 실측으로 확인
3. Iteration 25(R22, 일간/주간 탭 카드 라벨 접두어) 완료(2026-09-09)
4. R1~R22 전부 처리 완료. 남은 항목은 자동 일괄 백필(R4 잔여분)뿐 — 필요 시 별도 착수

