# Iteration 34 Test Result

## Scope

- PostgreSQL production-test bootstrap SQL validation.

## Commands

- `docker exec sfood-postgres psql -U sfood -d sfood_automation -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"`
- `docker cp src/main/resources/postgresql/schema-postgresql.sql sfood-postgres:/tmp/schema-postgresql.sql`
- `docker cp src/main/resources/postgresql/data-postgresql.sql sfood-postgres:/tmp/data-postgresql.sql`
- `docker exec sfood-postgres psql -U sfood -d sfood_automation -f /tmp/schema-postgresql.sql`
- `docker exec sfood-postgres psql -U sfood -d sfood_automation -f /tmp/data-postgresql.sql`
- `SPRING_PROFILES_ACTIVE=prod RDS_HOST=localhost RDS_PORT=5432 RDS_DB_NAME=sfood_automation RDS_USERNAME=sfood RDS_PASSWORD=sfoodpass ./gradlew bootRun`

## Results

- PASS: PostgreSQL schema SQL applied from an empty public schema.
- PASS: PostgreSQL data SQL applied.
- PASS: `goods.godo_sync_blocked_yn` exists after schema creation.
- PASS: seed timestamps are stored in ISO-8601 format.
- PASS: backend started successfully with the `prod` profile against Docker PostgreSQL.
