# Architecture — SFOOD-MALL-AUTOMATION-BACKEND

> 에쓰푸드몰 품목 판매 상태 자동화 백엔드 서비스  
> 참조 문서: SRS-SFOOD-MALL-AUTOMATION-BACKEND.md  
> 작성일: 2026-03-31

---

## 📐 시스템 전체 구조

### 시스템 컨텍스트 다이어그램

```
                        ┌─────────────────────────────────────────┐
                        │         내부 사용자 (물류팀 담당자)         │
                        │         @s-food.com 이메일 보유           │
                        └──────────────────┬──────────────────────┘
                                           │ HTTPS (Browser)
                                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  SFOOD-MALL-AUTOMATION-BACKEND                        │
│                  Java 21 / Spring Boot 3.5 / SQLite                  │
│                                                                       │
│   ┌──────────────┐   ┌──────────────┐   ┌────────────────────────┐  │
│   │  Auth API    │   │  Goods API   │   │  Batch Scheduler Admin │  │
│   │  (OTP 인증)  │   │  (품목 관리) │   │  (스케줄 관리)         │  │
│   └──────────────┘   └──────────────┘   └────────────────────────┘  │
│                                                                       │
│   ┌─────────────────────────────────────────────────────────────┐    │
│   │                    Batch Job Engine                          │    │
│   │           (WMS 스크랩핑 → 재고 분석 → 상태 변경)            │    │
│   └─────────────────────────────────────────────────────────────┘    │
│                                                                       │
│                        ┌──────────┐                                   │
│                        │  SQLite  │                                   │
│                        └──────────┘                                   │
└────────────────┬────────────────────┬────────────────────────────────┘
                 │                    │
        ┌────────▼────────┐  ┌────────▼────────┐
        │   WMS System    │  │  고도몰 Open API │
        │ (재고현황 수집)  │  │ (품절 상태 변경) │
        └─────────────────┘  └─────────────────┘
                 │                    │
        ┌────────▼────────┐  ┌────────▼────────┐
        │   SMTP Server   │  │  Slack Webhook  │
        │ (OTP/결과 메일)  │  │  (오류 알림)    │
        └─────────────────┘  └─────────────────┘
```

---

## 🏗️ Clean Architecture 레이어 설계

### 레이어 의존 방향

```
  ┌──────────────────────────────────────────────────────────────┐
  │                    Presentation Layer                         │
  │         RestController  /  Request DTO  /  Response DTO       │
  └──────────────────────────────┬───────────────────────────────┘
                                 │ 호출 (단방향)
  ┌──────────────────────────────▼───────────────────────────────┐
  │                    Application Layer                          │
  │              UseCase  /  ApplicationService                   │
  │         트랜잭션 경계 정의 / 도메인 흐름 조율                  │
  └──────────────────────────────┬───────────────────────────────┘
                                 │ 호출 (단방향)
  ┌──────────────────────────────▼───────────────────────────────┐
  │                      Domain Layer                             │
  │     Entity  /  DomainService  /  Policy  /  Enum             │
  │     Repository Interface (Port)                               │
  │          ← 외부 의존성 없음, 순수 비즈니스 로직만 보유 →       │
  └──────────────────────────────┬───────────────────────────────┘
                                 │ 구현 (역방향 의존성 주입)
  ┌──────────────────────────────▼───────────────────────────────┐
  │                   Infrastructure Layer                        │
  │    JPA Repository  /  외부 API Client  /  Scheduler          │
  │    MailSender  /  SlackNotifier  /  SessionIssuer            │
  └──────────────────────────────────────────────────────────────┘
```

**핵심 원칙**

- Domain Layer는 어떤 외부 프레임워크에도 의존하지 않는다
- Repository는 Domain Layer에 Interface(Port)로 선언하고 Infrastructure Layer에서 구현(Adapter)한다
- UseCase는 단일 책임 원칙을 지키며 하나의 비즈니스 흐름만 담당한다
- 비즈니스 로직은 반드시 Domain Layer 안에만 존재한다

---

## 📦 도메인 모듈 구성

서비스를 아래 5개의 도메인 모듈로 분리한다. 각 모듈은 독립적인 Presentation / Application / Domain / Infrastructure 레이어를 가진다.

