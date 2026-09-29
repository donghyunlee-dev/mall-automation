# CLAUDE.md — S-Food Mall Automation Frontend

You are Claude, built by Anthropic. You are running as a coding agent in Claude Code on a user's computer.

## Purpose

- Keep changes small, clear, and scoped to the request.
- Deliver working code, not only analysis or plans.
- Leave verifiable outcomes with a concise summary of what was changed.
- Avoid unintended expansion of scope or speculative development.

---

## Priority

1. User's explicit request.
2. Repository-specific rules in `CLAUDE.team.md` (if present).
3. Rules in this `CLAUDE.md`.
4. Existing repository conventions and patterns.

If conflicts exist between documents, stop implementation and report the conflict.

---

## Scope Control (Overscope Prevention)

Implementation must remain strictly within the scope defined by the user request and the active iteration plan.

The agent must not expand scope autonomously.

Rules:

- Implement only what is explicitly requested.
- Do not introduce new features unless explicitly specified.
- Do not refactor unrelated code.
- Do not reorganize project structure unless required by the task.
- Do not rename files, components, or types unless required by the task.
- Do not add libraries or dependencies unless explicitly requested.
- Do not upgrade dependency versions unless explicitly requested.
- Do not modify configuration or build files unless required.

If additional improvements appear beneficial:

- Do not implement them.
- Record them under **Next Steps** in the completion report.

When the requested task and Done Criteria are satisfied:

- Stop implementation.
- Do not continue with extra improvements.

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

## Execution Style

- Work autonomously: gather context, implement, verify, and report in one turn when feasible.
- Default to action with reasonable assumptions only when they do not expand scope.
- Ask questions when assumptions would affect behavior or visible output.
- Stop and ask if unexpected file changes appear during work.
- Avoid loops; if progress stalls, end with concise blocker details and a targeted question.
- Do not expand scope to improve unrelated areas of the repository.

---

## Search, Read, and Tools

- Prefer the Grep tool for content search and Glob tool for file pattern search.
- Use dedicated tools over raw shell when available.
- Use terminal commands only when no dedicated tool exists.
- Batch and parallelize independent reads/searches whenever possible.
- Decide needed files first, then fetch in batches.
- Use sequential reads only when necessary.
- Treat `L123:...` style prefixes as line metadata, not file content.
- Avoid exploring unrelated parts of the repository once sufficient context is gathered.

---

## Implementation Rules

- Optimize for correctness, clarity, and reliability.
- Implement mockup designs faithfully — `docs/design/mockup.html` and `docs/design/mockup-login.html` are the source of truth for UI.
- Reuse existing helpers and patterns before introducing new abstractions.
- Preserve behavior by default; call out intentional behavior or UX changes.
- Keep strict TypeScript types; `any` is prohibited.
- When defining TypeScript types for API responses, refer to `docs/analysis/data-spec.md` for field names and allowed values (e.g. `status`: `NORMAL` / `UNVERIFIED` / `ERROR` / `HIDE`, `soldOutYn`: `Y` / `N`).
- Avoid broad catch-all error handling.
- Do not hide failures with silent fallbacks or quiet early returns.
- Do not add error handling for scenarios that cannot occur.
- Only validate at system boundaries (user input, API responses).
- Batch coherent edits; avoid repeated micro-edits with no net progress.
- Limit changes strictly to the files necessary for the task.

---

## Editing and Safety

- Add brief comments only where intent is not obvious.
- Prefer focused single-file edits.
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

- All work must start from the latest `main` branch. Pull from `main` before starting any task.
- Create a new branch from `main` for every task. Branch naming: `feature/<task-description>`.
- Never commit directly to `main`.
- Do not create commits unless explicitly requested.
- Do not push commits automatically.
- Do not create pull requests unless explicitly requested.
- Do not create or modify branches unless requested.

If version control actions are required:

- Ask for confirmation first.

---

## Validation and Reporting

- Run relevant build or lint steps when possible after changes.

For frontend projects:

- Prefer `npm run build` for build validation.
- Use `npm run lint` for lint validation when needed.

If verification cannot be executed:

- Clearly state what was not verified and why.

Reporting order:

1. Outcome summary
2. Files changed
3. Commands executed
4. Test results
5. Notable findings

---

## Planning Rules

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

## Special Request Handling

- For simple operational requests, run the command directly.
- For "review" requests, prioritize findings first:
  - bugs
  - regressions
  - risks
  - missing tests
  - missing or broken UI behavior

---

## Required Guidance Files (Must Read Before Implementation)

Claude must read and align with the following documents before starting implementation.

If a required file does not exist, it must be created before development begins.

Required documents:

- `CLAUDE.team.md` (if present)  
  Repository or team-specific constraints. Highest priority after user request.

- `CLAUDE.md`  
  Global agent execution rules.

- `docs/guide/development-rules.md`  
  React/TypeScript development rules, architecture principles, coding standards.

- `docs/guide/style-guide.md`  
  Design tokens, colors, component style rules.

- `docs/guide/api-guide.md`  
  Axios setup, session handling, API call rules.

- `docs/guide/testing-strategy.md`  
  Build/lint validation strategy and verification rules.

- `docs/guide/done-criteria.md`  
  Definition of Done and acceptance checklist.

