# API Specification

## Auth APIs

### POST `/api/auth/email-verifications`

OTP verification code request endpoint.

Request

```json
{
  "email": "user@s-food.com"
}
```

Success Response

- `201 Created`

```json
{
  "verificationKey": "550e8400-e29b-41d4-a716-446655440000",
  "expiresInSeconds": 300,
  "message": "Verification code sent"
}
```

Error Responses

- `400 Bad Request`
- `429 Too Many Requests`

Notes

- only `@s-food.com` emails are accepted
- the OTP raw code is never stored in the database

### POST `/api/auth/email-verifications/confirm`

OTP verification confirmation endpoint.

Request

```json
{
  "verificationKey": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@s-food.com",
  "code": "123456"
}
```

Success Response

- `200 OK`

```json
{
  "authenticated": true,
  "email": "user@s-food.com"
}
```

Error Responses

- `400 Bad Request`
- `401 Unauthorized`
- `409 Conflict`
- `410 Gone`
- `429 Too Many Requests`

Notes

- successful confirmation issues an authenticated HTTP session
- successful confirmation creates or updates the authenticated user profile
- failed confirmation increments the attempt count

### GET `/api/auth/session`

Authenticated session status endpoint.

Success Response

- `200 OK`

```json
{
  "email": "user@s-food.com"
}
```

Error Responses

- `401 Unauthorized`

### POST `/api/auth/logout`

Authenticated logout endpoint.

Success Response

- `204 No Content`

Error Responses

- `401 Unauthorized`

Notes

- invalidates the current authenticated HTTP session
- the same session cannot access `/api/auth/session` or other protected APIs after logout

### GET `/api/auth/me`

Authenticated current-user profile endpoint.

Success Response

- `200 OK`

```json
{
  "email": "user@s-food.com",
  "emailNotificationYn": "N",
  "teamsNotificationYn": "N",
  "lastLoginAt": "2026-04-06T09:00:00Z",
  "createdAt": "2026-04-06T09:00:00Z",
  "updatedAt": "2026-04-06T09:00:00Z"
}
```

Error Responses

- `401 Unauthorized`
- `404 Not Found`

Notes

- returns the profile for the authenticated session email only

### PUT `/api/auth/me/notifications`

Authenticated current-user notification preference update endpoint.

Request

```json
{
  "emailNotificationYn": "Y",
  "teamsNotificationYn": "N"
}
```

Success Response

- `200 OK`

```json
{
  "email": "user@s-food.com",
  "emailNotificationYn": "Y",
  "teamsNotificationYn": "N",
  "lastLoginAt": "2026-04-06T09:00:00Z",
  "createdAt": "2026-04-06T09:00:00Z",
  "updatedAt": "2026-04-06T09:05:00Z"
}
```

Error Responses

- `400 Bad Request`
- `401 Unauthorized`
- `404 Not Found`

Notes

- both notification flags accept only `Y` or `N`
- updates only the authenticated user's row

## Batch Admin APIs

### GET `/api/admin/batch-schedules/{jobName}`

Batch schedule detail endpoint.

Success Response

- `200 OK`

```json
{
  "jobName": "product-sync",
  "enabled": true,
  "fixedDelaySeconds": 600,
  "running": false,
  "lastStartedAt": "2026-04-01T10:00:00Z",
  "lastEndedAt": "2026-04-01T10:00:45Z",
  "lastStatus": "SUCCESS",
  "lastMessage": "품절 3건, 정상 전환 1건"
}
```

Error Responses

- `401 Unauthorized`

### PUT `/api/admin/batch-schedules/{jobName}`

Batch schedule update endpoint.

Request

```json
{
  "enabled": true,
  "fixedDelaySeconds": 300,
  "modifiedBy": "user@s-food.com"
}
```

Success Response

- `200 OK`

Error Responses

- `400 Bad Request`
- `401 Unauthorized`

Notes

- `fixedDelaySeconds` must be at least `60`

### POST `/api/admin/batch-schedules/{jobName}/run`

Manual batch run trigger endpoint.

Success Response

- `200 OK`

```json
{
  "triggered": true,
  "status": "STARTED"
}
```

Alternative Response

```json
{
  "triggered": false,
  "status": "SKIPPED"
}
```

Error Responses

- `401 Unauthorized`

Notes

- returns `401` when the session is missing or expired

## Goods APIs

### POST `/api/goods`

Goods registration endpoint.

Request

```json
[
  {
    "godoGoodsCode": "10000001",
    "erpGoodsCode": "ERP-0001",
    "goodsName": "Sample Goods",
    "onlyMallYn": "N"
  }
]
```

Success Response

- `201 Created`

