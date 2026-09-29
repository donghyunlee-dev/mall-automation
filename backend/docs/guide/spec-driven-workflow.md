# Spec-Driven Workflow

## Purpose

This document defines the repository workflow for non-trivial work items.
The goal is to ensure that implementation follows an explicit task-level specification instead of starting directly from code changes.

---

## When To Apply

Apply this workflow when:

- a task is derived from `docs/product/prd.md`
- the user directly requests a feature, bug fix, or scoped change
- the work affects behavior, API, persistence, validation, or integration logic

You may skip task document creation only for trivial operational requests or very small edits with no behavioral impact.

---

## Task Workspace

Each non-trivial work item must use a task workspace:

```text
docs/tasks/{task-id}/
```

Required files:

- `spec.md`
- `plan.md`
- `tasks.md`
- `test-result.md`
- `result.md`

Starter templates are available under:

```text
docs/tasks/_template/
```

Create new task documents by copying and then filling the matching template files.

---

## Task Identifier Rule

Use a stable task identifier.

Preferred order:

1. PRD task ID when the work maps directly to a PRD task
2. Existing issue or ticket ID when one exists
3. User-request work ID in a short kebab-case format when there is no formal task ID

The same work item must keep the same task ID across follow-up edits.

---

## Workflow Phases

### 1. Specify

Create or update `docs/tasks/{task-id}/spec.md`.

`spec.md` must define:

- problem statement
- business context
- scope
- out of scope
- user or operator scenarios
- acceptance criteria
- edge cases
- assumptions

Do not write implementation details here unless they are part of the requirement itself.

### 2. Clarify

Resolve missing or ambiguous requirements before planning.

Clarifications must be recorded inside `spec.md` under a `Clarifications` section when needed.

Examples:

- exact request and response behavior
- validation boundaries
- whether a change is additive or replacing existing behavior
- whether data migration is required

If a critical ambiguity remains unresolved, stop implementation and ask the user.

### 3. Plan

Create or update `docs/tasks/{task-id}/plan.md`.

`plan.md` must define:

- impacted modules
- architecture and dependency approach
- data or API impact
- validation strategy
- risks and rollback considerations

`plan.md` translates the approved spec into a technical approach.

### 4. Task Breakdown

Create or update `docs/tasks/{task-id}/tasks.md`.

`tasks.md` must contain:

- implementation tasks
- test tasks
- documentation updates
- status markers for progress tracking

Use this file as the execution checklist.

### 5. Implement

Implement only after `spec.md` and `plan.md` are sufficient to remove behavioral ambiguity.

All code changes must stay within the documented scope.

### 6. Validate

Run relevant tests and record them in `docs/tasks/{task-id}/test-result.md`.

`test-result.md` must contain:

- scope of validation
- commands executed
- PASS and FAIL results
- failure notes
- retest results when fixes were applied

### 7. Report

Summarize the final outcome in `docs/tasks/{task-id}/result.md`.

`result.md` must contain:

- delivered scope
- changed files
- key decisions
- open gaps
- Next Steps

---

## Phase Gates

Do not move forward when these gates are not met.

- Do not implement before `spec.md` defines scope and acceptance criteria.
- Do not implement before `plan.md` defines the technical approach and validation plan.
- Do not mark work complete before `test-result.md` and `result.md` are updated.
- Do not expand scope beyond what is written in `spec.md` and confirmed by the user.

---

## Relationship To Other Guidance

- `AGENTS.md` defines repository-wide operating rules and required document reads.
- `development-rules.md` defines coding and architecture constraints.
- `testing-strategy.md` defines test categories and execution expectations.
- `done-criteria.md` defines completion conditions.
- `prd.md` and `data-spec.md` remain the product-level source documents.

This workflow governs how a single task is turned into implementation.
