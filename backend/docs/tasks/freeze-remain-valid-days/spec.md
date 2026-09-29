## Scope

- Add remain-valid-days criteria to common status config, goods, and goods status logs.
- Extend WMS stock DTO with freeze classification inputs from WMS.
- Apply WMS row filtering before aggregation with this rule:
  - `TEMP_TCD=FREEZE` uses `REMAIN_VALID_DAYS >= goods.remainValidDays`
  - all other rows use `VALID_PERCENT >= goods.safetyRate`
- Keep zone filtering first, then criteria filtering, then quantity aggregation, then sold-out/on-sale judgment.

## Acceptance Criteria

- `common_status_config` stores `remain_valid_days` per `GENERAL` / `EXCLUSIVE`.
- goods registration initializes `availableQty`, `safetyRate`, and `remainValidDays` from the matching common config.
- common config update propagates `remainValidDays` to goods in the same scope.
- goods persistence and API responses include `remainValidDays`.
- goods status logs persist `remainValidDays`.
- for log evidence:
  - `valid_percent` stores the maximum included valid percent when the applied rule is valid-percent based
  - `remain_valid_days` stores the maximum included remain-valid-days when the applied rule is freeze-day based
- WMS refinement order remains:
  - allowed zone filter
  - row-level criteria filter
  - lot-level quantity aggregation
  - sold-out/on-sale policy

## Non-Goals

- Do not store WMS `TEMP_TCD` on goods.
- Do not introduce a separate config table by temperature type.
- Do not change the existing zone eligibility model.
