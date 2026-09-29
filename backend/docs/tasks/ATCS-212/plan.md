# ATCS-212 Plan

## Requirement Summary

- persist login accounts in a `users` table after OTP confirmation
- expose authenticated user profile read and notification preference update APIs
- send batch result emails only to users whose email notification flag is enabled
- replace the plain-text batch result mail body with a structured HTML layout

## Business Context

- batch result emails currently go to the configured sender account instead of the users actually operating the system
- there is no persisted user preference model for email or Teams notifications
- the batch result email body is too plain to review quickly when multiple goods change in one run

## Scope

- add the `users` table, domain model, persistence adapter, and API/data spec updates
- create login-time create-or-update behavior for authenticated users with default notification flags set to `N`
- add authenticated API endpoints to read and update the current user's notification preferences
- update batch email sending to target opted-in users and render a richer HTML message

## Out Of Scope

- Teams notification delivery implementation
- frontend screen changes
- changes to OTP request behavior before confirmation

## Impacted Modules

- `auth.application.usecase`
- `auth.presentation`
- `auth.domain.port`
- `auth.infrastructure.persistence`
- `batch.infrastructure.job`
- `src/main/resources/schema.sql`
- `docs/product/data-spec.md`
- `docs/product/api-spec.md`

## Architecture Considerations

- keep the new user profile persistence behind an interface in the auth domain
- preserve the current OTP confirmation flow order by persisting the user before issuing the HTTP session
- keep batch email composition in the existing sender component while switching recipient resolution to the new user repository

## Risk Analysis

- login should not issue a session if the user profile cannot be created or updated
- batch email sending must safely skip delivery when no opted-in recipients exist
- the new HTML mail body must remain testable without a live SMTP server
