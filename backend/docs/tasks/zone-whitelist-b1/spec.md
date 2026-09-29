# zone-whitelist-b1 Spec

## Problem Statement

- WMS 재고 집계에서 `ZONE_CD`가 숫자인 경우만 허용되어 `B1` zone 재고가 배치 판단에서 제외된다.
- 운영 기준은 영문 zone 전체 허용이 아니라 `숫자 zone + 명시적 whitelist zone` 허용이다.

## Business Context

- 배치 품절/정상 전환은 WMS zone 기준에 직접 의존한다.
- 허용되어야 하는 `B1` zone 재고가 빠지면 품절 판단이 과도하게 보수적으로 계산될 수 있다.

## Scope

- WMS zone 허용 기준을 `숫자 zone 또는 whitelist zone(B1)`으로 변경한다.
- 배치 집계와 판매 상태 정책이 같은 zone 기준을 사용하도록 정리한다.
- 관련 테스트와 문서를 현재 규칙에 맞게 갱신한다.

## Out Of Scope

- 추가 whitelist zone 확장 UI 또는 설정화
- WMS 수량 계산 방식 외의 다른 판매 상태 정책 변경
- 배포 설정, 운영 데이터 보정, 수동 DB 수정

## User Or Operator Scenarios

- Scenario 1:
  동일 ERP 상품의 WMS 재고가 `ZONE_CD=B1` 에 존재하면 배치 집계에 포함된다.
- Scenario 2:
  `ZONE_CD=SHP` 같은 비허용 영문 zone 재고는 계속 제외된다.

## Acceptance Criteria

- AC1:
  `B1` zone row는 WMS 정제와 집계 대상에 포함된다.
- AC2:
  숫자 zone row는 기존처럼 계속 포함된다.
- AC3:
  whitelist에 없는 영문 zone row는 계속 제외된다.
- AC4:
  판매 상태 정책은 zone 허용 여부를 숫자 여부가 아니라 최종 허용 여부 기준으로 평가한다.
- AC5:
  관련 단위/플로우 테스트와 문서가 새 기준과 일치한다.

## Edge Cases

- Edge case 1:
  동일 ERP 코드에 `43`, `B1`, `SHP` zone이 섞여 있어도 `43`과 `B1`만 포함되어야 한다.
- Edge case 2:
  `ZONE_CD`가 null 또는 blank면 제외되어야 한다.

## Assumptions

- `B1`은 현재 유일한 whitelist zone이다.
- 허용 기준은 환경설정이 아닌 코드 상수로 유지해도 충분하다.

## Clarifications

- Question:
  기존 `EXCLUDED_ZONE_CODES` 모델을 유지할지 여부
  Answer:
  기준을 `숫자 + whitelist`로 바꾸는 것이 요구사항에 더 직접적이므로 zone 허용 판단을 중심으로 정리한다.
