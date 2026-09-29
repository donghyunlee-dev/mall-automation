1. Extend config, goods, and status-log models/schema with `remain_valid_days`. Done
2. Update registration/config synchronization flow to apply the new default criteria field. Done
3. Extend WMS DTO and row-level refinement to choose valid-percent vs remain-valid-days before aggregation. Done
4. Add regression tests for freeze and non-freeze cases and run the relevant suite. Done
