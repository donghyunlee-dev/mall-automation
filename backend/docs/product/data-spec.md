# SQLite Schema & ERD — SFOOD-MALL-AUTOMATION-BACKEND

> 에쓰푸드몰 품목 판매 상태 자동화 백엔드 서비스  
> Database: SQLite  
> 작성일: 2026-03-31 / 최종 수정: 2026-07-03 (회원/공통코드 도메인 테이블 추가)

---

## 📐 ERD 관계도

```
┌─────────────────────┐
│   email_verification │
│─────────────────────│
│ PK  id              │
│     verification_key│
│     email           │
│     code_hash       │
│     status          │
│     attempt_count   │
│     max_attempt_cnt │
│     requested_at    │
│     expires_at      │
│     verified_at     │
│     last_sent_at    │
│     request_ip      │
│     user_agent      │
│     created_at      │
│     updated_at      │
└─────────────────────┘
        (인증 독립 도메인 — goods와 직접 관계 없음)

┌─────────────────────┐
│        users         │
│─────────────────────│
│ PK  id              │
│ UK  email           │
│     email_notification_yn
│     teams_notification_yn
│     last_login_at
│     created_at
│     updated_at
└─────────────────────┘
        (인증 계정 및 알림 수신 설정 관리)


┌──────────────────────────┐         ┌─────────────────────────────┐
│         goods            │ 1     N │      goods_status_log        │
│──────────────────────────│─────────│─────────────────────────────│
│ PK  id                   │         │ PK  id                      │
│ UK  godo_goods_code      │◄────────│     godo_goods_code (FK-ref)│
│ UK  erp_goods_code       │◄────────│     erp_goods_code  (FK-ref)│
│     goods_name           │         │     wms_available_qty       │
│     only_mall_yn         │         │     criteria_qty            │
│     sold_out_yn          │         │     safety_rate             │
│     available_qty        │         │     valid_percent           │
│     safety_rate          │         │     before_status           │
│     godo_sync_yn         │         │     after_status            │
│     erp_sync_yn          │         │     changed_at              │
│     godo_sync_blocked_yn │         │                             │
│     description          │         │     created_at              │
│     message              │         └─────────────────────────────┘
│     status               │
│     created_id           │         ┌─────────────────────────────┐
│     created_at           │ 1     N │      api_transaction_log     │
│     updated_id           │─────────│─────────────────────────────│
│     updated_at           │         │ PK  id                      │
└──────────────────────────┘◄────────│     godo_goods_code (FK-ref)│
                                     │     api_type                │
                                     │     request_url             │
                                     │     request_body            │
                                     │     response_body           │
                                     │     result_code             │
                                     │     result_message          │
                                     │     is_success              │
                                     │     requested_at            │
                                     │     responded_at            │
                                     │     created_at              │
                                     └─────────────────────────────┘


┌──────────────────────────┐         ┌─────────────────────────────┐
│   batch_schedule_config  │ 1     N │     batch_execution_log      │
│──────────────────────────│─────────│─────────────────────────────│
│ PK  id                   │         │ PK  id                      │
│ UK  job_name             │◄────────│     job_name        (FK-ref)│
│     enabled              │         │     started_at              │
│     schedule_type        │         │     ended_at                │
│     fixed_delay_seconds  │         │     status                  │
│     initial_delay_seconds│         │     message                 │
│     description          │         │     triggered_by            │
│     last_modified_by     │         │     duration_ms             │
│     created_at           │         │     created_at              │
│     updated_at           │         └─────────────────────────────┘
└──────────────────────────┘
           │ 1     N
           │
           ▼
┌─────────────────────────────┐
│  batch_schedule_change_log  │
│─────────────────────────────│
│ PK  id                      │
│     job_name        (FK-ref)│
│     before_fixed_delay_sec  │
│     after_fixed_delay_sec   │
│     before_enabled          │
│     after_enabled           │
│     changed_by              │
│     changed_at              │
└─────────────────────────────┘


┌──────────────────────────┐
│   common_status_config   │
│──────────────────────────│   (goods 등록/수정 시 기준값 참조 — 논리적 연관)
│ PK  id                   │
│ UK  scope                │
│     out_of_stock_qty     │
│     on_sale_qty          │
│     safety_rate          │
│     description          │
│     updated_id           │
│     updated_at           │
└──────────────────────────┘


┌──────────────────────────┐
│      email_send_log      │
│──────────────────────────│
│ PK  id                   │
│     send_type            │
│     recipient            │
│     subject              │
│     is_success           │
│     error_message        │
│     sent_at              │
│     created_at           │
└──────────────────────────┘
```

> SQLite는 외래 키(FOREIGN KEY) 제약을 기본 비활성화 상태로 지원한다.  
> 본 스키마에서 `(FK-ref)` 표기는 애플리케이션 레벨에서 참조 무결성을 관리함을 의미한다.  
> 필요 시 `PRAGMA foreign_keys = ON;` 으로 활성화 가능하나, 배치 성능 고려하여 앱 레벨 관리를 권장한다.

---

## 📐 ERD 관계도 — 회원/공통코드 도메인