```
com.sfood.automation
├── auth          ← OTP 인증, 세션 관리
├── goods         ← 품목 마스터, 고도몰 API 연동
├── batch         ← 배치 스케줄러, 실행 엔진
├── wms           ← WMS 재고 스크랩핑
└── common        ← 공통 설정, 예외, 보안, Slack
```

---

## 🔐 Auth 모듈

### 역할

이메일 OTP 인증 및 Spring Session 기반 세션 발급을 담당한다.

### 컴포넌트 구조

```
auth
├── presentation
│   └── EmailVerificationController       REST API 진입점, 입력 검증 위임
├── application
│   ├── usecase
│   │   ├── RequestEmailVerificationUseCase   OTP 생성/발송 흐름 조율
│   │   └── ConfirmEmailVerificationUseCase   OTP 검증/세션 발급 흐름 조율
│   └── dto
│       ├── EmailVerificationRequest
│       ├── EmailVerificationResponse
│       ├── ConfirmVerificationRequest
│       └── ConfirmVerificationResponse
├── domain
│   ├── model
│   │   └── EmailVerification             인증 세션 애그리게이트 루트
│   ├── policy
│   │   └── OtpPolicy                     유효시간·시도횟수·Rate Limit 정책
│   ├── port
│   │   └── EmailVerificationRepository   저장소 인터페이스 (Domain 선언)
│   └── enums
│       └── VerificationStatus            PENDING/VERIFIED/EXPIRED/FAILED/CANCELLED
└── infrastructure
    ├── persistence
    │   └── EmailVerificationRepositoryImpl   JPA 구현체
    ├── otp
    │   ├── OtpGenerator                  SecureRandom 기반 6자리 생성
    │   └── OtpHashProvider               SHA-256(key:code:secret) 해시
    ├── mail
    │   └── OtpMailSender                 Spring Mail 기반 OTP 발송
    └── session
        └── SessionIssuer                 HttpSession 생성, Set-Cookie 응답
```

### 인증 흐름

```
[OTP 요청]
클라이언트
  → POST /api/auth/email-verifications
  → EmailVerificationController
      → @Valid 입력 검증 (이메일 형식)
  → RequestEmailVerificationUseCase
      → OtpPolicy.validateEmail()          도메인 정책: @s-food.com 검증
      → OtpPolicy.checkRateLimit()         도메인 정책: 60초·시간당 5회
      → 기존 PENDING 세션 CANCELLED 처리
      → OtpGenerator.generate()            SecureRandom 6자리
      → OtpHashProvider.hash()             SHA-256 해시 계산
      → EmailVerificationRepository.save() 해시값만 저장
      → OtpMailSender.send()               메일 발송
  ← verificationKey(UUID v4) 반환

[OTP 검증]
클라이언트
  → POST /api/auth/email-verifications/confirm
  → EmailVerificationController
  → ConfirmEmailVerificationUseCase
      → EmailVerificationRepository.findByKey()
      → EmailVerification.validate()       만료·상태·횟수 도메인 검증
      → OtpHashProvider.matches()          해시 비교
      → EmailVerification.markVerified()   VERIFIED 상태 전이
      → EmailVerificationRepository.save()
      → SessionIssuer.issue()              HttpSession 생성, Set-Cookie
  ← authenticated: true 반환

[세션 확인]
클라이언트
  → GET /api/auth/session (SESSION 쿠키 포함)
  → SessionAuthInterceptor                Spring Session 유효성 확인
  ← 200 OK / 401 Unauthorized
```

### 보안 설계

| 항목 | 구현 방식 |
|---|---|
| OTP 생성 | `SecureRandom` — `String.format("%06d", random.nextInt(1_000_000))` |
| OTP 저장 | 원문 저장 금지, `SHA-256(verificationKey + ":" + code + ":" + serverSecret)` |
| 세션 쿠키 | `HttpOnly`, `Path=/`, `Max-Age=86400` (24시간) |
| 도메인 검증 | `email.endsWith("@s-food.com")` — Domain Layer Policy |
| Rate Limit | `lastSentAt` 기준 60초, 1시간 내 5회 — DB 조회 기반 |

---

## 📦 Goods 모듈

### 역할

