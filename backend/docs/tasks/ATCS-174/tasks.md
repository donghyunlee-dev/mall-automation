# ATCS-174 Tasks

## Development Tasks

- point the shared SQLite test helper at the `_test` database file
- verify the shared bootstrap test asserts the `_test` filename contract
- update the backend guide documents that describe runtime and test DB usage
- document the implementation and validation outcomes for this Jira task

## Test Cases To Implement

- initialize the shared SQLite test database when the application starts in DB-backed tests
- assert that the shared test database file name is `sfood-mall-automation_test.db`
- preserve the runtime datasource default outside the shared test helper

## Modules To Modify

- `src/test/java/com/sfood/automation/support`
- `src/test/java/com/sfood/automation/common/bootstrap`
- `docs/guide`
- `docs/tasks/ATCS-174`

## New Classes Or Components

- none

## Integration Points

- `SharedSqliteTestDatabase`
- `SqliteBootstrapRunnerTest`
- `application.yml`

## Result

### Changed Files

- `backend/docs/guide/development-rules.md`
- `backend/docs/guide/done-criteria.md`
- `backend/docs/guide/testing-strategy.md`
- `backend/docs/tasks/ATCS-174/plan.md`
- `backend/docs/tasks/ATCS-174/spec.md`
- `backend/docs/tasks/ATCS-174/tasks.md`
- `backend/src/test/java/com/sfood/automation/common/bootstrap/SqliteBootstrapRunnerTest.java`
- `backend/src/test/java/com/sfood/automation/support/SharedSqliteTestDatabase.java`

### Summary

- separated the shared automated-test SQLite file from the runtime SQLite file by moving tests to `data/sfood-mall-automation_test.db`
- added a bootstrap assertion for the `_test` filename contract
- aligned the backend guides with the new runtime-versus-test DB split

### Test Results

- `./gradlew test --tests com.sfood.automation.common.bootstrap.SqliteBootstrapRunnerTest --tests com.sfood.automation.auth.presentation.Task09EndToEndIntegrationTest` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

### Review Notes

- runtime datasource configuration was intentionally left unchanged so controller and external API verification continue to use `data/sfood-mall-automation.db`
- git branch, commit, push, and PR creation remain out of scope because `backend/AGENTS.md` requires an explicit user request for repository operations