```
┌──────────────────────────┐         ┌─────────────────────────────┐
│       godo_member         │ 1     1 │    godo_member_business      │
│──────────────────────────│─────────│─────────────────────────────│
│ PK  id                    │         │ PK  id                      │
│ UK  mem_no                │◄────────│ FK  mem_no (UK)              │
│ UK  mem_id                │         │     company                 │
│     group_sno             │         │     business_no              │
│     member_fl             │         │     ceo_name    [암호화]      │
│     app_fl / app_dt       │         │     service / service_code   │
│     mem_name    [암호화]   │         │     item / item_code         │
│     mem_name_hash         │         │     com_address [암호화]      │
│     email       [암호화]   │         │     biz_license_file         │
│     email_hash            │         │     duplicate_mem_no         │
│     cell_phone  [암호화]   │         │     created_at / updated_at  │
│     cell_phone_hash       │         └─────────────────────────────┘
│     phone       [암호화]   │                    │ (app-level, mem_no 기준)
│     address     [암호화]   │                    ▼
│     birth_dt    [암호화]   │         ┌─────────────────────────────┐
│     entry_dt / entry_path │         │  godo_member_business_log    │
│     login_count           │         │─────────────────────────────│
│     sale_amount / mileage │         │ PK  id                      │
│     deposit                │         │     mem_no (ref)             │
│     deleted_fl / synced_at│         │     company ~ biz_license_file│
│     created_at / updated_at│        │     service_code / item_code │
└──────────────────────────┘         │     logged_at / logged_by    │
        │ 1     1                    │     created_at               │
        ▼                            └─────────────────────────────┘
┌──────────────────────────┐
│      member_partner       │ 1     N  ┌─────────────────────────────┐
│──────────────────────────│──────────│      member_partner_log      │
│ PK  id                    │          │─────────────────────────────│
│ FK  mem_no (UK)            │◄─────────│ PK  id                      │
│     partner_code          │          │     mem_no (ref)             │
│     partner_name          │          │     partner_code / name      │
│     tax_invoice_fl        │          │     tax_invoice_fl            │
│     member_status         │          │     member_status             │
│     created_at / updated_at│         │     logged_at / logged_by    │
└──────────────────────────┘          │     created_at               │
                                       └─────────────────────────────┘

┌──────────────────────────┐
│        common_code        │   (godo_member_business.service_code / item_code — 논리적 연관, FK 없음)
│──────────────────────────│   CommonCodeMappingService가 codeName ↔ code 캐시를 메모리에 유지
│ PK  id                    │
│ UK  (group_code, code)    │
│     group_name             │
│     code_name              │
│     note                   │
│     use_yn                 │
│     created_at / updated_at│
└──────────────────────────┘
```

> `godo_member_business` / `member_partner`는 `mem_no` 기준 1:1 관계이며 `godo_member`가 없어도 앱 레벨에서 생성될 수 없다(업로드·배치·화면 저장 API 모두 `godo_member` 존재를 전제).  
> 로그 테이블(`godo_member_business_log`, `member_partner_log`)은 FK를 걸지 않고 `mem_no`만 보관한다 — 원본 레코드가 삭제되어도 이력은 남는다.  
> `common_code`는 `godo_member_business.service_code`/`item_code`와 값으로만 연결되며 FK 제약은 없다 (`CommonCodeMappingService`가 애플리케이션 시작 시 `use_yn='Y'` 코드만 인메모리 캐시로 적재).

---

## 🗂️ 테이블 상세 명세 및 DDL

---

### 🔑 email_verification

> OTP 이메일 인증 세션 관리 테이블

**용도** — 이메일 인증 요청 시 생성되는 일시적 인증 세션을 저장한다. OTP 원문은 저장하지 않으며 해시값만 보관한다.

**status 허용값** — `PENDING` / `VERIFIED` / `EXPIRED` / `FAILED` / `CANCELLED`

```sql
CREATE TABLE email_verification (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    verification_key  TEXT    NOT NULL UNIQUE,
    email             TEXT    NOT NULL,
    code_hash         TEXT    NOT NULL,
    status            TEXT    NOT NULL DEFAULT 'PENDING',
    attempt_count     INTEGER NOT NULL DEFAULT 0,
    max_attempt_count INTEGER NOT NULL DEFAULT 5,
    requested_at      TEXT    NOT NULL,
    expires_at        TEXT    NOT NULL,
    verified_at       TEXT,
    last_sent_at      TEXT    NOT NULL,
    request_ip        TEXT,
    user_agent        TEXT,
    created_at        TEXT    NOT NULL,
    updated_at        TEXT    NOT NULL
);

CREATE UNIQUE INDEX uk_email_verification_verification_key
    ON email_verification(verification_key);

CREATE INDEX idx_email_verification_email_status
    ON email_verification(email, status);

CREATE INDEX idx_email_verification_expires_at
    ON email_verification(expires_at);
```

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `verification_key` | TEXT | ✅ | — | UUID v4, UNIQUE |
| `email` | TEXT | ✅ | — | 소문자 정규화 저장 |
| `code_hash` | TEXT | ✅ | — | SHA-256 해시값 (OTP 원문 저장 금지) |
| `status` | TEXT | ✅ | `PENDING` | 인증 상태 |
| `attempt_count` | INTEGER | ✅ | `0` | 현재 시도 횟수 |
| `max_attempt_count` | INTEGER | ✅ | `5` | 최대 허용 시도 횟수 |
| `requested_at` | TEXT | ✅ | — | 요청 일시 (ISO 8601) |
| `expires_at` | TEXT | ✅ | — | 만료 일시 (ISO 8601) |
| `verified_at` | TEXT | | — | 인증 완료 일시 |
| `last_sent_at` | TEXT | ✅ | — | 마지막 발송 일시 |
| `request_ip` | TEXT | | — | 요청 IP |
| `user_agent` | TEXT | | — | 요청 User-Agent |
| `created_at` | TEXT | ✅ | — | 생성 일시 |
| `updated_at` | TEXT | ✅ | — | 수정 일시 |

---

### 👤 users

> 로그인 사용자 계정 및 알림 수신 설정 테이블

**용도** — OTP 인증을 완료한 사용자 계정을 저장하고, 이메일/Teams 알림 수신 설정과 최근 로그인 시각을 관리한다.

**email_notification_yn 허용값** — `Y` / `N`

**teams_notification_yn 허용값** — `Y` / `N`

```sql
CREATE TABLE users (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    email                 TEXT    NOT NULL UNIQUE,
    email_notification_yn TEXT    NOT NULL DEFAULT 'N',
    teams_notification_yn TEXT    NOT NULL DEFAULT 'N',
    last_login_at         TEXT    NOT NULL,
    created_at            TEXT    NOT NULL,
    updated_at            TEXT    NOT NULL
);

CREATE UNIQUE INDEX uk_users_email
    ON users(email);

CREATE INDEX idx_users_email_notification_yn
    ON users(email_notification_yn);
```

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `email` | TEXT | ✅ | — | 로그인 사용자 이메일, UNIQUE |
| `email_notification_yn` | TEXT | ✅ | `N` | 이메일 알림 수신 여부 |
| `teams_notification_yn` | TEXT | ✅ | `N` | Teams 알림 수신 여부 |
| `last_login_at` | TEXT | ✅ | — | 마지막 로그인 일시 (ISO 8601) |
| `created_at` | TEXT | ✅ | — | 생성 일시 |
| `updated_at` | TEXT | ✅ | — | 수정 일시 |

---

### 📦 goods

> 품목 마스터 정보 테이블

**용도** — 에쓰푸드몰에서 판매되는 품목의 핵심 정보를 관리한다. 고도몰 코드와 ERP 코드를 연결하는 맵핑 마스터 역할을 한다.

