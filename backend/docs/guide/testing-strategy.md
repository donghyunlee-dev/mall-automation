# Testing Strategy

## Purpose

This document defines the testing strategy used for repository work.
The goal is to verify behavior, reduce regressions, and keep implementation aligned with documented requirements.

---

## Principles

- Write tests early enough to lock down the expected behavior.
- Prefer tests that describe behavior over tests that mirror implementation detail.
- Keep tests deterministic, isolated, and repeatable.
- Use the smallest test scope that gives confidence for the behavior under change.

---

## Test Layers

### Unit Tests

- Verify isolated business rules and transformations.
- Avoid unnecessary external dependencies.
- Use them for fast feedback on core logic.

### Flow or Application Tests

- Verify orchestration across collaborators.
- Confirm state transitions, sequencing, and interaction boundaries.

### Integration Tests

- Verify real integration points, persistence behavior, or external contracts where appropriate.
- Use them to validate assumptions that cannot be proven with isolated tests alone.

### Interface or API Tests

- Verify input validation, output shape, status handling, and user-visible behavior.
- Keep these focused on boundary behavior.

---

## Test Writing Rules

### Structure

Use a clear setup, action, and verification flow.

```text
Given: initial state
When: action is performed
Then: expected result is verified
```

### Naming

- Name tests after observable behavior.
- Make failure meaning obvious from the test name.

### General Rules

- One test should verify one main behavior.
- Tests must not depend on execution order.
- Test data should be easy to read and reason about.
- Avoid randomness unless it is part of the requirement.
- Control time, external state, and side effects where needed.

---

## What To Avoid

- tests that depend on hidden shared state
- flaky timing-based tests
- tests tightly coupled to implementation detail
- excessive mocking that removes the behavior under test
- production code changes made only to satisfy a weak test approach

---

## Coverage Expectations

- Cover important business behavior.
- Cover failure paths as well as happy paths.
- Cover boundary validation for externally visible behavior.
- Cover regressions for defects that were fixed.

Coverage percentage alone is not sufficient evidence of quality.

---

## Validation Workflow

Use the following order by default:

```text
spec review
-> clarification review
-> plan review
-> test design
-> failing test or missing-proof check
-> implementation
-> passing validation
-> result recording
```

Do not start non-trivial implementation before the task documents are prepared.

---

## Command Scope By Target Branch

전체 스위트(`./gradlew test`)는 실행 시간이 길다. 개발 중(feature 브랜치 → `develop`)에는 매번 전체 스위트를 돌리지 않고, 변경/개발 중인 기능에 관련된 테스트만 지정해서 실행한다.

- **feature 브랜치 작업 중 / `develop`으로 PR 낼 때**: 이번에 새로 작성했거나 수정한 클래스에 대응하는 테스트만 `--tests` 필터로 실행한다.
  ```bash
  ./gradlew test --tests "com.sfood.automation.<package>.<ClassName>Test"
  # 여러 개면 --tests를 여러 번 지정하거나 와일드카드 사용
  ./gradlew test --tests "com.sfood.automation.dashboard.*"
  ```
  - 대상 판단 기준: 이번 변경으로 새로 만들었거나 수정한 프로덕션 클래스, 그리고 그 클래스와 직접 연동되는(생성자 주입 등) 클래스의 테스트.
  - 기존 코드를 수정해 다른 클래스의 동작에 영향을 줄 수 있는 변경(예: 공용 유틸, 공유 설정, DB 스키마)이면 그 영향 범위에 있는 테스트도 포함한다.
- **`main`에 반영(머지)할 때**: 전체 스위트(`./gradlew test`, 필터 없이)를 반드시 실행하고 결과를 `test-result.md`에 기록한다.

`docs/tasks/{task-id}/test-result.md`의 `Commands`/`Results`에는 어떤 범위로 실행했는지(타깃 테스트 목록인지 전체 스위트인지)를 명시한다.

---

## Test Result Recording

Each task must record:

- commands or actions used for validation
- pass results
- fail results
- skipped or blocked checks
- retest results after fixes
- acceptance criteria or task items covered by validation

Path:

```text
docs/tasks/{task-id}/test-result.md
```
