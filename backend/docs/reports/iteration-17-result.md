# Iteration 17 Result

## Delivered Scope

- fixed the WMS stock request so the application sends the documented query string and JSON payload instead of the previous ad-hoc form body
- normalized `GET /api/auth/session` unauthorized responses to the shared `ErrorResponse` contract
- changed OTP mail send failures from a generic `500` into a dedicated `OTP_MAIL_DELIVERY_FAILED` business error
- added logging around OTP and WMS failure paths so runtime investigation no longer depends on generic batch messages only
- recorded the live API and direct external-call verification results for the shared SQLite DB at `data/sfood-mall-automation.db`

## Key Decisions

- preserved the existing user-provided `application.yml` fallback values and aligned the code to those values instead of forcing a new config shape
- kept the WMS login URL as a base endpoint and built the login query string in code, because the runtime client already owns `user-id` and `password`
- treated SMTP rejection as an external dependency failure and surfaced it as `502 Bad Gateway`
- kept the scope limited to the three confirmed defects from the live API check: session 401 shape, OTP error handling, and WMS request formatting

## Validation Summary

- targeted regression tests passed
- `./gradlew test` passed
- `./gradlew build` passed
- live API verification confirmed:
  - `GET /api/auth/session` now returns JSON on unauthorized access
  - `POST /api/auth/email-verifications` now returns `OTP_MAIL_DELIVERY_FAILED` instead of `INTERNAL_SERVER_ERROR`
  - authenticated `goods`, `config`, and `batch-schedules` APIs still respond normally
- direct WMS verification confirmed:
  - login succeeds with `ErrorCode=0`
  - stock lookup succeeds with `ErrorCode=0` and returns inventory data

## Remaining Gaps

- batch execution is still not fully stable against the shared SQLite file during live runtime. `batch_execution_log` contains intermittent failures, including:
  - `WMS scraping error`
  - `SQLITE_BUSY_SNAPSHOT ... database is locked`
- the last code change added detailed WMS exception messages, but a fresh batch run against the final restarted process was not captured again in this iteration document
- OTP delivery is still blocked by the current SMTP sender IP being listed by Spamhaus; this is an infrastructure issue, not an application formatting issue

## Next Steps

- run one more manual batch on the current process and inspect the now-detailed `WMS scraping` message if live scheduler failures continue
- reduce SQLite write contention during scheduler execution if `SQLITE_BUSY_SNAPSHOT` continues on the shared DB
- clear the outbound mail reputation issue or switch to an SMTP path that is not blocked by Spamhaus before relying on OTP mail delivery in real use
