# SRS — SFOOD-MALL-AUTOMATION-BACKEND

> Software Requirements Specification  
> 에쓰푸드몰 품목 판매 상태 자동화 백엔드 서비스  
> 작성 기준: 백엔드 개발 범위 한정  
> 작성일: 2026-03-31

---

## 📋 문서 개요

### 목적

본 문서는 에쓰푸드몰 품목 판매 상태 자동화 백엔드 서비스(`SFOOD-MALL-AUTOMATION-BACKEND`)의 기능 요구 명세를 정의한다. WMS 재고 데이터를 기반으로 에쓰푸드몰(고도몰) 품목의 품절/정상 상태를 자동으로 업데이트하는 시스템을 구현하기 위한 백엔드 요구사항을 기술한다.

### 범위

- **포함**: 백엔드 REST API, 배치 스케줄러, WMS 스크랩핑, 고도몰 API 연동, OTP 인증, 세션 관리, 품목 관리, 상태 변경 로그, 알림 기능
- **제외**: 프론트엔드 UI, 수동 배치 실행 API (프론트엔드 연동 부분), OMS/WMS 직접 주문 취소 처리

### 프로젝트 정보

| 항목 | 내용 |
|---|---|
| 서버명 | SFOOD-MALL-AUTOMATION-BACKEND |
| Git Repository | sfood-it-dev-ax-org/SFOOD-MALL-AUTOMATION-BACKEND |
| 언어/런타임 | Java 21 (Virtual Threads) |
| 프레임워크 | Spring Boot 3.5 |
| 빌드 도구 | Gradle 8.1 |
| 데이터베이스 | SQLite |

---

## 🏗️ 시스템 아키텍처 개요

### 기술 스택

| 분류 | 기술/라이브러리 | 버전 | 용도 |
|---|---|---|---|
| 언어 | Java | JDK 21 | 기본 개발 언어, Virtual Threads 활용 |
| 프레임워크 | Spring Boot | 3.5 | REST API 서버 기반 |
| 빌드 도구 | Gradle | 8.1 | 의존성 관리 및 빌드 |
| ORM | Spring Data JPA / Hibernate | 최신 | 엔티티-DB 매핑, CRUD |
| 쿼리 | QueryDSL | 최신 | 동적 쿼리 작성 |
| 세션 | Spring Session | 최신 | 서버 세션 저장/관리 (24시간 만료) |
| 연결 풀 | HikariCP | 내장 | DB 커넥션 풀 관리 |
| 유효성 검사 | Spring Validation | 내장 | Request DTO 검증 |
| 코드 간소화 | Lombok | 최신 | Boilerplate 코드 최소화 |
| 스케줄러 | Spring TaskScheduler | 내장 | Quartz 없이 동적 스케줄 관리 |
| 메일 | Spring Mail | 내장 | OTP 및 알림 이메일 발송 |
| 오류 알림 | Slack Webhook | - | 오류 발생 시 Slack 채널 알림 |

### 아키텍처 원칙

- **Clean Architecture** 기반 레이어 분리 (Presentation / Application / Domain / Infrastructure)
- **SOLID 원칙** 준수
- **도메인 중심 설계** — 비즈니스 로직은 Domain 레이어에만 존재
- 테스트 코드 우선 원칙 (TDD), JUnit 5 기반

---

## 🗄️ 데이터 모델 전체 구성

### 테이블 목록

| 테이블명 | 용도 |
|---|---|
| `email_verification` | OTP 이메일 인증 세션 |
| `goods` | 품목 마스터 정보 |
| `goods_status_log` | 품목 판매 상태 변경 로그 |
| `api_transaction_log` | 고도몰 API 송수신 로그 |
| `batch_schedule_config` | 배치 스케줄 설정 |
| `batch_execution_log` | 배치 실행 이력 로그 |
| `batch_schedule_change_log` | 스케줄 변경 이력 로그 |
| `common_status_config` | 공통 품목 판매 상태 검증 기준 |
| `email_send_log` | 이메일 발송 이력 로그 |

---

## 🔐 기능 요구사항 — OTP 이메일 인증

### 개요

사내 특정 인원만 접근 가능하도록 회사 이메일(`@s-food.com`) 기반 OTP 인증을 구현한다. 사용자 계정 DB를 관리하지 않으며, 이메일로 발송한 6자리 숫자 코드를 통해 일시적 인증 세션을 발급한다.

### 기능 요구사항

#### FR-AUTH-001 이메일 인증 요청

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `POST /api/auth/email-verifications` |
| 입력 | 이메일 주소 |
| 처리 | 이메일 형식 검증 → 회사 도메인 검증 → Rate Limit 검사 → OTP 생성 → 해시 저장 → 메일 발송 |
| 출력 | `verificationKey` (UUID v4), 만료 시간(초) |
| 응답 코드 | 201 Created / 400 Bad Request / 429 Too Many Requests |

**세부 규칙**

- 이메일은 `trim()` 적용 후 소문자 정규화
- 회사 메일 도메인 `@s-food.com` 만 허용
- 이메일 형식 정규식 검증 필수
- OTP는 `SecureRandom` 기반 6자리 숫자 문자열 생성 (`String`, 앞자리 0 허용)
- OTP 원문은 DB에 저장 금지 — 해시값만 저장
- 해시 계산 방식: `SHA-256(verificationKey + ":" + code + ":" + serverSecret)`
- 60초 이내 동일 이메일 재요청 시 `429` 반환
- 1시간 내 동일 이메일 5회 초과 발송 시 `429` 반환
- 기존 `PENDING` 상태 세션은 `CANCELLED` 처리 후 새 세션 발급
- OTP 유효 시간: 5분
- 최대 시도 횟수: 5회

#### FR-AUTH-002 이메일 인증 확인

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `POST /api/auth/email-verifications/confirm` |
| 입력 | `verificationKey`, 이메일, OTP 코드 |
| 처리 | 세션 조회 → 이메일 일치 확인 → 만료 여부 → 상태 확인 → 시도 횟수 → 해시 비교 → 세션 발급 |
| 출력 | 인증 성공 여부, 이메일 |
| 응답 코드 | 200 OK / 400 Bad Request / 401 Unauthorized / 409 Conflict / 410 Gone / 429 Too Many Requests |

