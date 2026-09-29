# ATCS-167 Plan

## Requirement Summary

- change the common status config update API to use the config identifier in the URL path
- remove `scope` from the update request body so only editable threshold fields remain
- keep the existing update behavior and response payload for the selected scope

## Business Context

- the current update contract exposes `scope` as if it were editable data even though it is the resource identifier
- the API should follow the read/list contract added in the prior task and make the update target explicit in the URL
- removing `scope` from the body prevents clients from sending conflicting identifier values

## Scope

- change `PUT /api/config/status` to `PUT /api/config/status/{scope}`
- remove `scope` from `UpdateCommonStatusConfigRequest`
- update the use case, controller, tests, and API documentation for the new contract

## Out Of Scope

- changes to the read API contract
- schema or seed-data changes
- frontend request wiring outside the backend API contract

## Impacted Modules

- `goods.presentation`
- `goods.application.dto`
- `goods.application.usecase`
- controller and use case tests
- API and iteration documentation

## Architecture Considerations

- keep the controller responsible for REST path binding while the use case remains responsible for persistence updates
- continue to use the existing repository port to resolve and save the selected scope
- preserve the current response DTO so downstream consumers only need to change the request path and body shape

## Risk Analysis

- changing the endpoint path can break existing clients if tests and docs are not updated consistently
- invalid or unknown path scope values must not silently update the wrong configuration
- request validation must still reject negative threshold values after the body shape changes