품목 마스터 CRUD, 고도몰 API 연동을 통한 품목 검증, 판매 상태 동기화를 담당한다.

### 컴포넌트 구조

```
goods
├── presentation
│   ├── GoodsController                   품목 CRUD REST API
│   ├── GoodsStatusLogController          상태 변경 이력 조회 API
│   └── CommonStatusConfigController      공통 판매 상태 기준 관리 API
├── application
│   ├── usecase
│   │   ├── RegisterGoodsUseCase          품목 등록 (중복체크 + 고도몰 검증)
│   │   ├── UpdateGoodsUseCase            품목 수정
│   │   ├── DeleteGoodsUseCase            품목 Hide/삭제 처리
│   │   ├── GetGoodsListUseCase           품목 목록 조회
│   │   └── SyncGodoStatusUseCase         고도몰 품절 상태 동기화 (배치 호출)
│   └── dto
│       ├── RegisterGoodsRequest / Response
│       ├── UpdateGoodsRequest / Response
│       └── GoodsListResponse
├── domain
│   ├── model
│   │   ├── Goods                         품목 애그리게이트 루트
│   │   └── CommonStatusConfig            공통 판매 상태 기준 엔티티
│   ├── policy
│   │   └── GoodsSoldOutPolicy            품절/정상 전환 판단 도메인 정책
│   ├── port
│   │   ├── GoodsRepository
│   │   ├── CommonStatusConfigRepository
│   │   └── GodoApiPort                   외부 고도몰 API 인터페이스
│   └── enums
│       ├── GoodsStatus                   NORMAL/UNVERIFIED/ERROR/HIDE
│       └── GoodsScope                    GENERAL/EXCLUSIVE
└── infrastructure
    ├── persistence
    │   ├── GoodsRepositoryImpl           JPA + QueryDSL
    │   └── CommonStatusConfigRepositoryImpl
    └── external
        └── GodoApiClient                 고도몰 Open API HTTP 클라이언트
            ├── GoodsSearchApiCaller      품목 조회 API 호출
            └── SoldoutStatusApiCaller    품절 상태 변경 API 호출
```

### 품목 등록 흐름

```
RegisterGoodsUseCase.execute()
  → GoodsRepository.existsByGodoCode()     고도몰 코드 중복 체크
  → GoodsRepository.existsByErpCode()      ERP 코드 중복 체크
  → [HIDE 품목인 경우] Goods.restore()     NORMAL 상태 복구
  → GodoApiPort.searchGoods(godoCode)      고도몰 품목 조회 API
  → Goods.applyGodoSyncResult()            GODO_SYNC_YN=Y, 품절여부 동기화
  → CommonStatusConfigRepository.findByScope()  전용/공용 기준값 조회
  → Goods.initializeCriteria()             AVAILABLE_QTY, SAFETY_RATE, REMAIN_VALID_DAYS 초기화
  → GoodsRepository.save()
```

### 판매 상태 판단 정책 (GoodsSoldOutPolicy)

Domain Layer 내 순수 Java 로직으로 구현한다. 외부 의존성 없음.

```
GoodsSoldOutPolicy.evaluate(goods, wmsStock) : StatusTransitionResult

  [사전 조건 검사]
  → ZONE_CD 허용 여부 확인
  → 허용 기준: 숫자 zone 또는 whitelist zone(B1)
  → LOT_ATTR_1 == "OA110" 확인
  → 위 조건 미충족 → SKIPPED 반환

  [Scope 결정]
  → Goods.onlyMallYn == Y → EXCLUSIVE
  → Goods.onlyMallYn == N → GENERAL

  [ON_SALE → OUT_OF_STOCK 검사]  (현재 SOLD_OUT_YN = N 인 경우만)
  → GENERAL:   합산 LALOC_QTY ≤ 20  → SET_OUT_OF_STOCK
  → EXCLUSIVE: 합산 LALOC_QTY ≤ 1   → SET_OUT_OF_STOCK

  [OUT_OF_STOCK → ON_SALE 검사]  (현재 SOLD_OUT_YN = Y 인 경우만)
  → 비냉동: VALID_PERCENT 기준을 통과하고 LOT 존재 AND 합산 LALOC_QTY ≥ goods.availableQty + 1 → SET_ON_SALE
  → 냉동:   REMAIN_VALID_DAYS 기준을 통과하고 LOT 존재 AND 합산 LALOC_QTY ≥ goods.availableQty + 1 → SET_ON_SALE

  → 조건 미충족 → NO_CHANGE 반환
```