**세부 규칙**

- `verificationKey` 기준으로 DB 조회
- 저장된 이메일과 요청 이메일 일치 여부 확인
- `expiresAt` 기준 만료 여부 확인 → 만료 시 `410` 반환
- 상태가 `PENDING`이 아닌 경우 `409` 반환
- `attemptCount >= maxAttemptCount` 초과 시 `429` 반환 및 `FAILED` 처리
- 입력 코드를 동일 방식으로 해시 계산 후 `codeHash`와 비교
- 코드 불일치 시 `attemptCount` 증가 후 `401` 반환
- 인증 성공 시 `status = VERIFIED`, `verifiedAt` 기록
- 인증 성공 시 Spring Session 생성, `Set-Cookie` 헤더 응답

#### FR-AUTH-003 세션 조회

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `GET /api/auth/session` |
| 처리 | 쿠키의 세션 유효성 확인 |
| 응답 코드 | 200 OK (유효) / 401 Unauthorized (무효 또는 만료) |

**세부 규칙**

- Spring Session + HttpOnly Cookie 기반
- 세션 유효 기간: 24시간
- 모든 보호 API 요청 시 세션 쿠키 포함 필수 (`credentials: include`)

### 도메인 모델 — EmailVerification

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | PK |
| `verificationKey` | String | UUID v4, UNIQUE |
| `email` | String | 소문자 정규화 저장 |
| `codeHash` | String | SHA-256 해시값 |
| `status` | VerificationStatus | PENDING / VERIFIED / EXPIRED / FAILED / CANCELLED |
| `attemptCount` | int | 현재 시도 횟수 |
| `maxAttemptCount` | int | 최대 허용 시도 횟수 (기본 5) |
| `requestedAt` | LocalDateTime | 요청 일시 |
| `expiresAt` | LocalDateTime | 만료 일시 |
| `verifiedAt` | LocalDateTime | 인증 완료 일시 |
| `lastSentAt` | LocalDateTime | 마지막 발송 일시 |
| `requestIp` | String | 요청 IP |
| `userAgent` | String | 요청 User-Agent |
| `createdAt` | LocalDateTime | 생성 일시 |
| `updatedAt` | LocalDateTime | 수정 일시 |

### DB 스키마 — email_verification

```sql
CREATE TABLE email_verification (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    verification_key   TEXT NOT NULL UNIQUE,
    email              TEXT NOT NULL,
    code_hash          TEXT NOT NULL,
    status             TEXT NOT NULL DEFAULT 'PENDING',
    attempt_count      INTEGER NOT NULL DEFAULT 0,
    max_attempt_count  INTEGER NOT NULL DEFAULT 5,
    requested_at       TEXT NOT NULL,
    expires_at         TEXT NOT NULL,
    verified_at        TEXT,
    last_sent_at       TEXT NOT NULL,
    request_ip         TEXT,
    user_agent         TEXT,
    created_at         TEXT NOT NULL,
    updated_at         TEXT NOT NULL
);

CREATE UNIQUE INDEX uk_email_verification_verification_key
    ON email_verification(verification_key);
CREATE INDEX idx_email_verification_email_status
    ON email_verification(email, status);
CREATE INDEX idx_email_verification_expires_at
    ON email_verification(expires_at);
```

---

## 📦 기능 요구사항 — 품목 정보 관리

### 개요

에쓰푸드몰에서 판매되는 품목을 등록·수정·상태 변경하고, 고도몰 API 연동을 통해 품목 정보의 유효성을 검증한다.

### 기능 요구사항

#### FR-GOODS-001 품목 등록

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `POST /api/goods` |
| 입력 | 고도몰 품목 코드, ERP 품목 코드, 품목명, 전용 구분(Y/N) |
| 처리 | 중복 체크 → 고도몰 API 조회 → 품목 정보 저장 |

**세부 규칙**

- 고도몰 품목 코드(`GODO_GOODS_CODE`) 중복 체크 → 중복 시 `"이미 등록된 품목입니다!"` 메시지 반환
- ERP 품목 코드(`ERP_GOODS_CODE`) 중복 체크 → 중복 시 동일 메시지 반환
- 중복 체크 시 상태가 `HIDE`인 품목인 경우 → `NORMAL` 상태로 복구 후 정상 등록과 동일 응답
- 중복이 아닌 경우 고도몰 품목 조회 API 호출하여 품목 존재 여부 확인
- 고도몰 API 정상 응답 시 `GODO_SYNC_YN = Y` 업데이트
- 고도몰 API 응답의 `soldOutFl` 값으로 품절 여부(`SOLD_OUT_YN`) 초기화
- 신규 등록 시 공통 판매 상태 설정(`common_status_config`) 테이블 기준으로 `AVAILABLE_QTY`, `SAFETY_RATE` 설정
- 등록자 이메일(`CREATED_ID`) 세션에서 추출하여 저장

#### FR-GOODS-002 품목 수정

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `PUT /api/goods/{goodsId}` |
| 수정 가능 필드 | 품목명, ERP 코드, 전용 구분 |
| 수정 불가 필드 | 고도몰 코드, 판매 상태 (고도몰 연동 데이터) |

**세부 규칙**

- 전용 구분 변경 시 해당 품목의 `AVAILABLE_QTY`, `SAFETY_RATE`를 공통 설정 테이블 기준으로 자동 업데이트
- 수정자 이메일(`UPDATED_ID`) 세션에서 추출하여 저장

#### FR-GOODS-003 품목 상태 변경 (Hide / 삭제)

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `DELETE /api/goods/{goodsId}` |

**세부 규칙**

- `GODO_SYNC_YN = Y` (고도몰 검증 완료 품목) → 물리 삭제 불가, `STATUS = HIDE`로 변경
- `GODO_SYNC_YN = N` (미검증 품목, 잘못된 등록) → 물리 삭제 처리

