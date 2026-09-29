# ATCS-174 Plan

## Requirement Summary

- separate the SQLite database used by automated tests from the default application database
- apply the `_test` suffix to the shared test database file name
- keep runtime controllers and external API integrations on the current application database

## Business Context

- DB-backed integration tests currently clean and reseed the same SQLite file used for local runtime verification
- test cleanup on the shared runtime DB can remove registered goods and break manual validation during development
- the Jira ticket requires a safe default so test execution no longer mutates runtime data

## Scope

- change the shared Spring test database helper to point at `data/sfood-mall-automation_test.db`
- keep the application default datasource path unchanged at `data/sfood-mall-automation.db`
- add or update focused test coverage that proves the shared test DB uses the `_test` file
- update the relevant backend guides and Jira task documentation to reflect the split

## Out Of Scope

- runtime datasource configuration changes for the application itself
- schema changes or data migrations
- refactoring unrelated tests away from the shared helper
- git branch, commit, push, or pull request automation

## Impacted Modules

- `support` test infrastructure
- Spring Boot bootstrap integration tests
- backend guide documents related to DB usage
- Jira task documentation under `backend/docs/tasks/ATCS-174`

## Architecture Considerations

- keep the separation at the shared test helper boundary so existing DB-backed tests inherit the safer path without per-test rewiring
- preserve the current runtime datasource contract by not changing `application.yml`
- use the filename suffix requirement directly in test code to avoid regressions back to the runtime DB path

## Risk Analysis

- if any test bypasses the shared helper and still points at the runtime DB, data interference can continue
- changing the shared helper affects many DB-backed tests, so full backend validation is needed
- documentation must be aligned or future work may reintroduce the shared DB assumption
