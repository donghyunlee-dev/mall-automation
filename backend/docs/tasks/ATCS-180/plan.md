# ATCS-180 Plan

## Requirement Summary

- restore the missing Godo sold-out status mapping during goods registration
- keep the fix scoped to the registration flow without changing unrelated goods behavior
- align the Godo goods search parser with the live XML response shape so sold-out data is read correctly

## Business Context

- goods registration is expected to align the local `sold_out_yn` state with the Godo goods API response
- the Jira ticket reports that registration can complete without refreshing the Godo sold-out mapping
- live verification showed the registration flow runs, but the goods search parser reads a simplified XML shape that differs from production responses

## Scope

- identify the registration branch where Godo sold-out mapping is skipped
- add focused tests for the affected registration behavior
- implement the minimal use-case fix so restored goods also reflect the latest Godo sold-out state
- add parser coverage for the live `Goods_Search.php` XML structure and update the parser to extract `soldOutFl` from that payload

## Out Of Scope

- changes to batch sold-out synchronization
- changes to goods update or delete behavior
- broad refactoring of the goods domain model or controller layer

## Impacted Modules

- `goods.application.usecase`
- `goods.infrastructure.external`
- goods registration test suite
- Godo goods search parser test suite
- iteration and task result documentation

## Architecture Considerations

- keep the existing `RegisterGoodsUseCase -> GodoApiPort -> GoodsRepository` flow intact
- preserve interface-based dependencies and avoid introducing new infrastructure wiring

## Risk Analysis

- the restore branch must remain behaviorally identical except for reapplying the Godo sold-out state
- tests need to confirm the fix without widening the registration response contract
- parser changes must preserve compatibility with the older simplified XML shape already covered by existing tests
