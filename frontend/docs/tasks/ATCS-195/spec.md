# ATCS-195 Spec

## Feature Specification
상품 목록 테이블의 판매 상태 컬럼에 "확인불가" 뱃지를 추가한다.

### 뱃지 표시 조건
| 조건 | 표시 |
|------|------|
| `status === 'ERROR'` | 확인불가 (앰버 #d97706) |
| `status !== 'ERROR'` && `soldOutYn === 'Y'` | 품절 (코랄) |
| `status !== 'ERROR'` && `soldOutYn === 'N'` | 판매중 (초록) |

## API Changes
없음. 기존 API 응답의 `status` 필드를 그대로 활용.

## Database Changes
없음.

## Domain Model Changes
없음.

## Validation Rules
- `status === 'ERROR'`이면 `soldOutYn` 값에 관계없이 확인불가 표시
- NORMAL / UNVERIFIED / HIDE 등 ERROR 외 모든 상태는 기존 `soldOutYn` 기준 유지

## Edge Cases
- `status`가 undefined/null인 경우: 기존 soldOutYn 기준으로 폴백
- UNVERIFIED, HIDE 상태: 확인불가로 표시하지 않음 (ERROR만 해당)

## Acceptance Criteria Mapping
| AC | 구현 위치 |
|----|----------|
| status: ERROR → 확인불가 뱃지 | ProductTable.tsx 조건 분기 |
| 앰버 색상 #d97706 | ProductTable.module.scss .bErrS |
| status: NORMAL, soldOutYn: N → 판매중 | 기존 로직 유지 |
| status: NORMAL, soldOutYn: Y → 품절 | 기존 로직 유지 |