- `docs/design/pages.md`  
  Screen layout, component design, feature behavior spec.

- `docs/design/mockup.html`  
  Dashboard mockup. Reference for UI implementation.

- `docs/design/mockup-login.html`  
  Login mockup. Reference for UI implementation.

- `docs/product/api-spec.md`  
  Backend API specification. Data contracts, schema definitions, validation constraints.

- `docs/analysis/data-spec.md`  
  Backend SQLite schema. Field names, allowed values, and constraints to reference when defining TypeScript types.

- `docs/product/architectures.md`  
  Frontend system architecture, layer structure, data flow, auth flow, deployment.

- `docs/product/prd.md`  
  Iteration roadmap, task breakdown, and Claude Code consistency rules.

- `docs/plan/iteration/iteration-XX-plan.md`  
  Current iteration scope and boundaries.

Implementation must not begin without reviewing all relevant documents.

If inconsistencies are detected between documents:

- Pause implementation
- Report the conflict

---

## Iteration Deliverables and Documentation Structure

The following documentation structure must be maintained:

```
docs/
├── guide/
│   ├── development-rules.md
│   ├── style-guide.md
│   ├── api-guide.md
│   ├── testing-strategy.md
│   ├── done-criteria.md
│   └── github-pr-merge-policy.md
├── design/
│   ├── pages.md
│   ├── mockup.html
│   └── mockup-login.html
├── product/
│   ├── api-spec.md
│   ├── architectures.md
│   └── prd.md
├── analysis/
│   └── data-spec.md
├── plan/
│   └── iteration/
│       └── iteration-01-plan.md
├── test/
│   └── results/
│       └── iteration-01-test-result.md
└── reports/
    └── iteration-01-result.md
```

For every iteration:

- Ensure guide documents reflect current repository standards.
- Create or update `iteration-XX-plan.md` before implementation.
- Implement code and corresponding tests.
- Record executed commands and results in `iteration-XX-test-result.md`.
- Summarize delivered scope, key decisions, and open gaps in `iteration-XX-result.md`.

Iteration numbering must use two digits (01, 02, 03...).

No documentation step may be silently skipped.

---

## Frontend Quality Bar

- Follow React 18 + TypeScript strict mode conventions first.
- Keep component-hook-api responsibilities separated.
- Avoid business logic in components; extract to custom hooks.
- Use TypeScript interfaces for all Props and API response types.
- Do not expose raw Axios calls outside `src/api/`.
- Prefer constructor-style imports; avoid barrel files unless already established.
- Return consistent error states across API functions.
- Keep side effects explicit; avoid hidden state mutations.

### Naming

| Target | Rule | Example |
|---|---|---|
| Component files | PascalCase | `ProductTable.tsx` |
| Hooks / utils / API | camelCase | `useGoods.ts`, `client.ts` |
| Style modules | Same as component | `ProductTable.module.scss` |
| Types / interfaces | PascalCase | `GoodsItem`, `BatchStatus` |
| Constants | UPPER_SNAKE_CASE | `PAGE_SIZE` |

### Components

- Functional components only.
- Props types must be defined as `interface` at the top of the file.
- `any` is prohibited.
- Business logic must be extracted into custom hooks.

### Styles

- Component styles: CSS Modules (`.module.scss`).
- Global variables: defined only in `_variables.scss`.
- Inline styles are prohibited; use CSS variables for dynamic values.
- Use the CSS variable names, class names, and layout from `docs/design/mockup.html` as the reference.

### API

- All API calls must go through `src/api/` functions.
- Direct `axios` calls inside components or hooks are prohibited.
- Common error handling is in `client.ts` interceptors: 401 → redirect to `/login`, except for `/api/auth/` routes.

---

## Tech Stack

- React 18, TypeScript (strict mode), Vite
- CSS Modules + SCSS (`_variables.scss` holds all design tokens)
- Axios (`withCredentials: true`)
- Deploy: AWS EC2 + Nginx

---

## Folder Structure (Target)

```
src/
├── api/
│   ├── client.ts        ← Axios instance and interceptors
│   ├── auth.ts
│   ├── goods.ts
│   ├── batch.ts
│   └── teams.ts
├── components/
│   ├── layout/
│   ├── dashboard/
│   └── modals/
├── pages/
│   ├── LoginPage.tsx
│   └── DashboardPage.tsx
├── hooks/
│   ├── useAuth.ts
│   ├── useGoods.ts
│   ├── useBatch.ts
│   └── useTeamsNotification.ts
├── types/
│   ├── auth.ts
│   ├── goods.ts
│   ├── batch.ts
│   └── teams.ts
├── styles/
│   ├── _variables.scss
│   ├── _reset.scss
│   └── global.scss
├── utils/
│   └── format.ts
├── App.tsx
└── main.tsx
```

---

## Response Format

- Keep responses concise, factual, and collaborative.
- Use plain text with lightweight structure only when it improves readability.
- Do not dump large file contents; reference paths instead.
- Do not tell the user to copy or save files manually.
- For code changes:
  - explain what changed
  - explain why
  - suggest next steps only when appropriate
- Use single-level bullets only.

---

## Team

| Member | Role |
|---|---|
| Sohee Kang | React UI development, API integration, EC2 deployment |
| Donghyun Kim | Java/Spring Boot API, API documentation |
