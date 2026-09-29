# AGENTS.md

This document defines repository-level operating rules for coding agents.

## Purpose

- Keep changes small, clear, and scoped to the request.
- Deliver working code, not only analysis or plans.
- Leave verifiable outcomes with clear notes on what was changed and tested.
- Avoid unintended expansion of scope or speculative development.

---

## Priority

1. User's explicit request.
2. Repository-specific rules in `AGENTS.team.md` (if present).
3. Rules in this `AGENTS.md`.
4. Existing repository conventions and patterns.

If conflicts exist between documents, stop implementation and report the conflict.

---

## Scope Control (Overscope Prevention)

Implementation must remain strictly within the scope defined by the user request and the active task specification and task plan.

The agent must not expand scope autonomously.

Rules:

- Implement only what is explicitly requested.
- Do not introduce new features unless explicitly specified.
- Do not refactor unrelated code.
- Do not reorganize project structure unless required by the task.
- Do not rename files, classes, or methods unless required by the task.
- Do not add dependencies unless explicitly requested.
- Do not change dependency versions unless explicitly requested.
- Do not modify configuration or build files unless required.

If additional improvements appear beneficial:

- Do not implement them.
- Record them under **Next Steps** in the task result document.

When the requested task and Done Criteria are satisfied:

- Stop implementation.
- Do not continue with extra improvements.

• Unless explicitly instructed otherwise, the AI Agent must not
  independently interpret, expand, generalize, infer, or supplement
  requirements. All implementation must remain strictly limited to the scope
  explicitly stated in the user request and project documents.
---

## Pre-Implementation Verification

Before writing code, the agent must verify the following:

- The relevant documentation has been read.
- The implementation scope is clearly defined.
- The minimal set of files required for change is identified.
- The expected validation steps are known.

If any of these are unclear, pause and request clarification.

Implementation must not begin without understanding the scope.

---

## Execution style

- Work autonomously: gather context, implement, verify, and report in one turn when feasible.
- Default to action with reasonable assumptions only when they do not expand scope.
- Ask questions when assumptions would affect behavior.
- Stop and ask if unexpected file changes appear during your work.
- Avoid loops; if progress stalls, end with concise blocker details and a targeted question.
- Do not expand scope to improve unrelated areas of the repository.

---

## Search, read, and tools

- Prefer `rg` / `rg --files` for search.
- Use dedicated tools over raw shell when available.
- Use terminal commands only when no dedicated tool exists.
- Batch and parallelize independent reads/searches whenever possible.
- Decide needed files first, then fetch in batches.
- Use sequential reads only when necessary.
- Treat `L123:...` style prefixes as line metadata, not file content.
- Avoid exploring unrelated parts of the repository once sufficient context is gathered.

---

## Implementation rules

- Optimize for correctness, clarity, and reliability over speed.
- Reuse existing helpers/patterns before introducing new abstractions.
- Preserve behavior by default; call out intentional behavior or UX changes.
- Keep type safety; avoid unnecessary casts.
- Avoid broad catch-all exception handling.
- Do not hide failures with silent fallbacks or quiet early returns.
- Batch coherent edits; avoid repeated micro-edits with no net progress.
- Limit changes strictly to the files necessary for the task.

---

## Editing and safety

- Use ASCII by default unless non-ASCII is already justified by file context.
- Add brief comments only where intent is not obvious.
- Prefer `apply_patch` for focused single-file edits.
- Do not revert user changes you did not make.
- Do not amend commits unless explicitly asked.

Never use destructive commands unless explicitly requested:

- `git reset --hard`
- `git checkout --`
- `git clean`

---

## Git and Repository Operations

Repository modification must be explicit.

Rules:

- Do not create commits unless explicitly requested.
- Do not push commits automatically.
- Do not create pull requests unless explicitly requested.
- Do not create or modify branches unless requested.

If version control actions are required:

- Ask for confirmation first.

---

## Validation and reporting

- Run relevant build, test, or lint steps when possible after changes.

If verification cannot be executed:

- Clearly state what was not verified and why.

Reporting order:

1. Outcome summary
2. Files changed
3. Commands executed
4. Test results
5. Notable findings

---

## Planning rules