#### FR-GOODS-004 품목 목록 조회

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `GET /api/goods` |
| 조회 조건 | 품절 여부, 전용 구분, 상태, 품목명/코드 검색 |
| 기본 필터 | `STATUS != HIDE` |

#### FR-GOODS-005 품목 단건 조회

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `GET /api/goods/{goodsId}` |
[sfood-sqlite-schema-erd.md](..%2F..%2F..%2Fsfood-sqlite-schema-erd.md)
### 도메인 모델 — Goods

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | PK |
| `godoGoodsCode` | String | 고도몰 품목 코드 (UNIQUE) |
| `erpGoodsCode` | String | ERP 품목 코드 (UNIQUE) |
| `goodsName` | String | 품목명 |
| `onlyMallYn` | String | 전용 구분 (Y=전용/N=공용) |
| `soldOutYn` | String | 품절 여부 (Y/N) |
| `availableQty` | Integer | 가용수량 기준 (EA) |
| `safetyRate` | Integer | 안전재고 기준 (%) |
| `godoSyncYn` | String | 고도몰 연동 확인 (Y/N) |
| `erpSyncYn` | String | ERP 품목 확인 (Y/N) |
| `description` | String | 비고 |
| `message` | String | 검증 관련 메시지 |
| `status` | GoodsStatus | NORMAL / UNVERIFIED / ERROR / HIDE |
| `createdId` | String | 생성 사용자 이메일 |
| `createdAt` | LocalDateTime | 생성 일시 |
| `updatedId` | String | 수정 사용자 이메일 |
| `updatedAt` | LocalDateTime | 수정 일시 |

### DB 스키마 — goods

```sql
CREATE TABLE goods (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    godo_goods_code  TEXT NOT NULL UNIQUE,
    erp_goods_code   TEXT NOT NULL UNIQUE,
    goods_name       TEXT NOT NULL,
    only_mall_yn     TEXT NOT NULL DEFAULT 'N',
    sold_out_yn      TEXT NOT NULL DEFAULT 'N',
    available_qty    INTEGER NOT NULL DEFAULT 0,
    safety_rate      INTEGER NOT NULL DEFAULT 0,
    godo_sync_yn     TEXT NOT NULL DEFAULT 'N',
    erp_sync_yn      TEXT NOT NULL DEFAULT 'N',
    description      TEXT,
    message          TEXT,
    status           TEXT NOT NULL DEFAULT 'UNVERIFIED',
    created_id       TEXT NOT NULL,
    created_at       TEXT NOT NULL,
    updated_id       TEXT,
    updated_at       TEXT
);

CREATE UNIQUE INDEX uk_goods_godo_goods_code ON goods(godo_goods_code);
CREATE UNIQUE INDEX uk_goods_erp_goods_code  ON goods(erp_goods_code);
CREATE INDEX idx_goods_status                ON goods(status);
CREATE INDEX idx_goods_sold_out_yn           ON goods(sold_out_yn);
```

---

## 🔄 기능 요구사항 — 공통 판매 상태 설정

### 개요

`analysis-limit.md`의 품절/정상 판단 기준을 DB로 관리한다. 전용(EXCLUSIVE) / 공용(GENERAL) 구분별로 가용수량 기준 및 안전재고율을 설정한다.

### 기능 요구사항

#### FR-CONFIG-001 공통 설정 조회

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `GET /api/config/status` |
| 응답 | scope 별 가용수량 기준, 안전재고율 기준 |

#### FR-CONFIG-002 공통 설정 수정

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `PUT /api/config/status` |
| 입력 | scope, 가용수량 기준(EA), 안전재고율 기준(%) |

### 판매 상태 판단 로직

**사전 조건 — 평가 제외 대상**

- Zone 코드가 숫자가 아닌 경우 → SKIP
- Zone 코드가 제외 코드 목록에 포함된 경우 → SKIP  
  (제외 코드: `EXT`, `MSC`, `PRC`, `RCV`, `SHP`, `WOR`, `DMG`, `RTN`)
- 창고 코드가 `OA110`이 아닌 경우 → SKIP

**품절 전환 조건 (`ON_SALE → OUT_OF_STOCK`)**

| Scope | 조건 |
|---|---|
| 공용 (GENERAL) | 합산 가용재고 ≤ 20 EA |
| 전용 (EXCLUSIVE) | 합산 가용재고 ≤ 1 EA |

**정상 전환 조건 (`OUT_OF_STOCK → ON_SALE`) — AND 조건**

| Scope | 조건 A | 조건 B |
|---|---|---|
| 공용 (GENERAL) | 비냉동: 잔여율 기준 충족 OR 냉동: 잔여일수 기준 충족, AND Lot 존재 | 합산 가용재고 ≥ 21 EA |
| 전용 (EXCLUSIVE) | 비냉동: 잔여율 기준 충족 OR 냉동: 잔여일수 기준 충족, AND Lot 존재 | 합산 가용재고 ≥ 2 EA |

**처리 원칙**

- 품목별 Lot 번호(`LOT_NO`)가 분리되어 있으므로 반드시 합산하여 판단
- 상태 전환은 현재 상태(`currentStatus`) 기준으로만 해당 방향 검사
- 멱등성 보장 — 동일 입력으로 중복 실행 시 상태 변경 없음
- SKIP 대상 품목은 반드시 로그에 이유 기록

### DB 스키마 — common_status_config

```sql
CREATE TABLE common_status_config (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    scope            TEXT NOT NULL UNIQUE,
    out_of_stock_qty INTEGER NOT NULL,
    on_sale_qty      INTEGER NOT NULL,
    safety_rate      INTEGER NOT NULL DEFAULT 50,
    description      TEXT,
    updated_id       TEXT,
    updated_at       TEXT NOT NULL
);
```

---

## 🌐 기능 요구사항 — 고도몰 API 연동

### 개요

고도몰(에쓰푸드몰) Open API를 통해 품목 정보를 조회하고, 품절/정상 상태를 변경한다. API 송수신 이력은 전량 DB에 저장한다.

### 기능 요구사항

#### FR-GODO-001 고도몰 품목 조회

