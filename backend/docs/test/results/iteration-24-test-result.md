# Iteration 24 Test Result

## Commands

- `./gradlew test --tests com.sfood.automation.auth.infrastructure.mail.OtpMailSenderTest --tests com.sfood.automation.auth.presentation.Task09EndToEndIntegrationTest`
- `./gradlew test`
- `./gradlew build`

## Results

- focused OTP mail tests: PASS
- full build: FAIL
- full test run: FAIL

## Failures

- `./gradlew build`
  - `SyncGodoStatusUseCaseTest.should_recover_on_sale_using_goods_available_qty_after_safety_filtered_aggregation()`
  - `SyncGodoStatusUseCaseTest.should_return_sync_result_counts()`
  - `SyncGodoStatusUseCaseTest.should_process_out_of_stock_before_on_sale()`
- `./gradlew test`
  - same existing `SyncGodoStatusUseCaseTest` failures surfaced
  - the test run was started in parallel with `./gradlew build`, which also caused Gradle XML test result write conflicts
## Notes

- OTP HTML mail assertions passed after changing the sender to MIME HTML and updating the end-to-end mail recording stub
- the remaining build failures are outside the ATCS-209 write scope and predate this mail-template change
