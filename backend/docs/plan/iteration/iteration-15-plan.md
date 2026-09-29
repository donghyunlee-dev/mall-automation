# Iteration 15 Plan

## Scope

- start Task 09 integration test and production-readiness verification
- align runtime configuration with PRD-required environment variable behavior
- verify the end-to-end flow from OTP authentication to manual batch execution
- verify operational checks for SQLite WAL mode, virtual threads, seeded schema/data, and log file creation
- verify final scenarios for protected APIs, schedule change logs, delete behavior, Godo failure handling, and WMS failure handling

## Validation Plan

- `./gradlew test --tests com.sfood.automation.auth.presentation.Task09EndToEndIntegrationTest --tests com.sfood.automation.common.bootstrap.SqliteBootstrapRunnerTest --tests com.sfood.automation.common.bootstrap.Task09OperationalReadinessTest --tests com.sfood.automation.common.bootstrap.RequiredEnvironmentStartupFailureTest --tests com.sfood.automation.common.security.SessionAuthInterceptorTest`
- `./gradlew test`
- `./gradlew build`
