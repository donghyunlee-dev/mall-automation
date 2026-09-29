# PRD — SFOOD-MALL-AUTOMATION-BACKEND

> Product Requirements Document  
> 에쓰푸드몰 품목 판매 상태 자동화 백엔드 서비스  
> 참조 문서: SRS / Architecture / sfood-sqlite-schema-erd  
> 작성일: 2026-03-31

---

## 📋 문서 목적

본 문서는 백엔드 개발을 **Task 단위**로 분리하여, 각 Task가 독립적으로 개발·테스트·완료 가능한 상태로 진행되도록 정의한다. AI Agent가 본 문서를 기반으로 순서대로 구현할 수 있도록 각 Task 안에 필요한 모든 정보(구현 대상, 클래스 목록, 테스트 시나리오, 완료 기준)를 명시한다.

---

## 🧭 Codex 문서 정합성 규칙

본 문서는 다음 문서와 함께 해석해야 하며, 구현 시작 전에 모두 읽고 정합성을 확인해야 한다.

- `AGENTS.md`
- `docs/guide/development-rules.md`
- `docs/guide/testing-strategy.md`
- `docs/guide/done-criteria.md`

정합성 원칙은 다음과 같다.

- 사용자 요청이 최우선이다.
- 본 문서의 범위를 초과하여 구현하지 않는다.
- 테스트를 먼저 작성한 뒤 구현한다.
- 구조 개선이 필요해 보여도 현재 Task 범위를 넘으면 구현하지 않는다.
- 완료 기준 충족 후 추가 작업 없이 종료한다.

---

## 🗺️ 전체 Task 로드맵

| Task | 명칭 | 핵심 목표 | 의존 |
|---|---|---|---|
| Task 01 | 프로젝트 기반 설정 | 프로젝트 초기화, 공통 인프라, DB 스키마 | 없음 |
| Task 02 | OTP 인증 도메인 | 이메일 OTP 발송·검증, 세션 발급 | Task 01 |
| Task 03 | 품목 관리 도메인 | 품목 CRUD, 공통 판매 상태 기준 설정 | Task 02 |
| Task 04 | 고도몰 API 연동 | 품목 조회·품절 상태 변경 API 클라이언트 | Task 03 |
| Task 05 | WMS 재고 스크랩핑 | WMS 로그인·재고수집·ERP 검증·데이터 정제 | Task 03 |
| Task 06 | 판매 상태 판단 엔진 | 품절/정상 전환 도메인 정책, 상태 변경 처리 | Task 04, Task 05 |
| Task 07 | 배치 스케줄러 | 동적 스케줄 관리, 배치 실행 엔진 통합 | Task 06 |
| Task 08 | 알림 및 로그 | 이메일 결과 알림, Slack 오류 알림 | Task 07 |
| Task 09 | 통합 테스트 및 마무리 | E2E 통합 테스트, 운영 설정 검증 | Task 08 |

---

## 🔧 공통 개발 원칙

모든 Task에서 반드시 준수한다.

**설계 원칙**
- Clean Architecture 레이어 의존 방향 준수 (Presentation → Application → Domain ← Infrastructure)
- SOLID 원칙 적용
- 비즈니스 로직은 Domain Layer 안에만 작성
- Repository는 Domain Layer에 Interface(Port)로 선언, Infrastructure Layer에서 구현
- 비즈니스 로직은 구체 구현이 아닌 인터페이스에 의존한다

**코드 원칙**
- Clean Code 기반 작성
- Lombok 사용 (`@Getter`, `@Builder`, `@RequiredArgsConstructor` 등)
- Mock 객체는 테스트 코드에서만 허용
- 모든 외부 의존 정보는 `application.yml` 환경변수로 분리
- Pulumi 또는 CloudFormation Description은 영문으로 작성한다

**테스트 원칙**
- 구현 코드 작성 전 테스트 코드 먼저 작성 (TDD)
- Domain / Policy → 순수 JUnit 5 (Mock 없음)
- UseCase → JUnit 5 + Mockito
- Controller → `@WebMvcTest`
- Repository → `@DataJpaTest`
- 외부 API는 테스트에서 실제 호출하지 않고 Stub, Mock, Fake 중 테스트 전용 대역으로 검증한다

**완료 기준 (Definition of Done)**
- 해당 Task의 모든 클래스 구현 완료
- 명시된 테스트 시나리오 전체 통과
- `./gradlew test` 전체 GREEN
- 환경변수 미주입 시 서버 기동 실패 확인

---

## 🚫 고도몰 품목 상태 변경 API 승인 규칙

다음 규칙은 본 문서 전체에 공통 적용한다.

대상 API:
- `Goods_Soldout_Status.php`

필수 규칙:
- 이 API는 자동 테스트에서 호출하지 않는다.
- 이 API는 통합 테스트에서 호출하지 않는다.
- 이 API의 실제 호출은 반드시 사전 승인 후 진행한다.
- 승인 전에는 Stub 또는 Mock 기반으로만 검증한다.
- 실제 상태 변경이 발생할 수 있으므로, Codex는 승인 없이 이 API를 실행하는 테스트 코드나 검증 코드를 작성하면 안 된다.

문서 내에 품절 상태 변경 관련 테스트 시나리오가 있더라도, 실제 호출 테스트가 아니라 **대역 객체 기반 검증**으로 해석해야 한다.

---

## Task 01 — 프로젝트 기반 설정

### 목표

Spring Boot 프로젝트를 초기화하고, 공통 인프라(예외 처리, 보안 인터셉터, Slack 알림, DB 연결)와 전체 DB 스키마를 한 번에 구성한다. 이후 모든 Task이 이 기반 위에서 진행된다.

### 구현 대상

**프로젝트 초기화**

```
build.gradle 의존성 구성
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-validation
  - spring-boot-starter-mail
  - spring-session-core
  - sqlite-jdbc:3.47.1.0
  - hibernate-community-dialects
  - querydsl-jpa:5.1.0:jakarta
  - jackson-dataformat-xml
  - lombok
  - spring-boot-starter-test

application.yml 구성
  - SQLite datasource (환경변수: DB_PATH)
  - JPA: hibernate.ddl-auto=validate
  - Spring Session: timeout=86400s
  - Virtual Threads: spring.threads.virtual.enabled=true
  - SMTP 설정 (환경변수: MAIL_HOST, MAIL_USERNAME)
  - Godo API 설정 (환경변수: GODO_PARTNER_KEY, GODO_KEY)
  - WMS 설정 (환경변수: WMS_USER_ID, WMS_PASSWORD)
  - Slack 설정 (환경변수: SLACK_WEBHOOK_URL)
  - OTP 설정 (환경변수: OTP_SERVER_SECRET)
```

**DB 스키마 초기화**

`schema.sql` 파일로 전체 9개 테이블 DDL 및 인덱스 생성 (sfood-sqlite-schema-erd 기준)

```
email_verification          + 인덱스 3개
goods                       + 인덱스 4개
goods_status_log            + 인덱스 3개
api_transaction_log         + 인덱스 3개
common_status_config        + 인덱스 1개
batch_schedule_config       + 인덱스 1개
batch_execution_log         + 인덱스 3개
batch_schedule_change_log   + 인덱스 2개
email_send_log              + 인덱스 2개
```

초기 데이터 `data.sql`

