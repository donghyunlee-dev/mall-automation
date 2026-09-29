# 회원·거래처 연동 고도화 설계

- 작성일: 2026-07-02
- 작성자: donghyun.lee@s-food.com
- 범위: backend

---

## 1. 개요

에쓰푸드몰 연동 관리 서비스에서 거래처 상태 관리, 거래처 자동 생성, 사업자/거래처 이력 관리, 공통코드 관리, 중복 거래처 검사, 수집 조건 개선을 위한 9개 항목의 백엔드 기능을 추가·수정한다.

---

## 2. DB 스키마 변경

### 2-1. member_partner 컬럼 변경

**추가**

| 컬럼 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `member_status` | TEXT | `'NORMAL'` | 거래처 상태. `NORMAL`(정상) / `INACTIVE`(불용) |

**삭제**

| 컬럼 | 사유 |
|------|------|
| `duplicate_check` | `godo_member_business.duplicate_mem_no`로 이관 |

### 2-2. godo_member_business 컬럼 추가

| 컬럼 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `duplicate_mem_no` | TEXT | NULL | 동일 사업자번호를 가진 다른 회원번호 목록. 다수일 경우 `;`로 구분. ex) `12345;23456` |

### 2-3. 신규 테이블: godo_member_business_log

사업자 정보 수정 전 기존 데이터를 전체 스냅샷으로 저장한다.

```sql
CREATE TABLE godo_member_business_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no          BIGINT  NOT NULL,
    company         TEXT,
    business_no     TEXT,
    ceo_name        TEXT,
    service         TEXT,
    item            TEXT,
    com_address     TEXT,
    biz_license_file TEXT,
    logged_at       TEXT NOT NULL,
    logged_by       TEXT,
    created_at      TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_mem_no
    ON godo_member_business_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_logged_at
    ON godo_member_business_log(logged_at);
```

### 2-4. 신규 테이블: member_partner_log

거래처 정보 수정 전 기존 데이터를 전체 스냅샷으로 저장한다.

```sql
CREATE TABLE member_partner_log (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no                BIGINT  NOT NULL,
    partner_registered_fl TEXT,
    partner_code          TEXT,
    partner_name          TEXT,
    tax_invoice_fl        TEXT,
    member_status         TEXT,
    logged_at             TEXT NOT NULL,
    logged_by             TEXT,
    created_at            TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_partner_log_mem_no
    ON member_partner_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_member_partner_log_logged_at
    ON member_partner_log(logged_at);
```

### 2-5. 신규 테이블: common_code (공통코드)

업태(`SERVICE`), 종목(`ITEM`) 등 범용 공통코드를 관리한다.

```sql
CREATE TABLE common_code (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    group_code  TEXT NOT NULL,
    group_name  TEXT NOT NULL,
    code        TEXT NOT NULL,
    code_name   TEXT NOT NULL,
    note        TEXT,
    use_yn      TEXT NOT NULL DEFAULT 'Y',
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL,
    UNIQUE (group_code, code)
);
CREATE INDEX IF NOT EXISTS idx_common_code_group_code
    ON common_code(group_code);
```

| 컬럼 | 설명 |
|------|------|
| `group_code` | 그룹코드명. ex) `SERVICE`, `ITEM` |
| `group_name` | 그룹명. ex) `업태`, `종목` |
| `code` | 공통코드 |
| `code_name` | 공통코드명 |
| `note` | 비고 |
| `use_yn` | 사용여부. `Y` / `N` |

---

## 3. API 설계

### 3-1. 사업자 정보 수정 (신규)

```
PUT /api/members/{memNo}/business
```

**Request Body**

```json
{
  "company": "달콤가든",
  "businessNo": "123-45-67890",
  "ceoName": "홍길동",
  "service": "도소매",
  "item": "식품",
  "comAddress": "서울시 강남구 ...",
  "bizLicenseFile": "biz_123.jpg"
}
```

**처리 순서**

1. `mem_no`로 기존 사업자 정보 조회
2. 기존 데이터를 `godo_member_business_log`에 저장 (logged_by = 세션 사용자 이메일)
3. `godo_member_business` 업데이트

> **logged_by 처리**: Controller에서 `session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY)`로 이메일을 가져와 UseCase에 `String loggedBy` 파라미터로 전달한다.

**Response**: 업데이트된 사업자 정보 반환

### 3-2. 사업자 정보 이력 조회 (신규)

