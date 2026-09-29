# member-partner-upsert Plan

## Summary

- Convert member partner save from update-only to upsert for existing members.
- Preserve TEMPLATE upload partner creation/update as an exception to normal upload behavior.
- Align product documentation with the screen-first partner creation flow.
- Add focused regression coverage for insert and update behavior.

## Impacted Modules

- `docs/product/api-spec.md`
- `docs/product/api-spec-member.md`
- `src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberPartnerUseCase.java`
- `src/test/java/com/sfood/automation/member/application/usecase/UpdateMemberPartnerUseCaseIntegrationTest.java`
- `src/test/java/com/sfood/automation/member/presentation/MemberManageControllerTest.java`

## Architecture Approach

- Keep orchestration in the existing application use case.
- Continue using `MemberRepository` as the persistence port.
- Create a new `MemberPartnerEntity` inside the use case only when the member exists and the partner row is absent.
- Preserve existing repository methods and avoid schema changes.

## Data And API Impact

- `PUT /api/members/{memNo}/partner` becomes an upsert endpoint for `member_partner`.
- TEMPLATE upload continues to create/update `member_partner`; LIST upload and batch do not.
- Existing `200 OK` response shape remains unchanged.
- `MEMBER_PARTNER_NOT_FOUND` is no longer expected from this endpoint.
- Upload documentation changes so partner rows are not created by normal upload or batch data import.

## Validation Plan

- Run focused use case integration tests for member partner save.
- Run focused controller tests for member management API.
- Run full test suite if focused tests pass and time permits.

## Risks

- Risk 1: Existing tests or clients may rely on the old 404 behavior for missing partner rows.
- Risk 2: Upload tests may still assume TEMPLATE creates partner rows and may need narrow expectation updates.

## Rollback Considerations

- Revert the use case to require `findPartnerByMemNo(...).orElseThrow(...)`.
- Restore documentation and tests describing update-only behavior.
