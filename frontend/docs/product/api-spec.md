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
- manual force sync validates only goods eligibility: `NORMAL`, ERP sync `Y`, Godo sync `Y`, and Godo sync block `N`
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
> 회원 업로드·사업자 정보 저장 시 업태·업종 텍스트를 코드로 자동 매핑하는 데 사용된다.

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /api/common-codes?groupCode&useYn` | 공통코드 목록 조회. 둘 다 생략 시 전체를 `groupCode, code` 오름차순 반환 |
| `POST /api/common-codes` | 공통코드 등록 — `{ groupCode, groupName, code, codeName, note, useYn }` → `201`. 동일 `groupCode`+`code` 존재 시 `400` |
| `PUT /api/common-codes/{id}` | 공통코드 수정 — 동일 요청 바디 → `200`. 대상 없음/코드 중복 시 `400` |

Error Responses

- `401 Unauthorized`

```typescript
interface CommonCode {
  id: number;
  groupCode: string;
  groupName: string;
  code: string;
  codeName: string;
  note: string | null;
  useYn: 'Y' | 'N';
  createdAt: string;
  updatedAt: string;
}

interface CommonCodeListResponse {
  codes: CommonCode[];
}

interface CommonCodeRequest {
  groupCode: string;
  groupName: string;
  code: string;
  codeName: string;
  note: string | null;
  useYn?: 'Y' | 'N';
}
```

---

## Member APIs

> 참조: `docs/product/api-spec-member.md` (상세 명세)
> 인증: 모든 엔드포인트 세션 쿠키 필수 — 미인증 시 `401`
> 개인정보: 목록 응답은 마스킹, 상세(`GET /api/members/{memNo}`) 응답은 복호화 평문
> 거래처 상태(`memberStatus`) 허용값: `UNREGISTERED`(미등록) / `NORMAL`(정상) / `INACTIVE`(불용) — 그 외 값 전달 시 `400`

| 엔드포인트 | 설명 |
|-----------|------|
| `POST /api/members/uploads` | 회원 일괄 업로드 — `{ type, fileName, applyDeletion, rows: any[][] }` → `{ totalInserted, totalUpdated, totalDeleted, totalFailed, failedRows, deletionSkipped, deletionSkippedReason, syncedAt }`. `type`: `"LIST"` (고도몰 43컬럼) 또는 `"TEMPLATE"` (관리 엑셀 51컬럼). 신규 회원은 두 타입 모두 `member_partner`가 자동 생성됨(`TEMPLATE`은 엑셀 값으로, `LIST`는 `memberStatus='UNREGISTERED'` 기본값으로 채번). **초기 전체 업로드는 TEMPLATE 사용** — 거래처 코드 등 관리 정보 포함. |
| `GET /api/members` | 회원 목록 (서버 페이징·검색·필터) — 마스킹 응답 |
| `GET /api/members/summary` | 요약 카드 카운트 — `{ totalCount, businessCount, b2bCount, pendingApprovalCount, lastSyncedAt }`. **구현 상태(2026-07-10): 백엔드 API 구현 완료, 프론트엔드 미연동.** 현재 요약 카드 화면 수요가 없어 의도적으로 보류 중 — 버그 아님. |
| `GET /api/members/{memNo}` | 회원 상세 — 복호화 평문 + `business` + `partner` 섹션 |
| `PUT /api/members/{memNo}/partner` | 파트너 정보 저장 (INSERT/UPDATE, 저장 전 이력 기록) — `{ partnerCode, partnerName, taxInvoiceFl, memberStatus, bizLicenseFile }` → `{ partnerCode, partnerName, taxInvoiceFl, memberStatus, bizLicenseFile, updatedAt }`. `partnerCode`가 다른 활성 회원과 중복되면 `409`, `memberStatus`가 허용값 외이면 `400`. |
| `PUT /api/members/{memNo}/business` | 사업자 정보 저장 (INSERT/UPDATE, 저장 전 이력 기록) — `{ company, businessNo, ceoName, serviceName, serviceCode, itemName, itemCode, comAddress, bizLicenseFile }` → 저장된 값 + `updatedAt` 반환. |
| `GET /api/members/{memNo}/business/logs` | 사업자 정보 변경 이력 조회 — `{ logs: [...] }`, `loggedAt` 최신순 |
| `GET /api/members/{memNo}/partner/logs` | 거래처 정보 변경 이력 조회 — `{ logs: [...] }`, `loggedAt` 최신순 |
| `DELETE /api/members/{memNo}` | 회원 단건 삭제 — 204 No Content. `member_status`가 `NORMAL`/`INACTIVE`(등록 이력 있음)이면 soft delete(`deleted_fl='y'`), 그 외(`UNREGISTERED` 또는 파트너 없음) hard delete(물리 삭제). 존재하지 않으면 `404`. |

### 회원 엑셀 다운로드 (그룹웨어 양식, 클라이언트 생성)

목록 화면의 "엑셀 다운로드" 버튼(`src/utils/exportMembersExcel.ts`)은 백엔드 API 없이, 선택한 회원의 `GET /api/members/{memNo}` 상세 응답을 병렬 조회해 프론트엔드에서 엑셀 파일을 직접 생성한다.

- **용도**: 그룹웨어(별도 서비스)에 거래처를 업로드하기 위한 양식. 본 시스템(에쓰푸드몰)의 `POST /api/members/uploads` TEMPLATE(51컬럼) 업로드와는 **다른 파일**이며 재업로드 대상이 아니다.
- **컬럼 수**: 53컬럼. 업태·업종을 명/코드 4컬럼(업태명·업태코드·업종명·업종코드)으로 분리해서 내려주기로 그룹웨어 측과 협의됨(2026-07). `api-spec-member.md`의 TEMPLATE(51컬럼)은 업태명·업종명 텍스트 2컬럼만 사용하므로 컬럼 수가 다르다 — 혼동 주의.
- 헤더·컬럼 순서는 `godomall-member-analysys-template.xlsx`를 기반으로 하되 업태/업종 4컬럼 분리분이 추가된 형태다.

### GET `/api/members` — Query Parameters

| 파라미터 | 기본값 | 허용값 |
|---------|--------|--------|
| `page` | 0 | 0-base |
| `size` | 20 | 10, 20, 30, 50 |
| `searchType` | MEM_ID | `MEM_ID`·`PARTNER_CODE`·`PARTNER_NAME`(부분검색 LIKE) \| `NAME` \| `EMAIL` \| `CELL_PHONE` (해시 정확일치). 파트너 검색은 `member_partner` 미등록 회원 제외 |
| `keyword` | — | 검색어 |
| `group` | ALL | `ALL` \| `GENERAL` \| `BUSINESS` \| `SKILLED` \| `B2B` |
| `appFl` | all | `all` \| `y` \| `n` |
| `memberStatus` | — | `UNREGISTERED`(미등록, `member_partner` 레코드 없음 포함) \| `NORMAL`(정상) \| `INACTIVE`(불용) — 생략·허용값 외 값이면 전체 조회 |
| `entryDtFrom` / `entryDtTo` | — | YYYY-MM-DD |
| `sortKey` | mem_no | `mem_no, mem_id, group_sno, app_fl, entry_dt, last_login_dt, sale_amount` |
| `sortDir` | desc | `asc` \| `desc` |

**정렬 주의:** 암호화 컬럼(이름·이메일·휴대폰)은 정렬 불가 — 프론트엔드에서 해당 컬럼의 정렬 UI 미노출.

### Member TypeScript Types

> 아래는 백엔드 응답 계약을 기준으로 한 참조 타입이다(실제 프론트 타입은 `src/types/member.ts` 참조).
> **갱신(2026-07-10)**: `memberStatus` 3단계 통합, 사업자 수정·이력 조회, 공통코드 연동은 이미 구현되어 있다.
> 아직 미구현인 것은 `GET /api/members/summary`(요약 카드) 연동뿐이며, 이는 현재 화면 수요가 없어 의도적으로 보류된 것이다(버그 아님).

```typescript
interface MemberListItem {
  memNo: number; memId: string;
  memName: string | null;      // 마스킹: "홍*동"
  groupSno: number; groupName: string;
  email: string | null;        // 마스킹: "ho***@example.com"
  company: string | null;
  businessNo: string | null;   // 사업자 등급 외 null
  ceoName: string | null;      // 복호화 평문, 사업자 등급 외 null
  serviceName: string | null;  // 업태명, 사업자 등급 외 null
  itemName: string | null;     // 업종명, 사업자 등급 외 null
  comAddress: string | null;   // 사업자 주소 복호화 평문, 사업자 등급 외 null
  partnerCode: string | null;
  partnerName: string | null;  // member_partner 없으면 null
  memberStatus: 'UNREGISTERED' | 'NORMAL' | 'INACTIVE' | null;  // member_partner 없으면 null
  entryDt: string; appFl: 'y' | 'n';
  duplicateMemNo: string | null;  // 동일 businessNo를 가진 다른 활성 회원의 memNo 목록(`;` 구분)
}

