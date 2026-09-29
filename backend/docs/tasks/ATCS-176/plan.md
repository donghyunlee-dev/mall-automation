# ATCS-176 Plan

## Requirement Summary

- fix the sold-out and on-sale judgment flow so it uses each goods item's own `availableQty` and `safetyRate`
- keep ERP-code matching as the first refinement step for WMS stock rows
- apply the per-goods `safetyRate` threshold before summing WMS rows for on-sale recovery checks

## Business Context

- the current batch logic still uses fixed or common-config thresholds when evaluating transitions
- that behavior ignores the per-goods criteria values now stored in `goods`, so batch updates do not reflect operator-edited thresholds
- the Jira issue requires the batch to judge transitions against the goods record itself

## Scope

- split WMS stock refinement into:
  - registered ERP-code aggregation for sold-out checks
  - per-goods safety-rate filtered aggregation for on-sale checks
- update the sold-out policy to compare aggregated qty against `goods.availableQty`
- remove the fixed `50` valid-percent and fixed/common quantity thresholds from the transition decision logic
- update focused tests for the refiner, policy, sync use case, and batch orchestration
- document the implementation and validation outcomes for this Jira task

## Out Of Scope

- ERP code verification behavior
- goods registration or goods update API changes
- schema changes
- frontend changes
- git branch, commit, push, or pull request automation

## Impacted Modules

- `wms.infrastructure.scraping`
- `goods.domain.policy`
- `goods.application.usecase`
- `batch.infrastructure.job`
- Jira task documentation under `backend/docs/tasks/ATCS-176`

## Architecture Considerations

- keep WMS row filtering in the refiner layer so aggregation remains a pure sum/group operation
- pass separate aggregated views to the sync use case because sold-out and on-sale checks have different refinement rules
- keep the transition policy dependent on `Goods` and `AggregatedStock`, using the goods record as the source of truth for thresholds

## Risk Analysis

- using one aggregated view for both transition directions would still be wrong because on-sale recovery needs valid-percent filtering while sold-out checks do not
- changing the sync use case signature affects orchestration and tests together
- tests must cover both registered-code filtering and safety-rate filtering to prevent regression back to fixed thresholds
