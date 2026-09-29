# GitHub PR Merge Policy

## Purpose

This document defines how pull requests targeting `main` are validated and when they are safe to merge.

## Workflow File

- GitHub Actions workflow: `.github/workflows/backend-pr-check.yml`

## Trigger Rule

The workflow runs when:

- a pull request targets `main`
- the pull request includes changes under `backend/**`
- the workflow file itself changes

It also supports manual execution through `workflow_dispatch`.

## Validation Steps

The workflow runs these commands from the `backend` directory:

- `./gradlew test --no-daemon`
- `./gradlew build --no-daemon`

## Merge Readiness Rule

A pull request is considered merge-ready only when:

- the PR target branch is `main`
- the `Backend PR Check / backend-build-test` GitHub Actions job is green
- required review conditions defined in repository settings are satisfied

## Required GitHub Settings

To prevent merging before validation completes, configure branch protection for `main` in GitHub:

1. Open repository `Settings`
2. Open `Branches`
3. Add or edit the branch protection rule for `main`
4. Enable `Require a pull request before merging`
5. Enable `Require status checks to pass before merging`
6. Add the required status check:
   `Backend PR Check / backend-build-test`

Optional but recommended:

- enable `Require branches to be up to date before merging`
- enable `Dismiss stale pull request approvals when new commits are pushed`

## Notes

- The workflow file can run validation, but merge blocking is enforced by GitHub branch protection, not by the YAML file alone.
- If a PR does not touch `backend/**`, this workflow does not run.
