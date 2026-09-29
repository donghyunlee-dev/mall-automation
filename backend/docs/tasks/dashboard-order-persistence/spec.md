# dashboard-order-persistence Spec

## Problem Statement

- `dashboard-order-schema` 태스크에서 `godo_order`/`godo_order_item` 테이블만 만들고 JPA 엔티티/리포지토리는 후속 태스크로 미뤘다. 이번 태스크는 그 다음 단계로, 두 테이블에 대한 Clean Architecture 영속성 계층(Entity + JpaRepository + Domain Port + RepositoryImpl)을 추가한다.
- 배경/설계 근거: `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 9.3.

## Business Context

- 이후 태스크(고도몰 주문 조회 API 클라이언트, `OrderSyncBatchJob`)가 파싱한 주문 데이터를 저장/조회할 때 이 영속성 계층을 사용한다.
- 21일 롤링 재조회 방식(9.2.3⑥)으로 같은 주문이 반복 수집되므로, "이미 있으면 갱신, 없으면 삽입"하는 upsert 동작이 핵심 요구사항이다.

## Scope

- `com.sfood.automation.dashboard` 신규 도메인 모듈 생성.
- `godo_order` ↔ `GodoOrderEntity`/`GodoOrderJpaRepository`/`GodoOrder`(도메인 레코드)/`GodoOrderRepository`(포트)/`GodoOrderRepositoryImpl`.
- `godo_order_item` ↔ 동일 구조(`GodoOrderItem*`).
- `order_no` 단일 UNIQUE, `(item_type, godo_line_no)` 복합 UNIQUE 기준으로 upsert(존재하면 갱신, 없으면 삽입).

## Out Of Scope

- 고도몰 주문 조회 API 클라이언트, `OrderSyncBatchJob`, 대시보드 조회 API, React 화면 — 모두 별도 태스크.
- 삭제/하드 삭제 기능(현재 요구사항에 없음).

## User Or Operator Scenarios

- Scenario 1: 21일 롤링 재조회 배치가 같은 주문을 다시 가져와 `upsert`를 호출하면, 기존 행이 갱신되고 새 행이 추가되지 않는다.
- Scenario 2: 같은 주문의 서로 다른 상품 라인(`orderGoodsData`/`addGoodsData`)이 같은 `godo_line_no` 값을 가지더라도 `item_type`이 다르면 별개 행으로 저장된다(9.2.3⑥/9.3.3 확정 사항).

## Acceptance Criteria

- AC1: `GodoOrderRepository.upsert()`를 같은 `orderNo`로 두 번 호출하면 테이블에 행이 1개만 남고, 두 번째 호출의 값으로 갱신되어 있다.
- AC2: `GodoOrderItemRepository.upsert()`를 같은 `(itemType, godoLineNo)`로 두 번 호출하면 행이 1개만 남고 갱신된다.
- AC3: `godoLineNo`가 같아도 `itemType`이 다르면 별개 행으로 저장된다.
- AC4: 존재하지 않는 `orderNo`로 조회하면 빈 `Optional`을 반환한다.
- AC5: 기존 테스트 스위트가 회귀 없이 통과한다.

## Edge Cases

- upsert 시 도메인 레코드의 `id` 필드는 무시하고, 조회된 기존 엔티티의 실제 PK를 기준으로 갱신한다(신규 삽입 시에는 새 PK 채번).

## Assumptions

- 날짜/금액 컬럼 타입은 `dashboard-order-schema`에서 확정한 대로 문자열(TEXT/VARCHAR)과 `BigDecimal`을 사용한다.
- upsert 판단 기준은 스키마의 UNIQUE 제약과 동일하게 애플리케이션에서도 명시적으로 조회 후 판단한다(DB의 `ON CONFLICT`에 의존하지 않음 — SQLite/PostgreSQL 양쪽에서 동일한 JPA 코드로 동작해야 하므로).

## Clarifications

- (없음 — `dashboard-order-schema`에서 이미 확정된 스키마를 그대로 매핑)