```sql
-- common_status_config 초기값
INSERT INTO common_status_config (scope, out_of_stock_qty, on_sale_qty, safety_rate, remain_valid_days, updated_at)
VALUES ('GENERAL', 20, 21, 50, 150, datetime('now'));
INSERT INTO common_status_config (scope, out_of_stock_qty, on_sale_qty, safety_rate, remain_valid_days, updated_at)
VALUES ('EXCLUSIVE', 1, 2, 50, 150, datetime('now'));

-- batch_schedule_config 초기값
INSERT INTO batch_schedule_config
  (job_name, enabled, schedule_type, fixed_delay_seconds, initial_delay_seconds, description, created_at, updated_at)
VALUES
  ('product-sync', 1, 'FIXED_DELAY', 600, 0, '품목 판매 상태 자동 동기화', datetime('now'), datetime('now'));
```

**공통 모듈 구현**

```
com.sfood.automation.common
├── config
│   ├── WebMvcConfig                CORS, SessionAuthInterceptor 등록
│   ├── JpaConfig                   SQLite Dialect, WAL 모드 PRAGMA 설정
│   └── HttpClientConfig            RestClient Bean 등록 (WMS용, Godo용 분리)
├── security
│   └── SessionAuthInterceptor      세션 쿠키 검증 인터셉터
│                                   화이트리스트: POST /api/auth/**, GET /api/auth/session
├── exception
│   ├── BusinessException           도메인 예외 기반 클래스 (RuntimeException 상속)
│   ├── ErrorCode                   Enum — 예외 코드 및 기본 메시지 정의
│   ├── ErrorResponse               표준 오류 응답 DTO {code, message, timestamp}
│   └── GlobalExceptionHandler      @RestControllerAdvice 전역 예외 핸들러
└── slack
    └── SlackNotifier               Webhook POST 전송 (오류 위치, 메시지, 시각 포함)
```

**예외 클래스 목록**

```
BusinessException (기반)
├── EmailFormatException        → 400
├── RateLimitException          → 429
├── OtpExpiredException         → 410
├── OtpMismatchException        → 401
├── OtpAlreadyUsedException     → 409
├── GoodsNotFoundException      → 404
├── DuplicateGoodsException     → 409
├── GodoApiException            → 502
├── WmsScrapingException        → 502
└── ScheduleValidationException → 400
```

### 테스트 시나리오

**SessionAuthInterceptor**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T1-1 | 화이트리스트 경로(`POST /api/auth/email-verifications`) 요청 | 세션 없이 통과 |
| T1-2 | 세션 없이 보호 경로(`GET /api/goods`) 요청 | 401 반환 |
| T1-3 | 유효한 세션 쿠키로 보호 경로 요청 | 다음 핸들러로 통과 |
| T1-4 | 만료된 세션 쿠키로 보호 경로 요청 | 401 반환 |

**GlobalExceptionHandler**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T1-5 | `BusinessException` 하위 예외 발생 | ErrorCode에 정의된 HTTP 상태 및 메시지 반환 |
| T1-6 | 처리되지 않은 `Exception` 발생 | 500 반환, ErrorResponse 구조 |

**SlackNotifier**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T1-7 | 오류 메시지 전송 시 Webhook URL로 POST 호출 | payload에 오류 위치, 메시지, 시각 포함 |

**DB 스키마**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T1-8 | 서버 기동 시 9개 테이블 생성 확인 | 테이블 존재 쿼리 성공 |
| T1-9 | common_status_config 초기 데이터 2건 확인 | GENERAL, EXCLUSIVE 존재 |
| T1-10 | batch_schedule_config 초기 데이터 1건 확인 | product-sync 존재 |

### 완료 기준

- 서버 정상 기동 (환경변수 전체 주입 상태)
- SQLite DB 파일 생성 및 9개 테이블 확인
- `./gradlew test` GREEN
- 화이트리스트 외 모든 경로 세션 없이 401 반환 확인

---

## Task 02 — OTP 인증 도메인

### 목표

이메일 OTP 발송·검증·세션 발급 전체 흐름을 구현한다. 보안 원칙(OTP 원문 미저장, SHA-256 해시, SecureRandom)을 도메인 레이어에서 완전히 캡슐화한다.

### 의존성 확인

Task 01 완료 필수 — `email_verification` 테이블, `SessionAuthInterceptor`, `GlobalExceptionHandler`

### 구현 대상

```
com.sfood.automation.auth
├── presentation
│   └── EmailVerificationController
│       POST /api/auth/email-verifications       → requestVerification()
│       POST /api/auth/email-verifications/confirm → confirmVerification()
│       GET  /api/auth/session                   → checkSession()
│
├── application
│   ├── usecase
│   │   ├── RequestEmailVerificationUseCase
│   │   │     execute(email) → EmailVerificationResponse
│   │   └── ConfirmEmailVerificationUseCase
│   │         execute(verificationKey, email, code) → ConfirmVerificationResponse
│   └── dto
│       ├── EmailVerificationRequest    {email}
│       ├── EmailVerificationResponse   {verificationKey, expiresInSeconds, message}
│       ├── ConfirmVerificationRequest  {verificationKey, email, code}
│       └── ConfirmVerificationResponse {authenticated, email}
│
├── domain
│   ├── model
│   │   └── EmailVerification
│   │         필드: id, verificationKey, email, codeHash, status,
│   │                attemptCount, maxAttemptCount, requestedAt, expiresAt,
│   │                verifiedAt, lastSentAt, requestIp, userAgent,
│   │                createdAt, updatedAt
│   │         메서드:
│   │           validate()              만료·상태·횟수 검증, 예외 발생
│   │           incrementAttempt()      attemptCount 증가, 최대 초과 시 FAILED 전이
│   │           markVerified()          VERIFIED 상태 전이, verifiedAt 기록
│   │           cancel()               CANCELLED 상태 전이
│   │           isExpired()            expiresAt 기준 만료 여부
│   │           isPending()            status == PENDING 여부
│   │
│   ├── policy
│   │   └── OtpPolicy
│   │         validateEmailDomain(email)     @s-food.com 검증 → EmailFormatException
│   │         checkResendLimit(lastSentAt)   60초 이내 재요청 → RateLimitException
│   │         checkHourlyLimit(email, repo)  1시간 5회 초과 → RateLimitException
│   │
│   ├── port
│   │   └── EmailVerificationRepository     Interface
│   │         save(EmailVerification)
│   │         findByVerificationKey(key)
│   │         findPendingByEmail(email)
│   │         countByEmailAndRequestedAtAfter(email, from)
│   │
│   └── enums
│       └── VerificationStatus   PENDING / VERIFIED / EXPIRED / FAILED / CANCELLED
│
└── infrastructure
    ├── persistence
    │   └── EmailVerificationRepositoryImpl   JpaRepository 구현
    ├── otp
    │   ├── OtpGenerator
    │   │     generate() → String
    │   │     구현: String.format("%06d", secureRandom.nextInt(1_000_000))
    │   └── OtpHashProvider
    │         hash(verificationKey, code, serverSecret) → String
    │         matches(verificationKey, code, serverSecret, storedHash) → boolean
    │         구현: SHA-256(key + ":" + code + ":" + secret)
    ├── mail
    │   └── OtpMailSender
    │         send(email, code)
    │         제목: "인증번호 안내"
    │         본문: 6자리 코드, 5분 유효
    └── session
        └── SessionIssuer
              issue(HttpServletRequest, email)   HttpSession 생성, email 저장
              invalidate(HttpServletRequest)     세션 무효화
```

