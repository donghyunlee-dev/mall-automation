# ATCS-168 Plan

## Requirement Summary

- fix the WMS inventory collection flow so the session cookie needed for stock scraping is preserved
- stop failing the batch job when the login response exposes the session cookie with a different header case
- keep the existing WMS login and stock-fetch sequence intact

## Business Context

- the `product-sync` batch currently fails before inventory collection because the login flow cannot find the session cookie
- the Jira ticket confirms the WMS session is present in browser cookies, so the backend should reliably recover that session and pass it to the stock request
- the fix must restore WMS stock collection without changing unrelated batch or goods-sync behavior

## Scope

- make WMS transport header lookup resilient for login cookie extraction
- cover the regression with focused WMS login tests
- document the requirement, implementation, and validation results for this Jira task

## Out Of Scope

- changes to WMS payload structure beyond session propagation
- scheduler, batch execution, or goods status policy changes
- configuration, branch, commit, push, or pull request automation

## Impacted Modules

- `wms.infrastructure.scraping`
- focused WMS scraping tests
- Jira task documentation under `backend/docs/tasks/ATCS-168`

## Architecture Considerations

- keep cookie parsing inside the WMS scraping infrastructure layer
- preserve the existing `WmsLoginClient -> WmsStockClient` responsibility split
- prefer a small transport-response helper instead of broad HTTP client changes

## Risk Analysis

- if the session cookie is missing entirely, the batch must still fail with a clear scraping exception
- header lookup changes must not weaken parsing behavior for valid `Set-Cookie` responses
- test coverage needs to prevent regressions for alternate header capitalization returned by the WMS endpoint