### 고도몰 API 클라이언트 설계

```
GodoApiClient
  │
  ├── 공통 처리
  │   ├── partner_key / key 헤더 주입 (환경설정)
  │   ├── XML 응답 파싱 (ResponseCode 추출)
  │   ├── Rate Limit 헤더 감지 (ratelimit-available-level: EXHAUSTED → 즉시 중단)
  │   └── ApiTransactionLog 저장 (요청 전 저장, 응답 후 업데이트)
  │
  ├── GoodsSearchApiCaller
  │   POST https://openhub.godo.co.kr/godomall5/goods/Goods_Search.php
  │   파라미터: partner_key, key, goodsNo
  │   응답 코드 000 → 정상 / 119 → 재시도 금지
  │
  └── SoldoutStatusApiCaller
      POST https://openhub.godo.co.kr/godomall5/goods/Goods_Soldout_Status.php
      파라미터: partner_key, key, goodsNo, soldoutFl(y/n)
      응답 코드 000 → 정상 / 429 → 백오프 후 재시도
```

---

## ⏰ Batch 모듈

### 역할

동적 배치 스케줄 관리, 배치 실행 엔진, 중복 실행 방지, 실행 이력 저장을 담당한다.

### 컴포넌트 구조

```
batch
├── presentation
│   └── BatchScheduleAdminController      스케줄 조회/수정/수동실행 API
├── application
│   └── usecase
│       ├── GetBatchScheduleUseCase
│       ├── UpdateBatchScheduleUseCase     DB 저장 + reschedule 호출
│       └── RunBatchNowUseCase             즉시 실행 트리거
├── domain
│   ├── model
│   │   ├── BatchScheduleConfig           스케줄 설정 애그리게이트
│   │   └── BatchExecutionLog             실행 이력 엔티티
│   ├── policy
│   │   └── BatchSchedulePolicy           최소 60초 검증 등 도메인 정책
│   ├── port
│   │   ├── BatchScheduleConfigRepository
│   │   └── BatchExecutionLogRepository
│   └── enums
│       ├── BatchJobType                  PRODUCT_SYNC
│       ├── BatchExecutionStatus          SUCCESS/FAIL/SKIPPED
│       └── BatchScheduleType             FIXED_DELAY
└── infrastructure
    ├── persistence
    │   ├── BatchScheduleConfigRepositoryImpl
    │   └── BatchExecutionLogRepositoryImpl
    ├── scheduler
    │   ├── TaskSchedulerConfig           ThreadPoolTaskScheduler Bean 설정
    │   └── DynamicSchedulerManager       런타임 스케줄 생명주기 관리
    └── job
        ├── ProductSyncBatchJob           실제 배치 비즈니스 로직
        └── BatchExecutionService         실행 전후 처리, 중복 방지
```

### DynamicSchedulerManager 설계

```java
// 내부 상태 — 실행 중인 스케줄 관리
Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>()

// 주요 메서드
initializeSchedules()    서버 기동 시 ApplicationRunner에서 호출
                         enabled=true 설정만 자동 등록
register(config)         ScheduledFuture 생성 및 Map 등록
reschedule(config)       cancel() → register() 순서로 재등록
cancel(jobName)          ScheduledFuture.cancel(false) 후 Map 제거
```

### BatchExecutionService 설계 (중복 실행 방지)

```java
// 단일 인스턴스 락
AtomicBoolean running = new AtomicBoolean(false)

execute(jobName, triggeredBy):
  → running.compareAndSet(false, true) 실패
      → BatchExecutionLog(SKIPPED) 저장
      → 종료
  → 성공 시 startedAt 기록
  → try:
      ProductSyncBatchJob.run()
  → catch:
      BatchExecutionLog(FAIL, errorMessage) 저장
      SlackNotifier.send(error)
  → finally:
      running.set(false)
      BatchExecutionLog(SUCCESS) 저장
```

### 배치 실행 전체 흐름