### API 명세

**POST /api/auth/email-verifications**

```
Request  { "email": "user@s-food.com" }
Response 201 { "verificationKey": "uuid-v4", "expiresInSeconds": 300 }
         400 { "code": "EMAIL_FORMAT_ERROR", "message": "..." }
         429 { "code": "RATE_LIMIT_EXCEEDED", "message": "..." }
```

**POST /api/auth/email-verifications/confirm**

```
Request  { "verificationKey": "uuid-v4", "email": "user@s-food.com", "code": "348271" }
Response 200 { "authenticated": true, "email": "user@s-food.com" }
         401 { "code": "OTP_MISMATCH", "message": "..." }
         409 { "code": "OTP_ALREADY_USED", "message": "..." }
         410 { "code": "OTP_EXPIRED", "message": "..." }
         429 { "code": "RATE_LIMIT_EXCEEDED", "message": "..." }
```

**GET /api/auth/session**

```
Response 200 { "email": "user@s-food.com" }
         401 (세션 없음 또는 만료)
```

### 테스트 시나리오

**OtpGenerator**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T2-1 | generate() 호출 | 6자리 숫자 문자열 반환 |
| T2-2 | 앞자리 0인 경우 (`"000123"`) | String 타입으로 보존, 정수 변환 금지 |
| T2-3 | 100회 반복 생성 | 모두 6자리, 숫자만 구성 확인 |

**OtpHashProvider**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T2-4 | 동일 입력으로 hash() 2회 호출 | 동일 해시값 반환 |
| T2-5 | 다른 code로 hash() 호출 | 다른 해시값 반환 |
| T2-6 | matches() — 올바른 code 입력 | true 반환 |
| T2-7 | matches() — 틀린 code 입력 | false 반환 |

**OtpPolicy**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T2-8 | `@s-food.com` 이메일 검증 | 통과 |
| T2-9 | 외부 도메인 이메일 검증 | `EmailFormatException` 발생 |
| T2-10 | 60초 이내 재요청 검사 | `RateLimitException` 발생 |
| T2-11 | 61초 이후 재요청 검사 | 통과 |

**EmailVerification (Domain Model)**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T2-12 | 만료된 세션에서 validate() 호출 | `OtpExpiredException` 발생 |
| T2-13 | VERIFIED 상태에서 validate() 호출 | `OtpAlreadyUsedException` 발생 |
| T2-14 | attemptCount = maxAttemptCount 상태에서 incrementAttempt() | status = FAILED 전이 |
| T2-15 | markVerified() 호출 | status = VERIFIED, verifiedAt 기록 |

**RequestEmailVerificationUseCase**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T2-16 | 정상 이메일 입력 | verificationKey 반환, DB 해시값만 저장 확인 |
| T2-17 | 기존 PENDING 세션 존재 시 재요청 | 기존 세션 CANCELLED, 새 세션 생성 |
| T2-18 | OTP 원문이 DB에 저장되지 않음 | codeHash 컬럼만 존재, 원문 없음 |

**ConfirmEmailVerificationUseCase**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T2-19 | 올바른 code 입력 | authenticated=true, 세션 생성 |
| T2-20 | 틀린 code 입력 | 401, attemptCount+1 |
| T2-21 | 5회 초과 입력 | 429, status=FAILED |
| T2-22 | 만료 후 입력 | 410 반환 |

**EmailVerificationController (@WebMvcTest)**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T2-23 | 이메일 누락 요청 | 400 반환 |
| T2-24 | code 필드가 7자리 문자열 | 400 반환 |
| T2-25 | 정상 요청 | 201 반환, verificationKey 존재 |

### 완료 기준

- OTP 발송 → 수신 → 인증 → 세션 발급 전체 흐름 동작 확인
- DB에 OTP 원문이 존재하지 않음 확인
- 인증 완료 후 보호 API 호출 시 정상 통과 확인
- `./gradlew test` GREEN

---

## Task 03 — 품목 관리 도메인

### 목표

품목 마스터 CRUD와 공통 판매 상태 기준 설정 API를 구현한다. 고도몰 API 연동은 Task 04에서 추가되므로, 이 단계에서는 `GodoApiPort`를 Stub으로 처리한다.

### 의존성 확인

Task 02 완료 필수 — 세션 인증 동작 확인

### 구현 대상

```
com.sfood.automation.goods
├── presentation
│   ├── GoodsController
│   │     POST   /api/goods              → registerGoods()
│   │     PUT    /api/goods/{id}         → updateGoods()
│   │     DELETE /api/goods/{id}         → deleteGoods()
│   │     GET    /api/goods              → getGoodsList()
│   │     GET    /api/goods/{id}         → getGoods()
│   │     GET    /api/goods/status-logs  → getStatusLogs()
│   └── CommonStatusConfigController
│         GET /api/config/status         → getConfig()
│         PUT /api/config/status         → updateConfig()
│
├── application
│   ├── usecase
│   │   ├── RegisterGoodsUseCase
│   │   ├── UpdateGoodsUseCase
│   │   ├── DeleteGoodsUseCase
│   │   ├── GetGoodsListUseCase
│   │   ├── GetGoodsUseCase
│   │   ├── GetGoodsStatusLogUseCase
│   │   ├── GetCommonStatusConfigUseCase
│   │   └── UpdateCommonStatusConfigUseCase
│   └── dto
│       ├── RegisterGoodsRequest   {godoGoodsCode, erpGoodsCode, goodsName, onlyMallYn}
│       ├── RegisterGoodsResponse  {id, godoGoodsCode, erpGoodsCode, status, message}
│       ├── UpdateGoodsRequest     {goodsName, erpGoodsCode, onlyMallYn, availableQty, safetyRate}
│       ├── GoodsResponse          (전체 필드)
│       ├── GoodsListResponse      (페이지, 필터 조건 포함)
│       ├── GoodsListCondition     {soldOutYn, onlyMallYn, status, keyword}
│       ├── GoodsStatusLogResponse
│       ├── CommonStatusConfigResponse
│       └── UpdateCommonStatusConfigRequest {scope, outOfStockQty, onSaleQty, safetyRate}
│
├── domain
│   ├── model
│   │   ├── Goods
│   │   │     필드: id, godoGoodsCode, erpGoodsCode, goodsName, onlyMallYn,
│   │   │            soldOutYn, availableQty, safetyRate, godoSyncYn, erpSyncYn,
│   │   │            description, message, status, createdId, createdAt, updatedId, updatedAt
│   │   │     메서드:
│   │   │       restore()              HIDE → NORMAL 상태 복구
│   │   │       hide()                NORMAL → HIDE 상태 전이
│   │   │       applyGodoSyncResult() GODO_SYNC_YN=Y, soldOutYn 동기화
│   │   │       initializeCriteria()  availableQty, safetyRate 초기화
│   │   │       changeScope()         onlyMallYn 변경, 기준값 재설정
│   │   │       markErpError()        ERP_SYNC_YN=N, STATUS=ERROR, message 기록
│   │   │       recoverErpSync()      ERP_SYNC_YN=Y, STATUS=NORMAL, message=null
│   │   │       updateSoldOut(yn)     SOLD_OUT_YN 업데이트
│   │   └── CommonStatusConfig
│   │         필드: id, scope, outOfStockQty, onSaleQty, safetyRate, updatedId, updatedAt
│   │
│   ├── port
│   │   ├── GoodsRepository             Interface
│   │   │     save, findById, findByGodoCode, findByErpCode
│   │   │     existsByGodoCode, existsByErpCode
│   │   │     findAll(condition), findAllErpSyncNGoods()
│   │   │     findAllByStatusNot(HIDE)
│   │   ├── CommonStatusConfigRepository Interface
│   │   │     findByScope(scope)
│   │   │     save(config)
│   │   ├── GoodsStatusLogRepository    Interface
│   │   │     save, findAll(godoCode, dateRange)
│   │   └── GodoApiPort                 Interface (Stub — Task 04에서 구현)
│   │         searchGoods(godoGoodsCode) → GodoGoodsInfo
│   │         updateSoldOutStatus(godoGoodsCode, soldOutFl) → GodoApiResult
│   │
│   └── enums
│       ├── GoodsStatus    NORMAL / UNVERIFIED / ERROR / HIDE
│       └── GoodsScope     GENERAL / EXCLUSIVE
│
└── infrastructure
    ├── persistence
    │   ├── GoodsJpaRepository          Spring Data JPA Interface
    │   ├── GoodsRepositoryImpl         GoodsRepository 구현 (QueryDSL 포함)
    │   ├── CommonStatusConfigJpaRepository
    │   ├── CommonStatusConfigRepositoryImpl
    │   ├── GoodsStatusLogJpaRepository
    │   └── GoodsStatusLogRepositoryImpl
    └── external
        └── GodoApiStub                 GodoApiPort Stub 구현 (Task 04 전까지)
                                        searchGoods → GodoGoodsInfo(미검증 상태 반환)
                                        updateSoldOutStatus → GodoApiResult(성공 반환)
```