**status 허용값** — `NORMAL` / `UNVERIFIED` / `ERROR` / `HIDE`

**only_mall_yn 허용값** — `Y` (전용, EXCLUSIVE) / `N` (공용, GENERAL)

**sold_out_yn 허용값** — `Y` (품절) / `N` (정상)

**godo_sync_yn 허용값** — `Y` (고도몰 API 검증 완료) / `N` (미검증)

**erp_sync_yn 허용값** — `Y` (WMS 재고 매핑 확인) / `N` (미확인 또는 오류)

**godo_sync_blocked_yn 허용값** — `Y` (자동 고도몰 전송 차단) / `N` (자동 전송 허용)

```sql
CREATE TABLE goods (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    godo_goods_code TEXT    NOT NULL UNIQUE,
    erp_goods_code  TEXT    NOT NULL,
    goods_name      TEXT    NOT NULL,
    only_mall_yn    TEXT    NOT NULL DEFAULT 'N',
    sold_out_yn     TEXT    NOT NULL DEFAULT 'N',
    available_qty   INTEGER NOT NULL DEFAULT 0,
    safety_rate     INTEGER NOT NULL DEFAULT 0,
    remain_valid_days INTEGER NOT NULL DEFAULT 0,
    godo_sync_yn    TEXT    NOT NULL DEFAULT 'N',
    erp_sync_yn     TEXT    NOT NULL DEFAULT 'N',
    godo_sync_blocked_yn TEXT NOT NULL DEFAULT 'N',
    description     TEXT,
    message         TEXT,
    status          TEXT    NOT NULL DEFAULT 'UNVERIFIED',
    created_id      TEXT    NOT NULL,
    created_at      TEXT    NOT NULL,
    updated_id      TEXT,
    updated_at      TEXT
);

CREATE UNIQUE INDEX uk_goods_godo_goods_code ON goods(godo_goods_code);
CREATE INDEX idx_goods_erp_goods_code        ON goods(erp_goods_code);

CREATE INDEX idx_goods_status       ON goods(status);
CREATE INDEX idx_goods_sold_out_yn  ON goods(sold_out_yn);
CREATE INDEX idx_goods_erp_sync_yn  ON goods(erp_sync_yn);
```

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `godo_goods_code` | TEXT | ✅ | — | 고도몰 품목 코드, UNIQUE |
| `erp_goods_code` | TEXT | ✅ | — | ERP 품목 코드 (WMS ITEM_CD 매핑 키) |
| `goods_name` | TEXT | ✅ | — | 품목명 |
| `only_mall_yn` | TEXT | ✅ | `N` | 전용 구분 (Y=전용/N=공용) |
| `sold_out_yn` | TEXT | ✅ | `N` | 품절 여부 |
| `available_qty` | INTEGER | ✅ | `0` | 가용수량 기준 (EA), 공통 설정에서 초기화 |
| `safety_rate` | INTEGER | ✅ | `0` | 안전재고 기준 (%), 공통 설정에서 초기화 |
| `remain_valid_days` | INTEGER | ✅ | `0` | 냉동 품목 잔여일수 기준, `common_status_config` 값으로 초기화 |
| `godo_sync_yn` | TEXT | ✅ | `N` | 고도몰 API 연동 확인 여부 |
| `erp_sync_yn` | TEXT | ✅ | `N` | WMS ERP 코드 매핑 확인 여부 |
| `godo_sync_blocked_yn` | TEXT | ✅ | `N` | 자동 고도몰 전송 차단 여부 |
| `description` | TEXT | | — | 비고 (사용자 작성) |
| `message` | TEXT | | — | 검증 관련 메시지 (오류 원인 등) |
| `status` | TEXT | ✅ | `UNVERIFIED` | 품목 상태 |
| `created_id` | TEXT | ✅ | — | 생성 사용자 이메일 |
| `created_at` | TEXT | ✅ | — | 생성 일시 |
| `updated_id` | TEXT | | — | 수정 사용자 이메일 |
| `updated_at` | TEXT | | — | 수정 일시 |

---

### 📋 goods_status_log

> 품목 판매 상태 변경 이력 로그 테이블

**용도** — 배치 작업에서 품목의 판매 상태가 변경될 때 판단 근거 데이터와 함께 저장한다.

**before_status / after_status 허용값** — `ON_SALE` / `OUT_OF_STOCK`

```sql
CREATE TABLE goods_status_log (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    godo_goods_code   TEXT    NOT NULL,
    erp_goods_code    TEXT    NOT NULL,
    wms_available_qty INTEGER NOT NULL,
    criteria_qty      INTEGER NOT NULL,
    safety_rate       INTEGER NOT NULL,
    valid_percent     INTEGER,
    remain_valid_days INTEGER,
    before_status     TEXT    NOT NULL,
    after_status      TEXT    NOT NULL,
    changed_at        TEXT    NOT NULL,
    created_at        TEXT    NOT NULL
);

CREATE INDEX idx_goods_status_log_godo_goods_code ON goods_status_log(godo_goods_code);
CREATE INDEX idx_goods_status_log_changed_at      ON goods_status_log(changed_at);
CREATE INDEX idx_goods_status_log_date_goods
    ON goods_status_log(changed_at, godo_goods_code);
```

| 컬럼 | 타입 | NOT NULL | 설명 |
|---|---|---|---|
| `id` | INTEGER | ✅ | PK |
| `godo_goods_code` | TEXT | ✅ | 고도몰 품목 코드 (goods 참조) |
| `erp_goods_code` | TEXT | ✅ | ERP 품목 코드 (goods 참조) |
| `wms_available_qty` | INTEGER | ✅ | 수집 시점 WMS 합산 가용재고 수량 |
| `criteria_qty` | INTEGER | ✅ | 판단 기준 가용재고 수량 |
| `safety_rate` | INTEGER | ✅ | 적용된 안전재고 기준율 (%) |
| `valid_percent` | INTEGER | | 비냉동 기준 판단에 사용된 WMS 잔여율 증거값 최대치 (%) |
| `remain_valid_days` | INTEGER | | 냉동 기준 판단에 사용된 WMS 잔여일수 증거값 최대치 |
| `before_status` | TEXT | ✅ | 변경 전 상태 |
| `after_status` | TEXT | ✅ | 변경 후 상태 |
| `changed_at` | TEXT | ✅ | 상태 변경 일시 |
| `created_at` | TEXT | ✅ | 로그 저장 일시 |