| 항목 | 내용 |
|---|---|
| 외부 엔드포인트 | `POST https://openhub.godo.co.kr/godomall5/goods/Goods_Search.php` |
| 인증 파라미터 | `partner_key`, `key` |
| 조회 기준 | 고도몰 품목 코드(`goodsNo`) |
| 응답 형식 | XML |

**세부 규칙**

- API 키는 환경설정 파일에 저장, 코드 외부 노출 금지
- 응답 `<code>000</code>` 이면 정상
- 정상 응답 시 `GODO_SYNC_YN = Y`, `soldOutFl` 값으로 품절 여부 동기화
- `<code>119</code>` (존재하지 않는 상품) 시 재시도 금지
- API 호출 전 Request 데이터 저장, 응답 후 Response 데이터 및 결과 저장

#### FR-GODO-002 고도몰 품절 상태 변경

| 항목 | 내용 |
|---|---|
| 외부 엔드포인트 | `POST https://openhub.godo.co.kr/godomall5/goods/Goods_Soldout_Status.php` |
| 파라미터 | `goodsNo` (상품코드), `soldoutFl` (y=품절 / n=정상) |

**세부 규칙**

- 상태 변화가 감지된 품목만 API 전송 (불필요한 중복 호출 방지)
- Rate Limit: 1초 100회 제한 — 응답 헤더 `ratelimit-available-level: EXHAUSTED` 감지 시 즉시 중단
- 429 응답 수신 시 백오프(back-off) 후 재시도
- 정상 응답(`code=000`) 시에만 서비스 품목 상태 업데이트
- 오류 응답 시 상태 변경 없음, 로그에 오류 기록

#### FR-GODO-003 API 송수신 로그 저장

**세부 규칙**

- 모든 고도몰 API 호출 전 Request 정보 저장
- 응답 수신 후 Response 정보 및 결과 업데이트
- 1건의 API 호출 = 1개의 로그 레코드

### DB 스키마 — api_transaction_log

```sql
CREATE TABLE api_transaction_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    api_type        TEXT NOT NULL,
    godo_goods_code TEXT NOT NULL,
    request_url     TEXT NOT NULL,
    request_body    TEXT,
    response_body   TEXT,
    result_code     TEXT,
    result_message  TEXT,
    is_success      INTEGER NOT NULL DEFAULT 0,
    requested_at    TEXT NOT NULL,
    responded_at    TEXT,
    created_at      TEXT NOT NULL
);

CREATE INDEX idx_api_transaction_log_godo_goods_code ON api_transaction_log(godo_goods_code);
CREATE INDEX idx_api_transaction_log_requested_at    ON api_transaction_log(requested_at);
CREATE INDEX idx_api_transaction_log_api_type        ON api_transaction_log(api_type);
```

---

## 📡 기능 요구사항 — WMS 재고 스크랩핑

### 개요

WMS 시스템에 브라우저 방식으로 접근하여 재고 현황 데이터를 수집한다. 수집 데이터는 저장하지 않고 배치 작업에서 즉시 사용한다.

### 기능 요구사항

#### FR-WMS-001 WMS 로그인 처리

| 항목 | 내용 |
|---|---|
| 방식 | HTTP 클라이언트로 로그인 URL 호출하여 세션 쿠키 획득 |
| 로그인 URL | `https://wms.s-food.com/sessionService/login?CURRENT_MENUCODE=LOGIN&SELLANGBEFORELOGIN=KO&USERID={USER_ID}&LOGIN_USERID={USER_ID}&PASSWORD={PASSWORD}` |
| 실행 주기 | 배치 실행마다 매번 로그인 수행 (세션 만료 문제 방지) |

**세부 규칙**

- `USER_ID`, `PASSWORD`는 환경설정 파일에 저장 (스크랩핑 전용 계정)
- 로그인 성공 시 세션 쿠키를 재고 현황 조회에 사용

#### FR-WMS-002 재고 현황 수집

| 항목 | 내용 |
|---|---|
| 조회 URL | `https://wms.s-food.com/invnTotListService/searchAllStock` |
| 통신 방식 | HTTP POST (세션 쿠키 포함) |
| 응답 형식 | JSON |

**Request URL 파라미터 (Query String — 고정값)**

| 파라미터 | 값 | 설명 |
|---|---|---|
| `CURRENT_MENUCODE` | `WMS141123` | 메뉴 코드 (고정) |
| `PAGE_SIZE` | `10000` | 페이지당 조회 건수 (고정) |
| `CURRENT_PAGE` | `1` | 페이지 번호 (고정) |
| `SES_LANG` | `KO` | 언어 코드 (고정) |
| `SES_USERGROUP` | `Observer` | 사용자 그룹 (고정) |
| `SES_WHSE` | `SLP001` | 창고(센터) 코드 (고정, 환경설정) |
| `SES_MULTI_LANG_YN` | `N` | 다국어 여부 (고정) |

**Request Body Payload (JSON — 고정 검색 조건)**

WMS 재고현황 조회 시 아래 고정 JSON 페이로드를 함께 전송한다. 센터 코드(SLP001)와 창고 코드(OA110) 필터를 서버 측에 전달하는 검색 조건이다.

```json
{
  "DS_SEARCHCONDITION": [
    {
      "ROW_STATUS": "normal",
      "val0": "WH_CD",
      "val1": "WH_CD",
      "val2": "=",
      "val3": "SLP001",
      "val4": "COMBO"
    },
    {
      "ROW_STATUS": "normal",
      "val0": "LOT_ATTR_1",
      "val1": "WAREHOUSE_LABEL",
      "val2": "IN",
      "val3": "OA110",
      "val4": "POP"
    }
  ]
}
```

**검색 조건 파라미터 설명**

| 필드 | 설명 |
|---|---|
| `val0` | 검색 대상 컬럼 식별자 |
| `val1` | 실제 DB 컬럼명 |
| `val2` | 비교 연산자 (`=`, `IN` 등) |
| `val3` | 검색 값 — `SLP001` (센터코드), `OA110` (창고코드) |
| `val4` | UI 컴포넌트 타입 (`COMBO`, `POP` 등) |