### 품목 등록 비즈니스 규칙

```
RegisterGoodsUseCase.execute(request, sessionEmail)

[1] 고도몰 코드 중복 체크
    existsByGodoCode(request.godoGoodsCode)
    → 중복이고 status=HIDE → goods.restore() 후 응답 (재등록으로 처리)
    → 중복이고 status≠HIDE → DuplicateGoodsException("이미 등록된 품목입니다!")

[2] ERP 코드 중복 체크
    existsByErpCode(request.erpGoodsCode)
    → 중복 → DuplicateGoodsException("이미 등록된 품목입니다!")

[3] 고도몰 API 조회 (GodoApiPort — Stub 응답)
    searchGoods(godoGoodsCode)
    → 정상 응답 → goods.applyGodoSyncResult() (GODO_SYNC_YN=Y, soldOutYn 동기화)
    → 오류 응답 → GODO_SYNC_YN=N 유지

[4] 공통 기준값 조회
    scope = onlyMallYn=Y → EXCLUSIVE / N → GENERAL
    findByScope(scope)
    → goods.initializeCriteria(config.availableQty, config.safetyRate)

[5] 저장
    goods.createdId = sessionEmail
    goodsRepository.save(goods)
```

### 품목 삭제 비즈니스 규칙

```
DeleteGoodsUseCase.execute(id)

→ goods.godoSyncYn = Y → goods.hide()   (물리 삭제 금지)
→ goods.godoSyncYn = N → 물리 삭제      (잘못 등록된 품목)
```

### 품목 수정 비즈니스 규칙

```
UpdateGoodsUseCase.execute(id, request)

→ goodsName, erpGoodsCode, onlyMallYn, availableQty, safetyRate를 요청값으로 저장
→ duplicate ERP code 검증은 기존과 동일하게 유지
```

### 테스트 시나리오

**Goods (Domain Model)**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T3-1 | HIDE 상태 품목 restore() 호출 | status = NORMAL |
| T3-2 | NORMAL 상태 품목 hide() 호출 | status = HIDE |
| T3-3 | applyGodoSyncResult(Y, soldOutFl=Y) 호출 | godoSyncYn=Y, soldOutYn=Y |
| T3-4 | changeScope() 후 기준값 변경 확인 | availableQty, safetyRate 갱신 |
| T3-5 | markErpError() 호출 | status=ERROR, erpSyncYn=N, message 존재 |
| T3-6 | recoverErpSync() 호출 | erpSyncYn=Y, status=NORMAL, message=null |

**RegisterGoodsUseCase**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T3-7 | 정상 신규 등록 | goods 저장, UNVERIFIED 상태 |
| T3-8 | 고도몰 코드 중복 (status=NORMAL) | DuplicateGoodsException |
| T3-9 | 고도몰 코드 중복 (status=HIDE) | restore() 후 정상 응답 |
| T3-10 | ERP 코드 중복 | DuplicateGoodsException |
| T3-11 | EXCLUSIVE scope 등록 | EXCLUSIVE 기준값으로 초기화 |
| T3-12 | GENERAL scope 등록 | GENERAL 기준값으로 초기화 |

**DeleteGoodsUseCase**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T3-13 | godoSyncYn=Y 품목 삭제 요청 | status=HIDE, 물리 삭제 없음 |
| T3-14 | godoSyncYn=N 품목 삭제 요청 | DB에서 물리 삭제 |

**UpdateGoodsUseCase**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T3-15 | availableQty, safetyRate 수정 시 | 요청값으로 저장 |
| T3-16 | duplicate ERP code로 수정 시 | DuplicateGoodsException |

**GoodsController (@WebMvcTest)**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T3-17 | 세션 없이 POST /api/goods | 401 반환 |
| T3-18 | godoGoodsCode 누락 요청 | 400 반환 |
| T3-19 | 정상 등록 요청 | 201 반환 |

**GoodsRepositoryImpl (@DataJpaTest)**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T3-20 | status=HIDE 조건 제외 목록 조회 | HIDE 품목 미포함 |
| T3-21 | erpSyncYn=N 조건 조회 | N 품목만 반환 |
| T3-22 | 다중 조건(soldOutYn, onlyMallYn) 필터 조회 | 조건 일치 품목만 반환 |

### 완료 기준

- 품목 등록·수정·삭제·조회 API 정상 동작
- HIDE 복구, 물리 삭제 분기 확인
- 전용 구분 변경 시 기준값 자동 변경 확인
- `./gradlew test` GREEN

---

## Task 04 — 고도몰 API 연동

### 목표

고도몰 Open API 클라이언트를 구현하고, Task 03의 `GodoApiStub`을 실제 구현체로 교체한다. API 송수신 로그를 전량 저장한다.

### 의존성 확인

Task 03 완료 필수 — `GodoApiPort` Interface, `ApiTransactionLog` 테이블

### 구현 대상

