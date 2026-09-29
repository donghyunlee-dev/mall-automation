# Iteration 34 Plan

## Scope

- Fix PostgreSQL production-test bootstrap SQL.
- Add the missing `goods.godo_sync_blocked_yn` column to the PostgreSQL schema.
- Store PostgreSQL seed timestamps in the ISO-8601 format accepted by the application parser.
- Do not change runtime Java logic or SQLite schema.

## Validation Plan

- Recreate the Docker PostgreSQL public schema from the PostgreSQL SQL files.
- Apply `schema-postgresql.sql` and `data-postgresql.sql`.
- Start the backend with `SPRING_PROFILES_ACTIVE=prod` against the Docker PostgreSQL database.
