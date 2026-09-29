# Member API Specification

> 작성일: 2026-06-11 / 최종 수정: 2026-07-10 (업태/업종 필드명 `service`/`item` → `serviceName`/`itemName` 표기 정정, 그룹웨어 다운로드 양식 혼동 방지 안내 추가)
> 대상 서비스: SFOOD-MALL-AUTOMATION Backend
> 인증: 모든 엔드포인트는 유효한 세션 쿠키(`withCredentials: true`) 필요 — 미인증 시 `401`
> 참조: `backend/docs/plan/member-management-ui-plan.md`, `backend/docs/analysis/godomall-member-table.md`
> 공통코드(업태/업종) API: `backend/docs/product/api-spec.md` "Common Code APIs" 참조
> 거래처 상태(`memberStatus`) 허용값: `UNREGISTERED`(미등록) / `NORMAL`(정상) / `INACTIVE`(불용) — 그 외 값 전달 시 `400`

---

## 개요

| 엔드포인트 | 설명 |
|-----------|------|
| `POST /api/members/uploads` | 회원 일괄 업로드 (단일 원자적 API) |
| `GET /api/members` | 회원 목록 (서버 페이징·검색·필터) |
| `GET /api/members/summary` | 요약 카드 카운트 |
| `GET /api/members/{memNo}` | 회원 상세 (복호화 평문) |
| `PUT /api/members/{memNo}/partner` | 파트너 코드·거래처 정보 저장 (insert/update, 저장 전 이력 기록) |
| `PUT /api/members/{memNo}/business` | 사업자 정보 저장 (insert/update, 저장 전 이력 기록) |
| `GET /api/members/{memNo}/business/logs` | 사업자 정보 변경 이력 조회 |
| `GET /api/members/{memNo}/partner/logs` | 거래처 정보 변경 이력 조회 |
| `DELETE /api/members/{memNo}` | 회원 단건 삭제 (soft/hard delete 분기) |

---

## 업로드 APIs

### POST `/api/members/uploads`

엑셀에서 파싱한 전체 행을 한 번의 요청으로 업로드한다. 정규화·암호화·upsert·소프트 삭제가 단일 트랜잭션으로 처리되며, 실패 시 전체 롤백된다.

#### LIST 타입 요청 예시

`type: LIST` — 고도몰에서 내려받은 회원 목록 파일. 정기 동기화(10~20건 소량) 시 사용.

```json
{
  "type": "LIST",
  "fileName": "godo_member_20260611.xls",
  "applyDeletion": false,
  "rows": [
    [12345, "2025-01-15 00:00:00", null, "y", "pc", 46169.42, "business", 9, "hong123", "홍씨", "홍길동", "hong@example.com", "010-1234-5678", null, "06000 서울시 강남구 테헤란로 123", "y", "y", 125000, "1500점", "0원", 3, "(주)테스트", "123-45-67890", "홍대표", "도매", "식품", "06000 서울시 강남구 테헤란로 123", null, null, "m", 45748.0, null, null, null, null, 1, null, "식품제조", null, "홍담당", null, null, null]
  ]
}
```

#### TEMPLATE 타입 요청 예시

`type: TEMPLATE` — 에쓰푸드몰 관리 엑셀(51컬럼). 엑셀의 거래처 관련 컬럼(거래처등록·거래처코드·거래처명·세금계산서여부) 값으로 `member_partner`를 직접 생성·갱신한다.

