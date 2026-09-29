# ATCS-177 Spec

## Feature Specification

- `GET /api/goods` returns `recentUpdatedAt` and `recentUpdatedBy` for each list item
- when `updated_at` and `updated_id` exist, those values are returned as the latest modification metadata
- when either update field is absent because the goods item has never been edited, the response falls back to `created_at` and `created_id`

## API Changes

- goods list response items gain:
  - `recentUpdatedAt`
  - `recentUpdatedBy`
- no request parameters change
- no changes to other goods endpoint response contracts

## Database Changes

- no schema changes
- no data migration changes
- existing `goods.created_id`, `goods.created_at`, `goods.updated_id`, and `goods.updated_at` values are reused

## Domain Model Changes

- no domain model changes
- fallback derivation is handled in the application mapping layer

## Validation Rules

- `recentUpdatedAt` must never be null in the list response
- `recentUpdatedBy` must never be null in the list response
- updated metadata is preferred only when present

## Edge Cases

- goods created but never updated
- goods updated once after registration
- mixed lists where some items have update metadata and others do not

## Exception Scenarios

- if only the list controller test changes, fallback behavior can still regress in the use case mapper
- if the shared goods DTO is modified instead of introducing a list-specific DTO, unrelated endpoints change without requirement coverage

## Acceptance Criteria Mapping

- `품목 리스트 조회 API 최근수정일자, 최근수정자 필드 추가`:
  - satisfied by adding `recentUpdatedAt` and `recentUpdatedBy` to goods list items
- `생성, 수정 데이터 컬럼 확인해서 판단`:
  - satisfied by preferring updated metadata and falling back to created metadata
- `API 조회 시 최근수정일자, 최근수정자 데이터 조회`:
  - verified by controller serialization coverage
- `API 조회 시 최근수정일자, 최근수정자 데이터 null 값이면 실패`:
  - verified by use case tests covering the fallback behavior