---

### 🌐 api_transaction_log

> 고도몰 API 송수신 이력 로그 테이블

**용도** — 고도몰 Open API 호출 전 Request, 응답 후 Response를 1건의 레코드로 저장한다.

**api_type 허용값** — `GOODS_SEARCH` (품목 조회) / `SOLDOUT_STATUS` (품절 상태 변경)

**is_success 허용값** — `0` (실패) / `1` (성공)

```sql
CREATE TABLE api_transaction_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    api_type        TEXT    NOT NULL,
    godo_goods_code TEXT    NOT NULL,
    request_url     TEXT    NOT NULL,
    request_body    TEXT,
    response_body   TEXT,
    result_code     TEXT,
    result_message  TEXT,
    is_success      INTEGER NOT NULL DEFAULT 0,
    requested_at    TEXT    NOT NULL,
    responded_at    TEXT,
    created_at      TEXT    NOT NULL
);

CREATE INDEX idx_api_transaction_log_godo_goods_code ON api_transaction_log(godo_goods_code);
CREATE INDEX idx_api_transaction_log_requested_at    ON api_transaction_log(requested_at);
CREATE INDEX idx_api_transaction_log_api_type        ON api_transaction_log(api_type);
```

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `api_type` | TEXT | ✅ | — | API 종류 구분 |
| `godo_goods_code` | TEXT | ✅ | — | 대상 고도몰 품목 코드 |
| `request_url` | TEXT | ✅ | — | 요청 URL |
| `request_body` | TEXT | | — | 요청 파라미터 (전송 전 저장) |
| `response_body` | TEXT | | — | 응답 원문 (수신 후 저장) |
| `result_code` | TEXT | | — | 고도몰 응답 코드 (예: `000`, `119`) |
| `result_message` | TEXT | | — | 고도몰 응답 메시지 |
| `is_success` | INTEGER | ✅ | `0` | 처리 결과 (0=실패/1=성공) |
| `requested_at` | TEXT | ✅ | — | 요청 일시 |
| `responded_at` | TEXT | | — | 응답 수신 일시 |
| `created_at` | TEXT | ✅ | — | 레코드 생성 일시 |

---

### ⚙️ common_status_config

> 공통 품목 판매 상태 검증 기준 테이블

**용도** — 전용(EXCLUSIVE) / 공용(GENERAL) scope 별로 품절/정상 전환 기준값을 저장한다. 품목 등록 시 이 테이블의 값을 읽어 goods의 `available_qty`, `safety_rate`, `remain_valid_days`를 초기화한다.

**scope 허용값** — `GENERAL` (공용) / `EXCLUSIVE` (전용)

```sql
CREATE TABLE common_status_config (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    scope            TEXT    NOT NULL UNIQUE,
    out_of_stock_qty INTEGER NOT NULL,
    on_sale_qty      INTEGER NOT NULL,
    safety_rate      INTEGER NOT NULL,
    remain_valid_days INTEGER NOT NULL,
    description      TEXT,
    updated_id       TEXT,
    updated_at       TEXT    NOT NULL
);

CREATE UNIQUE INDEX uk_common_status_config_scope ON common_status_config(scope);
```

**초기 데이터**

| scope | out_of_stock_qty | on_sale_qty | safety_rate | remain_valid_days | 설명 |
|---|---|---|---|---|---|
| `GENERAL` | 20 | 21 | 50 | 150 | 공용 품목 기준 |
| `EXCLUSIVE` | 1 | 2 | 50 | 150 | 전용 품목 기준 |

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `scope` | TEXT | ✅ | — | 적용 범위 (UNIQUE) |
| `out_of_stock_qty` | INTEGER | ✅ | — | 품절 전환 기준 가용재고 이하값 (EA) |
| `on_sale_qty` | INTEGER | ✅ | — | 정상 전환 기준 가용재고 이상값 (EA) |
| `safety_rate` | INTEGER | ✅ | — | 정상 전환 기준 잔여율 이상값 (%) |
| `remain_valid_days` | INTEGER | ✅ | — | 냉동 품목 정상 전환/품절 판단용 잔여일수 기준 |
| `description` | TEXT | | — | 설명 |
| `updated_id` | TEXT | | — | 마지막 수정 사용자 이메일 |
| `updated_at` | TEXT | ✅ | — | 수정 일시 |

---

### 🗓️ batch_schedule_config

> 배치 스케줄 설정 테이블

**용도** — 배치 잡의 실행 주기, 활성화 여부 등 설정을 저장한다. 서버 재기동 시 이 테이블을 읽어 스케줄을 자동 복원한다.

**schedule_type 허용값** — `FIXED_DELAY`

```sql
CREATE TABLE batch_schedule_config (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name              TEXT    NOT NULL UNIQUE,
    enabled               INTEGER NOT NULL DEFAULT 1,
    schedule_type         TEXT    NOT NULL DEFAULT 'FIXED_DELAY',
    fixed_delay_seconds   INTEGER NOT NULL,
    initial_delay_seconds INTEGER NOT NULL DEFAULT 0,
    description           TEXT,
    last_modified_by      TEXT,
    created_at            TEXT    NOT NULL,
    updated_at            TEXT    NOT NULL
);

CREATE UNIQUE INDEX uk_batch_schedule_config_job_name ON batch_schedule_config(job_name);
```

**초기 데이터**

| job_name | enabled | fixed_delay_seconds | 설명 |
|---|---|---|---|
| `product-sync` | 1 | 600 | 10분 주기 품목 판매 상태 동기화 |

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `job_name` | TEXT | ✅ | — | 잡 식별자 (UNIQUE) |
| `enabled` | INTEGER | ✅ | `1` | 활성화 여부 (0=비활성/1=활성) |
| `schedule_type` | TEXT | ✅ | `FIXED_DELAY` | 스케줄 방식 |
| `fixed_delay_seconds` | INTEGER | ✅ | — | 실행 간격 (초, 최소 60) |
| `initial_delay_seconds` | INTEGER | ✅ | `0` | 초기 지연 시간 (초) |
| `description` | TEXT | | — | 설명 |
| `last_modified_by` | TEXT | | — | 마지막 수정자 이메일 |
| `created_at` | TEXT | ✅ | — | 생성 일시 |
| `updated_at` | TEXT | ✅ | — | 수정 일시 |

---