```
[서버 기동]
ApplicationRunner.run()
  └─ DynamicSchedulerManager.initializeSchedules()
       └─ BatchScheduleConfigRepository.findAllByEnabledTrue()
            └─ register(config) → TaskScheduler.scheduleWithFixedDelay()

[스케줄 실행 시점 — Fixed Delay 10분]
TaskScheduler trigger
  └─ BatchExecutionService.execute("product-sync", SCHEDULER)
       └─ AtomicBoolean 락 확인
            └─ ProductSyncBatchJob.run()
                 └─ [1] WmsLoginClient.login()
                 └─ [2] WmsStockClient.fetchAll()
                 └─ [3] ErpCodeVerifier.verify()         ERP_SYNC_YN=N 대상
                 └─ [4] StockDataRefiner.refine()        미등록 품목 제거
                 └─ [5] SoldOutEvaluator.extractTargets() 품절 대상 추출
                 └─ [6] SoldoutStatusApiCaller.call()    고도몰 API 전송
                 └─ [7] GoodsRepository.updateSoldOut()  상태 업데이트
                 └─ [8] OnSaleEvaluator.extractTargets() 정상 대상 추출
                 └─ [9] SoldoutStatusApiCaller.call()    고도몰 API 전송
                 └─ [10] GoodsRepository.updateOnSale()  상태 업데이트
                 └─ [11] GoodsStatusLogRepository.saveAll()
                 └─ [12] BatchResultMailSender.send()

[스케줄 변경 요청]
PUT /api/admin/batch-schedules/{jobName}
  └─ UpdateBatchScheduleUseCase
       └─ BatchSchedulePolicy.validate()   최소 60초 검증
       └─ BatchScheduleConfigRepository.save()
       └─ BatchScheduleChangeLogRepository.save()
       └─ DynamicSchedulerManager.reschedule(config)
```

---

## 🌐 WMS 모듈

### 역할

WMS 시스템에 HTTP 클라이언트로 접근하여 로그인 세션을 획득하고 재고현황 데이터를 수집한다.

### 컴포넌트 구조

```
wms
└── infrastructure
    └── scraping
        ├── WmsLoginClient        로그인 URL 호출, 세션 쿠키 획득
        ├── WmsStockClient        재고현황 URL + Payload 전송, JSON 파싱
        ├── WmsStockResponse      재고현황 응답 DTO (DS_OUT 배열)
        ├── WmsStockItem          개별 재고 항목 DTO (ITEM_CD, LALOC_QTY, VALID_PERCENT, REMAIN_VALID_DAYS, TEMP_TCD 등)
        ├── WmsStockAggregator    ERP 코드 기준 수량 합산 + 증거값 집계
        ├── ErpCodeVerifier       ERP_SYNC_YN=N 품목 대상 매핑 검증
        └── StockDataRefiner      미등록 품목 재고 메모리 정제
```

### WMS 스크랩핑 흐름

```
WmsLoginClient.login()
  → GET https://wms.s-food.com/sessionService/login
    파라미터: USERID, PASSWORD (환경설정)
  → 응답 Set-Cookie 세션 쿠키 추출
  ← HttpCookie 반환

WmsStockClient.fetchAll(cookie)
  → POST https://wms.s-food.com/invnTotListService/searchAllStock
    Query: CURRENT_MENUCODE=WMS141123, PAGE_SIZE=10000, SES_WHSE=SLP001 등
    Body: DS_SEARCHCONDITION [WH_CD=SLP001, LOT_ATTR_1=OA110]
  → JSON 파싱 → DS_OUT 배열 역직렬화
  → ErrorCode != 0 → 예외 발생, 배치 중단
  ← List<WmsStockItem> 반환

ErpCodeVerifier.verify(stockItems, targetGoods)
  → targetGoods: ERP_SYNC_YN=N 인 goods 목록 (DB 조회)
  → stockItems에서 ITEM_CD Set 구성
  → targetGoods 순회
      ├─ ITEM_CD 존재 → ERP_SYNC_YN=Y, STATUS=NORMAL, MESSAGE=null
      └─ ITEM_CD 없음 → STATUS=ERROR, MESSAGE 기록

StockDataRefiner.refine(stockItems, allGoods)
  → allGoods 전체 ERP_GOODS_CODE → Set<String> 구성
  → stockItems에서 ITEM_CD가 Set에 없는 항목 제거
  → zone 허용 여부 확인
  → row 기준 필터
      ├─ TEMP_TCD = FREEZE      → REMAIN_VALID_DAYS >= goods.remainValidDays
      └─ 그 외                  → VALID_PERCENT >= goods.safetyRate
  ← 정제된 List<WmsStockItem> 반환
```