interface MemberPage {
  content: MemberListItem[]; page: number; size: number;
  totalElements: number; totalPages: number;
}

interface MemberSummary {
  totalCount: number; businessCount: number; b2bCount: number;
  pendingApprovalCount: number; lastSyncedAt: string | null;
}

interface MemberDetail {
  memNo: number; memId: string; memName: string | null;   // 복호화 평문
  groupSno: number; groupName: string; appFl: 'y' | 'n'; appDt: string | null;
  email: string | null; cellPhone: string | null; phone: string | null;
  address: string | null; entryDt: string; lastLoginDt: string | null;
  entryPath: string | null; maillingFl: 'y' | 'n'; smsFl: 'y' | 'n';
  nickName: string | null; sexFl: 'm' | 'w' | null; birthDt: string | null;
  marriFl: 'y' | 'n' | null; marriDate: string | null;
  job: string | null; interest: string | null;
  loginCount: number; saleAmount: number; mileage: number; deposit: number;
  recommId: string | null; ex1: string | null; ex2: string | null; ex3: string | null;
  ex4: string | null; ex5: string | null; ex6: string | null; memo: string | null;
  business: MemberBusinessDetail | null;
  partner: MemberPartnerDetail | null;
}

interface MemberBusinessDetail {
  company: string | null; businessNo: string | null; ceoName: string | null;
  serviceName: string | null; serviceCode: string | null;
  itemName: string | null; itemCode: string | null;
  comAddress: string | null; bizLicenseFile: string | null;
  duplicateMemNo: string | null;  // 동일 businessNo를 가진 다른 활성 회원의 memNo 목록(`;` 구분)
}

