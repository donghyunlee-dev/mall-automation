# GitHub PR Merge Policy

## Purpose

This document defines the branching strategy, how pull requests targeting `main` are validated, and when they are safe to merge.

## Branching Strategy

- All work must start from the latest `main` branch.
- Create a new branch from `main` before starting any task.
- Branch naming convention: `feature/<task-description>` (e.g., `feature/login-page`, `feature/product-table`)
- Never commit directly to `main`.

## Workflow File

- GitHub Actions workflow: `.github/workflows/frontend-pr-check.yml`

## Trigger Rule

The workflow runs when:

- a pull request targets `main`
- the pull request includes changes under `frontend/**`
- the workflow file itself changes

It also supports manual execution through `workflow_dispatch`.

## Validation Steps

The workflow runs these commands from the `frontend` directory:

- `npm ci`
- `npm run build`
- `npm run lint`

## Merge Readiness Rule

A pull request is considered merge-ready only when:

- the PR target branch is `main`
- the `Frontend PR Check / frontend-build-lint` GitHub Actions job is green
- required review conditions defined in repository settings are satisfied

## Required GitHub Settings

To prevent merging before validation completes, configure branch protection for `main` in GitHub:

1. Open repository `Settings`
2. Open `Branches`
3. Add or edit the branch protection rule for `main`
4. Enable `Require a pull request before merging`
5. Enable `Require status checks to pass before merging`
6. Add the required status check:
   `Frontend PR Check / frontend-build-lint`

Optional but recommended:

- enable `Require branches to be up to date before merging`
- enable `Dismiss stale pull request approvals when new commits are pushed`

## Notes

- The workflow file can run validation, but merge blocking is enforced by GitHub branch protection, not by the YAML file alone.
- If a PR does not touch `frontend/**`, this workflow does not run.