### 📊 batch_execution_log

> 배치 실행 이력 로그 테이블

**용도** — 배치 잡의 실행 시작/종료/결과를 저장한다. 수동 실행과 스케줄 실행을 `triggered_by`로 구분한다.

**status 허용값** — `SUCCESS` / `FAIL` / `SKIPPED`

**triggered_by 허용값** — `SCHEDULER` / `MANUAL`

```sql
CREATE TABLE batch_execution_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name     TEXT    NOT NULL,
    started_at   TEXT    NOT NULL,
    ended_at     TEXT,
    status       TEXT    NOT NULL,
    message      TEXT,
    triggered_by TEXT    NOT NULL,
    duration_ms  INTEGER,
    created_at   TEXT    NOT NULL
);

CREATE INDEX idx_batch_execution_log_job_name   ON batch_execution_log(job_name);
CREATE INDEX idx_batch_execution_log_started_at ON batch_execution_log(started_at);
CREATE INDEX idx_batch_execution_log_status     ON batch_execution_log(status);
```

| 컬럼 | 타입 | NOT NULL | 설명 |
|---|---|---|---|
| `id` | INTEGER | ✅ | PK |
| `job_name` | TEXT | ✅ | 잡 식별자 (batch_schedule_config 참조) |
| `started_at` | TEXT | ✅ | 실행 시작 일시 |
| `ended_at` | TEXT | | 실행 종료 일시 |
| `status` | TEXT | ✅ | 실행 결과 상태 |
| `message` | TEXT | | 결과 요약 메시지 |
| `triggered_by` | TEXT | ✅ | 실행 트리거 구분 |
| `duration_ms` | INTEGER | | 실행 소요 시간 (ms) |
| `created_at` | TEXT | ✅ | 레코드 생성 일시 |

---

### 🔁 batch_schedule_change_log

> 배치 스케줄 변경 이력 로그 테이블

**용도** — 배치 실행 주기나 활성화 여부가 변경될 때 변경 전후 값을 이력으로 저장한다.

```sql
CREATE TABLE batch_schedule_change_log (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name               TEXT    NOT NULL,
    before_fixed_delay_sec INTEGER,
    after_fixed_delay_sec  INTEGER,
    before_enabled         INTEGER,
    after_enabled          INTEGER,
    changed_by             TEXT,
    changed_at             TEXT    NOT NULL
);

CREATE INDEX idx_batch_schedule_change_log_job_name  ON batch_schedule_change_log(job_name);
CREATE INDEX idx_batch_schedule_change_log_changed_at ON batch_schedule_change_log(changed_at);
```

| 컬럼 | 타입 | NOT NULL | 설명 |
|---|---|---|---|
| `id` | INTEGER | ✅ | PK |
| `job_name` | TEXT | ✅ | 잡 식별자 |
| `before_fixed_delay_sec` | INTEGER | | 변경 전 실행 간격 (초) |
| `after_fixed_delay_sec` | INTEGER | | 변경 후 실행 간격 (초) |
| `before_enabled` | INTEGER | | 변경 전 활성화 여부 |
| `after_enabled` | INTEGER | | 변경 후 활성화 여부 |
| `changed_by` | TEXT | | 변경자 이메일 |
| `changed_at` | TEXT | ✅ | 변경 일시 |

---

### 📤 email_send_log

> 이메일 발송 이력 로그 테이블

**용도** — OTP 인증 메일 및 배치 결과 알림 메일 발송 이력을 저장한다.

**send_type 허용값** — `OTP` / `BATCH_RESULT`

**is_success 허용값** — `0` (실패) / `1` (성공)

```sql
CREATE TABLE email_send_log (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    send_type     TEXT    NOT NULL,
    recipient     TEXT    NOT NULL,
    subject       TEXT    NOT NULL,
    is_success    INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    sent_at       TEXT    NOT NULL,
    created_at    TEXT    NOT NULL
);

CREATE INDEX idx_email_send_log_sent_at   ON email_send_log(sent_at);
CREATE INDEX idx_email_send_log_send_type ON email_send_log(send_type);
```

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `send_type` | TEXT | ✅ | — | 발송 유형 |
| `recipient` | TEXT | ✅ | — | 수신자 이메일 |
| `subject` | TEXT | ✅ | — | 메일 제목 |
| `is_success` | INTEGER | ✅ | `0` | 발송 성공 여부 |
| `error_message` | TEXT | | — | 발송 실패 시 오류 메시지 |
| `sent_at` | TEXT | ✅ | — | 발송 시도 일시 |
| `created_at` | TEXT | ✅ | — | 레코드 생성 일시 |

---

### 👥 godo_member

> 고도몰 회원 마스터 테이블 (엑셀 업로드 전용 — 고도몰 회원 API 미제공)

**용도** — 고도몰에서 내려받은 회원 정보를 저장한다. 데이터 유입은 엑셀 업로드(`POST /api/members/uploads`) 및 에쓰푸드 어드민 자동 수집 배치뿐이다. `mem_no`가 upsert 키다.

**group_sno 허용값** — `1`(일반회원) / `2`(사업자회원_화이트) / `3`(사업자회원_로제) / `5`(사업자회원_레드) / `6`(사업자회원_VIP) / `7`(관리자 등급) / `8`(기능장회원) / `9`(B2B 거래처)

**member_fl 허용값** — `personal` / `business`

**app_fl 허용값** — `y`(승인) / `n`(미승인)

**sex_fl 허용값** — `m` / `w`

**marri_fl 허용값** — `y` / `n` / `NULL`

**entry_path 허용값** — `pc` / `mobile`

**mailling_fl / sms_fl 허용값** — `y`(수신) / `n`(거부)

**expiration_fl 허용값** — `1` / `3` / `5` / `999` (개인정보 유효기간, 연 단위)

**deleted_fl 허용값** — `y`(소프트 삭제) / `n`(정상)

**PII 암호화 컬럼(AES-256-GCM, at rest)** — `mem_name`, `email`, `cell_phone`, `phone`, `address`, `birth_dt` (설계 문서: `docs/analysis/godomall-member-encryption-design.md`)

**검색용 해시 컬럼(SHA-256)** — `mem_name_hash`, `email_hash`, `cell_phone_hash` — `mem_name`/`email`은 원문 그대로, `cell_phone`은 하이픈 제거 후 해시