### 재고 데이터 집계 (LOT 합산)

```
WmsStockItem은 ITEM_CD + LOT_NO 조합으로 복수 존재
→ ITEM_CD 기준으로 그룹핑
→ LALOC_QTY 합산
→ 로그 증거값으로 VALID_PERCENT / REMAIN_VALID_DAYS 최대값 보관
→ LOT 존재 여부 = 합산 결과 건수 > 0
```

---

## 🔧 Common 모듈

### 역할

전체 모듈에서 공통으로 사용하는 설정, 예외 처리, 보안 인터셉터, Slack 알림을 제공한다.

### 컴포넌트 구조

```
common
├── config
│   ├── WebMvcConfig               CORS, Interceptor 등록
│   ├── JpaConfig                  SQLite Dialect 설정
│   └── HttpClientConfig           WMS / 고도몰용 RestClient Bean
├── security
│   └── SessionAuthInterceptor     세션 쿠키 검증, 미인증 시 401 반환
│                                  인증 불필요 경로 화이트리스트 관리
├── exception
│   ├── GlobalExceptionHandler     @RestControllerAdvice 전역 예외 처리
│   ├── BusinessException          도메인 예외 기반 클래스
│   └── ErrorResponse              표준 오류 응답 DTO
├── slack
│   └── SlackNotifier              Webhook POST 전송
│                                  오류 발생 위치, 메시지, 시각 포함
└── logging
    └── RequestLoggingFilter       API 요청/응답 로깅 (개발 환경)
```

### SessionAuthInterceptor 인증 처리

```
화이트리스트 (인증 불필요):
  - POST /api/auth/email-verifications
  - POST /api/auth/email-verifications/confirm
  - GET  /api/auth/session

인증 필요 경로:
  - /api/goods/**
  - /api/admin/**
  - /api/config/**

처리 흐름:
  → HttpSession 조회 (SESSION 쿠키 기반)
  → 세션 없음 또는 만료 → 401 Unauthorized 반환
  → 세션 유효 → 다음 핸들러로 통과
```

### GlobalExceptionHandler 응답 코드 매핑

| 예외 | HTTP 상태 | 설명 |
|---|---|---|
| `EmailFormatException` | 400 | 이메일 형식 오류 |
| `RateLimitException` | 429 | Rate Limit 초과 |
| `OtpExpiredException` | 410 | OTP 만료 |
| `OtpMismatchException` | 401 | OTP 불일치 |
| `OtpAlreadyUsedException` | 409 | 이미 처리된 인증 세션 |
| `GoodsNotFoundException` | 404 | 품목 없음 |
| `DuplicateGoodsException` | 409 | 품목 중복 |
| `GodoApiException` | 502 | 고도몰 API 오류 |
| `WmsScrapingException` | 502 | WMS 수집 오류 |
| `ScheduleValidationException` | 400 | 스케줄 설정값 오류 |
| `Exception` (기타) | 500 | 서버 내부 오류 |

---

## 🗄️ 데이터 접근 설계

### SQLite + JPA 구성

```yaml
spring:
  datasource:
    url: jdbc:sqlite:sfood-automation.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate
    show-sql: false
```

**주의사항**

- SQLite는 동시 쓰기(Write)에 파일 레벨 락이 적용됨
- 단일 서버 운영이므로 배치 실행 중 API 요청의 쓰기 충돌 가능성을 고려
- HikariCP connection pool size는 최소값(1~3)으로 설정 권장

### QueryDSL 활용 범위

동적 조건이 필요한 조회에만 QueryDSL을 사용하고 단순 CRUD는 Spring Data JPA 기본 메서드를 사용한다.

| 대상 | 방식 |
|---|---|
| 품목 목록 조회 (다중 조건 필터) | QueryDSL |
| 상태 변경 로그 조회 (날짜·품목 복합) | QueryDSL |
| 배치 실행 로그 조회 | QueryDSL |
| 단건 조회, 저장, 삭제 | Spring Data JPA |

