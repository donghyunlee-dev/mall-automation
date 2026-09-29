# dashboard-order-persistence Tasks

## Implementation Tasks

- [x] `GodoOrderEntity`, `GodoOrderJpaRepository` 작성
- [x] `GodoOrder` 도메인 레코드, `GodoOrderRepository` 포트 작성
- [x] `GodoOrderRepositoryImpl` — 최초 naive 구현(RED 유도) → upsert 로직 구현(GREEN)
- [x] `GodoOrderItemEntity`, `GodoOrderItemJpaRepository` 작성
- [x] `GodoOrderItem` 도메인 레코드, `GodoOrderItemRepository` 포트 작성
- [x] `GodoOrderItemRepositoryImpl` — 동일하게 TDD로 upsert 구현

## Test Tasks

- [x] `GodoOrderRepositoryImplTest` — 신규 삽입 / 중복 upsert / 조회 실패 케이스
- [x] `GodoOrderItemRepositoryImplTest` — 신규 삽입 / 중복 upsert / item_type 구분 케이스
- [x] 각 테스트 RED 확인 후 구현, GREEN 확인
- [x] 전체 스위트(`./gradlew test`) 회귀 확인

## Documentation Tasks

- [x] 본 태스크 문서(spec/plan/tasks) 작성 — test-result/result는 전체 테스트 결과 확인 후 마무리

## Progress Notes

- 두 리포지토리 모두 "naive(always insert) 구현 → UNIQUE 제약 위반으로 RED 확인 → 조회 후 갱신하는 upsert로 수정 → GREEN" 순서로 TDD 사이클을 실제로 거쳤다(레드를 눈으로 확인함).

## Review Notes

- (없음)