```sql
CREATE TABLE IF NOT EXISTS godo_member (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no          BIGINT  NOT NULL UNIQUE,
    mem_id          TEXT    NOT NULL UNIQUE,
    group_sno       INTEGER NOT NULL DEFAULT 1,
    member_fl       TEXT    NOT NULL DEFAULT 'personal',
    app_fl          TEXT    NOT NULL DEFAULT 'n',
    app_dt          TEXT,
    mem_name        TEXT,
    mem_name_hash   TEXT,
    nick_name       TEXT,
    sex_fl          TEXT,
    birth_dt        TEXT,
    marri_fl        TEXT,
    marri_date      TEXT,
    job             TEXT,
    interest        TEXT,
    email           TEXT,
    email_hash      TEXT,
    cell_phone      TEXT,
    cell_phone_hash TEXT,
    phone           TEXT,
    fax             TEXT,
    mailling_fl     TEXT    NOT NULL DEFAULT 'y',
    sms_fl          TEXT    NOT NULL DEFAULT 'y',
    address         TEXT,
    entry_dt        TEXT,
    entry_path      TEXT    NOT NULL DEFAULT 'pc',
    last_login_dt   TEXT,
    login_count     INTEGER NOT NULL DEFAULT 0,
    sale_amount     DOUBLE  NOT NULL DEFAULT 0,
    mileage         DOUBLE  NOT NULL DEFAULT 0,
    deposit         DOUBLE  NOT NULL DEFAULT 0,
    recomm_id       TEXT,
    ex1             TEXT,
    ex2             TEXT,
    ex3             TEXT,
    ex4             TEXT,
    ex5             TEXT,
    ex6             TEXT,
    expiration_fl   INTEGER NOT NULL DEFAULT 1,
    memo            TEXT,
    deleted_fl      TEXT NOT NULL DEFAULT 'n',
    synced_at       TEXT NOT NULL,
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_godo_member_mem_no ON godo_member(mem_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_godo_member_mem_id ON godo_member(mem_id);
CREATE INDEX IF NOT EXISTS idx_godo_member_group_sno       ON godo_member(group_sno);
CREATE INDEX IF NOT EXISTS idx_godo_member_app_fl          ON godo_member(app_fl);
CREATE INDEX IF NOT EXISTS idx_godo_member_member_fl       ON godo_member(member_fl);
CREATE INDEX IF NOT EXISTS idx_godo_member_entry_dt        ON godo_member(entry_dt);
CREATE INDEX IF NOT EXISTS idx_godo_member_deleted_fl      ON godo_member(deleted_fl);
CREATE INDEX IF NOT EXISTS idx_godo_member_mem_name_hash   ON godo_member(mem_name_hash);
CREATE INDEX IF NOT EXISTS idx_godo_member_email_hash      ON godo_member(email_hash);
CREATE INDEX IF NOT EXISTS idx_godo_member_cell_phone_hash ON godo_member(cell_phone_hash);
```

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `mem_no` | BIGINT | ✅ | — | 고도몰 회원 번호, UNIQUE — upsert 키 |
| `mem_id` | TEXT | ✅ | — | 로그인 아이디, UNIQUE |
| `group_sno` | INTEGER | ✅ | `1` | 회원 등급 코드 |
| `member_fl` | TEXT | ✅ | `personal` | 개인/사업자 구분 |
| `app_fl` / `app_dt` | TEXT | app_fl ✅ | `n` | 가입 승인 여부·승인일 |
| `mem_name` / `mem_name_hash` | TEXT | | — | 이름 (암호화) / 검색용 해시 |
| `nick_name` | TEXT | | — | 닉네임 |
| `sex_fl` | TEXT | | — | 성별 |
| `birth_dt` | TEXT | | — | 생년월일 (암호화) |
| `marri_fl` / `marri_date` | TEXT | | — | 결혼 여부 / 결혼기념일 |
| `job` / `interest` | TEXT | | — | 직업 코드 / 관심분야 코드(`\|` 구분자) |
| `email` / `email_hash` | TEXT | | — | 이메일 (암호화) / 검색용 해시 |
| `cell_phone` / `cell_phone_hash` | TEXT | | — | 휴대폰 (암호화) / 검색용 해시(하이픈 제거 후) |
| `phone` | TEXT | | — | 전화번호 (암호화) |
| `fax` | TEXT | | — | 팩스번호 |
| `mailling_fl` / `sms_fl` | TEXT | ✅ | `y` | 메일/SMS 수신 여부 |
| `address` | TEXT | | — | 주소 전체 (우편번호 포함, 암호화) |
| `entry_dt` / `entry_path` | TEXT | entry_path ✅ | `pc` | 가입일 / 가입경로 |
| `last_login_dt` | TEXT | | — | 최종 로그인일 |
| `login_count` | INTEGER | ✅ | `0` | 방문횟수 |
| `sale_amount` / `mileage` / `deposit` | DOUBLE | ✅ | `0` | 주문금액 / 마일리지 / 예치금 |
| `recomm_id` | TEXT | | — | 추천인 아이디 |
| `ex1`~`ex6` | TEXT | | — | 에쓰푸드몰 추가 정보 (업종, 세금계산서 발행 여부, 담당자 등) |
| `expiration_fl` | INTEGER | ✅ | `1` | 개인정보 유효기간 |
| `memo` | TEXT | | — | 남기는말씀 |
| `deleted_fl` | TEXT | ✅ | `n` | 소프트 삭제 여부 |
| `synced_at` | TEXT | ✅ | — | 마지막 동기화(업로드 반영) 시각 |
| `created_at` / `updated_at` | TEXT | ✅ | — | 생성/수정 일시 |

---

### 🏢 godo_member_business

> 고도몰 회원 사업자 정보 테이블

**용도** — 사업자 정보(`company`/`business_no` 등)가 있는 회원의 사업자 정보를 저장한다. `mem_no` 1:1 관계이며 `godo_member`를 FK로 참조한다.

> ⚠️ **`group_sno`와 이 테이블의 존재 여부는 별개다.** 에쓰푸드몰 회원 수집은 미승인(`app_fl='n'`) 조건으로 이루어지며, 이 시점에는 사업자 회원이어도 `group_sno`가 기본값 `1`(일반회원)로 수집된다(`FetchNewMembersUseCase`도 `group_sno`가 아닌 `company`/`business_no` 값 유무로 저장 여부를 판단함). ERP 등록 후 관리자가 수동으로 `group_sno`를 사업자 등급(`2,3,5,6,8,9`)으로 변경하기 전까지는 `group_sno=1`이면서 이 테이블에 레코드가 존재하는 상태가 정상적으로 발생한다. **따라서 사업자 정보 노출 여부를 판단할 때 `group_sno`로 게이팅하면 안 되며, 반드시 이 테이블의 레코드 존재 여부로 판단해야 한다** (2026-07-08: `GetMemberDetailUseCase`가 `group_sno` 기준으로 게이팅하여 이 구간의 사업자 정보가 조회되지 않던 버그를 수정함).

