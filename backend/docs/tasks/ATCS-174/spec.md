# ATCS-174 Spec

## Feature Specification

- automated tests that use the shared SQLite test helper must run against `data/sfood-mall-automation_test.db`
- the application default datasource must remain `data/sfood-mall-automation.db`
- the `_test` suffix is part of the shared test DB contract

## API Changes

- no API path, request, or response changes

## Database Changes

- no schema changes
- no runtime database path changes
- shared automated tests now bootstrap and mutate `data/sfood-mall-automation_test.db`

## Domain Model Changes

- no domain model changes

## Validation Rules

- `SharedSqliteTestDatabase` registers `DB_PATH=data/sfood-mall-automation_test.db`
- Spring Boot bootstrap tests create and validate the `_test` SQLite file
- the runtime datasource configuration in `application.yml` continues to default to `data/sfood-mall-automation.db`

## Edge Cases

- repeated local test runs should keep mutating only the `_test` database file
- local runtime verification should continue to use the non-test DB file unless explicitly overridden

## Exception Scenarios

- if the shared helper still points to the runtime DB, tests can continue deleting runtime goods data
- if runtime configuration is accidentally changed to the `_test` DB, external verification flows will run against the wrong dataset

## Acceptance Criteria Mapping

- `별도의 테스트 DB를 생성한다`:
  - satisfied by the shared test helper using `data/sfood-mall-automation_test.db`
- `db 파일 이름에 _test 를 붙인다`:
  - verified by the bootstrap test asserting `sfood-mall-automation_test.db`
- `tests 시 XXXX_test.db를 사용한다`:
  - satisfied because DB-backed tests reuse `SharedSqliteTestDatabase`
- `외부에서 controller api 호출할 때는 기존 DB를 사용한다`:
  - satisfied by keeping the runtime datasource default unchanged in `application.yml`
