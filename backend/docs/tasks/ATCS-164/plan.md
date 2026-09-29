# ATCS-164 Plan

## Requirement Summary

- change the goods registration POST API body from a single object to a list of goods objects
- persist every goods item received in the request body
- keep existing duplicate, restore, Godo sync, and criteria initialization rules for each item

## Business Context

- the web flow already supports single goods registration
- Excel upload needs bulk registration without introducing a separate API
- the registration contract should support both one item and multiple items with one endpoint

## Scope

- update `POST /api/goods` request contract to accept a JSON array
- process all items through the existing registration flow
- return registration results for all saved items
- update tests and API documentation for the new request and response shape

## Out Of Scope

- Excel parsing or upload UI changes
- new batch or async registration behavior
- changes to update, delete, or list goods APIs
- schema changes

## Impacted Modules

- `goods.presentation`
- `goods.application.dto`
- `goods.application.usecase`
- controller and use case tests
- API documentation

## Architecture Considerations

- keep controller thin and delegate registration logic to the use case
- preserve the current domain and port contracts where possible
- keep the registration operation transactional so list registration is handled as one unit of work

## Risk Analysis

- validation must apply to each list item, not just the list container
- response shape changes can break existing callers if not updated together
- duplicate goods within the payload or against persisted data can abort the transaction