```
com.sfood.automation.goods.infrastructure.external
├── GodoApiProperties           @ConfigurationProperties 환경설정 바인딩
│     baseUrl, partnerKey, key
├── GodoApiClient               GodoApiPort 구현체 (Stub 교체)
│     공통 처리:
│       - partner_key / key 파라미터 자동 주입
│       - XML 응답 파싱 (ObjectMapper with XmlMapper)
│       - ratelimit-available-level 헤더 감지 → GodoApiException
│       - ApiTransactionLog 저장 (요청 전 PENDING, 응답 후 결과 업데이트)
├── GoodsSearchApiCaller        품목 조회 API 담당
│     call(godoGoodsCode) → GodoGoodsInfo
│     - POST Goods_Search.php
│     - code=000 → 정상 파싱
│     - code=119 → GodoApiException (재시도 금지)
│     - code=999 → GodoApiException (인증 오류)
├── SoldoutStatusApiCaller      품절 상태 변경 API 담당
│     call(godoGoodsCode, soldOutFl) → GodoApiResult
│     - POST Goods_Soldout_Status.php
│     - code=000 → 성공
│     - 429 응답 → 백오프 1초 후 1회 재시도 → 실패 시 GodoApiException
│     - 테스트 환경에서는 실제 호출 금지, 승인 전까지 Stub 또는 Mock으로만 검증
├── GodoGoodsInfo               응답 DTO {goodsNo, goodsNm, soldOutFl, stockFl}
├── GodoApiResult               결과 DTO {success, resultCode, resultMessage}
└── ApiTransactionLogRepository Interface + Impl
      save(log), updateResponse(id, responseBody, resultCode, isSuccess)
```

**ApiTransactionLog 저장 흐름**

```
API 호출 전:
  log = new ApiTransactionLog(apiType, godoGoodsCode, requestUrl, requestBody)
  log.requestedAt = now()
  repository.save(log) → log.id 확보

API 응답 후:
  log.responseBody = responseBody
  log.resultCode   = parsed code
  log.resultMessage = parsed msg
  log.isSuccess    = (code == "000")
  log.respondedAt  = now()
  repository.update(log)
```

### 테스트 시나리오

**GoodsSearchApiCaller**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T4-1 | code=000 응답 | GodoGoodsInfo 정상 파싱 |
| T4-2 | code=119 응답 | GodoApiException 발생, 재시도 없음 |
| T4-3 | code=999 응답 | GodoApiException 발생 |
| T4-4 | API 호출 전 ApiTransactionLog 저장 확인 | requestedAt, requestBody 존재 |
| T4-5 | API 응답 후 ApiTransactionLog 업데이트 확인 | resultCode, isSuccess 업데이트 |

**SoldoutStatusApiCaller**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T4-6 | Stub/Mock 기반 code=000 응답 | GodoApiResult.success=true |
| T4-7 | Stub/Mock 기반 429 응답 1회 후 성공 | 재시도 1회 후 성공 반환 |
| T4-8 | Stub/Mock 기반 429 응답 연속 2회 | GodoApiException 발생 |
| T4-9 | Stub/Mock 기반 EXHAUSTED 헤더 감지 | 즉시 GodoApiException 발생 |
| T4-10 | 실제 `Goods_Soldout_Status.php` 호출 테스트 시도 | 금지, 사전 승인 필요 |

**RegisterGoodsUseCase — Stub → 실제 구현 교체 후 재검증**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T4-11 | 존재하는 고도몰 코드 등록 | GODO_SYNC_YN=Y, soldOutYn 동기화 |
| T4-12 | 존재하지 않는 고도몰 코드 등록 | GODO_SYNC_YN=N 유지, 오류 미발생 |

### 완료 기준

- 고도몰 API 품목 조회 실제 호출 동작 확인
- 고도몰 품목 상태 변경 API는 승인 전까지 실제 호출하지 않음 확인
- 모든 API 호출에 대해 ApiTransactionLog 저장 확인
- Rate Limit 감지 시 즉시 중단 확인
- `./gradlew test` GREEN

---

## Task 05 — WMS 재고 스크랩핑

### 목표

WMS 시스템에서 재고 데이터를 수집하고, ERP 코드 검증 및 재고 데이터 정제 로직을 구현한다. 판매 상태 변경 로직(Task 06) 이전에 독립적으로 동작 가능한 상태로 완성한다.

### 의존성 확인

Task 03 완료 필수 — `Goods` 도메인, `GoodsRepository`

### 구현 대상

```
com.sfood.automation.wms.infrastructure.scraping
├── WmsProperties               @ConfigurationProperties 환경설정 바인딩
│     loginUrl, stockUrl, userId, password, centerCode, warehouseCode
├── WmsLoginClient
│     login() → HttpCookie
│     - GET WMS 로그인 URL (userId, password 파라미터)
│     - Set-Cookie 헤더에서 세션 쿠키 추출
│     - 응답 오류 시 WmsScrapingException
├── WmsStockClient
│     fetchAll(cookie) → List<WmsStockItem>
│     - POST 재고현황 URL
│     - Query: CURRENT_MENUCODE, PAGE_SIZE=10000, SES_WHSE 등
│     - Body: DS_SEARCHCONDITION [WH_CD=SLP001, LOT_ATTR_1=OA110]
│     - JSON 파싱 → DS_OUT 배열 역직렬화
│     - ErrorCode != 0 → WmsScrapingException
├── WmsStockResponse            응답 DTO {DS_TOTALCOUNT, ErrorMsg, ErrorCode, DS_OUT}
├── WmsStockItem                개별 재고 DTO
│     필드: ITEM_CD, ITEM_NM, LALOC_QTY, VALID_PERCENT, REMAIN_VALID_DAYS,
│           LOT_NO, LOT_ATTR_1, ZONE_CD, TEMP_TCD, LOT_HLD_SCD, WH_CD, VALID_DATETIME
├── WmsStockAggregator          LOT 합산 처리
│     aggregate(items) → Map<String, AggregatedStock>
│     - ITEM_CD 기준 그룹핑
│     - LALOC_QTY 합산
│     - VALID_PERCENT / REMAIN_VALID_DAYS 증거값 최대치 보관
│     - LOT 존재 여부 = 그룹 건수 > 0
├── AggregatedStock             합산 결과 DTO {itemCd, totalInvnQty, validPercent, remainValidDays, lotExists}
├── ErpCodeVerifier
│     verify(stockItems, erpSyncNGoods) → List<ErpVerifyResult>
│     - erpSyncNGoods: goods 중 ERP_SYNC_YN=N 목록
│     - stockItems에서 ITEM_CD Set 구성
│     - 매핑 성공 → ErpVerifyResult(goods.id, Y, NORMAL)
│     - 매핑 실패 → ErpVerifyResult(goods.id, N, ERROR, message)
├── ErpVerifyResult             결과 DTO {goodsId, erpSyncYn, status, message}
└── StockDataRefiner
      refine(stockItems, allErpCodes) → List<WmsStockItem>
      - allErpCodes: goods 전체 ERP_GOODS_CODE Set
      - ITEM_CD가 Set에 없는 stockItem 제거
      - zone 허용 후 row 기준 필터 적용
      - TEMP_TCD=FREEZE → REMAIN_VALID_DAYS 기준
      - 그 외 → VALID_PERCENT 기준
      - 정제된 목록 반환 (메모리 연산만, WMS 원본 무관)
```

### 테스트 시나리오

**WmsLoginClient**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T5-1 | 로그인 성공 응답 | HttpCookie 반환 |
| T5-2 | 로그인 실패 (비정상 응답) | WmsScrapingException 발생 |

**WmsStockClient**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T5-3 | ErrorCode=0 정상 응답 | List<WmsStockItem> 반환 |
| T5-4 | ErrorCode!=0 응답 | WmsScrapingException 발생 |
| T5-5 | Request Body에 DS_SEARCHCONDITION 포함 확인 | WH_CD=SLP001, LOT_ATTR_1=OA110 존재 |