**PII 암호화 컬럼** — `ceo_name`, `com_address`. `business_no`(사업자번호)는 평문 저장(암호화 제외 확정, `docs/analysis/godomall-member-encryption-design.md`)

```sql
CREATE TABLE IF NOT EXISTS godo_member_business (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no          BIGINT  NOT NULL UNIQUE,
    company         TEXT,
    business_no     TEXT,
    ceo_name        TEXT,
    service         TEXT,
    item            TEXT,
    com_address     TEXT,
    biz_license_file TEXT,
    duplicate_mem_no TEXT,
    service_code    TEXT,
    item_code       TEXT,
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL,
    FOREIGN KEY (mem_no) REFERENCES godo_member(mem_no)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_godo_member_business_mem_no
    ON godo_member_business(mem_no);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_business_no
    ON godo_member_business(business_no);
```

| 컬럼 | 타입 | NOT NULL | 설명 |
|---|---|---|---|
| `id` | INTEGER | ✅ | PK |
| `mem_no` | BIGINT | ✅ | `godo_member.mem_no` 참조, UNIQUE |
| `company` | TEXT | | 상호 |
| `business_no` | TEXT | | 사업자번호 `XXX-XX-XXXXX` (평문) |
| `ceo_name` | TEXT | | 대표자명 (암호화) |
| `service` / `service_code` | TEXT | | 업태 텍스트 / `common_code`(그룹코드 `SERVICE`) 매칭 코드 |
| `item` / `item_code` | TEXT | | 종목 텍스트 / `common_code`(그룹코드 `ITEM`) 매칭 코드 |
| `com_address` | TEXT | | 사업장 주소 전체 (우편번호 포함, 암호화) |
| `biz_license_file` | TEXT | | 사업자등록증 파일명 |
| `duplicate_mem_no` | TEXT | | 동일 `business_no`를 가진 다른 활성 회원의 `mem_no` 목록(`;` 구분). 에쓰푸드 어드민 자동 수집 배치(`FetchNewMembersUseCase`)에서만 갱신되며, 회원 상세 조회 API(`GET /api/members/{memNo}`) 응답의 `business.duplicateMemNo`와 회원 목록 조회 API(`GET /api/members`) 응답의 `content[].duplicateMemNo`로 노출됨 |
| `created_at` / `updated_at` | TEXT | ✅ | 생성/수정 일시 |

---

### 🤝 member_partner

> 에쓰푸드 자체 관리 거래처 정보 테이블

**용도** — 회원과 ERP 거래처를 연결하는 정보를 저장한다. `godo_member`와 `mem_no` 1:1 관계. TEMPLATE/LIST 업로드, 배치 자동 수집(신규 회원), 또는 `PUT /api/members/{memNo}/partner` 화면 저장 API에서 생성·수정된다.

**member_status 허용값** — `UNREGISTERED`(미등록: 그룹웨어를 통해 ERP에 거래처가 생성되지 않은 상태) / `NORMAL`(정상: 사업장 정보로 ERP 거래처 등록 완료) / `INACTIVE`(불용: ERP에서 거래처 사용 중지). 거래처 등록 여부와 상태를 이 컬럼 하나로 통합 관리한다(Req 12) — 자동 생성(배치·LIST 업로드) 시 기본값은 `UNREGISTERED`

**tax_invoice_fl 허용값** — `y` / `n`

```sql
CREATE TABLE IF NOT EXISTS member_partner (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no                BIGINT  NOT NULL UNIQUE,
    partner_code          TEXT,
    partner_name          TEXT,
    tax_invoice_fl        TEXT NOT NULL DEFAULT 'n',
    member_status         TEXT NOT NULL DEFAULT 'NORMAL',
    created_at            TEXT NOT NULL,
    updated_at            TEXT NOT NULL,
    FOREIGN KEY (mem_no) REFERENCES godo_member(mem_no)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_member_partner_mem_no
    ON member_partner(mem_no);
CREATE INDEX IF NOT EXISTS idx_member_partner_partner_code
    ON member_partner(partner_code);
CREATE INDEX IF NOT EXISTS idx_member_partner_member_status
    ON member_partner(member_status);
```

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `mem_no` | BIGINT | ✅ | — | `godo_member.mem_no` 참조, UNIQUE |
| `partner_code` | TEXT | | — | ERP 거래처 코드. 다른 활성 회원과 중복 불가(`PUT /partner` 저장 시 검증) |
| `partner_name` | TEXT | | — | 거래처명 |
| `tax_invoice_fl` | TEXT | ✅ | `n` | 세금계산서 발행 여부 |
| `member_status` | TEXT | ✅ | `NORMAL` | 거래처 상태 (`UNREGISTERED`/`NORMAL`/`INACTIVE`) |
| `created_at` / `updated_at` | TEXT | ✅ | — | 생성/수정 일시 |

> **2026-07-03 변경(Req 12)**: 기존 `partner_registered_fl`(등록 여부, y/n) 컬럼을 폐지하고 `member_status` 하나로 통합했다. 마이그레이션 시 `partner_registered_fl='y'` → `NORMAL`, 그 외 → `UNREGISTERED`로 값을 이관한 뒤 컬럼을 삭제한다 (`SqliteSchemaMigration`, `schema-member-postgresql.sql` 참조).

---

### 📜 godo_member_business_log

> 사업자 정보 변경 이력 로그 테이블

**용도** — `PUT /api/members/{memNo}/business` 저장 직전의 `godo_member_business` 스냅샷을 보관한다. FK 없이 `mem_no`만 보관하며, 원본 레코드가 삭제되어도 이력은 유지된다.

**PII 암호화 컬럼(AES-256-GCM, at rest)** — `ceo_name`, `com_address`. 원본 `godo_member_business` 테이블과 동일한 컬럼이 동일하게 암호화되어야 하며, `GodoMemberBusinessLogEntity`에도 `godo_member_business`와 동일한 `@Convert(EncryptedFieldConverter.class)`가 적용되어 있어야 한다 (2026-07-03 수정: 두 컬럼에 컨버터가 누락되어 평문 저장되던 결함 수정).