```json
{
  "items": [
    {
      "id": 1,
      "godoGoodsCode": "10000001",
      "erpGoodsCode": "ERP-0001",
      "status": "UNVERIFIED",
      "message": null
    }
  ]
}
```

Error Responses

- `400 Bad Request`
- `401 Unauthorized`
- `409 Conflict`

Notes

- send one or more goods objects in the request array
- the response contains one result item per saved goods entry

### PUT `/api/goods/{id}`

Goods update endpoint.

Request

```json
{
  "erpGoodsCode": "ERP-0001",
  "goodsName": "Updated Goods",
  "onlyMallYn": "Y",
  "availableQty": 7,
  "safetyRate": 40,
  "remainValidDays": 150
}
```

Success Response

- `200 OK`

```json
{
  "id": 1,
  "godoGoodsCode": "10000001",
  "erpGoodsCode": "ERP-0001",
  "goodsName": "Updated Goods",
  "onlyMallYn": "Y",
  "soldOutYn": "N",
  "availableQty": 7,
  "safetyRate": 40,
  "remainValidDays": 150,
  "godoSyncYn": "Y",
  "erpSyncYn": "Y",
  "godoSyncBlockedYn": "N",
  "message": null,
  "status": "UNVERIFIED"
}
```

Error Responses

- `400 Bad Request`
- `401 Unauthorized`
- `404 Not Found`
- `409 Conflict`

Notes

- `availableQty`, `safetyRate`, and `remainValidDays` are required and must be zero or greater

### PATCH `/api/goods/{id}/godo-sync-block`

Goods automatic Godo sync block update endpoint.

Request

```json
{
  "blockedYn": "Y"
}
```

Success Response

- `200 OK`

```json
{
  "id": 1,
  "godoGoodsCode": "10000001",
  "erpGoodsCode": "ERP-0001",
  "goodsName": "Updated Goods",
  "onlyMallYn": "Y",
  "soldOutYn": "N",
  "availableQty": 7,
  "safetyRate": 40,
  "remainValidDays": 150,
  "godoSyncYn": "Y",
  "erpSyncYn": "Y",
  "godoSyncBlockedYn": "Y",
  "message": null,
  "status": "NORMAL"
}
```

Error Responses

- `400 Bad Request`
- `401 Unauthorized`
- `404 Not Found`

Notes

- `blockedYn` accepts only `Y` or `N`
- enabling the block with `blockedYn=Y` is allowed only when the goods status is `NORMAL`
- disabling the block with `blockedYn=N` is allowed regardless of the current goods status

### POST `/api/goods/godo-status/force-sync`

Manual Godo sold-out status force-sync endpoint.

Request

```json
{
  "godoGoodsCodes": ["10000001", "10000002"],
  "targetSoldOutYn": "Y"
}
```

Success Response

- `200 OK`

```json
{
  "items": [
    {
      "godoGoodsCode": "10000001",
      "result": "SUCCESS",
      "message": null,
      "soldOutYn": "Y"
    },
    {
      "godoGoodsCode": "10000002",
      "result": "SKIPPED",
      "message": "Godo sync is blocked",
      "soldOutYn": "N"
    }
  ]
}
```

Error Responses

- `400 Bad Request`
- `401 Unauthorized`

Notes

- `godoGoodsCodes` must not be empty
- `targetSoldOutYn` accepts only `Y` or `N`
- manual force sync validates only goods eligibility: Godo sync `Y` and Godo sync block `N`
- this endpoint does not require `status == NORMAL` or `erpSyncYn == Y`
- this endpoint does not use WMS quantity, valid-percent, zone, or lot checks
- each selected goods code is processed independently and returned with item-level result
- item `result` values are `SUCCESS`, `SKIPPED`, `FAILED`, or `NOT_FOUND`

### DELETE `/api/goods/{id}`

Goods delete endpoint.

Success Response

- `204 No Content`

Error Responses

- `401 Unauthorized`
- `404 Not Found`

### GET `/api/goods`

Goods list endpoint.

Query Parameters

- `soldOutYn`: optional, `Y` or `N`
- `onlyMallYn`: optional, `Y` or `N`
- `status`: optional
- `keyword`: optional, matches goods name or godo goods code

Success Response

- `200 OK`

```json
{
  "items": [
    {
      "id": 1,
      "godoGoodsCode": "10000001",
      "erpGoodsCode": "ERP-0001",
      "goodsName": "Sample Goods",
      "onlyMallYn": "N",
      "soldOutYn": "N",
      "availableQty": 20,
      "safetyRate": 50,
      "remainValidDays": 150,
      "godoSyncYn": "Y",
      "erpSyncYn": "Y",
      "godoSyncBlockedYn": "N",
      "message": null,
      "status": "UNVERIFIED",
      "recentUpdatedAt": "2026-04-02T10:30:00Z",
      "recentUpdatedBy": "editor@s-food.com"
    }
  ]
}
```

