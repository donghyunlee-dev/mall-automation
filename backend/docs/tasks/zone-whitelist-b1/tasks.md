# zone-whitelist-b1 Tasks

## Implementation Tasks

- [x] zone 허용 기준을 `숫자 + whitelist(B1)`로 재정의
- [x] 정제/집계/정책 계층에 같은 기준 반영

## Test Tasks

- [x] `B1` 포함 및 비허용 영문 zone 제외 테스트 추가/수정
- [x] 관련 배치/정책 회귀 테스트 실행

## Documentation Tasks

- [x] product 문서의 zone 기준 설명 갱신
- [x] task 결과 문서 정리

## Progress Notes

- 브랜치 `feature/zone-whitelist-b1` 생성
- `WmsZoneEligibility` 추가로 zone 허용 기준을 단일화
- `AggregatedStock`는 `allowedZone` 기준으로 정책이 소비하도록 정리

## Review Notes

- zone 허용 판단은 중복 모델보다 단일 의미로 유지하는 쪽이 안전하다.
