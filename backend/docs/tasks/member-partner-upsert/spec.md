# member-partner-upsert Spec

## Problem Statement

- The member partner update API is documented and implemented as update-only.
- This conflicts with the required flow where member data is created by normal upload or batch first, and partner data is created later by a user from the screen.

## Business Context

- Partner information is SFOOD-managed data entered after a member exists.
- Normal member upload and member sync batch should not create partner records.
- TEMPLATE upload is an exception because it can contain partner columns.
- The screen save API must support the first save and later edits through the same endpoint.

## Scope

- Update member API documentation to define partner save as insert-or-update.
- Update upload documentation so normal upload or batch-created members do not receive partner records, while TEMPLATE upload remains a partner-capable exception.
- Change `PUT /api/members/{memNo}/partner` to insert `member_partner` when the member exists and no partner record exists.
- Preserve update behavior when a partner record already exists.
- Preserve duplicate partner code validation across active members.
- Preserve `bizLicenseFile` update behavior for existing business records.

## Out Of Scope

- Do not change member upload parsing beyond partner creation policy.
- Do not add a separate partner creation endpoint.
- Do not change the database schema.
- Do not change frontend behavior in this task.
- Do not alter delete behavior except as impacted by the existing partner record state.

## User Or Operator Scenarios

- Scenario 1: A member is created by LIST upload or member sync batch. A user later saves partner information from the screen. The API inserts a new `member_partner` row.
- Scenario 2: A member already has partner information. A user edits it from the screen. The API updates the existing `member_partner` row.
- Scenario 3: A user attempts to save partner information for a missing member. The API returns `MEMBER_NOT_FOUND`.
- Scenario 4: A user attempts to save a partner code already used by another active member. The API returns `DUPLICATE_PARTNER_CODE`.

## Acceptance Criteria

- AC1: `PUT /api/members/{memNo}/partner` inserts a partner row when the member exists and no partner row exists.
- AC2: The same API updates the existing partner row when it exists.
- AC3: LIST upload-created members can receive partner information through the API.
- AC4: LIST upload and batch flows do not create partner records from normal member data.
- AC4-1: TEMPLATE upload can still create or update partner records from template partner columns.
- AC5: Missing members still return `MEMBER_NOT_FOUND`.
- AC6: Duplicate partner code validation still applies before insert or update.
- AC7: Documentation no longer describes the API as update-only.

## Edge Cases

- `partnerRegisteredFl` is null and defaults to `n`.
- `taxInvoiceFl` is null and defaults to `n`.
- `partnerCode` is null and duplicate validation is skipped.
- `bizLicenseFile` is provided for a member without a business row and remains ignored.

## Assumptions

- `mem_no` remains the unique logical key for `member_partner`.
- The existing `PUT` endpoint is the intended screen save API.
- No data migration is required for existing members without partner records.

## Clarifications

- Question: Should TEMPLATE upload continue creating partner records?
  Answer: Yes. TEMPLATE upload is an exception because partner columns can be present there.
