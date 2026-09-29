# Iteration 22 Test Result

## Commands


- `./gradlew test --tests com.sfood.automation.goods.infrastructure.external.GoodsSearchApiCallerTest --tests com.sfood.automation.goods.infrastructure.external.GodoApiClientTest --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest`

## Results

- focused parser and registration validation passed after the XML parser fix

## Failures

- initial focused parser run failed in `GoodsSearchApiCallerTest.should_parse_goods_info_from_live_godo_xml_shape`
- no focused failures remained after the parser update

## Notes

- production `api_transaction_log.response_body` showed `soldOutFl` nested under `data -> return -> goods_data`
- the parser now supports both the prior simplified XML shape and the live production XML shape