Error Responses

- `401 Unauthorized`

Notes

- goods in `HIDE` status are excluded from the list by default
- `recentUpdatedAt` and `recentUpdatedBy` use `updated_at` and `updated_id` when present, otherwise they fall back to `created_at` and `created_id`

### GET `/api/goods/{id}`

Goods detail endpoint.

Success Response

- `200 OK`

```json
{
  "id": 1,
  "godoGoodsCode": "10000001",
  "erpGoodsCode": "ERP-0001",
  "goodsName": "Sample Goods",
  "onlyMallYn": "N",
  "soldOutYn": "N",
  "availableQty": 20,
  "safetyRate": 50,
  "remainValidDays": 150,
  "godoSyncYn": "Y",
  "erpSyncYn": "Y",
  "godoSyncBlockedYn": "N",
  "description": "상품 상세 메모",
  "message": null,
  "status": "UNVERIFIED",
  "createdId": "user@s-food.com",
  "createdAt": "2026-04-01T09:00:00Z",
  "updatedId": "editor@s-food.com",
  "updatedAt": "2026-04-02T10:30:00Z"
}
```

Error Responses

- `401 Unauthorized`
- `404 Not Found`

Notes

- `description`, `createdId`, `createdAt`, `updatedId`, and `updatedAt` are returned when present on the goods record
- `updatedId` and `updatedAt` remain `null` when the goods record has never been updated
- `godoSyncBlockedYn` indicates whether automatic Godo status sync is blocked for the goods

### GET `/api/goods/status-logs`

Goods sold-out status change log query endpoint.

Query Parameters

- `godoGoodsCode`: optional
- `changedFrom`: optional, ISO-8601 timestamp lower bound
- `changedTo`: optional, ISO-8601 timestamp upper bound

Success Response

- `200 OK`

```json
{
  "items": [
    {
      "id": 1,
      "godoGoodsCode": "10000001",
      "erpGoodsCode": "ERP-0001",
      "wmsAvailableQty": 15,
      "criteriaQty": 20,
      "safetyRate": 50,
      "validPercent": 75,
      "remainValidDays": null,
      "beforeStatus": "ON_SALE",
      "afterStatus": "OUT_OF_STOCK",
      "changedAt": "2026-03-31T12:00:00Z",
      "createdAt": "2026-03-31T12:00:00Z"
    }
  ]
}
```

Error Responses

- `401 Unauthorized`

## Common Status Config APIs

### GET `/api/config/status`

Common status config query endpoint.

Success Response

- `200 OK`

```json
[
  {
    "scope": "GENERAL",
    "outOfStockQty": 20,
    "onSaleQty": 21,
    "safetyRate": 50,
    "remainValidDays": 150
  },
  {
    "scope": "EXCLUSIVE",
    "outOfStockQty": 1,
    "onSaleQty": 2,
    "safetyRate": 50,
    "remainValidDays": 150
  }
]
```

Error Responses

- `401 Unauthorized`

### PUT `/api/config/status/{scope}`

Common status config update endpoint.

Request

```json
{
  "outOfStockQty": 10,
  "onSaleQty": 11,
  "safetyRate": 40,
  "remainValidDays": 150
}
```

Success Response

- `200 OK`

```json
{
  "scope": "GENERAL",
  "outOfStockQty": 10,
  "onSaleQty": 11,
  "safetyRate": 40,
  "remainValidDays": 150
}
```

Error Responses

- `400 Bad Request`
- `401 Unauthorized`
- `404 Not Found`

Notes

- `scope` is provided through the URL path and is not editable in the request body
- `remainValidDays` stores the freeze-goods remaining-valid-days threshold applied to goods in the selected scope

---

## Common Code APIs

> 업태(SERVICE)·업종(ITEM) 등 공통 코드를 관리하는 범용 그룹코드/코드 CRUD API.
> 회원 업로드·사업자 정보 저장 시 업태·업종 텍스트를 코드로 자동 매핑하는 데 사용된다 (`backend/docs/product/api-spec-member.md` 참조).

### GET `/api/common-codes`

공통코드 목록 조회 endpoint.

Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `groupCode` | string | — | 그룹코드로 필터 (예: `SERVICE`, `ITEM`) |
| `useYn` | string | — | `Y` \| `N` 사용 여부로 필터 |

Success Response

- `200 OK`

```json
{
  "codes": [
    {
      "id": 1,
      "groupCode": "SERVICE",
      "groupName": "업태",
      "code": "01",
      "codeName": "도매",
      "note": null,
      "useYn": "Y",
      "createdAt": "2026-06-25 10:00:00",
      "updatedAt": "2026-06-25 10:00:00"
    }
  ]
}
```

