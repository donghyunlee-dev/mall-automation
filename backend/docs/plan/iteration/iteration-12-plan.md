# Iteration 12 Plan

## Scope

- change SQLite bootstrap journal mode for external DB tool access
- verify bootstrap keeps schema initialization while disabling WAL mode
- add iteration documents for the SQLite access adjustment

## Validation Plan

- `./gradlew test --tests com.sfood.automation.common.bootstrap.SqliteBootstrapRunnerTest`
- `./gradlew test`
- `./gradlew build`