> `val3`의 `SLP001`, `OA110` 값은 환경설정 파일로 관리하며 코드에 하드코딩 금지

**Response 주요 필드 (`DS_OUT` 배열 내 각 항목)**

| 필드명 | 설명 | 비고 |
|---|---|---|
| `ITEM_CD` | ERP 품목 코드 | 서비스 품목 매핑 키 |
| `ITEM_NM` | 품목명 | |
| `INVN_QTY` | 가용재고 수량 | LOT별 분리, 합산 필요 |
| `VALID_PERCENT` | 잔여율 (%) | 정상 전환 조건 판단에 사용 |
| `TEMP_TCD` | 온도 유형 | `FREEZE` 이면 잔여일수 기준 적용 |
| `LOT_NO` | 로트 번호 | 품목별 복수 LOT 존재 가능 |
| `LOT_ATTR_1` | 창고 코드 | `OA110` 고정 대상 |
| `ZONE_CD` | 존 코드 | 제외 존 코드 필터링 기준 |
| `LOT_HLD_SCD` | 재고 상태 | |
| `WH_CD` | 센터 코드 | `SLP001` 고정 |
| `VALID_DATETIME` | 유통기한 | |
| `REMAIN_VALID_DAYS` | 잔여 유효일수 | `TEMP_TCD=FREEZE` 인 경우 기준 판단에 사용 |
| `DS_TOTALCOUNT` | 전체 조회 건수 | |
| `ErrorMsg` | 오류 메시지 | `"OK"` 이면 정상 |
| `ErrorCode` | 오류 코드 | `0` 이면 정상 |

**세부 규칙**

- 수집 데이터는 DB에 저장하지 않음 (실시간 데이터)
- `ErrorCode != 0` 또는 `ErrorMsg != "OK"` 인 경우 배치 작업 중단 후 오류 알림
- 등록된 품목의 `ERP_GOODS_CODE`와 매핑되지 않는 재고 데이터는 즉시 제거 (처리 부하 방지)
- 동일 `ITEM_CD`에 대해 `LOT_NO` 별로 분리된 `INVN_QTY`를 반드시 합산하여 판단
- zone 허용 여부를 먼저 확인한 뒤, LOT 합산 전에 row 기준 필터를 적용
  - `TEMP_TCD = FREEZE` → `goods.REMAIN_VALID_DAYS` 기준 사용
  - 그 외 → `goods.SAFETY_RATE` 기준 사용
- Request Payload의 `val3` 값(`SLP001`, `OA110`)은 환경설정 파일로 관리

#### FR-WMS-003 ERP 코드 검증 및 재고 데이터 정제

배치 작업에서 판매 상태 변경 처리 시작 **전**에 반드시 아래 두 단계를 순서대로 실행한다.

**[Step 1] ERP 코드 검증 — `ERP_SYNC_YN = N` 품목 대상 한정**

매 배치 실행 시 전체 goods를 대상으로 ERP 코드를 검증하면 수집된 재고 데이터 전체를 반복 탐색하는 부하가 발생한다. 따라서 `ERP_SYNC_YN = N`인 품목만 대상으로 검증을 수행한다.

처리 흐름:

```
goods 테이블에서 ERP_SYNC_YN = N 인 품목 목록 조회
  │
  ├─ 대상 없음 → Step 1 종료, Step 2로 이동
  │
  └─ 대상 있음 → 수집된 WMS 재고 데이터에서 ITEM_CD 기준으로 탐색
        │
        ├─ 재고에 ITEM_CD 존재 → ERP_SYNC_YN = Y, STATUS = NORMAL, MESSAGE 초기화
        │
        └─ 재고에 ITEM_CD 없음 → ERP_SYNC_YN = N 유지, STATUS = ERROR 유지
                                   MESSAGE = "WMS 재고에서 ERP 코드를 찾을 수 없습니다"
```

세부 규칙:

- 검증 대상은 `ERP_SYNC_YN = N`인 품목만으로 한정 (매 배치마다 전체 품목 재검증 금지)
- WMS 재고 데이터 탐색 시 `ITEM_CD` 값이 goods의 `ERP_GOODS_CODE`와 일치하는지 비교
- 재고 데이터에서 매핑 성공 시: `ERP_SYNC_YN = Y`, `STATUS = NORMAL`, `MESSAGE = null`로 업데이트
- 재고 데이터에서 매핑 실패 시: `ERP_SYNC_YN = N` 유지, `STATUS = ERROR`, `MESSAGE`에 오류 원인 기록
- `STATUS = ERROR` 품목은 이후 판매 상태 변경 대상에서 제외

**[Step 2] 재고 데이터 정제 — 미등록 품목 제거**

Step 1 완료 후, 수집된 전체 WMS 재고 데이터에서 goods 테이블에 등록되지 않은 품목의 재고를 메모리상에서 제거한다. 이 단계는 판매 상태 변경 처리의 정확성과 성능을 동시에 보장하기 위해 필수로 실행한다.

처리 흐름:

```
goods 테이블 전체 ERP_GOODS_CODE 목록 조회 (Set으로 관리)
  │
  └─ WMS 재고 데이터 순회
        │
        ├─ ITEM_CD 가 ERP_GOODS_CODE Set에 존재 → 유지
        │
        └─ ITEM_CD 가 ERP_GOODS_CODE Set에 없음 → 재고 데이터에서 제거 (메모리)
```

세부 규칙:

- 정제는 메모리상 수집 데이터에서만 수행하며 WMS 원본 데이터에는 영향 없음
- Step 2 완료 후 남은 재고 데이터만 판매 상태 변경 로직에 전달
- Step 1(ERP 검증)을 Step 2(정제) 이전에 반드시 먼저 실행해야 `ERP_SYNC_YN = N → Y` 복구된 품목이 정제 단계에서 누락되지 않음

---

## ⏰ 기능 요구사항 — 배치 스케줄러

### 개요

Spring TaskScheduler 기반으로 Quartz 없이 동적 스케줄 관리를 구현한다. 서비스 재시작 없이 배치 주기를 변경할 수 있으며, 중복 실행을 방지한다.

### 기능 요구사항