```sql
CREATE TABLE IF NOT EXISTS godo_member_business_log (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no           BIGINT  NOT NULL,
    company          TEXT,
    business_no      TEXT,
    ceo_name         TEXT,
    service          TEXT,
    item             TEXT,
    com_address      TEXT,
    biz_license_file TEXT,
    service_code     TEXT,
    item_code        TEXT,
    logged_at        TEXT NOT NULL,
    logged_by        TEXT,
    created_at       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_mem_no
    ON godo_member_business_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_logged_at
    ON godo_member_business_log(logged_at);
```

| 컬럼 | 타입 | NOT NULL | 설명 |
|---|---|---|---|
| `id` | INTEGER | ✅ | PK |
| `mem_no` | BIGINT | ✅ | 대상 회원 번호 (참조, FK 없음) |
| `company` ~ `item_code` | TEXT | | 변경 전 `godo_member_business` 스냅샷 (컬럼 구성 동일) |
| `logged_at` | TEXT | ✅ | 변경 발생(저장 직전) 일시 |
| `logged_by` | TEXT | | 변경 수행자 세션 이메일. 세션 정보 없으면 `NULL` |
| `created_at` | TEXT | ✅ | 로그 저장 일시 |

---

### 📜 member_partner_log

> 거래처 정보 변경 이력 로그 테이블

**용도** — `PUT /api/members/{memNo}/partner` 저장 직전의 `member_partner` 스냅샷을 보관한다. FK 없이 `mem_no`만 보관한다.

```sql
CREATE TABLE IF NOT EXISTS member_partner_log (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no                BIGINT  NOT NULL,
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
CREATE INDEX IF NOT EXISTS idx_member_partner_log_changed_at
    ON member_partner_log(logged_at);
```

| 컬럼 | 타입 | NOT NULL | 설명 |
|---|---|---|---|
| `id` | INTEGER | ✅ | PK |
| `mem_no` | BIGINT | ✅ | 대상 회원 번호 (참조, FK 없음) |
| `partner_code` ~ `member_status` | TEXT | | 변경 전 `member_partner` 스냅샷 (컬럼 구성 동일) |
| `logged_at` | TEXT | ✅ | 변경 발생(저장 직전) 일시 |
| `logged_by` | TEXT | | 변경 수행자 세션 이메일. 세션 정보 없으면 `NULL` |
| `created_at` | TEXT | ✅ | 로그 저장 일시 |

---

### 🔢 common_code

> 범용 그룹코드/코드 마스터 테이블

**용도** — 업태(`SERVICE`)·업종(`ITEM`) 등 그룹코드별 코드 목록을 관리한다. `CommonCodeMappingService`가 서버 기동 시 `use_yn='Y'` 코드를 인메모리 캐시로 적재하여, 회원 업로드·사업자 정보 저장 시 업태/업종 텍스트 → 코드 자동 매핑에 사용한다 (`GET/POST/PUT /api/common-codes`).

**use_yn 허용값** — `Y` / `N`

**초기 데이터** — `SERVICE`(업태) 79건, `ITEM`(업종) 6,372건

```sql
CREATE TABLE IF NOT EXISTS common_code (
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

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `group_code` | TEXT | ✅ | — | 그룹코드 (예: `SERVICE`, `ITEM`). `(group_code, code)` UNIQUE |
| `group_name` | TEXT | ✅ | — | 그룹코드명 (예: 업태, 업종) |
| `code` | TEXT | ✅ | — | 코드값 |
| `code_name` | TEXT | ✅ | — | 코드명. 텍스트 매칭 키(trim + lowercase) |
| `note` | TEXT | | — | 비고 |
| `use_yn` | TEXT | ✅ | `Y` | 사용 여부. `N`은 매핑 캐시에서 제외 |
| `created_at` / `updated_at` | TEXT | ✅ | — | 생성/수정 일시 |

---

## 🔤 공통 규칙

### 날짜/시간 저장 형식

SQLite는 네이티브 날짜 타입이 없으므로 `TEXT` 타입으로 ISO 8601 형식을 사용한다.

```
2026-03-31T10:00:00
```

### Boolean 저장 방식

SQLite는 BOOLEAN 타입이 없으므로 `INTEGER`로 저장한다.

| 값 | 의미 |
|---|---|
| `0` | false |
| `1` | true |

Y/N 구분이 필요한 컬럼(`sold_out_yn`, `godo_sync_yn` 등)은 `TEXT`(`Y`/`N`)로 저장한다.

### 인덱스 전략

| 패턴 | 인덱스 대상 |
|---|---|
| 품목 기준 이력 조회 | `godo_goods_code` |
| 날짜 범위 조회 | `changed_at`, `started_at`, `requested_at`, `sent_at` |
| 상태 기반 필터 | `status`, `erp_sync_yn`, `sold_out_yn` |
| 복합 조회 (날짜 + 품목) | `(changed_at, godo_goods_code)` |

---

## 📋 전체 테이블 목록 요약

| 테이블명 | 역할 | 초기 데이터 |
|---|---|---|
| `email_verification` | OTP 인증 세션 | — |
| `goods` | 품목 마스터 | 사용자 등록 |
| `goods_status_log` | 판매 상태 변경 이력 | — |
| `api_transaction_log` | 고도몰 API 송수신 이력 | — |
| `common_status_config` | 판매 상태 기준값 설정 | GENERAL / EXCLUSIVE 2건 |
| `batch_schedule_config` | 배치 스케줄 설정 | product-sync 1건 |
| `batch_execution_log` | 배치 실행 이력 | — |
| `batch_schedule_change_log` | 스케줄 변경 이력 | — |
| `email_send_log` | 이메일 발송 이력 | — |
| `godo_member` | 고도몰 회원 마스터 (엑셀 업로드 전용) | — |
| `godo_member_business` | 회원 사업자 정보 | — |
| `member_partner` | 회원-ERP 거래처 연결 정보 | — |
| `godo_member_business_log` | 사업자 정보 변경 이력 | — |
| `member_partner_log` | 거래처 정보 변경 이력 | — |
| `common_code` | 그룹코드/코드 마스터 (업태·업종 매핑) | SERVICE 79건 / ITEM 6,372건 |
