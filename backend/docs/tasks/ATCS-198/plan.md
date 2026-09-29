# ATCS-198 Plan

## Requirement Summary

- Extend the goods update API contract so `availableQty` and `safetyRate` are required update fields.
- Persist the two fields into the `goods` table when a goods item is updated.
- Align the API documentation with the enforced request/response behavior.

## Business Context

- The web UI needs to edit per-goods inventory criteria directly instead of relying only on common defaults.
- Missing or null criteria values can silently overwrite data with invalid defaults unless the API rejects them.

## Scope

- Enforce required validation for `availableQty` and `safetyRate` on `PUT /api/goods/{id}`.
- Keep the existing goods update flow that stores the two values into the domain model and persistence layer.
- Update tests and API documentation for the stricter contract.

## Out Of Scope

- Goods registration API changes.
- Batch sync logic or sold-out decision policy changes.
- Frontend UI changes.

## Impacted Modules

- `goods.application.dto`
- `goods.presentation`
- `goods.application.usecase`
- `docs/product`

## Architecture Considerations

- Keep validation at the API boundary with Bean Validation on the request DTO.
- Preserve the existing interface-based use case and repository flow without changing domain or persistence responsibilities.

## Risk Analysis

- Tightening validation can break clients that currently omit the fields.
- Primitive request fields currently allow missing JSON values to degrade to `0`; changing to boxed types is required to detect omission.
