# Iteration 03 Plan

## Scope

- Continue Task 01 common infrastructure
- Add session authentication interceptor
- Add common exception model and global exception handler
- Add Slack notifier and HTTP client baseline config

## Objectives

- enforce session checks for protected API paths
- return consistent JSON error responses through `@RestControllerAdvice`
- provide a reusable Slack webhook notifier
- validate the common infrastructure with tests first

## Approval Steps

1. Sync `main` and create working branch - Done
2. Add tests for interceptor, exception handler, and Slack notifier - Done
3. Implement common infrastructure - Done
4. Run local validation and publish PR - Done

## Validation Plan

- `./gradlew test`
- `./gradlew build`