#### FR-BATCH-001 자동 배치 작업 실행

**배치 작업 순서**

```
[1] WMS 로그인 시도
      │
[2] WMS 재고현황 전체 데이터 수집
      │
      ├─ ErrorCode != 0 또는 ErrorMsg != "OK" → 배치 중단, 오류 알림
      │
[3] ERP 코드 검증 (FR-WMS-003 Step 1)
      └─ goods 중 ERP_SYNC_YN = N 인 품목만 대상
           ├─ WMS 재고에서 ITEM_CD 매핑 성공 → ERP_SYNC_YN = Y, STATUS = NORMAL
           └─ 매핑 실패 → STATUS = ERROR 유지, MESSAGE 기록
      │
[4] 재고 데이터 정제 (FR-WMS-003 Step 2)
      └─ goods 전체 ERP_GOODS_CODE 기준으로 미등록 품목 재고 메모리에서 제거
      │
[5] 정상(ON_SALE) 상태 품목 중 품절 조건 충족 품목 추출
      └─ STATUS = NORMAL, SOLD_OUT_YN = N 인 품목 대상
      │
[6] 고도몰 품절 상태 변경 API 전송 (건별)
      │
[7] API 송수신 로그 저장
      │
[8] 정상 응답 확인 후 goods SOLD_OUT_YN = Y 업데이트
      │
[9] 품절(OUT_OF_STOCK) 상태 품목 중 정상 조건 충족 품목 추출
      └─ STATUS = NORMAL, SOLD_OUT_YN = Y 인 품목 대상
      │
[10] 고도몰 정상 상태 변경 API 전송 (건별)
      │
[11] API 송수신 로그 저장
      │
[12] 정상 응답 확인 후 goods SOLD_OUT_YN = N 업데이트
      │
[13] 상태 변경 로그 저장 (goods_status_log)
      │
[14] 변경 이력 이메일 전송 (변경 건수 > 0 인 경우)
```

**세부 규칙**

- 실행 간격: Fixed Delay 방식 (기본 10분, 최소 60초 이상)
- `AtomicBoolean` 기반 단일 서버 중복 실행 방지
- 이미 실행 중인 경우 → `SKIPPED` 로그 저장 후 종료
- ERP 코드 검증([3])과 재고 데이터 정제([4])는 반드시 판매 상태 변경([5]~[12]) 이전에 실행
- `STATUS = ERROR` 품목은 [5], [9] 단계 대상에서 제외
- 배치 실패 시 스케줄 자체는 유지 (자동 취소 없음)
- 실행 로그 DB 저장 필수 (`SCHEDULER` 트리거 구분)

#### FR-BATCH-002 동적 스케줄 변경

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `PUT /api/admin/batch-schedules/{jobName}` |
| 변경 가능 항목 | 활성화 여부, 실행 간격(초), 초기 지연(초) |

**세부 규칙**

- 서비스 재시작 없이 런타임 스케줄 변경 가능
- DB 저장 → 기존 스케줄 취소 → 새 설정으로 재등록
- `fixedDelaySeconds` 최소 60초 미만 입력 시 `400` 반환
- 변경 이력 `batch_schedule_change_log` 저장
- 서버 재기동 시 `enabled = true` 설정 자동 복원 (`ApplicationRunner`)

#### FR-BATCH-003 배치 상태 조회

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `GET /api/admin/batch-schedules/{jobName}` |
| 응답 | 설정 정보, 실행 상태, 최근 실행 결과 |

#### FR-BATCH-004 수동 배치 실행 트리거

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `POST /api/admin/batch-schedules/{jobName}/run` |
| 처리 | 즉시 배치 작업 실행 |

**세부 규칙**

- 현재 실행 중이면 `SKIPPED` 처리 후 응답
- `triggeredBy = MANUAL` 로그 저장

### 도메인 모델 — BatchScheduleConfig

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | PK |
| `jobName` | String | 잡 식별자 (UNIQUE) |
| `enabled` | boolean | 활성화 여부 |
| `scheduleType` | BatchScheduleType | FIXED_DELAY |
| `fixedDelaySeconds` | int | 실행 간격 (최소 60초) |
| `initialDelaySeconds` | int | 초기 지연 시간 (기본 0) |
| `description` | String | 설명 |
| `lastModifiedBy` | String | 마지막 수정자 |
| `createdAt` | LocalDateTime | 생성 일시 |
| `updatedAt` | LocalDateTime | 수정 일시 |

### 도메인 모델 — BatchExecutionLog

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | PK |
| `jobName` | String | 잡 식별자 |
| `startedAt` | LocalDateTime | 시작 일시 |
| `endedAt` | LocalDateTime | 종료 일시 |
| `status` | BatchExecutionStatus | SUCCESS / FAIL / SKIPPED |
| `message` | String | 결과 요약 메시지 |
| `triggeredBy` | String | SCHEDULER / MANUAL |
| `durationMs` | Long | 실행 소요 시간 (ms) |
| `createdAt` | LocalDateTime | 저장 일시 |

### DB 스키마 — batch_schedule_config

```sql
CREATE TABLE batch_schedule_config (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name              TEXT NOT NULL UNIQUE,
    enabled               INTEGER NOT NULL DEFAULT 1,
    schedule_type         TEXT NOT NULL DEFAULT 'FIXED_DELAY',
    fixed_delay_seconds   INTEGER NOT NULL,
    initial_delay_seconds INTEGER NOT NULL DEFAULT 0,
    description           TEXT,
    last_modified_by      TEXT,
    created_at            TEXT NOT NULL,
    updated_at            TEXT NOT NULL
);
```

### DB 스키마 — batch_execution_log

```sql
CREATE TABLE batch_execution_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name     TEXT NOT NULL,
    started_at   TEXT NOT NULL,
    ended_at     TEXT,
    status       TEXT NOT NULL,
    message      TEXT,
    triggered_by TEXT NOT NULL,
    duration_ms  INTEGER,
    created_at   TEXT NOT NULL
);

CREATE INDEX idx_batch_execution_log_job_name   ON batch_execution_log(job_name);
CREATE INDEX idx_batch_execution_log_started_at ON batch_execution_log(started_at);
CREATE INDEX idx_batch_execution_log_status     ON batch_execution_log(status);
```

