# Iteration 01 Plan

## Scope

- Task 01 only: project bootstrap and common infrastructure
- No expansion beyond PRD Task 01
- Work proceeds by explicit user approval at each stage

## Objectives

- Initialize Spring Boot backend project skeleton
- Add core configuration for web, JPA, session, mail, and HTTP clients
- Define SQLite schema and seed data for the 9 required tables
- Implement common modules:
  - config
  - security
  - exception
  - slack
- Verify Task 01 scenarios with tests written before implementation

## Constraints

- Follow AGENTS.md, guide documents, PRD, and data spec
- Use interface-first design where external dependencies are involved
- Do not call real Godo sold-out status change APIs
- Keep changes minimal and task-scoped

## Approval Steps

1. Branch creation and iteration document setup - Done
2. Test design and test implementation - Pending
3. Task 01 production implementation - Pending
4. Local verification with Gradle test/build - Pending
5. Commit, push, and PR creation - Pending

## Expected Deliverables

- `build.gradle`, `settings.gradle`, `gradlew`, Gradle wrapper files
- `src/main` and `src/test` skeleton for Task 01 scope
- `schema.sql` and `data.sql`
- `docs/test/results/iteration-01-test-result.md`
- `docs/reports/iteration-01-result.md`

## Validation Plan

- Write tests first for interceptor, exception handler, slack notifier, and schema bootstrap
- Run `./gradlew test`
- Run `./gradlew build` if needed for integration validation

## Open Items

- None at this stage
