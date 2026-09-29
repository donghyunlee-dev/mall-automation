# prod-deploy-config Result

## Delivered Scope

- Split Spring Boot environment configuration into common, local, and production profile files.
- Added a GitHub Actions production deployment workflow that builds the boot jar and uploads it to S3.
- Added Spring Boot Actuator and opened `GET /actuator/health` for AWS target group health checks.

## Changed Files

- `.github/workflows/deploy-prod.yml`
- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-prod.yml`
- `build.gradle`
- `src/main/java/com/sfood/automation/common/security/SessionAuthInterceptor.java`
- `src/test/java/com/sfood/automation/common/security/SessionAuthInterceptorTest.java`
- `docs/tasks/prod-deploy-config/spec.md`
- `docs/tasks/prod-deploy-config/plan.md`
- `docs/tasks/prod-deploy-config/tasks.md`
- `docs/tasks/prod-deploy-config/test-result.md`
- `docs/tasks/prod-deploy-config/result.md`

## Key Decisions

- Common non-RDS application values remain in `application.yml` with their existing fixed defaults as requested.
- The workflow only uses the existing AWS GitHub Secrets used by frontend deployment: `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.
- EC2 download, `.env` creation, and application restart are intentionally left outside GitHub Actions for now.

## Open Gaps

- The app EC2 instance must download the uploaded jar from S3 and run/restart the backend outside this workflow.
- The artifact S3 prefix currently reuses the existing production bucket configured in the workflow.

## Next Steps

- Keep `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`; unused SSH, bastion, SSM, app EC2, and RDS deployment secrets can be removed after confirming no other workflow uses them.