```
GET /api/members/{memNo}/business/logs
```

**Response**: `godo_member_business_log` 목록 (최신순)

```json
{
  "logs": [
    {
      "id": 1,
      "memNo": 12345,
      "company": "달콤가든",
      "businessNo": "123-45-67890",
      "ceoName": "홍길동",
      "service": "도소매",
      "item": "식품",
      "comAddress": "...",
      "bizLicenseFile": "biz_123.jpg",
      "loggedAt": "2026-07-01 10:00:00",
      "loggedBy": "admin@s-food.com"
    }
  ]
}
```

### 3-3. 거래처 정보 이력 조회 (신규)

```
GET /api/members/{memNo}/partner/logs
```

**Response**: `member_partner_log` 목록 (최신순)

```json
{
  "logs": [
    {
      "id": 1,
      "memNo": 12345,
      "partnerRegisteredFl": "y",
      "partnerCode": "99412",
      "partnerName": "(에쓰푸드몰)달콤가든",
      "taxInvoiceFl": "y",
      "memberStatus": "NORMAL",
      "loggedAt": "2026-07-01 10:00:00",
      "loggedBy": "admin@s-food.com"
    }
  ]
}
```

### 3-4. 공통코드 API (신규)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/common-codes?groupCode={groupCode}` | 공통코드 목록 조회 (groupCode 미입력 시 전체) |
| `POST` | `/api/common-codes` | 공통코드 등록 |
| `PUT` | `/api/common-codes/{id}` | 공통코드 수정 |

**POST/PUT Request Body**

```json
{
  "groupCode": "SERVICE",
  "groupName": "업태",
  "code": "001",
  "codeName": "도소매",
  "note": "",
  "useYn": "Y"
}
```

---

## 4. 비즈니스 로직 변경

### 4-1. 거래처 자동 생성 (#2)

적용 대상: `FetchNewMembersUseCase`, `UploadMembersUseCase`

회원 신규 저장 시 `member_partner` 레코드를 자동 생성한다. 기존에 이미 존재하면 생성하지 않는다.

```
partner_code = MAX(CAST(partner_code AS INTEGER)) + 1  -- 전체 테이블 기준
partner_name = "(에쓰푸드몰)" + company                -- godo_member_business.company
member_status = 'NORMAL'
partner_registered_fl = 'n'
tax_invoice_fl = 'n'
```

- `partner_code` 계산은 저장 직전 단일 쿼리로 수행한다. (동시성: 운영 환경이 단일 인스턴스이므로 트랜잭션 내 MAX+1로 충분)
- `company`가 null인 경우: `partner_name = "(에쓰푸드몰)"`로 처리
- 기존 `UploadMembersUseCase`에서 TEMPLATE 타입일 때만 partner를 처리하던 분기를 제거하고, 모든 업로드 타입에서 자동 생성 로직을 적용한다

### 4-2. 거래처 수정 시 로그 저장 (#5)

`UpdateMemberPartnerUseCase.execute()` 내 기존 저장 전에 로그를 먼저 생성한다.

```
1. findPartnerByMemNo(memNo) 로 현재 데이터 조회
2. 조회된 데이터가 존재하면 member_partner_log 에 스냅샷 저장
3. 기존 partner 업데이트 (기존 로직 유지)
```

로그 미존재 시(최초 저장)에는 로그를 생성하지 않는다.

### 4-3. 중복 거래처 검사 (#7)

`FetchNewMembersUseCase.execute()` 내 사업자 정보 저장 완료 후 실행한다.

```
수집된 각 회원의 business_no 기준으로
godo_member_business 전체에서 동일 business_no를 가진 다른 mem_no 목록 조회
→ 목록을 ';' 로 join → godo_member_business.duplicate_mem_no 에 UPDATE
```

- 단독: `"12345"` / 다수: `"12345;23456"`
- 중복 없으면 `NULL`로 저장

### 4-4. 수집 조건 + 사업자 정보 저장 조건 변경 (#8, #9)

**URL 조건**: `SfoodAdminMemberClient.buildListUrl()`의 날짜(`entryDt`) 및 `appFl=n` 유지 — URL 변경 없음.

**`FetchNewMembersUseCase` 내부**:

