# zone-whitelist-b1 Result

## Delivered Scope

- WMS zone 허용 기준을 `숫자 zone + whitelist zone(B1)`으로 변경했다.
- 정제/집계/판매 상태 정책이 같은 zone 허용 의미를 사용하도록 `allowedZone` 기준으로 정리했다.
- `B1` 포함과 비허용 영문 zone 제외를 검증하는 회귀 테스트와 문서를 갱신했다.

## Changed Files

- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/WmsZoneEligibility.java`
- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefiner.java`
- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/WmsStockAggregator.java`
- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/AggregatedStock.java`
- `src/main/java/com/sfood/automation/goods/domain/policy/GoodsSoldOutPolicy.java`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefinerTest.java`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJobTest.java`
- `src/test/java/com/sfood/automation/goods/domain/policy/GoodsSoldOutPolicyTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping/LiveWmsRefinementIntegrationTest.java`
- `docs/product/prd.md`
- `docs/product/architectures.md`

## Key Decisions

- zone 허용 기준은 `EXCLUDED_ZONE_CODES` 중심이 아니라 `WmsZoneEligibility.isAllowed()` 중심으로 통합했다.
- 집계 결과는 `numericZone/excludedZone` 조합 대신 `allowedZone` 하나로 정책에 전달한다.

## Open Gaps

- whitelist zone은 현재 코드 상수(`B1`)로 관리한다.

## Next Steps

- whitelist zone이 추가될 가능성이 커지면 운영 설정 또는 관리 화면으로 분리 여부를 검토한다.