Error Responses

- `401 Unauthorized`

Notes

- `groupCode`와 `useYn`을 둘 다 생략하면 전체 코드를 `groupCode`, `code` 오름차순으로 반환
- `groupCode`만 전달하면 사용 여부와 무관하게 해당 그룹 전체 반환

### POST `/api/common-codes`

공통코드 등록 endpoint.

Request

```json
{
  "groupCode": "SERVICE",
  "groupName": "업태",
  "code": "80",
  "codeName": "신규업태",
  "note": null,
  "useYn": "Y"
}
```

Success Response

- `201 Created` (본문 없음)

Error Responses

- `400 Bad Request` — 동일 `groupCode`+`code` 조합이 이미 존재
- `401 Unauthorized`

Notes

- `useYn` 생략 시 `"Y"`로 저장

### PUT `/api/common-codes/{id}`

공통코드 수정 endpoint.

Request

```json
{
  "groupCode": "SERVICE",
  "groupName": "업태",
  "code": "80",
  "codeName": "신규업태(수정)",
  "note": "비고",
  "useYn": "N"
}
```

Success Response

- `200 OK` (본문 없음)

Error Responses

- `400 Bad Request` — 대상 코드가 존재하지 않거나, 변경하려는 `groupCode`+`code` 조합이 자신을 제외한 다른 레코드에 이미 존재
- `401 Unauthorized`

Notes

- `useYn`을 생략하면 기존 값 유지

---

## Member APIs

> 전체 상세 명세: `backend/docs/product/api-spec-member.md`
> 인증: 모든 엔드포인트 세션 쿠키 필수
> 개인정보: 목록 마스킹 / 상세 복호화 평문 / 모든 암호화는 저장 시점(at rest)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/members/uploads` | 회원 일괄 업로드 (단일 원자적 API) |
| GET | `/api/members` | 목록 (서버 페이징·검색·필터·정렬) |
| GET | `/api/members/summary` | 요약 카드 4종 카운트 |
| GET | `/api/members/{memNo}` | 상세 (복호화 평문) |
| PUT | `/api/members/{memNo}/partner` | 파트너 코드·거래처 정보 저장 (insert/update, 저장 전 이력 기록) |
| PUT | `/api/members/{memNo}/business` | 사업자 정보 저장 (insert/update, 저장 전 이력 기록) |
| GET | `/api/members/{memNo}/business/logs` | 사업자 정보 변경 이력 조회 |
| GET | `/api/members/{memNo}/partner/logs` | 거래처 정보 변경 이력 조회 |
| DELETE | `/api/members/{memNo}` | 회원 단건 삭제 (soft/hard delete 분기) |

### 구현 위치

| 클래스 | 경로 |
|--------|------|
| `MemberUploadController` | `member/presentation/` |
| `MemberQueryController` | `member/presentation/` |
| `MemberManageController` | `member/presentation/` |
| `CommonCodeController` | `common_code/presentation/` |
| `UploadMembersUseCase` | `member/application/usecase/` |
| `GetMemberListUseCase` | `member/application/usecase/` |
| `GetMemberSummaryUseCase` | `member/application/usecase/` |
| `GetMemberDetailUseCase` | `member/application/usecase/` |
| `UpdateMemberPartnerUseCase` | `member/application/usecase/` |
| `UpdateMemberBusinessUseCase` | `member/application/usecase/` |
| `GetMemberBusinessLogsUseCase` | `member/application/usecase/` |
| `GetMemberPartnerLogsUseCase` | `member/application/usecase/` |
| `DeleteMemberUseCase` | `member/application/usecase/` |
| `CommonCodeUseCase` | `common_code/application/usecase/` |
| `CommonCodeMappingService` | `common_code/application/usecase/` (업태/업종 텍스트→코드 인메모리 캐시) |

### 검색 타입별 DB 전략

| searchType | 쿼리 | 허용 여부 |
|-----------|------|----------|
| MEM_ID | `mem_id LIKE '%keyword%'` | 부분 검색 |
| NAME | `mem_name_hash = HashUtil.hash(keyword)` | 정확일치만 |
| EMAIL | `email_hash = HashUtil.hash(keyword)` | 정확일치만 |
| CELL_PHONE | `cell_phone_hash = HashUtil.hash(keyword.replaceAll("[^0-9]", ""))` | 정확일치만 |

### 소프트 삭제 가드

`applyDeletion = true`이더라도 삭제 예정 건수가 기존 회원의 **10% 초과** 시 적용 보류.
응답: `deletionSkipped: true`, `deletionSkippedReason` 포함, HTTP `200` (경고 상태)
