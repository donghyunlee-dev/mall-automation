# dashboard-order-persistence Test Result

## Scope

- `godo_order`/`godo_order_item` 영속성 계층(Entity/JpaRepository/Port/RepositoryImpl)의 upsert·조회 동작 검증.
- 전체 백엔드 테스트 스위트 회귀 확인.

## Environment

- Profile: `local` (기본 프로파일)
- Database: SQLite (`sfood-mall-automation_test.db`, `@DataJpaTest` + `SharedSqliteTestDatabase`)
- External dependencies: 없음

## Commands

- `./gradlew test --tests "com.sfood.automation.dashboard.infrastructure.persistence.GodoOrderRepositoryImplTest"`
- `./gradlew test --tests "com.sfood.automation.dashboard.infrastructure.persistence.GodoOrderItemRepositoryImplTest"`
- `./gradlew test` (전체 스위트)

## Results

- `GodoOrderRepositoryImplTest`: naive(always-insert) 구현으로 1차 실행 → `should_update_existing_row_instead_of_inserting_duplicate_when_order_no_already_exists` **RED**(`order_no` UNIQUE 제약 위반, `SQLiteException`). upsert를 "조회 후 갱신"으로 수정 → 3 tests **GREEN**.
- `GodoOrderItemRepositoryImplTest`: 동일 패턴으로 naive 구현 1차 실행 → `should_update_existing_row_instead_of_inserting_duplicate_when_type_and_line_no_already_exist` **RED**(`(item_type, godo_line_no)` UNIQUE 제약 위반). 수정 후 3 tests **GREEN**.
- 전체 스위트 재실행: **436 tests, 0 failed, 0 errors, 4 skipped** → `BUILD SUCCESSFUL` (5m 28s).
- PASS: 436/436 (스킵 4건은 기존부터 skip 처리되어 있던 케이스, 이번 변경과 무관).
- FAIL: 없음.

## Failure Notes

- 두 리포지토리 모두 의도적으로 naive 구현을 먼저 만들어 RED를 실제로 관찰했고, 두 실패 모두 예상한 원인(자연키 UNIQUE 제약 위반)으로 발생했음을 확인함. 우연히 통과하거나 엉뚱한 이유로 실패한 케이스는 없었음.

## Retest Results

- upsert 로직 수정 후 각 테스트 클래스 단독 실행: PASS.
- 전체 스위트 재실행: PASS (436/436).

## Acceptance Criteria Coverage

- AC1 (주문 upsert 시 행 1개 유지 + 갱신): 충족 — `should_update_existing_row_instead_of_inserting_duplicate_when_order_no_already_exists`.
- AC2 (아이템 upsert 시 행 1개 유지 + 갱신): 충족 — `should_update_existing_row_instead_of_inserting_duplicate_when_type_and_line_no_already_exist`.
- AC3 (같은 line_no, 다른 item_type은 별개 행): 충족 — `should_allow_same_line_no_for_different_item_types`.
- AC4 (없는 order_no 조회 시 빈 Optional): 충족 — `should_return_empty_when_order_no_not_found`.
- AC5 (기존 테스트 회귀 없음): 충족 — 436/436 통과.
