# Iteration 33 Plan

## Scope

- Fix the failing `BatchResultMailSenderTest`.
- Keep the change limited to the batch result email table header text.
- Do not change mail recipients, send flow, logging, or batch behavior.

## Validation Plan

- `./gradlew test --tests com.sfood.automation.batch.infrastructure.job.BatchResultMailSenderTest`
- `./gradlew test`
