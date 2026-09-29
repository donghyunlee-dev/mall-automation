# ATCS-222 Plan

## Requirement Summary

- add a goods-level setting that blocks automatic Godo sold-out status transmission
- allow the setting to be changed through a backend API for the frontend
- ensure blocked goods are skipped by the batch before any Godo API call is made

## Business Context

- operators need to keep a goods item in `NORMAL` while temporarily preventing automatic Godo state updates
- the existing `status` field already controls goods lifecycle and must not be overloaded with transmission-policy meaning

## Scope

- add a persisted `Y/N` flag for automatic Godo transmission blocking on `goods`
- add a dedicated API to update the flag
- validate that blocking can only be enabled for `NORMAL` goods
- skip batch transmission when the flag is enabled
- expose the flag in goods detail responses

## Out Of Scope

- manual forced Godo status transmission
- batch scheduler behavior changes unrelated to the goods transmission block
- broad refactoring of goods status or sync semantics

## Impacted Modules

- `goods` domain model and persistence mapping
- goods update/use case layer for the new block-setting API
- goods controller DTOs and responses
- Godo sync use case batch transmission guard
- SQLite schema and product data spec

## Architecture Considerations

- keep transmission policy separate from goods lifecycle status
- keep validation in the application/domain path, not in the controller
- preserve the existing `GodoApiPort` abstraction and only change decision logic before the call

## Risk Analysis

- schema changes can break JPA validation if entity and SQL are not updated together
- response DTO changes can affect controller tests and any integration tests asserting goods detail shape
- the batch skip condition must not accidentally block all sync candidates
