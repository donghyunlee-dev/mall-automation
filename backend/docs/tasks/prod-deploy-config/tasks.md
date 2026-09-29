# prod-deploy-config Tasks

## Implementation Tasks

- [x] Create task documentation.
- [x] Split local SQLite settings into `application-local.yml`.
- [x] Prepare production YAML with RDS values externalized and non-RDS fixed values copied.
- [x] Add GitHub Actions deployment workflow through bastion.
- [x] Add actuator health endpoint support for AWS health checks.

## Test Tasks

- [x] Verify generated YAML files.
- [x] Run focused configuration/context validation where feasible.

## Documentation Tasks

- [x] Record validation in `test-result.md`.
- [x] Record delivered scope in `result.md`.

## Progress Notes

- User confirmed RDS SQL has already been applied.
- Bastion and app EC2 connection details must remain external.

## Review Notes

- `APP_DEPLOY_DIR` remains required to avoid hardcoding app EC2 user paths.