```java
// 변경 전: hasBusiness() 조건으로 필터
List<CrawledMember> bizMembers = crawledMembers.stream()
    .filter(m -> m.groupSno() != null && MemberGroup.of(m.groupSno()).hasBusiness())
    .toList();

// 변경 후: 사업자 정보 필드 유무로 판단 (groupSno 조건 제거)
List<CrawledMember> bizMembers = crawledMembers.stream()
    .filter(m -> m.businessNo() != null || m.company() != null)
    .toList();
```

**`UploadMembersUseCase` 내부**:

```java
// 변경 전
if (MemberGroup.of(member.getGroupSno()).hasBusiness()) { ... }

// 변경 후: 사업자 정보 필드 유무로 판단
GodoMemberBusinessEntity biz = ... normalizer.applyBusinessFields(...)
if (biz의 company/businessNo 중 하나라도 non-null) { ... }
```

---

## 5. 클래스/파일 구성

### 신규 생성

| 파일 | 설명 |
|------|------|
| `GodoMemberBusinessLogEntity` | godo_member_business_log 엔티티 |
| `GodoMemberBusinessLogJpaRepository` | JPA Repository |
| `MemberPartnerLogEntity` | member_partner_log 엔티티 |
| `MemberPartnerLogJpaRepository` | JPA Repository |
| `CommonCodeEntity` | common_code 엔티티 |
| `CommonCodeJpaRepository` | JPA Repository |
| `UpdateMemberBusinessUseCase` | 사업자 정보 수정 UseCase |
| `GetMemberBusinessLogsUseCase` | 사업자 정보 이력 조회 UseCase |
| `GetMemberPartnerLogsUseCase` | 거래처 정보 이력 조회 UseCase |
| `CommonCodeUseCase` | 공통코드 CRUD UseCase |
| `CommonCodeController` | 공통코드 API Controller |
| `MemberBusinessUpdateRequest` | 사업자 수정 요청 DTO |
| `MemberBusinessLogResponse` | 사업자 이력 응답 DTO |
| `MemberPartnerLogResponse` | 거래처 이력 응답 DTO |
| `CommonCodeRequest` | 공통코드 요청 DTO |
| `CommonCodeResponse` | 공통코드 응답 DTO |

### 기존 수정

| 파일 | 변경 내용 |
|------|-----------|
| `MemberPartnerEntity` | `memberStatus` 필드 추가, `duplicateCheck` 필드 제거 |
| `GodoMemberBusinessEntity` | `duplicateMemNo` 필드 추가 |
| `UpdateMemberPartnerUseCase` | 로그 저장 로직 추가, `duplicateCheck` 관련 코드 제거 (`setDuplicateCheck` 호출 및 Request DTO 필드) |
| `FetchNewMembersUseCase` | `hasBusiness()` 조건 제거, 사업자 정보 필드 유무로 판단, 거래처 자동 생성 로직 추가, 중복 거래처 검사 추가 |
| `UploadMembersUseCase` | `hasBusiness()` 조건 제거, 사업자 정보 필드 유무로 판단, 거래처 자동 생성 로직 추가 |
| `MemberRepository` (port + impl) | 신규 메서드 추가 (로그 저장, MAX partner_code 조회, 중복 거래처 검사) |
| `MemberManageController` | 사업자 수정/이력, 거래처 이력 API 엔드포인트 추가 |
| `schema.sql` (SQLite) | 신규 테이블 3개, 컬럼 변경 반영 |
| `schema-member-postgresql.sql` (PostgreSQL) | 동일 |

---

## 6. 테스트 계획

| 항목 | 검증 방법 |
|------|-----------|
| member_status 기본값 | 거래처 자동 생성 후 `NORMAL` 확인 |
| 거래처 자동 생성 | 배치/업로드 후 member_partner 레코드 존재 및 partner_code/name 정확성 확인 |
| 사업자 수정 + 로그 | 수정 API 호출 → 로그 1건 생성, 본 데이터 업데이트 확인 |
| 거래처 수정 + 로그 | UpdateMemberPartnerUseCase 호출 → 로그 1건 생성 확인 |
| 이력 조회 API | mem_no 기준 로그 목록 정렬(최신순) 확인 |
| 중복 거래처 검사 | 동일 business_no 2개 회원 수집 후 duplicate_mem_no 세미콜론 join 확인 |
| 사업자 정보 조건 변경 | 일반회원(groupSno=1)이어도 company/businessNo 있으면 godo_member_business 저장 확인 |
| 공통코드 CRUD | 등록→조회→수정→use_yn=N 비활성화 흐름 확인 |