### DB 스키마 — batch_schedule_change_log

```sql
CREATE TABLE batch_schedule_change_log (
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name                 TEXT NOT NULL,
    before_fixed_delay_sec   INTEGER,
    after_fixed_delay_sec    INTEGER,
    before_enabled           INTEGER,
    after_enabled            INTEGER,
    changed_by               TEXT,
    changed_at               TEXT NOT NULL
);

CREATE INDEX idx_batch_schedule_change_log_job_name ON batch_schedule_change_log(job_name);
```

---

## 📝 기능 요구사항 — 품목 상태 변경 로그

### 개요

배치 작업에서 고도몰 API 호출을 통해 품목 상태가 변경될 때 상세 근거 데이터를 로그로 저장한다.

### 기능 요구사항

#### FR-LOG-001 상태 변경 로그 저장

**저장 정보**

- 고도몰 품목 코드
- ERP 품목 코드
- WMS 합산 가용재고 수량
- 판단 기준 가용재고 수량
- 안전재고 기준율 (%)
- 잔여율 증거값 (%)
- 잔여일수 증거값
- 변경 전 상태
- 변경 후 상태
- 변경 일시

**세부 규칙**

- API 정상 응답 확인 후 로그 저장
- 날짜별, 품목별 조회를 위한 인덱스 필수

#### FR-LOG-002 상태 변경 로그 조회

| 항목 | 내용 |
|---|---|
| 요청 엔드포인트 | `GET /api/goods/status-logs` |
| 조회 조건 | 날짜 범위, 고도몰 품목 코드 |

### DB 스키마 — goods_status_log

```sql
CREATE TABLE goods_status_log (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    godo_goods_code       TEXT NOT NULL,
    erp_goods_code        TEXT NOT NULL,
    wms_available_qty     INTEGER NOT NULL,
    criteria_qty          INTEGER NOT NULL,
    safety_rate           INTEGER NOT NULL,
    valid_percent         INTEGER,
    before_status         TEXT NOT NULL,
    after_status          TEXT NOT NULL,
    changed_at            TEXT NOT NULL,
    created_at            TEXT NOT NULL
);

CREATE INDEX idx_goods_status_log_godo_goods_code ON goods_status_log(godo_goods_code);
CREATE INDEX idx_goods_status_log_changed_at      ON goods_status_log(changed_at);
```

---

## 📧 기능 요구사항 — 이메일 알림

### 개요

배치 작업에서 품목 상태 변경이 발생하면 변경 내역을 이메일로 전송한다.

### 기능 요구사항

#### FR-MAIL-001 배치 결과 이메일 전송

**세부 규칙**

- 배치 실행 후 상태 변경 로그 기준으로 테이블 형태로 메일 발송
- 상태 변경이 없을 경우 발송 생략 가능 (설정으로 제어)
- 발송 이력은 `email_send_log` 테이블에 저장

**메일 환경 설정**

```yaml
spring:
  mail:
    host: {HOST}
    port: 25
    username: {USERNAME}
    properties:
      mail:
        smtp:
          auth: false
        starttls:
          enable: true
```

### DB 스키마 — email_send_log

```sql
CREATE TABLE email_send_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    send_type    TEXT NOT NULL,
    recipient    TEXT NOT NULL,
    subject      TEXT NOT NULL,
    is_success   INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    sent_at      TEXT NOT NULL,
    created_at   TEXT NOT NULL
);

CREATE INDEX idx_email_send_log_sent_at   ON email_send_log(sent_at);
CREATE INDEX idx_email_send_log_send_type ON email_send_log(send_type);
```

---

## 🚨 기능 요구사항 — 오류 알림 (Slack)

### 개요

API, 배치 작업, DB 작업 등 전체 서비스에서 발생하는 오류를 Slack 채널로 실시간 알림한다.

### 기능 요구사항

#### FR-SLACK-001 Slack 오류 알림

**세부 규칙**

- 모든 `try ~ catch` 블록의 Exception 발생 시 Slack 메시지 전송
- Logback Slack Appender 또는 Webhook 방식으로 구현
- 알림 포함 정보: 오류 발생 위치, 오류 메시지, 발생 시각
- 애플리케이션 로그 파일에도 동시 기록

**Slack Webhook 환경 설정**

```yaml
slack:

```

---

## 🏗️ 아키텍처 레이어 설계

### 전체 레이어 구조

```
Presentation Layer       REST Controller, Request/Response DTO
        │
Application Layer        UseCase, 트랜잭션 경계, 흐름 제어
        │
Domain Layer             Entity, DomainService, Policy, Enum, Repository Interface
        │
Infrastructure Layer     JPA Repository 구현체, 외부 API 클라이언트, 스케줄러, 메일, Slack
```

### 패키지 구조 (권장)

```
com.sfood.automation
├── auth
│   ├── presentation
│   │   └── EmailVerificationController
│   ├── application
│   │   ├── usecase
│   │   │   ├── RequestEmailVerificationUseCase
│   │   │   └── ConfirmEmailVerificationUseCase
│   │   └── dto
│   ├── domain
│   │   ├── model
│   │   │   └── EmailVerification
│   │   ├── policy
│   │   │   └── OtpPolicy
│   │   └── enums
│   │       └── VerificationStatus
│   └── infrastructure
│       ├── persistence
│       ├── otp
│       │   ├── OtpGenerator
│       │   └── OtpHashProvider
│       ├── mail
│       │   └── MailSender
│       └── session
│           └── SessionIssuer
├── goods
│   ├── presentation
│   │   └── GoodsController
│   ├── application
│   │   └── usecase
│   ├── domain
│   │   ├── model
│   │   │   └── Goods
│   │   └── enums
│   │       └── GoodsStatus
│   └── infrastructure
│       ├── persistence
│       └── external
│           └── GodoApiClient
├── batch
│   ├── presentation
│   │   └── BatchScheduleAdminController
│   ├── application
│   │   └── usecase
│   │       ├── GetBatchScheduleUseCase
│   │       ├── UpdateBatchScheduleUseCase
│   │       └── RunBatchNowUseCase
│   ├── domain
│   │   ├── model
│   │   │   ├── BatchScheduleConfig
│   │   │   └── BatchExecutionLog
│   │   ├── policy
│   │   │   └── BatchSchedulePolicy
│   │   └── enums
│   │       ├── BatchJobType
│   │       ├── BatchExecutionStatus
│   │       └── BatchScheduleType
│   └── infrastructure
│       ├── persistence
│       ├── scheduler
│       │   ├── DynamicSchedulerManager
│       │   └── TaskSchedulerConfig
│       └── job
│           ├── ProductSyncBatchJob
│           └── BatchExecutionService
├── wms
│   └── infrastructure
│       └── scraping
│           ├── WmsLoginClient
│           └── WmsStockClient
└── common
    ├── config
    ├── exception
    │   └── GlobalExceptionHandler
    ├── slack
    │   └── SlackNotifier
    └── security
        └── SessionAuthInterceptor
```

