# SQLite Schema & ERD — SFOOD-MALL-AUTOMATION-BACKEND

> 에쓰푸드몰 품목 판매 상태 자동화 백엔드 서비스  
> Database: SQLite  
> 작성일: 2026-03-31

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

### 📦 goods

> 품목 마스터 정보 테이블

**용도** — 에쓰푸드몰에서 판매되는 품목의 핵심 정보를 관리한다. 고도몰 코드와 ERP 코드를 연결하는 맵핑 마스터 역할을 한다.

**status 허용값** — `NORMAL` / `UNVERIFIED` / `ERROR` / `HIDE`

**only_mall_yn 허용값** — `Y` (전용, EXCLUSIVE) / `N` (공용, GENERAL)

**sold_out_yn 허용값** — `Y` (품절) / `N` (정상)

**godo_sync_yn 허용값** — `Y` (고도몰 API 검증 완료) / `N` (미검증)

**erp_sync_yn 허용값** — `Y` (WMS 재고 매핑 확인) / `N` (미확인 또는 오류)

```sql
CREATE TABLE goods (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    godo_goods_code TEXT    NOT NULL UNIQUE,
    erp_goods_code  TEXT    NOT NULL UNIQUE,
    goods_name      TEXT    NOT NULL,
    only_mall_yn    TEXT    NOT NULL DEFAULT 'N',
    sold_out_yn     TEXT    NOT NULL DEFAULT 'N',
    available_qty   INTEGER NOT NULL DEFAULT 0,
    safety_rate     INTEGER NOT NULL DEFAULT 0,
    godo_sync_yn    TEXT    NOT NULL DEFAULT 'N',
    erp_sync_yn     TEXT    NOT NULL DEFAULT 'N',
    description     TEXT,
    message         TEXT,
    status          TEXT    NOT NULL DEFAULT 'UNVERIFIED',
    created_id      TEXT    NOT NULL,
    created_at      TEXT    NOT NULL,
    updated_id      TEXT,
    updated_at      TEXT
);

CREATE UNIQUE INDEX uk_goods_godo_goods_code ON goods(godo_goods_code);
CREATE UNIQUE INDEX uk_goods_erp_goods_code  ON goods(erp_goods_code);

CREATE INDEX idx_goods_status       ON goods(status);
CREATE INDEX idx_goods_sold_out_yn  ON goods(sold_out_yn);
CREATE INDEX idx_goods_erp_sync_yn  ON goods(erp_sync_yn);
```

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `godo_goods_code` | TEXT | ✅ | — | 고도몰 품목 코드, UNIQUE |
| `erp_goods_code` | TEXT | ✅ | — | ERP 품목 코드, UNIQUE (WMS ITEM_CD 매핑 키) |
| `goods_name` | TEXT | ✅ | — | 품목명 |
| `only_mall_yn` | TEXT | ✅ | `N` | 전용 구분 (Y=전용/N=공용) |
| `sold_out_yn` | TEXT | ✅ | `N` | 품절 여부 |
| `available_qty` | INTEGER | ✅ | `0` | 가용수량 기준 (EA), 공통 설정에서 초기화 |
| `safety_rate` | INTEGER | ✅ | `0` | 안전재고 기준 (%), 공통 설정에서 초기화 |
| `godo_sync_yn` | TEXT | ✅ | `N` | 고도몰 API 연동 확인 여부 |
| `erp_sync_yn` | TEXT | ✅ | `N` | WMS ERP 코드 매핑 확인 여부 |
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
| `valid_percent` | INTEGER | | WMS 잔여율 (%) |
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

**용도** — 전용(EXCLUSIVE) / 공용(GENERAL) scope 별로 품절/정상 전환 기준값을 저장한다. 품목 등록 시 이 테이블의 값을 읽어 goods의 `available_qty`, `safety_rate`를 초기화한다.

**scope 허용값** — `GENERAL` (공용) / `EXCLUSIVE` (전용)

```sql
CREATE TABLE common_status_config (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    scope            TEXT    NOT NULL UNIQUE,
    out_of_stock_qty INTEGER NOT NULL,
    on_sale_qty      INTEGER NOT NULL,
    safety_rate      INTEGER NOT NULL DEFAULT 50,
    description      TEXT,
    updated_id       TEXT,
    updated_at       TEXT    NOT NULL
);

CREATE UNIQUE INDEX uk_common_status_config_scope ON common_status_config(scope);
```

**초기 데이터**

| scope | out_of_stock_qty | on_sale_qty | safety_rate | 설명 |
|---|---|---|---|---|
| `GENERAL` | 20 | 21 | 50 | 공용 품목 기준 |
| `EXCLUSIVE` | 1 | 2 | 50 | 전용 품목 기준 |

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | INTEGER | ✅ | AUTOINCREMENT | PK |
| `scope` | TEXT | ✅ | — | 적용 범위 (UNIQUE) |
| `out_of_stock_qty` | INTEGER | ✅ | — | 품절 전환 기준 가용재고 이하값 (EA) |
| `on_sale_qty` | INTEGER | ✅ | — | 정상 전환 기준 가용재고 이상값 (EA) |
| `safety_rate` | INTEGER | ✅ | `50` | 정상 전환 기준 잔여율 이상값 (%) |
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
