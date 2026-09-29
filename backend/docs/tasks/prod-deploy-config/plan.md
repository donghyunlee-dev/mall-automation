# prod-deploy-config Plan

## Summary

- Move local SQLite settings into `application-local.yml`.
- Keep common application settings in `application.yml` and set local as the default active profile.
- Keep production RDS settings externalized in `application-prod.yml`, while copying non-RDS application values as fixed defaults.
- Add `.github/workflows/deploy-prod.yml` for jar build and S3 artifact upload.

## Impacted Modules

- Spring Boot resource configuration.
- GitHub Actions workflow configuration.
- Task documentation.

## Architecture Approach

- Use Spring profiles for environment separation.
- Use the existing AWS GitHub Secrets used by frontend deployment.
- Leave EC2 download and restart outside this workflow for now.

## Data And API Impact

- No API contract changes.
- No database schema changes.
- Production runtime depends on the already-applied PostgreSQL schema and data.

## Validation Plan

- Inspect YAML files for expected profile split and placeholder usage.
- Verify the workflow no longer references bastion, SSH, SSM, app EC2, or RDS secrets.

## Risks

- Risk 1: Hardcoded non-RDS values include sensitive values by user request.
- Risk 2: EC2 download and restart are manual or externalized for now.

## Rollback Considerations

- Revert the added workflow and profile YAML changes if deployment setup needs a different operational model.