### 트랜잭션 경계

```
Application Layer (UseCase) 에서 @Transactional 선언
  - 단일 UseCase = 단일 트랜잭션 원칙
  - 배치 작업은 품목 단위로 트랜잭션 분리
    (1건 실패가 전체 롤백으로 이어지지 않도록)
  - 외부 API 호출은 트랜잭션 외부에서 실행
    (API 호출 성공 확인 후 DB 업데이트 트랜잭션 시작)
```

---

## 🔄 요청 처리 흐름 (API 요청)

```
HTTP Request
  │
  ▼
SessionAuthInterceptor          세션 쿠키 검증 (화이트리스트 제외)
  │
  ▼
RestController                  @Valid 입력 검증, DTO 변환
  │
  ▼
UseCase                         비즈니스 흐름 조율, @Transactional
  │
  ├──► Domain (Entity / Policy) 순수 비즈니스 로직 실행
  │
  └──► Repository (Port)        데이터 접근
         │
         ▼
      Infrastructure (JPA / External API)
  │
  ▼
ResponseDTO                     표준 응답 구조 반환
  │
  ▼
HTTP Response
```

---

## ⚙️ 환경 설정 구조

### application.yml 구성

```yaml
spring:
  datasource:
    url: jdbc:sqlite:${DB_PATH:sfood-automation.db}
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate
  session:
    timeout: 86400s                      # 24시간
  mail:
    host: ${MAIL_HOST}
    port: 25
    username: ${MAIL_USERNAME}
    properties:
      mail.smtp.auth: false
      mail.starttls.enable: true

godo:
  api:
    base-url: https://openhub.godo.co.kr
    partner-key: ${GODO_PARTNER_KEY}     # 환경변수 주입 필수
    key: ${GODO_KEY}                     # 환경변수 주입 필수

wms:
  login-url: https://wms.s-food.com/sessionService/login
  stock-url: https://wms.s-food.com/invnTotListService/searchAllStock
  user-id: ${WMS_USER_ID}               # 환경변수 주입 필수
  password: ${WMS_PASSWORD}             # 환경변수 주입 필수
  center-code: SLP001
  warehouse-code: OA110

slack:
  webhook-url: ${SLACK_WEBHOOK_URL}     # 환경변수 주입 필수

otp:
  server-secret: ${OTP_SERVER_SECRET}   # 환경변수 주입 필수, 해시 솔트
  expires-minutes: 5
  max-attempt: 5
  resend-limit-seconds: 60
```

### 환경변수 목록 (민감 정보)

| 환경변수 | 설명 | Git 노출 |
|---|---|---|
| `GODO_PARTNER_KEY` | 고도몰 제휴사 인증키 | 금지 |
| `GODO_KEY` | 고도몰 API 사용자 키 | 금지 |
| `WMS_USER_ID` | WMS 스크랩핑 전용 계정 ID | 금지 |
| `WMS_PASSWORD` | WMS 스크랩핑 전용 계정 PW | 금지 |
| `SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL | 금지 |
| `OTP_SERVER_SECRET` | OTP 해시 서버 시크릿 | 금지 |
| `MAIL_HOST` | SMTP 서버 호스트 | 금지 |
| `MAIL_USERNAME` | SMTP 계정 | 금지 |

---

## 📦 Gradle 의존성 구성

```groovy
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.springframework.session:spring-session-core'

    // SQLite
    implementation 'org.xerial:sqlite-jdbc:3.47.1.0'
    implementation 'org.hibernate.orm:hibernate-community-dialects'

    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // HTTP Client (WMS / Godo API)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'  // WebClient
    // 또는 Spring 6.1+ RestClient (spring-boot-starter-web 내장)

    // XML 파싱 (고도몰 API 응답)
    implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-xml'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
}