interface MemberPartnerDetail {
  partnerCode: string | null;
  partnerName: string | null; taxInvoiceFl: 'y' | 'n';
  memberStatus: 'UNREGISTERED' | 'NORMAL' | 'INACTIVE'; updatedAt: string;
}

// GET /api/members query params
interface MemberListQuery {
  page?: number;
  size?: number;
  searchType?: 'MEM_ID' | 'NAME' | 'EMAIL' | 'CELL_PHONE' | 'PARTNER_CODE' | 'PARTNER_NAME';
  keyword?: string;
  group?: 'ALL' | 'GENERAL' | 'BUSINESS' | 'SKILLED' | 'B2B';
  appFl?: 'y' | 'n';
  memberStatus?: 'UNREGISTERED' | 'NORMAL' | 'INACTIVE';  // UNREGISTERED는 member_partner 없음(IS NULL) 포함. 생략 시 전체
  entryDtFrom?: string;
  entryDtTo?: string;
  sortKey?: string;
  sortDir?: 'asc' | 'desc';
}

// PUT /api/members/{memNo}/partner 요청 바디
interface MemberPartnerUpdateRequest {
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: 'y' | 'n' | null;
  memberStatus: 'UNREGISTERED' | 'NORMAL' | 'INACTIVE' | null;  // null 전달 시 NORMAL로 처리, 허용값 외 값은 400
  bizLicenseFile: string | null;
}

// PUT /api/members/{memNo}/partner 응답
interface MemberPartnerUpdateResponse {
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: 'y' | 'n';
  memberStatus: 'UNREGISTERED' | 'NORMAL' | 'INACTIVE';
  bizLicenseFile: string | null;
  updatedAt: string;
}

// PUT /api/members/{memNo}/business 요청 바디
interface MemberBusinessUpdateRequest {
  company: string | null;
  businessNo: string | null;
  ceoName: string | null;
  serviceName: string | null;
  serviceCode: string | null;
  itemName: string | null;
  itemCode: string | null;
  comAddress: string | null;
  bizLicenseFile: string | null;
}

// PUT /api/members/{memNo}/business 응답
interface MemberBusinessUpdateResponse {
  company: string | null;
  businessNo: string | null;
  ceoName: string | null;
  serviceName: string | null;
  serviceCode: string | null;
  itemName: string | null;
  itemCode: string | null;
  comAddress: string | null;
  bizLicenseFile: string | null;
  updatedAt: string;
}

// GET /api/members/{memNo}/business/logs, GET /api/members/{memNo}/partner/logs 공통 형태
interface MemberBusinessLogItem {
  id: number; memNo: number;
  company: string | null; businessNo: string | null; ceoName: string | null;
  serviceName: string | null; serviceCode: string | null;
  itemName: string | null; itemCode: string | null;
  comAddress: string | null; bizLicenseFile: string | null;
  loggedAt: string; loggedBy: string | null;
}

interface MemberPartnerLogItem {
  id: number; memNo: number;
  partnerCode: string | null; partnerName: string | null;
  taxInvoiceFl: string | null; memberStatus: string | null;
  loggedAt: string; loggedBy: string | null;
}

// 수정 가능/불가 컬럼 정책
// - PUT .../partner 수정 가능: member_partner 4개 컬럼(partnerCode/partnerName/taxInvoiceFl/memberStatus) + godo_member_business.bizLicenseFile
// - PUT .../business 수정 가능: godo_member_business 전체 컬럼(사업자등록증 파일명 포함)
// - 수정 불가(읽기 전용): godo_member 전체, godo_member_business의 company/businessNo/ceoName/service*/item*/comAddress는
//   위 PUT .../business로만 수정 — 엑셀 재업로드로도 갱신됨
// - member_partner 레코드는 TEMPLATE/LIST 업로드(신규 회원 자동 생성), 배치 자동 수집(신규 회원), 화면 저장 API에서 생성됨
```
