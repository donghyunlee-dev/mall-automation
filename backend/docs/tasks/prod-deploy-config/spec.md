# prod-deploy-config Spec

## Problem Statement

- Prepare environment configuration separation for production deployment.
- Prepare GitHub Actions deployment through a bastion host without hardcoding RDS, bastion, or app EC2 connection details.

## Business Context

- The backend will be deployed to an EC2 instance and will connect to AWS RDS PostgreSQL in production.
- GitHub Actions must copy the built jar to the app EC2 instance through a bastion host and restart the application.

## Scope

- Separate local default configuration from production profile configuration.
- Keep RDS, bastion, and app EC2 values externalized.
- Copy the current fixed non-RDS application values into production configuration as requested.
- Add a GitHub Actions workflow YAML for build and bastion-based deployment.

## Out Of Scope

- Do not provision AWS resources.
- Do not change application business behavior.
- Do not create commits, branches, or pull requests.
- Do not apply database SQL because the user confirmed it is already complete.

## User Or Operator Scenarios

- Operator runs the app locally without selecting a profile and uses SQLite defaults.
- Operator deploys from GitHub Actions to EC2 with `prod` profile and RDS settings supplied externally.
- Operator deploys to a private app EC2 instance reachable only through a bastion host.

## Acceptance Criteria

- AC1: Production profile uses PostgreSQL/RDS placeholders supplied from environment or GitHub Secrets.
- AC2: Local profile remains SQLite-based.
- AC3: GitHub Actions workflow builds the jar, copies it through bastion, writes production environment values, and restarts the app.
- AC4: Bastion, app EC2, and RDS details are not hardcoded.

## Edge Cases

- Missing GitHub Secrets should fail the deployment before the app is successfully restarted.
- App EC2 user may not have passwordless `sudo systemctl`; this remains an operational prerequisite.
- App deploy directory must be provided as a GitHub Secret because app EC2 paths are not hardcoded.

## Assumptions

- RDS schema and initial data have already been applied.
- EC2 has JDK 21 installed.
- A `systemd` service named `sfood-backend` will be used or created separately on the app EC2 instance.

## Clarifications

- Question: Should non-RDS secret-like application values remain fixed in YAML for now?
  Answer: Yes. The user asked to copy the existing fixed values except for RDS, bastion, and app EC2 information.
