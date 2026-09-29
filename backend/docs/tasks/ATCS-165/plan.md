# ATCS-165 Plan

## Requirement Summary

- fix the goods update API so `PUT /api/goods/{id}` no longer returns a generic `500` for duplicate or invalid update cases
- keep successful goods updates working as before
- return a summarized cause message to the client when the update fails for a known business reason

## Business Context

- goods registration is already used for bulk test data creation
- follow-up edits to those goods must be reliable for operator workflows
- the current generic `INTERNAL_SERVER_ERROR` response hides the actionable cause of the failed update

## Scope

- inspect and fix the goods update flow in the backend
- prevent duplicate ERP-code updates from reaching the database as unhandled failures
- expose a clearer error message for known update failures
- add and update automated tests and task docs

## Out Of Scope

- frontend changes
- goods registration API changes
- schema or migration work
- unrelated exception-handling refactors outside the affected update path

## Impacted Modules

- `goods.application.usecase`
- `goods.presentation`
- `common.exception`
- goods controller and use case tests
- API and task documentation

## Architecture Considerations

- keep the controller thin and preserve the existing controller -> use case flow
- keep duplicate detection in the application layer instead of depending on persistence exceptions
- surface business-error messages through the shared error response format without changing unrelated handlers

## Risk Analysis

- duplicate detection must ignore the current goods row and only reject conflicts with other rows
- changing shared error-response behavior can affect existing business exceptions if done too broadly
- scope changes from `GENERAL` to `EXCLUSIVE` still need the correct config-driven criteria reset
