# prod-deploy-config Test Result

## Scope

- Validate Spring profile separation and production deployment workflow preparation.

## Environment

- Profile: default `local` profile during Gradle tests.
- Database: SQLite test/local configuration.
- External dependencies: Not called directly.

## Commands

- `./gradlew test`
- `rg -n "BASTION_|APP_EC2_|APP_DEPLOY_DIR|APP_SERVICE_NAME|SSM_|RDS_" ../.github/workflows/deploy-prod.yml`
- `./gradlew clean bootJar --no-daemon`
- `./gradlew test --tests com.sfood.automation.common.security.SessionAuthInterceptorTest --no-daemon`

## Results

- PASS: `./gradlew test` completed successfully.
- PASS: No bastion, app EC2, SSM, or RDS deployment secrets are referenced by the backend deploy workflow.
- PASS: `./gradlew clean bootJar --no-daemon` completed successfully after running with filesystem permission outside the sandbox for Gradle cache writes.
- FAIL: GitHub Actions run `24756983883` failed during S3 multipart upload with `RequestTimeout`.
- PASS: Retest GitHub Actions run `24757160107` completed successfully after S3 upload settings adjustment.
- PASS: `SessionAuthInterceptorTest` verifies `GET /actuator/health` is allowed without session.

## Failure Notes

- S3 upload failure was network/upload-timeout related, not a build or credential failure.

## Retest Results

- PASS: GitHub Actions run `24757160107` built the backend jar and uploaded it to S3 successfully.

## Acceptance Criteria Coverage

- AC1: Covered by `application-prod.yml` using `${RDS_HOST}`, `${RDS_DB_NAME}`, `${RDS_USERNAME}`, and `${RDS_PASSWORD}` without fixed defaults.
- AC2: Covered by `application-local.yml` and default `local` profile.
- AC3: Covered by `.github/workflows/deploy-prod.yml`.
- AC4: Covered by limiting workflow secret usage to existing AWS credentials and excluding bastion, app EC2, SSM, and RDS deployment secrets.