// QueryDSL Q클래스 생성 경로 설정
def querydslDir = "$buildDir/generated/querydsl"
sourceSets {
    main.java.srcDirs += [querydslDir]
}
tasks.withType(JavaCompile) {
    options.generatedSourceOutputDirectory = file(querydslDir)
}
```

---

## 🧪 테스트 전략

### 원칙

- 비즈니스 로직 구현 **전** 테스트 코드 작성 (TDD)
- 테스트 코드 이외 Mock 사용 금지
- JUnit 5 + Mockito 기반

### 레이어별 테스트 범위

| 레이어 | 테스트 종류 | 도구 |
|---|---|---|
| Domain (Policy / Entity) | 단위 테스트 | JUnit 5 (Mock 없이 순수 Java) |
| UseCase | 단위 테스트 | JUnit 5 + Mockito (Repository Mock) |
| Controller | 슬라이스 테스트 | @WebMvcTest |
| Repository | 슬라이스 테스트 | @DataJpaTest (H2 In-memory) |
| 배치 통합 | 통합 테스트 | @SpringBootTest |

### 핵심 테스트 시나리오

**GoodsSoldOutPolicy**

- Zone 코드 제외 목록 포함 시 SKIPPED 반환 확인
- 창고 코드 OA110 외 입력 시 SKIPPED 반환 확인
- GENERAL 범용: 합산 재고 ≤ 20 시 SET_OUT_OF_STOCK 반환
- EXCLUSIVE 전용: 합산 재고 ≤ 1 시 SET_OUT_OF_STOCK 반환
- GENERAL 정상 전환: 잔여율 ≥ 50 AND 재고 ≥ 21 시 SET_ON_SALE 반환
- 동일 입력 반복 시 동일 결과 반환 (멱등성)

**RequestEmailVerificationUseCase**

- 정상 이메일 입력 시 verificationKey 발급 확인
- `@s-food.com` 외 도메인 입력 시 실패 확인
- 60초 이내 재요청 시 429 반환 확인
- OTP 해시만 저장되고 원문은 저장되지 않음 확인

**ConfirmEmailVerificationUseCase**

- 올바른 코드 입력 시 VERIFIED 전환 및 세션 발급 확인
- 만료된 요청 시 410 반환 확인
- 코드 불일치 시 attemptCount 증가 확인
- 5회 초과 시 FAILED 처리 확인

**ErpCodeVerifier**

- ERP_SYNC_YN=N 품목만 검증 대상으로 처리됨 확인
- WMS 재고에 ITEM_CD 있을 시 ERP_SYNC_YN=Y 업데이트 확인
- WMS 재고에 ITEM_CD 없을 시 STATUS=ERROR 업데이트 확인

**DynamicSchedulerManager**

- enabled=true 설정만 서버 기동 시 등록됨 확인
- reschedule() 호출 시 기존 취소 후 재등록됨 확인

**BatchExecutionService**

- 실행 중 추가 실행 시 SKIPPED 처리 확인
- 성공 시 SUCCESS 로그 저장 확인
- 예외 발생 시 FAIL 로그 및 Slack 알림 확인

---

## 🚀 운영 고려사항

### 로그 전략

```
[애플리케이션 로그]
- 파일: logs/sfood-automation.log
- 레벨: INFO (운영), DEBUG (개발)
- 로테이션: 일별, 최대 30일 보관

[Slack 알림 대상]
- WMS 스크랩핑 실패
- 고도몰 API 연속 오류
- 배치 실행 FAIL
- 예상치 못한 RuntimeException

[DB 로그 (감사용)]
- goods_status_log: 상태 변경 근거 전량 보관
- api_transaction_log: API 송수신 전량 보관
- batch_execution_log: 실행 이력 전량 보관
```

### 배포 체크리스트

- `application.yml` 내 환경변수 전체 주입 확인
- `PRAGMA journal_mode=WAL` SQLite 설정 (쓰기 성능 개선)
- JVM 옵션: `--enable-preview` (Virtual Threads, Java 21 정식 지원)
- 초기 데이터 확인: `common_status_config` GENERAL/EXCLUSIVE 2건, `batch_schedule_config` product-sync 1건

### Virtual Threads 설정

```yaml
spring:
  threads:
    virtual:
      enabled: true     # Spring Boot 3.2+ 지원, Tomcat 스레드 자동 Virtual Thread 전환
```

I/O 바운드 작업(WMS HTTP, 고도몰 HTTP, SMTP, DB)에서 스레드 블로킹 없이 처리 효율 향상.
