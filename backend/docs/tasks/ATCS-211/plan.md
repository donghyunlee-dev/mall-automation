# ATCS-211 Plan

## Requirement Summary

- fix the Godo goods search integration so `godo_sync_yn` becomes `Y` only when the response actually contains the searched goods
- keep `godo_sync_yn` as `N` when the Godo API returns `code=000` but no matching goods payload
- keep the change scoped to goods registration and Godo goods search interpretation

## Business Context

- the current registration flow marks goods as Godo-synced when the search API returns success code `000`, even if the response has an empty `return`
- this causes local goods rows to look linked to a sellable Godo item when the searched item does not actually exist in the response

## Scope

- add focused tests for empty and mismatched Godo goods search results
- update the registration flow so it only applies Godo sync when the returned `goodsNo` matches the searched goods code
- keep hidden goods restore behavior aligned to the same matching rule

## Out Of Scope

- batch sold-out synchronization changes
- changes to ERP sync behavior
- broad refactoring of the goods domain or external adapter layers

## Impacted Modules

- `goods.application.usecase`
- `goods.infrastructure.external`
- goods registration tests
- Godo goods search parser tests
- Jira task and iteration documentation

## Architecture Considerations

- preserve the existing `RegisterGoodsUseCase -> GodoApiPort -> GoodsRepository` structure
- keep the fix inside the existing domain and adapter contracts with minimal surface-area change

## Risk Analysis

- hidden goods restores previously marked as Godo-synced must be reset correctly when the current Godo response is empty
- the fix must still allow live `Goods_Search` responses with nested `goods_data` payloads to sync normally
- the API transaction log should continue recording `code=000` success for transport-level success even when local sync remains `N`
