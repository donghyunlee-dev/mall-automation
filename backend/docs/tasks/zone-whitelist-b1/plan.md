# zone-whitelist-b1 Plan

## Summary

- zone 허용 판단을 `숫자 zone 또는 whitelist zone(B1)`으로 재정의하고, 정제/집계/정책 계층이 같은 의미를 사용하도록 수정한다.

## Impacted Modules

- `com.sfood.automation.wms.infrastructure.scraping`
- `com.sfood.automation.goods.domain.policy`
- 관련 테스트 패키지
- product/task 문서

## Architecture Approach

- WMS 정제 계층에서 단일 zone 허용 판정을 제공한다.
- 집계 결과는 정책이 그대로 소비할 수 있도록 허용 여부를 명시한다.
- 정책 계층은 기존 `numeric/excluded` 조합 대신 최종 허용 여부를 기준으로 판단한다.

## Data And API Impact

- 외부 API 계약 변경 없음
- DB 스키마 변경 없음
- 배치의 WMS 집계 입력 기준만 변경

## Validation Plan

- `StockDataRefinerTest`에 `B1` 포함/비허용 영문 zone 제외 케이스 추가
- `GoodsSoldOutPolicyTest`에 허용 zone 기반 판단 회귀 검증
- `ProductSyncBatchJobTest`에 `B1` 포함 시나리오 반영
- 필요 범위 `./gradlew test --tests ...` 실행

## Risks

- Risk 1:
  zone 허용 의미를 부분적으로만 바꾸면 정제 결과와 정책 판단이 어긋날 수 있다.
- Risk 2:
  기존 `numericZone` 의미를 그대로 둔 채 `B1`을 허용하면 테스트와 문서가 모순될 수 있다.

## Rollback Considerations

- zone 허용 로직과 관련 테스트만 되돌리면 기존 숫자-only 기준으로 복구 가능하다.