**WmsStockAggregator**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T5-6 | 동일 ITEM_CD 3개 LOT (30, 20, 10) | totalInvnQty=60, lotExists=true |
| T5-7 | ITEM_CD 단일 LOT | totalInvnQty=해당값, lotExists=true |
| T5-8 | VALID_PERCENT 최솟값 선택 | 3개 중 가장 낮은 값 |

**ErpCodeVerifier**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T5-9 | ERP_SYNC_YN=Y 품목은 검증 대상 제외 | 결과 목록에 포함 안 됨 |
| T5-10 | ERP_SYNC_YN=N 품목 → WMS에 ITEM_CD 존재 | ErpVerifyResult(Y, NORMAL) |
| T5-11 | ERP_SYNC_YN=N 품목 → WMS에 ITEM_CD 없음 | ErpVerifyResult(N, ERROR, message) |
| T5-12 | ERP_SYNC_YN=N 품목 0건 | 빈 결과 목록 반환 |

**StockDataRefiner**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T5-13 | 미등록 ITEM_CD 포함 재고 목록 정제 | 미등록 항목 제거된 목록 반환 |
| T5-14 | 전체 ITEM_CD가 등록된 경우 | 원본 목록 그대로 반환 |
| T5-15 | 원본 stockItems 변경 없음 확인 | 입력 리스트 불변 (새 리스트 반환) |

### 완료 기준

- WMS 로그인 → 재고 수집 → ERP 검증 → 정제 전체 흐름 단위 테스트 통과
- LOT 합산 로직 정확성 확인
- ERP_SYNC_YN=Y 품목은 불필요하게 검증되지 않음 확인
- `./gradlew test` GREEN

---

## Task 06 — 판매 상태 판단 엔진

### 목표

수집·정제된 WMS 재고 데이터를 기반으로 품목의 품절/정상 상태 전환을 판단하고, 고도몰 API 호출 후 상태를 업데이트하는 핵심 비즈니스 엔진을 구현한다.

### 의존성 확인

Task 04 (GodoApiClient), Task 05 (WmsStockAggregator, StockDataRefiner) 완료 필수

### 구현 대상

```
com.sfood.automation.goods.domain.policy
└── GoodsSoldOutPolicy
      evaluate(goods, aggregatedStock) → StatusTransitionResult

      [사전 조건 검사]
      - ZONE_CD 허용 여부 확인
        허용 기준: 숫자 zone 또는 whitelist zone(B1)
      - 허용 zone이 아니면 SKIPPED
      - LOT_ATTR_1 != "OA110" → SKIPPED

      [Scope 결정]
      - goods.onlyMallYn = Y → EXCLUSIVE
      - goods.onlyMallYn = N → GENERAL

      [ON_SALE → OUT_OF_STOCK]  (goods.soldOutYn = N 인 경우만)
      - GENERAL:   totalInvnQty ≤ 20 → SET_OUT_OF_STOCK
      - EXCLUSIVE: totalInvnQty ≤ 1  → SET_OUT_OF_STOCK

      [OUT_OF_STOCK → ON_SALE]  (goods.soldOutYn = Y 인 경우만)
      - GENERAL:   minValidPercent ≥ 50 AND lotExists AND totalInvnQty ≥ 21 → SET_ON_SALE
      - EXCLUSIVE: minValidPercent ≥ 50 AND lotExists AND totalInvnQty ≥ 2  → SET_ON_SALE

      - 조건 미충족 → NO_CHANGE

com.sfood.automation.goods.domain.model
└── StatusTransitionResult      Enum
      SET_OUT_OF_STOCK / SET_ON_SALE / NO_CHANGE / SKIPPED

com.sfood.automation.goods.application.usecase
└── SyncGodoStatusUseCase
      execute(refinedStockItems) → SyncResult

      [품절 처리]
      [1] soldOutYn=N, status=NORMAL 품목 조회
      [2] aggregatedStock 구성 (WmsStockAggregator)
      [3] 품목별 GoodsSoldOutPolicy.evaluate()
      [4] SET_OUT_OF_STOCK 대상 → SoldoutStatusApiCaller.call(godoCode, "y")
      [5] API 성공 → goods.updateSoldOut("Y") → 저장
      [6] GoodsStatusLog 저장

      [정상 전환 처리]
      [7] soldOutYn=Y, status=NORMAL 품목 조회
      [8] 품목별 GoodsSoldOutPolicy.evaluate()
      [9] SET_ON_SALE 대상 → SoldoutStatusApiCaller.call(godoCode, "n")
      [10] API 성공 → goods.updateSoldOut("N") → 저장
      [11] GoodsStatusLog 저장

com.sfood.automation.goods.domain.model
└── GoodsStatusLog
      필드: godoGoodsCode, erpGoodsCode, wmsAvailableQty,
             criteriaQty, safetyRate, validPercent,
             beforeStatus, afterStatus, changedAt, createdAt
```

### 테스트 시나리오

**GoodsSoldOutPolicy**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T6-1 | ZONE_CD="EXT" | SKIPPED |
| T6-2 | ZONE_CD="104" (숫자), LOT_ATTR_1="OA110" | 평가 진행 |
| T6-3 | LOT_ATTR_1="OA115" (OA110 아님) | SKIPPED |
| T6-4 | GENERAL, soldOutYn=N, totalInvnQty=20 | SET_OUT_OF_STOCK |
| T6-5 | GENERAL, soldOutYn=N, totalInvnQty=21 | NO_CHANGE |
| T6-6 | EXCLUSIVE, soldOutYn=N, totalInvnQty=1 | SET_OUT_OF_STOCK |
| T6-7 | EXCLUSIVE, soldOutYn=N, totalInvnQty=2 | NO_CHANGE |
| T6-8 | GENERAL, soldOutYn=Y, validPercent=50, lotExists=true, qty=21 | SET_ON_SALE |
| T6-9 | GENERAL, soldOutYn=Y, validPercent=49, lotExists=true, qty=21 | NO_CHANGE |
| T6-10 | GENERAL, soldOutYn=Y, validPercent=50, lotExists=false, qty=21 | NO_CHANGE |
| T6-11 | EXCLUSIVE, soldOutYn=Y, validPercent=50, lotExists=true, qty=2 | SET_ON_SALE |
| T6-12 | 동일 입력 2회 평가 | 동일 결과 (멱등성) |
| T6-13 | STATUS=ERROR 품목 | 평가 대상 제외 확인 |

**SyncGodoStatusUseCase**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T6-14 | 품절 대상 3건 API 전송 요청 | Stub/Mock SoldoutStatusApiCaller 3건 호출 |
| T6-15 | API 성공 후 goods.soldOutYn=Y | 저장 확인 |
| T6-16 | API 실패 시 goods 상태 변경 없음 | 미변경 확인 |
| T6-17 | 상태 변경 발생 시 GoodsStatusLog 저장 | wmsAvailableQty, criteriaQty 포함 |
| T6-18 | NO_CHANGE 품목은 API 미호출 | SoldoutStatusApiCaller 호출 없음 |
| T6-19 | 품절 처리 후 정상 전환 처리 순서 보장 | 품절 먼저, 정상 전환 후 |
| T6-20 | 실제 고도몰 품목 상태 변경 API 호출 테스트 시도 | 금지, 사전 승인 필요 |

