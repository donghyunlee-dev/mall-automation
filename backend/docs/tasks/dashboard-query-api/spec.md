# dashboard-query-api Spec

## Problem Statement

- 대시보드 프론트엔드가 화면(일간/주간/월간/분기 4개 탭)을 그릴 수 있도록 조회 전용 API가 필요하다. 9.7 계획의 4번 단계.

## Business Context

- 사용자 결정: 엔드포인트는 탭 단위로 묶지 않고 **데이터 요소별로 세분화**한다(속도 영향 우려). 재사용 가능성이 있는 API는 확장을 고려해 `period` 파라미터로 일/주/월/분기를 공통 처리하도록 설계한다. 복잡한 집계 쿼리는 QueryDSL로 작성한다.

## Scope

계획했던 7개 API 전부를 이번 태스크에서 완성한다(최초에는 `summary` 1개만 수직 슬라이스로 먼저 만들어 패턴을 확립한 뒤, 같은 패턴으로 나머지 6개를 이어서 구현).

- `GodoOrderRepository`가 아닌 별도 **조회 전용 포트** `DashboardQueryRepository` 신설(6.1 지표 정의 기준 집계).
- `DashboardPeriod`(DAILY/WEEKLY/MONTHLY/QUARTERLY) + `DashboardPeriodResolver`(period+파라미터 → 날짜 범위, 그리고 revenue-trend/grade-pivot용 버킷 범위 계산) — 7개 API 전체가 공유하는 재사용 컴포넌트.
- `GET /api/dashboard/summary`: 매출/주문건수/주문자수/주문빈도/평균SKU/평균수량/AOV/ARPPU.
- `GET /api/dashboard/hourly-revenue`: 일간 전용 0~23시 매출.
- `GET /api/dashboard/grade-revenue`: 등급별 매출(도넛차트용).
- `GET /api/dashboard/grade-detail`: 등급별 상세 지표 테이블.
- `GET /api/dashboard/top-items`: 인기 품목 Top N(선물 제외, 잠봉 슬라이스 롤업 포함).
- `GET /api/dashboard/revenue-trend`: N개 구간(일/주/월/분기) 매출·주문 추이 — "최근 20일/5주/2년/8분기"를 이 API 하나로 처리.
- `GET /api/dashboard/grade-pivot`: 등급×기간 매트릭스(지정 metric 기준).
- `GET /api/dashboard/yoy-comparison`: 전년 동월/동분기 비교(MONTHLY/QUARTERLY 전용).

## Out Of Scope

- React 화면 연동.
- 캐시(요구사항상 캐시 없음이 이미 확정 — 구현 안 함).
- 대용량 데이터 기준 성능 튜닝(9.6 열린 질문과 동일 — 실제 데이터 볼륨 확인 전까지는 별도 최적화 안 함).

## User Or Operator Scenarios

- Scenario 1: 프론트가 `GET /api/dashboard/summary?period=DAILY&date=2026-03-24`를 호출하면 그날 주문 기준 지표를 받는다.
- Scenario 2: `GET /api/dashboard/summary?period=QUARTERLY&year=2026&quarter=1`을 호출하면 해당 분기 출고 기준 지표를 받는다.

## Acceptance Criteria

- AC1: DAILY/WEEKLY는 `godo_order.ordered_at` 기준, MONTHLY/QUARTERLY는 `godo_order_item.oms_shipped_at` 기준으로 조회한다(9.3.4 규칙 그대로 이식). `revenue-trend`/`grade-pivot`은 DAY/WEEK 버킷은 주문일자, MONTH/QUARTER 버킷은 출고일 기준.
- AC2: 월간/분기 집계 시, 조회 기간에 걸리는 아이템이 하나라도 있는 주문은 헤더 매출 전체가 1회만 합산된다(9.3.4 "주문 단위 dedup" 규칙 재현).
- AC3: 필수 파라미터가 없으면 400(Bad Request)을 반환한다.
- AC4: 세션 인증 없이 호출하면 401을 반환한다(기존 `SessionAuthInterceptor` 재사용).
- AC5: 주문/아이템이 없는 기간은 0으로 채워진 응답을 반환한다(예외 아님).
- AC6: `top-items`는 "선물" 포함 상품을 제외하고, "잠봉 슬라이스 500g/400g"(단 "한정" 제외)를 하나의 롤업 행으로 합산한다(8번 섹션 5항, 원본 하드코딩 로직 유지 결정 그대로 이식).
- AC7: `grade-pivot`은 특정 버킷에 등급이 존재하지 않으면 0으로 채워 매트릭스 형태를 유지한다.
- AC8: `yoy-comparison`은 DAILY/WEEKLY 요청 시 400을 반환한다(원본 화면에도 월간/분기에만 있는 기능).

## Edge Cases

- 분기 4분기(10~12월) 경계 계산이 연도를 넘기지 않고 정확히 떨어져야 한다.
- 동일 회원이 같은 기간에 여러 번 주문해도 `buyerCount`는 중복 집계되지 않는다.
- `revenue-trend`/`grade-pivot`의 분기 버킷이 연도 경계를 넘어갈 때(예: 2025 Q4 → 2026 Q1) 정확히 계산되어야 한다.

## Assumptions

- ARPPU는 원래 "월간/분기에만 표시"였지만, 백엔드는 모든 period에 대해 항상 계산해서 내려주고 **표시 여부는 프론트가 결정**한다(백엔드 로직 단순화).
- 트랜잭션 격리·동시성 이슈는 조회 전용 API라 고려하지 않는다.
- **"선물" 제외 로직의 적용 범위 한정**: 원본 Streamlit은 데이터 로드 시점에 "선물" 포함 상품 라인을 통째로 제거해, 선물만 담긴 주문의 매출(`결제_final`)까지 간접적으로 집계에서 빠졌다. 신규 스키마는 헤더(주문)/라인(상품) 테이블이 분리되어 있어, 이번 구현은 **`top-items`(품목 랭킹)에서만** "선물" 상품 라인을 제외하고, `summary`/`grade-revenue`/`grade-detail` 등 주문 헤더 기반 매출 집계에는 반영하지 않았다. "선물만 있는 주문"을 매출 집계에서도 빼야 하는지는 현업 확인이 필요한 별도 이슈로 남긴다(8번 섹션 5항과 연계).

## Clarifications

- Question: 엔드포인트를 탭 단위 vs 데이터 단위 중 무엇으로 나눌지, 재사용 설계를 어떻게 할지, 집계 쿼리 기술을 무엇으로 할지?
  Answer: 데이터 단위로 세분화 + period 파라미터로 재사용 + QueryDSL 도입. (사용자 확정)
