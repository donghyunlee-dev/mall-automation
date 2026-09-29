# dashboard-order-persistence Result

## Delivered Scope

- `com.sfood.automation.dashboard` 신규 도메인 모듈 생성.
- `godo_order`/`godo_order_item` 테이블에 대한 Clean Architecture 영속성 계층(Entity, JpaRepository, 도메인 레코드, Port, RepositoryImpl) 구현.
- 두 리포지토리 모두 자연키(`order_no` / `(item_type, godo_line_no)`) 기준 upsert(존재하면 갱신, 없으면 삽입) 동작을 TDD로 검증.

## Changed Files

- `backend/src/main/java/com/sfood/automation/dashboard/domain/model/GodoOrder.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/domain/model/GodoOrderItem.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/domain/port/GodoOrderRepository.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/domain/port/GodoOrderItemRepository.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/persistence/GodoOrderEntity.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/persistence/GodoOrderJpaRepository.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/persistence/GodoOrderRepositoryImpl.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/persistence/GodoOrderItemEntity.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/persistence/GodoOrderItemJpaRepository.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/persistence/GodoOrderItemRepositoryImpl.java` (신규)
- `backend/src/test/java/com/sfood/automation/dashboard/infrastructure/persistence/GodoOrderRepositoryImplTest.java` (신규)
- `backend/src/test/java/com/sfood/automation/dashboard/infrastructure/persistence/GodoOrderItemRepositoryImplTest.java` (신규)

## Key Decisions

- **upsert는 애플리케이션에서 명시적으로 "조회 후 갱신"으로 구현**: DB의 `ON CONFLICT`/`INSERT ... ON DUPLICATE KEY` 같은 방언별 문법에 의존하지 않고, JPA로 자연키 조회 후 엔티티를 갱신하는 방식을 택함 — SQLite/PostgreSQL 양쪽에서 동일한 자바 코드로 동작해야 한다는 요구(9.5)에 부합.
- **surrogate PK 유지**: 기존 코드베이스 관례(`GoodsEntity`, `GodoMemberEntity` 등)를 따라 자연키가 있어도 `id`(IDENTITY)를 PK로 두고 자연키는 UNIQUE 제약으로만 둠.
- **naive 구현을 거쳐 실제 RED를 관찰**: 두 리포지토리 모두 "항상 insert"로 먼저 구현해 UNIQUE 제약 위반으로 실패하는 것을 확인한 뒤 upsert 로직으로 교체 — TDD의 "테스트가 실제로 실패하는 이유를 확인한다" 원칙을 지킴.

## Open Gaps

- PostgreSQL 환경에서의 `BigDecimal` 매핑은 실제로 검증하지 못함(SQLite 테스트로만 확인). 운영 반영 시점에 별도 확인 필요.
- 삭제/하드 삭제 기능 없음(현재 요구사항에 없어 미구현).

## Next Steps

- 고도몰 주문 조회 API 클라이언트(`GodoApiClient` 확장 또는 신규 클라이언트) 구현.
- `OrderSyncBatchJob` 구현 — 이번에 만든 `GodoOrderRepository`/`GodoOrderItemRepository`를 사용해 API 응답을 upsert.
- 구현 완료 후 `batch_schedule_config.order-sync`를 `enabled=1`로 전환.
