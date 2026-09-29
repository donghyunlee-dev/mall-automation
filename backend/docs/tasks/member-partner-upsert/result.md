# member-partner-upsert Result

## Delivered Scope

- Changed `PUT /api/members/{memNo}/partner` from update-only to insert-or-update for existing members.
- Kept TEMPLATE upload as a partner-capable exception that can create or update `member_partner` rows.
- Kept LIST upload and normal batch-originated member sync from creating or updating `member_partner` rows.
- Updated product API documentation to state that partner data is created and maintained through TEMPLATE upload or the screen save API.
- Updated regression tests for insert-on-missing, existing update, duplicate partner code validation, and upload non-creation behavior.

## Changed Files

- `docs/product/api-spec.md`
- `docs/product/api-spec-member.md`
- `src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberPartnerUseCase.java`
- `src/main/java/com/sfood/automation/member/application/usecase/UploadMembersUseCase.java`
- `src/test/java/com/sfood/automation/member/application/usecase/UpdateMemberPartnerUseCaseIntegrationTest.java`
- `src/test/java/com/sfood/automation/member/application/usecase/UploadMembersUseCaseIntegrationTest.java`
- `src/test/java/com/sfood/automation/member/presentation/MemberManageControllerTest.java`
- `docs/tasks/member-partner-upsert/spec.md`
- `docs/tasks/member-partner-upsert/plan.md`
- `docs/tasks/member-partner-upsert/tasks.md`
- `docs/tasks/member-partner-upsert/test-result.md`
- `docs/tasks/member-partner-upsert/result.md`

## Key Decisions

- `member_partner` is user-managed data and is not created by LIST upload or batch-originated normal member creation.
- TEMPLATE upload remains an exception because it can include partner columns.
- The existing `PUT` endpoint remains the screen save API and now creates the first partner row when needed.
- Missing member behavior remains `MEMBER_NOT_FOUND`.
- Duplicate partner code validation runs before both insert and update.

## Open Gaps

- `MemberPartnerNotFoundException` remains in the common exception package for compatibility, but this endpoint no longer uses it.

## Next Steps

- Consider removing unused partner normalization helpers and the old `MEMBER_PARTNER_NOT_FOUND` exception in a separate cleanup task if no other flow needs them.