### 완료 기준

- 품절/정상 전환 판단 정책 전 케이스 테스트 통과
- API 성공 응답 후에만 goods 상태 업데이트 확인
- GoodsStatusLog 근거 데이터 저장 확인
- `./gradlew test` GREEN

---

## Task 07 — 배치 스케줄러

### 목표

Spring TaskScheduler 기반 동적 스케줄러를 구현하고, Task 06까지 구현된 전체 배치 작업 흐름(WMS 수집 → ERP 검증 → 정제 → 상태 판단 → API 전송)을 ProductSyncBatchJob으로 통합한다.

### 의존성 확인

Task 06 완료 필수 — `SyncGodoStatusUseCase`, WMS 모듈 전체

### 구현 대상

```
com.sfood.automation.batch
├── presentation
│   └── BatchScheduleAdminController
│         GET  /api/admin/batch-schedules/{jobName}  → getSchedule()
│         PUT  /api/admin/batch-schedules/{jobName}  → updateSchedule()
│         POST /api/admin/batch-schedules/{jobName}/run → runNow()
│
├── application
│   └── usecase
│       ├── GetBatchScheduleUseCase
│       │     execute(jobName) → BatchScheduleResponse
│       │     응답: 설정정보 + running 상태 + 마지막 실행 결과
│       ├── UpdateBatchScheduleUseCase
│       │     execute(jobName, request) → void
│       │     BatchSchedulePolicy.validate() → save → changeLog 저장 → reschedule
│       └── RunBatchNowUseCase
│             execute(jobName) → BatchRunResult
│             이미 실행 중 → SKIPPED 응답
│             미실행 중 → 즉시 실행 트리거 (MANUAL)
│
├── domain
│   ├── model
│   │   ├── BatchScheduleConfig
│   │   │     필드: id, jobName, enabled, scheduleType,
│   │   │            fixedDelaySeconds, initialDelaySeconds,
│   │   │            description, lastModifiedBy, createdAt, updatedAt
│   │   └── BatchExecutionLog
│   │         필드: id, jobName, startedAt, endedAt, status,
│   │                message, triggeredBy, durationMs, createdAt
│   ├── policy
│   │   └── BatchSchedulePolicy
│   │         validate(fixedDelaySeconds)
│   │         → fixedDelaySeconds < 60 → ScheduleValidationException
│   ├── port
│   │   ├── BatchScheduleConfigRepository
│   │   │     findByJobName, findAllByEnabledTrue, save
│   │   ├── BatchExecutionLogRepository
│   │   │     save, findLatestByJobName, findAll(jobName, dateRange)
│   │   └── BatchScheduleChangeLogRepository
│   │         save
│   └── enums
│       ├── BatchJobType           PRODUCT_SYNC
│       ├── BatchExecutionStatus   SUCCESS / FAIL / SKIPPED
│       └── BatchScheduleType      FIXED_DELAY
│
└── infrastructure
    ├── persistence
    │   ├── BatchScheduleConfigRepositoryImpl
    │   ├── BatchExecutionLogRepositoryImpl
    │   └── BatchScheduleChangeLogRepositoryImpl
    ├── scheduler
    │   ├── TaskSchedulerConfig
    │   │     ThreadPoolTaskScheduler Bean (corePoolSize=2)
    │   └── DynamicSchedulerManager
    │         Map<String, ScheduledFuture<?>> scheduledTasks (ConcurrentHashMap)
    │         initializeSchedules()   ApplicationRunner 호출
    │         register(config)        scheduleWithFixedDelay()
    │         reschedule(config)      cancel() → register()
    │         cancel(jobName)         ScheduledFuture.cancel(false)
    │         isRunning(jobName)      AtomicBoolean 상태 조회
    └── job
        ├── BatchExecutionService
        │     AtomicBoolean running
        │     execute(jobName, triggeredBy)
        │       → compareAndSet(false, true) 실패 → SKIPPED 로그
        │       → try: ProductSyncBatchJob.run()
        │       → catch: FAIL 로그, SlackNotifier.send()
        │       → finally: running.set(false), SUCCESS/FAIL 로그 저장
        └── ProductSyncBatchJob
              run()
              [1] WmsLoginClient.login()
              [2] WmsStockClient.fetchAll(cookie)
              [3] GoodsRepository.findAllErpSyncNGoods() → ErpCodeVerifier.verify()
                  → ErpVerifyResult 목록 → goods 상태 일괄 업데이트
              [4] GoodsRepository.findAllErpCodes() → StockDataRefiner.refine()
              [5] SyncGodoStatusUseCase.execute(refinedStock)
              [6] BatchResultMailSender.send(changedLogs)  (Task 08 연동)
```

### API 명세

**GET /api/admin/batch-schedules/{jobName}**

```
Response 200
{
  "jobName": "product-sync",
  "enabled": true,
  "fixedDelaySeconds": 600,
  "running": false,
  "lastStartedAt": "2026-03-31T10:00:00",
  "lastEndedAt": "2026-03-31T10:00:45",
  "lastStatus": "SUCCESS",
  "lastMessage": "품절 3건, 정상 전환 1건"
}
```

**PUT /api/admin/batch-schedules/{jobName}**

```
Request  { "enabled": true, "fixedDelaySeconds": 300, "modifiedBy": "user@s-food.com" }
Response 200
         400 fixedDelaySeconds < 60
```

**POST /api/admin/batch-schedules/{jobName}/run**

```
Response 200 { "triggered": true,  "status": "STARTED"  }
         200 { "triggered": false, "status": "SKIPPED"  }
```

### 테스트 시나리오

**BatchSchedulePolicy**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T7-1 | fixedDelaySeconds=60 | 통과 |
| T7-2 | fixedDelaySeconds=59 | ScheduleValidationException |
| T7-3 | fixedDelaySeconds=0 | ScheduleValidationException |

**DynamicSchedulerManager**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T7-4 | enabled=true 설정 → initializeSchedules() | register() 호출 확인 |
| T7-5 | enabled=false 설정 → initializeSchedules() | register() 미호출 |
| T7-6 | reschedule() 호출 | cancel() 후 register() 순서 확인 |
| T7-7 | 존재하지 않는 jobName cancel() | 예외 없이 처리 |

**BatchExecutionService**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T7-8 | 미실행 상태에서 execute() | ProductSyncBatchJob.run() 호출 |
| T7-9 | 실행 중 상태에서 execute() | SKIPPED 로그 저장, Job 미호출 |
| T7-10 | Job 성공 후 | SUCCESS 로그, durationMs 기록 |
| T7-11 | Job 예외 발생 후 | FAIL 로그, SlackNotifier 호출 |
| T7-12 | finally 블록에서 running=false | 재실행 가능 상태 복원 |

**UpdateBatchScheduleUseCase**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T7-13 | 정상 변경 요청 | DB 저장, changeLog 저장, reschedule() 호출 |
| T7-14 | fixedDelaySeconds=30 요청 | ScheduleValidationException, DB 미저장 |

**ProductSyncBatchJob (통합 흐름)**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T7-15 | WMS 로그인 실패 시 | WmsScrapingException 발생, 이후 단계 미실행 |
| T7-16 | ERP 검증 → 정제 → 상태 판단 순서 | 순서 보장 확인 |
| T7-17 | STATUS=ERROR 품목은 판매 상태 변경 대상 제외 | 해당 품목 API 미호출 |