---

## 🔒 비기능 요구사항

### 보안

| 항목 | 요구사항 |
|---|---|
| OTP 원문 | DB 저장 절대 금지 |
| OTP 생성기 | `SecureRandom` 강제 사용 (`Random` 금지) |
| API 키 | 환경설정 파일 저장, 코드 및 Git 노출 금지 |
| 세션 | HttpOnly Cookie, 24시간 만료 |
| WMS 인증 정보 | 환경설정 파일 저장, 스크랩핑 전용 계정 사용 |
| 회사 메일 | `@s-food.com` 도메인만 허용 |

### 성능

| 항목 | 요구사항 |
|---|---|
| Virtual Threads | Java 21 Virtual Threads 활용으로 I/O 처리 효율화 |
| WMS 데이터 필터링 | 서비스 등록 품목 외 재고 데이터는 즉시 제거 (처리 부하 감소) |
| 고도몰 API Rate Limit | 1초 100회 제한 준수, EXHAUSTED 헤더 감지 시 선제 중단 |
| DB 인덱스 | 날짜별·품목별 조회 패턴에 맞는 인덱스 필수 구성 |

### 신뢰성

| 항목 | 요구사항 |
|---|---|
| 배치 중복 실행 | `AtomicBoolean` 기반 단일 인스턴스 중복 실행 방지 |
| 배치 실패 복원 | 배치 실패 시 스케줄 유지, 자동 재시작 없음 |
| API 호출 멱등성 | 동일 상태 품목에 중복 API 호출 금지 |
| 서버 재기동 | `ApplicationRunner`로 스케줄 자동 복원 |

### 운영성

| 항목 | 요구사항 |
|---|---|
| 로그 | 모든 오류 Application 로그 파일 기록 |
| Slack 알림 | 오류 발생 시 즉시 Slack 채널 알림 |
| 스케줄 변경 | 서비스 재시작 없이 런타임 주기 변경 가능 |
| 환경 설정 | 민감 정보 전체 환경설정 파일로 분리 관리 |

### 코드 품질

| 항목 | 요구사항 |
|---|---|
| 설계 원칙 | SOLID, Clean Architecture 준수 |
| 코드 스타일 | Clean Code 기반 |
| 테스트 | TDD — 비즈니스 로직 구현 전 테스트 코드 우선 작성 |
| Mock 사용 | 테스트 코드 이외 Mock 사용 금지 |
| 외부 의존 | 불필요한 라이브러리 의존성 추가 금지 (Quartz 제외) |

---

## 🔗 외부 시스템 연동 명세

### 고도몰 Open API

| 항목 | 내용 |
|---|---|
| Base URL (Real) | `https://openhub.godo.co.kr` |
| Base URL (Sandbox) | `http://sbopenhub.godo.co.kr` |
| 통신 방식 | HTTP POST |
| 인코딩 | UTF-8 |
| 응답 형식 | XML |
| Rate Limit | 1초 100회 이하 |

### WMS 시스템

| 항목 | 내용 |
|---|---|
| 기관 | 에쓰푸드 물류창고 WMS |
| 접근 방식 | HTTP 클라이언트 (브라우저 스크랩핑 방식) |
| 인증 | 세션 기반 (매 배치 실행 시 로그인) |
| 응답 형식 | JSON |
| 창고 코드 | OA110 (물류창고) 고정 |

### Slack

| 항목 | 내용 |
|---|---|
| 방식 | Incoming Webhook |
| 용도 | 오류 발생 실시간 알림 |

### 이메일 (SMTP)

| 항목 | 내용 |
|---|---|
| 용도 | OTP 발송, 배치 변경 이력 알림 |
| 포트 | 25 |
| 인증 | 없음 (STARTTLS 사용) |

---

## 📌 제약사항 및 가정

### 제약사항

- Quartz 스케줄러 사용 금지
- 단일 서버 환경 전제 (분산 환경 미지원)
- WMS 재고 데이터는 DB에 영속화하지 않음
- OTP 원문은 DB에 저장하지 않음
- 고도몰 코드와 판매 상태는 API 연동을 통해서만 확인 가능 (직접 수정 불가)
- Mock 객체는 테스트 코드에서만 사용

### 가정

- WMS에는 스크랩핑 전용 계정이 사전에 생성되어 있음
- 고도몰 API 키(`partner_key`, `key`)는 사전에 발급되어 있음
- Slack Webhook URL은 사전에 생성되어 있음
- 서버는 단일 인스턴스로 운영
- 배치 작업 중 수동 실행 요청이 동시에 들어올 수 있음

---

## 📄 산출물 계획

| 문서 | 설명 |
|---|---|
| SRS (본 문서) | 기능 요구 명세서 |
| PRD | 제품 요구사항 문서 (비즈니스 목표, 우선순위) |
| Architecture Document | 시스템 아키텍처 상세 설계 |
| API Specification | REST API 엔드포인트 상세 명세 (OpenAPI 3.0) |
| DB Schema | 전체 테이블 ERD 및 DDL |
| Test Specification | JUnit 5 기반 테스트 시나리오 목록 |
