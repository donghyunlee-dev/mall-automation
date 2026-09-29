# 회원 이력 조회 API 중복 호출 제거 — 설계

> 작성일: 2026-07-10
> 대상 화면: 회원 상세 모달의 "수정이력" 모달(`MemberDetailModal.tsx`, `MemberHistoryModal.tsx`)

## 배경

회원 상세 화면에서 "수정이력"을 열면 `GET /api/members/{memNo}/business/logs` (또는 `.../partner/logs`)가 2번 호출되는 문제가 재현 테스트로 확인됐다.

원인은 동일 데이터를 조회하는 두 개의 독립된 코드 경로가 서로의 진행 상태를 모른 채 존재하기 때문이다.

- `MemberDetailModal.tsx`: 회원 상세가 로드되면 즉시 이력을 미리 불러온다(수정이력 버튼을 눌렀을 때 로딩 없이 바로 보여주기 위함).
- `MemberHistoryModal.tsx`: 이력 모달이 열릴 때, 부모가 준 이력 데이터가 아직 없으면 스스로 다시 조회한다.

부모의 미리 불러오기 응답이 아직 안 돌아온 상태에서 사용자가 "수정이력" 버튼을 누르면, 이미 나가 있는 요청과 별개로 자식이 또 요청을 보내 API가 2번 호출된다(재현 테스트로 확인 완료 — 응답이 늦을수록 항상 재현됨, 응답이 클릭보다 먼저 끝나면 1번만 호출).

## 방향

`MemberHistoryModal`의 자체 조회 로직을 완전히 제거하고, `MemberDetailModal`을 이력 데이터의 유일한 조회 주체로 만든다. `MemberHistoryModal`은 순수하게 부모가 전달한 데이터(그리고 에러 상태)를 표시만 하는 컴포넌트가 된다.

## 핵심 동작

1. **정상 케이스**: 미리 불러오기가 "수정이력" 버튼을 누르기 전에 끝나 있으면 지금과 동일하게 즉시 표시된다.
2. **미리 불러오기가 아직 안 끝난 상태에서 버튼을 누른 경우**: 모달은 스스로 요청하지 않고 "불러오는 중..."을 표시하며 부모의 응답을 기다린다. API는 항상 1번만 호출된다.
3. **미리 불러오기가 실패한 경우**: 부모가 소유한 에러 상태(`businessLogsError`/`partnerLogsError`)를 모달에 전달해 에러 메시지를 표시한다. 자체 재시도는 없다 — 다시 시도하려면 상세 모달을 닫고 다시 열어야 한다(회원을 다시 불러오면 미리 불러오기가 재실행됨). 재시도 버튼 등은 이번 수정 범위에 포함하지 않는다.

## 구현 범위

### `MemberDetailModal.tsx`

- `businessLogsError`/`partnerLogsError` state(`string | null`, 초기값 `null`)를 추가한다.
- 이력 미리 불러오기 effect의 `.then`에서 성공 시 에러를 `null`로 초기화하고, `.catch`에서 각각의 에러 state에 안내 메시지를 채운다.
- `prevMember` 변경 시 `businessLogs`/`partnerLogs`와 함께 두 에러 state도 `null`로 초기화한다.
- `MemberHistoryModal` 렌더 시 `memNo` prop을 제거하고, `businessLogsError`/`partnerLogsError` prop을 새로 전달한다.

### `MemberHistoryModal.tsx`

- 자체 조회 `useEffect`, `fetchedBizLogs`/`fetchedPartnerLogs`/`error`/`prevMemNo`/`prevKind` state를 전부 제거한다.
- `getMemberBusinessLogs`/`getMemberPartnerLogs` import와 `memNo` prop을 제거한다(더 이상 사용하지 않음).
- `businessLogs`/`partnerLogs`/`businessLogsError`/`partnerLogsError` prop을 직접 사용하도록 단순화한다(`effectiveBizLogs` 같은 병합 로직 제거).
- `useState`가 더 이상 필요 없어지면 import에서 제거한다.

## 테스트 계획

- `MemberDetailModal.test.tsx`(확장): 이력 API가 정확히 1번만 호출되는지 검증하는 테스트(미리 불러오기가 지연되는 상황을 흉내내 "수정이력" 버튼을 클릭해도 추가 호출이 없는지 확인), 미리 불러오기 실패 시 에러 메시지가 이력 모달에 표시되는지 검증하는 테스트.

## 범위 밖

- 실패한 이력 조회에 대한 재시도(다시 시도) 버튼은 이번 수정에 포함하지 않는다.
- `MemberHistoryModal`을 이 부모 없이 단독으로 재사용할 수 있는 기존의 "폴백 조회" 능력은 이번 변경으로 없어진다 — 사용자가 명시적으로 선택한 방향(Option B)이다.
