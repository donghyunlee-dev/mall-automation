# Iteration 02 Plan

## Scope

- Add GitHub Actions validation for backend pull requests targeting `main`
- Add repository guidance for merge readiness and required checks

## Objectives

- Run backend test and build automatically on PRs to `main`
- Document how to configure `main` branch protection so PRs cannot be merged before checks pass

## Approval Steps

1. Sync `main` and create working branch - Done
2. Add GitHub Actions workflow - Done
3. Add merge policy documentation - Done
4. Validate locally and publish PR - Pending

## Validation Plan

- Run `./gradlew test`
- Run `./gradlew build`
- Review workflow YAML and target branch filters