```json
{
  "type": "TEMPLATE",
  "fileName": "sfood_member_template_20260611.xlsx",
  "applyDeletion": true,
  "rows": [
    ["hong123", "O", 990954, "(에쓰푸드몰)테스트거래처", "(에쓰푸드몰)테스트거래처", "O", "확인완료", "license.jpg", 12345, "2025-01-15 00:00:00", null, "y", "pc", 46169.42, "business", 9, "hong123", "홍씨", "홍길동", "hong@example.com", "010-1234-5678", null, "06000 서울시 강남구 테헤란로 123", "y", "y", 125000, "1500점", "0원", 3, "(주)테스트", "123-45-67890", "홍대표", "도매", "식품", "06000 서울시 강남구 테헤란로 123", null, null, "m", 45748.0, null, null, null, null, 1, null, "식품제조", null, "홍담당", null, null, null]
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | string | ✅ | `LIST` (고도몰 list 파일, 43컬럼) 또는 `TEMPLATE` (관리 엑셀, 51컬럼) |
| `fileName` | string | ✅ | 원본 파일명 (로그·이력용) |
| `applyDeletion` | boolean | ✅ | `true`: 미수신 회원 소프트 삭제 적용. 전체 회원 파일일 때만 `true`로 전송 |
| `rows` | array of array | ✅ | 행 배열. 각 행은 엑셀 원본 셀 값 배열 (String / Double / null). 정규화는 서버에서 수행 |

---

**LIST 파일 (43컬럼) 인덱스 순서:**

| 인덱스 | 컬럼명 | 타입 | 정규화 |
|--------|--------|------|--------|
| 0 | 회원번호 | Double/String | Long 변환 — upsert 키 |
| 1 | 등록일 | Double/String | 날짜 변환 (시리얼·문자열 혼재) |
| 2 | 가입승인일 | Double/String | 날짜 변환 |
| 3 | 가입승인여부 | String | "y"/"n" |
| 4 | 가입경로 | String | "pc"/"mobile" |
| 5 | 최종로그인일 | Double/String | 날짜 변환 |
| 6 | 회원구분 | String | "personal"/"business" |
| 7 | 등급 | Double | Long 변환 (1~9) |
| 8 | 아이디 | String | — |
| 9 | 닉네임 | String | — |
| 10 | 이름 | String | 암호화 + 해시 생성 |
| 11 | 이메일 | String | 암호화 + 해시 생성 |
| 12 | 휴대폰번호 | String | 암호화 + 해시 생성 (하이픈 제거 후 해시) |
| 13 | 전화번호 | String | 암호화 |
| 14 | 주소 | String | 암호화 (우편번호 포함 단일 문자열) |
| 15 | 메일수신여부 | String | "y"/"n" |
| 16 | SMS수신여부 | String | "y"/"n" |
| 17 | 주문금액 | Double/String | 숫자 변환 |
| 18 | 마일리지 | String | "1500점" → 1500 |
| 19 | 예치금 | String | "0원" → 0 |
| 20 | 방문횟수 | Double | Long 변환 |
| 21 | 상호 | String | — |
| 22 | 사업자번호 | String | 평문 저장 |
| 23 | 대표자명 | String | 암호화 |
| 24 | 업태 | String | — |
| 25 | 종목 | String | — |
| 26 | 사업자 주소 | String | 암호화 (우편번호 포함 단일 문자열) |
| 27 | 팩스 번호 | String | — |
| 28 | 추천인아이디 | String | — |
| 29 | 성별 | String | "m"/"w" |
| 30 | 생일 | Double/String | 날짜 변환 + 암호화 |
| 31 | 결혼여부 | String | "y"/"n"/null |
| 32 | 결혼기념일 | String | — |
| 33 | 직업 | String | — |
| 34 | 관심분야 | String | — |
| 35 | 개인정보유효기간 | Double | Long 변환 (1/3/5/999) |
| 36 | 남기는말씀 | String | — |
| 37~42 | 추가정보1~6 | String | — |

---

**TEMPLATE 파일 (51컬럼) 인덱스 순서:**

인덱스 0~7은 에쓰푸드몰 관리 전용 컬럼, 인덱스 8~50은 LIST 0~42와 동일하다.

| 인덱스 | 컬럼명 | 타입 | 저장 대상 | 비고 |
|--------|--------|------|-----------|------|
| 0 | 아이디(중복) | String | — | 참조용, 저장 안 함 |
| 1 | 거래처등록 | String | `member_partner.member_status` | TEMPLATE 업로드 예외 저장, "O" → `NORMAL`(정상), 그 외 → `UNREGISTERED`(미등록) |
| 2 | 거래처코드 | Double | `member_partner.partner_code` | TEMPLATE 업로드 예외 저장, 숫자 → 문자열 변환 |
| 3 | 거래처명 | String | `member_partner.partner_name` | TEMPLATE 업로드 예외 저장 |
| 4 | 거래처명(중복) | String | — | 참조용, 저장 안 함 |
| 5 | 세금계산서여부 | String | `member_partner.tax_invoice_fl` | TEMPLATE 업로드 예외 저장, "O" → "y", 그 외 → "n" |
| 6 | 중복여부 확인 | String | — | 참조용, 저장 안 함 (`member_partner.duplicate_check` 컬럼 폐지됨) |
| 7 | 파일명(사업자등록증) | String | `godo_member_business.biz_license_file` | — |
| 8 | 회원번호 | Double/String | `godo_member.mem_no` | upsert 키 |
| 9~50 | (LIST 1~42와 동일) | — | — | LIST 컬럼 인덱스 +8 |

> ⚠️ **혼동 주의 — "그룹웨어 업로드 양식"과는 다른 파일이다.** 프론트엔드 회원 목록 화면의 "엑셀 다운로드" 기능(`frontend/src/utils/exportMembersExcel.ts`)은 이 TEMPLATE(51컬럼)과 별개로, 업태·업종을 명/코드 4컬럼(업태명·업태코드·업종명·업종코드)으로 분리한 **53컬럼** 파일을 생성한다. 이는 그룹웨어(별도 서비스)에 거래처를 업로드하기 위한 양식으로, 그룹웨어 쪽에서 업태/업종 코드를 함께 요구하기로 협의되어 있다(2026-07 협의). 이 53컬럼 파일은 백엔드 API 없이 프론트엔드가 상세 조회 응답을 조합해 클라이언트에서 생성하며, **본 시스템의 TEMPLATE 업로드(51컬럼)로 재업로드할 목적이 아니다**. 상세는 `frontend/docs/product/api-spec.md` "회원 엑셀 다운로드(그룹웨어 양식)" 참조.

Success Response

- `200 OK`

```json
{
  "totalInserted": 150,
  "totalUpdated": 22970,
  "totalDeleted": 36,
  "totalFailed": 2,
  "failedRows": [
    { "rowIndex": 42, "reason": "회원번호(mem_no)가 없습니다" },
    { "rowIndex": 1201, "reason": "아이디(mem_id)가 없습니다" }
  ],
  "deletionSkipped": false,
  "deletionSkippedReason": null,
  "syncedAt": "2026-06-11T14:30:00Z"
}
```

| 필드 | 설명 |
|------|------|
| `totalInserted` | 신규 삽입 총계 |
| `totalUpdated` | 갱신 총계 |
| `totalDeleted` | 소프트 삭제된 회원 수 (`applyDeletion=false`이면 0) |
| `totalFailed` | 실패 행 총계 |
| `failedRows` | 실패 행 목록 (rowIndex·reason). 실패 행은 건너뛰고 나머지는 커밋됨 |
| `deletionSkipped` | 삭제 대상이 10% 초과로 보류되었으면 `true` |
| `deletionSkippedReason` | 보류 사유 메시지 |
| `syncedAt` | 업로드 완료 시각 (ISO-8601 UTC) |

Error Responses

- `401 Unauthorized`

Notes

- 전체 요청이 단일 트랜잭션으로 처리된다. 서버 오류 시 전체 롤백.
- `mem_no`가 없거나 파싱 불가인 행은 `failedRows`에 기록되고 나머지 행은 계속 처리됨
- upsert 기반이므로 동일 파일 재전송 시 중복 오류 없음
- 삭제 보류(`deletionSkipped=true`) 시 삭제는 적용되지 않으나 응답은 `200`으로 반환 (경고 상태)
- TEMPLATE 업로드는 거래처 관련 컬럼 값으로 `member_partner`를 직접 생성·갱신함
- LIST 업로드는 **신규 회원**에 한해 `member_partner`를 자동 생성함 — `memberStatus='UNREGISTERED'`, `partnerCode`는 기존 최대 코드 값 + 1부터 자동 채번, `partnerName`은 `"(에쓰푸드몰)" + 상호`. 기존 회원의 `member_partner`는 변경하지 않음
- 사업자 정보(`company` 또는 `businessNo`가 있는 행)의 업태명·업종명 텍스트(`godo_member_business.service_name`/`item_name`)는 `common_code`(그룹코드 `SERVICE`/`ITEM`) 매칭을 통해 `serviceCode`/`itemCode`로 자동 변환되어 함께 저장됨 — 매칭 실패 시 코드는 `null`. 엑셀에는 업태명·업종명 텍스트 2개 컬럼만 존재하며, 코드 2개(`serviceCode`/`itemCode`)는 백엔드가 매칭해서 채우는 파생 컬럼이다
- 파트너 코드·거래처 정보를 화면에서 직접 저장·수정하려면 `PUT /api/members/{memNo}/partner`를 사용함

---

## 조회 APIs

### GET `/api/members`

회원 목록을 서버 사이드 페이징으로 조회한다. 응답의 개인정보 필드는 **마스킹 처리**되어 반환된다.

Query Parameters

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | number | `0` | 0-base 페이지 번호 |
| `size` | number | `20` | 페이지 크기 (허용값: 10, 20, 30, 50) |
| `searchType` | string | `MEM_ID` | `MEM_ID` \| `NAME` \| `EMAIL` \| `CELL_PHONE` \| `PARTNER_CODE` \| `PARTNER_NAME` |
| `keyword` | string | — | 검색어. `MEM_ID`·`PARTNER_CODE`·`PARTNER_NAME`은 부분검색(LIKE), 이름·이메일·휴대폰은 **해시 정확일치** |
| `group` | string | `ALL` | `ALL` \| `GENERAL` \| `BUSINESS` \| `SKILLED` \| `B2B` |
| `appFl` | string | `all` | `all` \| `y` (승인) \| `n` (미승인) |
| `memberStatus` | string | — | `UNREGISTERED`(미등록, `member_partner` 레코드 없음 포함) \| `NORMAL`(정상) \| `INACTIVE`(불용) — 생략·허용값 외 값 전달 시 전체 조회 |
| `entryDtFrom` | string | — | 가입일 시작 (YYYY-MM-DD) |
| `entryDtTo` | string | — | 가입일 종료 (YYYY-MM-DD) |
| `sortKey` | string | `mem_no` | 정렬 컬럼 (아래 허용 목록 참조) |
| `sortDir` | string | `desc` | `asc` \| `desc` |

**정렬 허용 컬럼:**
`mem_no`, `mem_id`, `group_sno`, `app_fl`, `entry_dt`, `last_login_dt`, `sale_amount`

> 암호화 컬럼(이름·이메일·휴대폰)은 DB 정렬 불가 — 허용 목록 외 `sortKey` 전달 시 `mem_no DESC`로 대체

**group 필터 매핑:**

| group 값 | DB 조건 |
|---------|--------|
| `ALL` | 제한 없음 |
| `GENERAL` | `group_sno = 1` |
| `BUSINESS` | `group_sno IN (2, 3, 5, 6)` — B2B 제외 |
| `SKILLED` | `group_sno = 8` — 기능장회원 |
| `B2B` | `group_sno = 9` |

**searchType별 검색 방식:**

| searchType | DB 조건 |
|-----------|--------|
| `MEM_ID` | `mem_id LIKE '%{keyword}%'` |
| `NAME` | `mem_name_hash = SHA256_HASH(keyword)` |
| `EMAIL` | `email_hash = SHA256_HASH(keyword)` |
| `CELL_PHONE` | `cell_phone_hash = SHA256_HASH(keyword.replaceAll("[^0-9]", ""))` |
| `PARTNER_CODE` | `member_partner.partner_code LIKE '%{keyword}%'` (파트너 미등록 회원은 제외) |
| `PARTNER_NAME` | `member_partner.partner_name LIKE '%{keyword}%'` (파트너 미등록 회원은 제외) |

Success Response

- `200 OK`

```json
{
  "content": [
    {
      "memNo": 12345,
      "memId": "hong123",
      "memName": "홍*동",
      "groupSno": 9,
      "groupName": "B2B 거래처",
      "email": "ho***@example.com",
      "company": "(주)테스트",
      "businessNo": "123-45-67890",
      "ceoName": "홍대표",
      "serviceName": "도매",
      "itemName": "식품",
      "comAddress": "06000 서울시 강남구 테헤란로 123",
      "partnerCode": "990954",
      "partnerName": "(에쓰푸드몰)테스트거래처",
      "memberStatus": "NORMAL",
      "entryDt": "2025-01-15 00:00:00",
      "appFl": "y",
      "duplicateMemNo": "12346;12347"
    },
    {
      "memNo": 12346,
      "memId": "kim456",
      "memName": "김*",
      "groupSno": 1,
      "groupName": "일반회원",
      "email": "ki***@test.com",
      "company": null,
      "businessNo": null,
      "ceoName": null,
      "serviceName": null,
      "itemName": null,
      "comAddress": null,
      "partnerCode": null,
      "partnerName": null,
      "memberStatus": null,
      "entryDt": "2025-02-10 00:00:00",
      "appFl": "n",
      "duplicateMemNo": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 23120,
  "totalPages": 1156
}
```

| 필드 | 설명 |
|------|------|
| `content[].memName` | 이름 마스킹: 첫 글자 + * (2자: "홍*", 3자: "홍*동") |
| `content[].email` | 이메일 마스킹: 로컬 앞 2자 + "***@" + 도메인 |
| `content[].company` | `godo_member_business` 레코드 존재 시 값 있음(등급과 무관), 그 외 `null` |
| `content[].businessNo` | 사업자번호 평문. `godo_member_business` 레코드 없으면 `null` |
| `content[].ceoName` | 대표자명 복호화 평문. `godo_member_business` 레코드 없으면 `null` |
| `content[].serviceName` | 업태명. `godo_member_business` 레코드 없으면 `null` |
| `content[].itemName` | 업종명. `godo_member_business` 레코드 없으면 `null` |
| `content[].comAddress` | 사업자 주소 복호화 평문. `godo_member_business` 레코드 없으면 `null` |
| `content[].partnerCode` | `member_partner` 등록된 경우만 값 있음, 그 외 `null` |
| `content[].partnerName` | 거래처명. `member_partner` 없으면 `null` |
| `content[].memberStatus` | 거래처 상태(`UNREGISTERED`/`NORMAL`/`INACTIVE`). `member_partner` 레코드 없으면 `null` |
| `content[].duplicateMemNo` | 동일 `businessNo`를 가진 다른 활성 회원의 `memNo` 목록(`;` 구분). `godo_member_business` 레코드 없거나 중복 없으면 `null` |

> `content[]`의 사업자 관련 필드(`company`~`duplicateMemNo`)는 `godo_member_business` 레코드 존재 여부로만 결정되며 `groupSno`(등급)와 무관하다. 미승인(`app_fl='n'`) 수집 직후에는 사업자 회원도 `groupSno=1`(일반회원)일 수 있으나, 이 경우에도 `godo_member_business` 레코드가 있으면 값이 채워진다.

Error Responses

- `401 Unauthorized`

Notes

- `deleted_fl = 'y'` 회원은 목록에 포함되지 않음
- `keyword` 없이 `searchType`만 전달 시 검색 조건 무시
- 이름·이메일·휴대폰 검색은 정확히 일치하는 값만 반환 (부분 검색 불가)
- `memberStatus=UNREGISTERED` 전달 시 `member_partner` 레코드가 없는 회원(null)도 미등록으로 간주하여 함께 반환

---

### GET `/api/members/summary`

요약 카드 4종 카운트와 마지막 동기화 시각을 반환한다.

Success Response

- `200 OK`

```json
{
  "totalCount": 23156,
  "businessCount": 4850,
  "b2bCount": 124,
  "pendingApprovalCount": 12,
  "lastSyncedAt": "2026-06-11T14:30:00"
}
```

| 필드 | 설명 |
|------|------|
| `totalCount` | 전체 활성 회원 수 (`deleted_fl = 'n'`) |
| `businessCount` | 사업자 회원 수 (`group_sno IN (2,3,5,6)`) — B2B 제외 |
| `b2bCount` | B2B 거래처 수 (`group_sno = 9`) |
| `pendingApprovalCount` | 미승인 회원 수 (`app_fl = 'n'`) |
| `lastSyncedAt` | 가장 최근 `synced_at` 값. 업로드 이력 없으면 `null` |

Error Responses

- `401 Unauthorized`

---

### GET `/api/members/{memNo}`

회원 상세 정보를 반환한다. 개인정보 필드는 **복호화 평문**으로 반환된다 (마스킹 없음).

Path Parameter

| 파라미터 | 설명 |
|---------|------|
| `memNo` | 고도몰 회원 번호 |

Success Response

- `200 OK`

```json
{
  "memNo": 12345,
  "memId": "hong123",
  "memName": "홍길동",
  "groupSno": 9,
  "groupName": "B2B 거래처",
  "appFl": "y",
  "appDt": "2025-01-20",
  "email": "hong@example.com",
  "cellPhone": "010-1234-5678",
  "phone": "02-123-4567",
  "address": "06000 서울시 강남구 테헤란로 123 101호",
  "entryDt": "2025-01-15",
  "lastLoginDt": "2026-06-01",
  "entryPath": "pc",
  "maillingFl": "y",
  "smsFl": "y",
  "nickName": null,
  "sexFl": "m",
  "birthDt": "1985-05-10",
  "marriFl": null,
  "marriDate": null,
  "job": null,
  "interest": null,
  "loginCount": 42,
  "saleAmount": 125000.0,
  "mileage": 1500.0,
  "deposit": 0.0,
  "recommId": null,
  "ex1": "식품제조",
  "ex2": "Y",
  "ex3": "홍담당 대리",
  "ex4": null,
  "ex5": null,
  "ex6": null,
  "memo": null,
  "business": {
    "company": "(주)테스트",
    "businessNo": "123-45-67890",
    "ceoName": "홍길동",
    "serviceName": "도매",
    "serviceCode": "01",
    "itemName": "식품",
    "itemCode": "0123",
    "comAddress": "06000 서울시 강남구 테헤란로 123",
    "bizLicenseFile": "license_hong123.pdf",
    "duplicateMemNo": "12346;12347"
  },
  "partner": {
    "partnerCode": "990954",
    "partnerName": "(에쓰푸드몰)테스트거래처",
    "taxInvoiceFl": "y",
    "memberStatus": "NORMAL",
    "updatedAt": "2026-06-11T14:30:00"
  }
}
```

| 필드 | 설명 |
|------|------|
| `business.serviceCode` / `business.itemCode` | 업태/종목 텍스트를 `common_code`(그룹코드 `SERVICE`/`ITEM`)에 매칭한 코드. 매칭 실패 시 `null` |
| `business.duplicateMemNo` | 동일 `businessNo`를 가진 다른 활성 회원의 `memNo` 목록(`;` 구분). 중복 없으면 `null` |
| `partner.memberStatus` | 거래처 상태. 허용값: `UNREGISTERED`(미등록) / `NORMAL`(정상) / `INACTIVE`(불용) |

| 섹션 | 반환 조건 |
|------|----------|
| `business` | `godo_member_business` 레코드 존재 시 (`group_sno` 등급과 무관, `null` if not found) |
| `partner` | `member_partner` 레코드 존재 시 (`null` if not found) |

> ⚠️ **`group_sno`로 게이팅하지 말 것**: 에쓰푸드몰 회원 수집은 미승인(`app_fl='n'`) 조건으로 이루어지며, 이 시점에는 사업자 회원이어도 `group_sno`가 기본값 `1`(일반회원)로 수집된다. ERP 등록 후 관리자가 수동으로 등급을 사업자로 변경하기 전까지는 `group_sno=1`이면서 `godo_member_business` 레코드는 이미 존재하는 상태가 정상적으로 발생한다. 따라서 `business` 노출 여부는 반드시 `godo_member_business` 레코드 존재 여부로만 판단해야 한다 (2026-07-08 버그 수정: `GetMemberDetailUseCase`가 등급 기준(`MemberGroup.hasBusiness()`)으로 게이팅하여 이 구간의 사업자 정보가 조회되지 않던 문제를 수정함).

Error Responses

- `401 Unauthorized`
- `404 Not Found` — memNo에 해당하는 회원 없음

Notes

- 복호화는 서버에서만 수행하며 클라이언트에 평문 전달 (상세 화면 확정 정책, 2026-06-11)
- `deleted_fl = 'y'` 회원은 `404`로 응답

---

## 수정 API

### PUT `/api/members/{memNo}/partner`

에쓰푸드 자체 관리 컬럼(`member_partner` + `biz_license_file`)을 저장한다.
회원이 존재하고 `member_partner` 레코드가 없으면 INSERT, 이미 있으면 UPDATE한다.
기존 레코드가 있는 경우 값을 덮어쓰기 전에 변경 전 상태를 `member_partner_log`에 먼저 기록한다.

> **수정 가능 컬럼 범위**: 에쓰푸드 자체 관리 컬럼만 수정 가능.
> 고도몰에서 받아 업로드된 컬럼(`godo_member`, `godo_member_business` 대부분)은 수정 불가 — 엑셀 재업로드로만 갱신.

| 테이블 | 수정 가능 필드 |
|--------|--------------|
| `member_partner` | `partnerCode`, `partnerName`, `taxInvoiceFl`, `memberStatus` |
| `godo_member_business` | `bizLicenseFile` (사업자 레코드 존재 시만 갱신) |

Path Parameter

| 파라미터 | 설명 |
|---------|------|
| `memNo` | 고도몰 회원 번호 |

Request

```json
{
  "partnerCode": "990954",
  "partnerName": "(에쓰푸드몰)테스트거래처",
  "taxInvoiceFl": "y",
  "memberStatus": "NORMAL",
  "bizLicenseFile": "license_hong123.pdf"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `partnerCode` | string \| null | 거래처 코드. null 허용 (코드 삭제 목적). 다른 활성 회원이 사용 중인 코드면 `409` |
| `partnerName` | string \| null | 거래처명. null 허용 |
| `taxInvoiceFl` | string | `"y"` 또는 `"n"`. null 전달 시 `"n"` 으로 처리 |
| `memberStatus` | string | 거래처 상태. `UNREGISTERED` \| `NORMAL` \| `INACTIVE`. null 전달 시 `"NORMAL"` 으로 처리, 그 외 값 전달 시 `400` |
| `bizLicenseFile` | string \| null | 사업자등록증 파일명. 사업자 레코드 없는 회원은 무시됨 |

Success Response

- `200 OK`

```json
{
  "partnerCode": "990954",
  "partnerName": "(에쓰푸드몰)테스트거래처",
  "taxInvoiceFl": "y",
  "memberStatus": "NORMAL",
  "bizLicenseFile": "license_hong123.pdf",
  "updatedAt": "2026-06-15T04:15:28.123456789Z"
}
```

Error Responses

- `400 Bad Request` — `memberStatus`가 `UNREGISTERED`/`NORMAL`/`INACTIVE` 중 하나가 아님 (`MEMBER_STATUS_INVALID`)
- `401 Unauthorized`
- `404 Not Found` — `memNo`에 해당하는 회원 없음 (`MEMBER_NOT_FOUND`)
- `409 Conflict` — `partnerCode`를 다른 활성 회원(`deleted_fl='n'`)이 이미 사용 중 (`DUPLICATE_PARTNER_CODE`)

Notes

- `member_partner` 레코드는 TEMPLATE/LIST 업로드, 배치 자동 수집, 또는 이 화면 저장 API에서 생성됨
- LIST 업로드와 배치 재동기화 시 `member_partner`는 변경되지 않으므로 수동 입력 값이 보존됨
- `bizLicenseFile`은 `godo_member_business` 레코드가 없는 회원에 대해서는 무시됨 (에러 없음)
- 기존 레코드가 있으면 저장 전 변경 전 값을 `member_partner_log`에 기록함 (`GET /api/members/{memNo}/partner/logs`로 조회)

---

### PUT `/api/members/{memNo}/business`

사업자 정보(`godo_member_business`)를 저장한다.
회원이 존재하고 사업자 레코드가 없으면 INSERT, 이미 있으면 UPDATE한다.
기존 레코드가 있는 경우 값을 덮어쓰기 전에 변경 전 상태를 `godo_member_business_log`에 먼저 기록한다.

Path Parameter

| 파라미터 | 설명 |
|---------|------|
| `memNo` | 고도몰 회원 번호 |

Request

```json
{
  "company": "(주)테스트",
  "businessNo": "123-45-67890",
  "ceoName": "홍길동",
  "serviceName": "도매",
  "serviceCode": "01",
  "itemName": "식품",
  "itemCode": "0123",
  "comAddress": "06000 서울시 강남구 테헤란로 123",
  "bizLicenseFile": "license_hong123.pdf"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `company` | string \| null | 상호 |
| `businessNo` | string \| null | 사업자번호 (평문) |
| `ceoName` | string \| null | 대표자명 (저장 시 암호화) |
| `serviceName` | string \| null | 업태명 |
| `serviceCode` | string \| null | 업태 공통코드. 화면에서 `common_code`(그룹코드 `SERVICE`) 선택값을 그대로 전달 |
| `itemName` | string \| null | 업종명 |
| `itemCode` | string \| null | 업종 공통코드. 화면에서 `common_code`(그룹코드 `ITEM`) 선택값을 그대로 전달 |
| `comAddress` | string \| null | 사업장 주소 (저장 시 암호화) |
| `bizLicenseFile` | string \| null | 사업자등록증 파일명. `null` 전달 시 기존 값 유지(덮어쓰지 않음) |

Success Response

- `200 OK`

```json
{
  "company": "(주)테스트",
  "businessNo": "123-45-67890",
  "ceoName": "홍길동",
  "serviceName": "도매",
  "serviceCode": "01",
  "itemName": "식품",
  "itemCode": "0123",
  "comAddress": "06000 서울시 강남구 테헤란로 123",
  "bizLicenseFile": "license_hong123.pdf",
  "updatedAt": "2026-06-15T04:15:28.123456789Z"
}
```

Error Responses

- `401 Unauthorized`
- `404 Not Found` — `memNo`에 해당하는 회원 없음 (`MEMBER_NOT_FOUND`)

Notes

- 이 API는 업로드 엑셀과 달리 `serviceName`/`itemName` 텍스트를 자동으로 코드 매칭하지 않음 — `serviceCode`/`itemCode`는 요청 값을 그대로 저장
- 기존 레코드가 있으면 저장 전 변경 전 값을 `godo_member_business_log`에 기록함 (`GET /api/members/{memNo}/business/logs`로 조회)
- 응답은 저장 직후의 `godo_member_business` 값을 그대로 반환함 (`PUT /partner`와 동일한 패턴, 2026-07-03부터)

---

## 이력 조회 APIs

### GET `/api/members/{memNo}/business/logs`

사업자 정보 변경 이력을 `loggedAt` 최신순으로 전체 반환한다.

Success Response

- `200 OK`

```json
{
  "logs": [
    {
      "id": 1,
      "memNo": 12345,
      "company": "(주)테스트",
      "businessNo": "123-45-67890",
      "ceoName": "홍길동",
      "serviceName": "도매",
      "serviceCode": "01",
      "itemName": "식품",
      "itemCode": "0123",
      "comAddress": "06000 서울시 강남구 테헤란로 123",
      "bizLicenseFile": "license_hong123.pdf",
      "loggedAt": "2026-06-30 10:00:00",
      "loggedBy": "user@s-food.com"
    }
  ]
}
```

| 필드 | 설명 |
|------|------|
| `logs[]` | `PUT /api/members/{memNo}/business` 호출로 값이 덮어써지기 **직전**의 스냅샷 목록 |
| `logs[].loggedBy` | 변경을 수행한 세션 사용자 이메일. 세션 정보가 없으면 `null` |

Error Responses

- `401 Unauthorized`

Notes

- 회원이 존재하지 않거나 이력이 없으면 `logs: []` 빈 배열 반환 (404 아님)

---

### GET `/api/members/{memNo}/partner/logs`

거래처 정보 변경 이력을 `loggedAt` 최신순으로 전체 반환한다.

Success Response

- `200 OK`

```json
{
  "logs": [
    {
      "id": 1,
      "memNo": 12345,
      "partnerCode": "990954",
      "partnerName": "(에쓰푸드몰)테스트거래처",
      "taxInvoiceFl": "n",
      "memberStatus": "NORMAL",
      "loggedAt": "2026-06-30 10:00:00",
      "loggedBy": "user@s-food.com"
    }
  ]
}
```

| 필드 | 설명 |
|------|------|
| `logs[]` | `PUT /api/members/{memNo}/partner` 호출로 값이 덮어써지기 **직전**의 스냅샷 목록 |
| `logs[].loggedBy` | 변경을 수행한 세션 사용자 이메일. 세션 정보가 없으면 `null` |

Error Responses

- `401 Unauthorized`

Notes

- 회원이 존재하지 않거나 이력이 없으면 `logs: []` 빈 배열 반환 (404 아님)

---

## 삭제 API

### DELETE `/api/members/{memNo}`

회원을 단건 삭제한다. `member_partner.member_status` 값에 따라 soft delete 또는 hard delete로 분기된다.

Path Parameter

| 파라미터 | 설명 |
|---------|------|
| `memNo` | 고도몰 회원 번호 |

**삭제 분기 기준:**

| 조건 | 처리 방식 |
|------|----------|
| `member_partner.member_status`가 `NORMAL`(정상) 또는 `INACTIVE`(불용) — ERP에 한 번이라도 등록된 이력이 있는 거래처 | Soft delete: `godo_member.deleted_fl = 'y'` 설정 |
| 그 외 (파트너 레코드 없음 또는 `member_status = 'UNREGISTERED'`) | Hard delete: `member_partner` → `godo_member_business` → `godo_member` 순으로 물리 삭제 |

> Hard delete 시 FK 참조 순서를 지켜야 한다 (SQLite에 ON DELETE CASCADE 없음).

Success Response

- `204 No Content`

Error Responses

- `401 Unauthorized`
- `404 Not Found` — `memNo`에 해당하는 회원 없음

Notes

- Soft delete된 회원은 목록(`GET /api/members`) 응답에서 제외됨 (`deleted_fl = 'n'` 필터)
- Hard delete는 복구 불가 — 프론트엔드에서 삭제 전 안내 필요

---

## 공통 오류 형식

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid member status",
  "path": "/api/members/12345/partner"
}
```
