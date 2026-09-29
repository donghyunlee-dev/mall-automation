# Iteration 04 Plan

## Scope

- Start Task 02 OTP authentication domain
- Implement OTP request, confirmation, and session check baseline flow
- Add domain, policy, use case, controller, and persistence skeleton for email verification

## Objectives

- generate and hash OTP codes without storing the raw code
- validate email domain and rate limits
- issue authenticated session on successful confirmation
- expose REST endpoints for request, confirm, and session check

## Validation Plan

- `./gradlew test`
- `./gradlew build`
