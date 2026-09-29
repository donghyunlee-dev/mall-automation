# ATCS-177 Plan

## Requirement Summary

- add recent modification metadata to the goods list API response
- populate the response with the latest update timestamp and updater email
- fall back to the creation timestamp and creator email when update metadata is missing
- update the backend API documentation for the list endpoint contract

## Business Context

- the goods list screen needs one display column for the latest modified timestamp and another for the latest modifier
- existing goods data already stores both created and updated metadata, but the list API does not expose it
- the fallback rule prevents nulls for goods that have never been edited after registration

## Scope

- extend only the goods list endpoint response shape
- keep the detail, create, update, and delete endpoint behavior unchanged
- add focused automated coverage for the fallback mapping and JSON serialization
- document the Jira task and iteration validation results

## Out Of Scope

- changes to goods persistence fields or schema
- changes to goods detail or update response contracts
- frontend changes
- git branch, commit, push, or pull request automation

## Impacted Modules

- `goods.application.dto`
- `goods.application.usecase`
- `goods.presentation`
- `docs/product/api-spec.md`
- Jira and iteration documentation under `docs/tasks`, `docs/plan`, `docs/test/results`, and `docs/reports`

## Architecture Considerations

- keep the repository and domain model unchanged because the needed metadata already exists on `Goods`
- use a list-specific DTO so only the list endpoint contract changes
- derive fallback values in the application mapping layer, not in the controller

## Risk Analysis

- reusing the existing shared `GoodsResponse` would unintentionally change the detail and update endpoints
- omitting the fallback would violate the ticket acceptance criteria for goods without update history
- tests must verify both the mapping rule and the serialized field names to avoid silent contract drift