### 완료 기준

- 서버 기동 시 product-sync 스케줄 자동 등록 확인
- 런타임 주기 변경 후 새 주기로 재등록 확인
- 중복 실행 방지 동작 확인
- 전체 배치 흐름 통합 테스트 통과
- `./gradlew test` GREEN

---

## Task 08 — 알림 및 로그

### 목표

배치 실행 결과 이메일 알림, OTP 발송 이메일, Slack 오류 알림을 완성한다.

### 의존성 확인

Task 07 완료 필수 — `ProductSyncBatchJob`, `BatchExecutionService`

### 구현 대상

```
com.sfood.automation.common.slack
└── SlackNotifier (Task 01 구현 → 실제 Webhook 연결 검증)
      send(location, message, occurredAt)
      payload 형식:
        {
          "text": "[ERROR] {location}\n{message}\n발생시각: {occurredAt}"
        }

com.sfood.automation.auth.infrastructure.mail
└── OtpMailSender (Task 02 구현 → 실제 SMTP 연결 검증)
      제목: "[에쓰푸드몰] 인증번호 안내"
      본문:
        인증번호: {code}
        유효시간: 5분
        본인이 요청하지 않았다면 이 메일을 무시하세요.

com.sfood.automation.batch.infrastructure.job
└── BatchResultMailSender
      send(List<GoodsStatusLog>)
      - 변경 건수 = 0 이면 발송 생략
      - 제목: "[에쓰푸드몰] 품목 판매 상태 변경 알림 {datetime}"
      - 본문: 변경 품목 테이블 (품목명, 고도몰코드, 변경 전, 변경 후, 근거 재고수량)
      - EmailSendLog 저장 (성공/실패 모두)

com.sfood.automation.goods.infrastructure.persistence
└── EmailSendLogRepositoryImpl
      save(EmailSendLog)
```

**EmailSendLog 저장 규칙**

```
발송 시도 전: EmailSendLog(sendType, recipient, subject, isSucess=0) 저장
발송 성공 후: isSuccess=1 업데이트
발송 실패 시: isSuccess=0, errorMessage 기록 → SlackNotifier.send()
```

### 테스트 시나리오

**BatchResultMailSender**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T8-1 | 변경 로그 0건 | 메일 미발송, EmailSendLog 미저장 |
| T8-2 | 변경 로그 3건 | 메일 1건 발송, 테이블 형식 본문 확인 |
| T8-3 | 발송 성공 | EmailSendLog isSuccess=1 |
| T8-4 | 발송 실패 | EmailSendLog isSuccess=0, errorMessage 기록, SlackNotifier 호출 |

**OtpMailSender**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T8-5 | 정상 발송 | 제목, 수신자, code 포함 확인 |
| T8-6 | 발송 실패 시 | MailException 전파 (UseCase에서 처리) |

**SlackNotifier**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T8-7 | send() 호출 | Webhook URL로 POST, text 필드 포함 |
| T8-8 | Webhook 오류 시 | 예외 미전파 (로그만 기록) |

### 완료 기준

- 배치 실행 후 변경 건수 > 0 이면 이메일 발송 확인
- 변경 건수 = 0 이면 이메일 미발송 확인
- 오류 발생 시 Slack 알림 전송 확인
- EmailSendLog 저장 확인
- `./gradlew test` GREEN

---

## Task 09 — 통합 테스트 및 마무리

### 목표

전체 서비스의 E2E 시나리오를 통합 테스트로 검증하고, 운영 배포 준비를 완료한다.

### 의존성 확인

Task 01~8 모두 완료 필수

### 구현 대상

**통합 테스트 시나리오 (@SpringBootTest)**

```
시나리오 A — 인증 → 품목 등록 → 배치 실행 전체 흐름

[1] POST /api/auth/email-verifications → verificationKey 확보
[2] POST /api/auth/email-verifications/confirm → SESSION 쿠키 확보
[3] POST /api/goods → 품목 등록
[4] PUT  /api/admin/batch-schedules/product-sync → 주기 변경 (300초)
[5] POST /api/admin/batch-schedules/product-sync/run → 수동 배치 실행
[6] GET  /api/goods/{id} → 품목 상태 변경 확인
[7] GET  /api/goods/status-logs → 상태 변경 로그 확인
```

**운영 준비 체크리스트 검증**

| 항목 | 검증 방법 |
|---|---|
| 환경변수 누락 시 기동 실패 | 필수 환경변수 제거 후 기동 시도 |
| SQLite WAL 모드 적용 | `PRAGMA journal_mode` 쿼리 결과 확인 |
| Virtual Threads 활성화 | 스레드 이름에 `virtual` 포함 확인 |
| schema.sql 전체 테이블 생성 | 9개 테이블 존재 확인 |
| data.sql 초기 데이터 | 초기 2+1건 확인 |
| API 응답 구조 표준화 | ErrorResponse 구조 일관성 확인 |
| 로그 파일 생성 | `logs/sfood-automation.log` 확인 |

**최종 테스트 시나리오**

| # | 시나리오 | 기대 결과 |
|---|---|---|
| T9-1 | 인증 없이 전체 보호 API 호출 | 모두 401 반환 |
| T9-2 | OTP 발송 → 인증 → 세션 → 품목 등록 → 배치 수동 실행 | 전체 흐름 정상 |
| T9-3 | 배치 실행 중 수동 실행 요청 | SKIPPED 반환 |
| T9-4 | 스케줄 주기 변경 후 변경 이력 조회 | batch_schedule_change_log 존재 |
| T9-5 | godoSyncYn=Y 품목 삭제 | HIDE 처리, 물리 삭제 없음 |
| T9-6 | godoSyncYn=N 품목 삭제 | 물리 삭제 |
| T9-7 | 고도몰 품목 상태 변경 API는 승인 없는 실제 호출 없이 대역으로 검증 | 정책 준수 확인 |
| T9-8 | 고도몰 API 오류 발생 | Slack 알림 전송, goods 상태 미변경 |
| T9-9 | WMS 수집 실패 | 배치 FAIL 처리, 스케줄 유지 |

### 완료 기준

- 전체 통합 테스트 시나리오 통과
- `./gradlew test` GREEN (단위 + 통합 포함)
- 운영 체크리스트 전 항목 통과
- 환경변수 누락 시 기동 실패 확인

---

## 📎 Task 간 의존 관계 요약

```
Task 01 (기반 설정)
    │
    ▼
Task 02 (OTP 인증)
    │
    ▼
Task 03 (품목 관리)
    │
    ├──────────────────────┐
    ▼                      ▼
Task 04           Task 05
(고도몰 API)          (WMS 스크랩핑)
    │                      │
    └──────────┬───────────┘
               ▼
          Task 06
          (판매 상태 판단 엔진)
               │
               ▼
          Task 07
          (배치 스케줄러)
               │
               ▼
          Task 08
          (알림 및 로그)
               │
               ▼
          Task 09
          (통합 테스트)
```

---

## 📦 참조 문서

| 문서 | 위치 |
|---|---|
| SRS | `SRS-SFOOD-MALL-AUTOMATION-BACKEND.md` |
| Architecture | `ARCHITECTURE-SFOOD-MALL-AUTOMATION-BACKEND.md` |
| DB Schema & ERD | `sfood-sqlite-schema-erd.md` |