- Skip formal planning for trivial tasks.
- If using a plan, use multi-step items.
- Keep plan statuses updated.
- Do not end with plan-only output unless the user explicitly asked for planning.

Plan items must end with one of:

- Done
- Blocked (with reason)
- Cancelled (with reason)

Plans must not introduce scope expansion.

---

## Special request handling

- For simple operational requests (for example current time), run the command directly.
- For "review" requests, prioritize findings first:
  - bugs
  - regressions
  - risks
  - missing tests

---

# Required Guidance Files (Must Read Before Implementation)

Codex must read and align with the following documents before starting implementation.

If a required file does not exist, it must be created before development begins.

Required documents:

- `AGENTS.team.md` (if present)  
  Repository or team-specific constraints. Highest priority after user request.

- `AGENTS.md`  
  Global agent execution rules.

- `docs/guide/development-rules.md`  
  Coding standards and architectural constraints.

- `docs/guide/testing-strategy.md`  
  Test categories, coverage scope, execution rules.

- `docs/guide/done-criteria.md`  
  Definition of Done and acceptance checklist.

- `docs/guide/spec-driven-workflow.md`  
  Task kickoff workflow, document roles, and phase gates.

- `docs/product/prd.md`  
  Functional requirements and business objectives.

- `docs/product/data-spec.md`  
  Data contracts, schema definitions, validation constraints.

Implementation must not begin without reviewing all relevant documents.

If inconsistencies are detected between documents:

- Pause implementation
- Report the conflict

---

# Task Deliverables and Documentation Structure

The following documentation structure must be maintained:


```
docs/
├─ guide/
│  ├─ development-rules.md
│  ├─ testing-strategy.md
│  ├─ done-criteria.md
│  └─ spec-driven-workflow.md
├─ product/
│  ├─ prd.md
│  └─ data-spec.md
└─ tasks/
   └─ {task-id}/
      ├─ spec.md
      ├─ plan.md
      ├─ tasks.md
      ├─ test-result.md
      └─ result.md
```

For every non-trivial task:

- Determine the task identifier from the PRD task ID or a user-request work ID.
- Create or update `docs/tasks/{task-id}/spec.md` before technical planning.
- Resolve open questions through the spec-driven workflow before implementation.
- Create or update `docs/tasks/{task-id}/plan.md` before code changes.
- Create or update `docs/tasks/{task-id}/tasks.md` before or during implementation.
- Implement code and corresponding tests.
- Record executed commands and test outcomes in `docs/tasks/{task-id}/test-result.md`.
- Summarize delivered scope, key decisions, and open gaps in `docs/tasks/{task-id}/result.md`.

Task IDs must be stable and traceable to the originating request or PRD task.

No required documentation step may be silently skipped.

---

# Interface-First Design Rule

Business logic must not be directly coupled to concrete service classes.

Core contracts must be defined using interfaces.

Mandatory requirements:

Define an interface when:

- Multiple implementations exist or are expected.
- The component integrates with external systems (DB, HTTP, MQ, filesystem).
- Business rules vary by partner, tenant, or context.

Structure rules:

- Place interfaces in application or core packages.
- Place concrete implementations in infrastructure or adapter packages.
- Inject interfaces, never concrete implementations.

If a concrete service already exists:

- Introduce an interface without altering behavior.
- Migrate callers to depend on the interface.

Naming conventions:

Interface example:

OrderReader

Implementation examples:

JpaOrderReader  
HttpOrderReader

Design principles:

- Follow the Interface Segregation Principle.
- Keep interfaces cohesive.

Testing rule:

Test doubles (Fake/Stub):

- Allowed only in test code
- Never used in development or production runtime wiring

---

# General Quality Bar

- Respect the repository structure and existing conventions.
- Keep responsibilities separated across boundaries.
- Avoid placing decision logic in boundary handling code.
- Use explicit boundary models where external input or output is involved.
- Do not expose internal persistence or transport models directly.
- Keep validation intentional and close to the boundary.
- Keep state changes and side effects explicit.

---

# Response format

- Keep responses concise, factual, and collaborative.
- Use plain text with lightweight structure only when it improves readability.
- Do not dump large file contents; reference paths instead.
- Do not tell the user to copy or save files manually.
- For code changes:
  - explain what changed
  - explain why
  - suggest next steps only when appropriate
- Use single-level bullets only.
