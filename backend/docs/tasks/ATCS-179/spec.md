# ATCS-179 Spec

## Feature Specification

- `GET /api/goods/{id}` returns all relevant `goods` table fields already available in the domain model
- newly exposed fields cover the current gaps in the detail response contract:
  - `description`
  - `createdId`
  - `createdAt`
  - `updatedId`
  - `updatedAt`

## API Changes

- extend `GoodsResponse` with the missing fields
- keep existing field names and response semantics unchanged

## Database Changes

- none

## Domain Model Changes

- none

## Validation Rules

- existing path-variable and not-found behavior remain unchanged
- timestamp fields may be `null` when the underlying goods record has not been updated

## Edge Cases

- goods created but never updated should return `updatedId: null` and `updatedAt: null`
- nullable `description` should remain `null` when absent

## Exception Scenarios

- missing goods id returns the existing `404 Not Found`
- missing or expired session still returns the existing `401 Unauthorized`

## Acceptance Criteria Mapping

- AC: goods detail API returns the goods data structure
  - map the missing fields from the domain model into `GoodsResponse`
- AC: response is considered failed if fields are not added
  - cover the expanded contract with focused tests and API spec updates
