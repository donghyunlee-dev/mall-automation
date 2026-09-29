# 📧 OTP 이메일 인증 설계 분석서

> 이메일 기반 6자리 OTP 인증 및 서버 세션 발급 아키텍처 분석 가이드

---

## 🎯 목적 및 배경

웹 화면에서 이메일 입력 후 **6자리 인증번호를 메일로 발송**하고, 사용자가 입력한 인증번호를 검증하여 메인 화면 진입을 허용하는 기능을 구현한다.

### 핵심 운영 조건

- **2개의 API** 호출로 인증 흐름 완성
- 인증번호는 **짧은 수명의 일회용 코드**로 처리
- 인증번호 **원문을 DB에 저장하지 않고 해시값만 저장** (보안)
- **SQLite** 기반 단순 운영 전제
- 인증 성공 시 **서버 세션** 발급

---

## ✅ 구현 목표

| 항목 | 내용 |
|------|------|
| 인증 방식 | 이메일 OTP 6자리 |
| OTP 생성 | `SecureRandom` 사용 |
| OTP 저장 | 해시값만 DB 저장 (원문 저장 금지) |
| 세션 발급 | 인증 성공 시 서버 세션 생성 |
| 만료 정책 | 기본 5분 |
| 시도 횟수 제한 | 기본 5회 |
| 재발송 제한 | 동일 이메일 60초 이내 재요청 제한 |
| 시간당 발송 제한 | 동일 이메일 1시간 내 5회 이하 |
| DB | SQLite |

---

## 🔄 전체 인증 흐름

### 인증 요청 흐름

```
사용자 이메일 입력
  → 이메일 형식 검증
  → Rate Limit 검사 (재발송 / 시간당 횟수)
  → verificationKey (UUID v4) 생성
  → OTP 6자리 생성 (SecureRandom)
  → OTP 해시 계산 및 DB 저장
  → 메일 발송
  → verificationKey 반환
```

### 인증 확인 흐름

```
사용자 OTP 입력
  → verificationKey + email + code 전달
  → DB에서 인증 세션 조회
  → 만료 여부 확인
  → 상태 확인 (PENDING 여부)
  → 시도 횟수 확인
  → 입력 code 해시 계산 후 저장값과 비교
  → 성공 시 VERIFIED 처리
  → 서버 세션 생성 후 성공 응답
  → 메인 화면 진입 허용
```

---

## 🌐 API 명세

### 이메일 인증 요청

```
POST /api/auth/email-verifications
```

**Request**
```json
{
  "email": "user@example.com"
}
```

**Response**
```json
{
  "verificationKey": "2d3a1f12-8d9e-4b44-9a2a-6d8c3b881234",
  "expiresInSeconds": 300,
  "message": "Verification code sent."
}
```

**응답 코드**

| 상태코드 | 조건 |
|----------|------|
| `201 Created` | 정상 발송 완료 |
| `400 Bad Request` | 이메일 형식 오류 |
| `429 Too Many Requests` | 재요청 / 시간당 발송 제한 초과 |

### 이메일 인증 확인

```
POST /api/auth/email-verifications/confirm
```

**Request**
```json
{
  "verificationKey": "2d3a1f12-8d9e-4b44-9a2a-6d8c3b881234",
  "email": "user@example.com",
  "code": "348271"
}
```

**Response**
```json
{
  "authenticated": true,
  "message": "Email verification completed."
}
```

> 인증 성공 시 서버 세션을 생성하고 `Set-Cookie` 헤더로 세션 쿠키를 반환한다.

**응답 코드**

| 상태코드 | 조건 |
|----------|------|
| `200 OK` | 인증 성공 |
| `400 Bad Request` | 입력 형식 오류 |
| `401 Unauthorized` | 코드 불일치 |
| `409 Conflict` | 이미 처리 완료됨 (VERIFIED / FAILED) |
| `410 Gone` | 인증 만료 |
| `429 Too Many Requests` | 시도 횟수 초과 |

---

## 🔑 verificationKey 정책

| 항목 | 내용 |
|------|------|
| 역할 | 인증 요청 단위 식별, 이메일과 OTP 검증 대상 연결 |
| 생성 방식 | UUID v4 |
| 보안 요건 | 외부 노출값이므로 예측 불가능해야 함 |
| 저장 정책 | DB에 UNIQUE 저장, 인증 확인 시 주요 조회 기준 |

---

## 🔐 보안 설계

### OTP 해시 저장 방식

> 인증번호 원문을 DB에 저장하지 않는다.

**해시 계산 방식 (권장)**

```
SHA-256(verificationKey + ":" + code + ":" + serverSecret)
```

**검증 방식**
```
사용자 입력 code
  → 동일 방식으로 해시 계산
  → DB 저장값(codeHash)과 비교
```

**원문 저장 금지 이유**
- OTP는 복호화가 필요하지 않음
- DB 유출 시 OTP 원문 노출 방지
- 해시 비교로 충분히 검증 가능

### OTP 생성 정책

| 항목 | 내용 |
|------|------|
| 형식 | 숫자 6자리 문자열 (앞자리 0 허용) |
| 생성기 | `SecureRandom` 사용 (`Random` 사용 금지) |
| 처리 타입 | 반드시 `String` 으로 처리 (정수형 금지) |
| 유효시간 | 5분 |
| 최대 시도 | 5회 |

---

## 📦 도메인 모델 설계

### EmailVerification

> 이메일 인증 세션 애그리게이트 루트

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `verificationKey` | String | UUID v4, UNIQUE |
| `email` | String | 소문자 정규화 저장 |
| `codeHash` | String | OTP 해시값 |
| `status` | VerificationStatus | 현재 상태 |
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

### VerificationStatus

```
PENDING    → 인증 대기
VERIFIED   → 인증 성공
EXPIRED    → 만료됨
FAILED     → 시도 횟수 초과 또는 정책 위반
CANCELLED  → 재발송 등으로 기존 세션 무효화
```

**상태 전이 규칙**

```
PENDING → VERIFIED
PENDING → EXPIRED
PENDING → FAILED
PENDING → CANCELLED
```

---

## 🗄️ DB 설계 (SQLite)

### email_verification

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

## 🏗️ 아키텍처 설계

Clean Architecture 원칙 기반으로 레이어를 분리한다.

### 레이어 구조

```
Presentation Layer
└── EmailVerificationController

Application Layer
├── RequestEmailVerificationUseCase
└── ConfirmEmailVerificationUseCase

Domain Layer
├── EmailVerification       ← 애그리게이트 루트
├── VerificationStatus      ← Enum
└── OtpPolicy               ← 도메인 정책 (유효시간, 시도 횟수 등)

Infrastructure Layer
├── EmailVerificationRepository
├── OtpGenerator
├── OtpHashProvider
├── MailSender
├── SessionIssuer           ← 인증 성공 후 서버 세션 생성
└── SQLite persistence
```

### 패키지 구조 (권장)

```
com.example.auth
├── presentation
│   └── EmailVerificationController
├── application
│   ├── usecase
│   │   ├── RequestEmailVerificationUseCase
│   │   └── ConfirmEmailVerificationUseCase
│   └── dto
│       ├── EmailVerificationRequest
│       ├── EmailVerificationResponse
│       ├── ConfirmVerificationRequest
│       └── ConfirmVerificationResponse
├── domain
│   ├── model
│   │   └── EmailVerification
│   ├── policy
│   │   └── OtpPolicy
│   └── enums
│       └── VerificationStatus
└── infrastructure
    ├── persistence
    │   └── EmailVerificationRepository
    ├── otp
    │   ├── OtpGenerator
    │   └── OtpHashProvider
    ├── mail
    │   └── MailSender
    └── session
        └── SessionIssuer
```

---

## ⚙️ 핵심 컴포넌트 책임

### RequestEmailVerificationUseCase

| 처리 단계 | 내용 |
|----------|------|
| 이메일 검증 | trim → lowercase → 형식 검증 |
| Rate Limit 확인 | 60초 이내 재요청 / 1시간 내 5회 초과 여부 |
| 기존 세션 처리 | 기존 PENDING 세션 → CANCELLED 처리 |
| verificationKey 생성 | UUID v4 |
| OTP 생성 | `OtpGenerator` (SecureRandom) |
| 해시 저장 | `OtpHashProvider` → DB 저장 |
| 메일 발송 | `MailSender` 호출 |
| 응답 반환 | verificationKey + expiresInSeconds |

### ConfirmEmailVerificationUseCase

| 처리 단계 | 내용 |
|----------|------|
| 세션 조회 | verificationKey 기준 DB 조회 |
| 이메일 일치 확인 | 요청 email vs 저장 email 비교 |
| 만료 여부 확인 | `expiresAt` 기준 |
| 상태 확인 | PENDING 여부 검증 |
| 시도 횟수 확인 | `attemptCount < maxAttemptCount` |
| 해시 비교 | 입력 code → 해시 계산 → `codeHash` 비교 |
| 성공 처리 | `status = VERIFIED`, `verifiedAt` 기록 |
| 세션 발급 | `SessionIssuer` 호출 → `Set-Cookie` 응답 |

### OtpGenerator

```java
// SecureRandom 기반 6자리 숫자 문자열 생성
// String.format("%06d", random.nextInt(1_000_000)) 방식 사용
// 앞자리 0 허용
```

### OtpHashProvider

```java
// SHA-256(verificationKey + ":" + code + ":" + serverSecret)
// 생성 메서드와 검증 메서드를 분리하여 제공
```

### SessionIssuer

```java
// 인증 성공 시 HttpSession 생성
// 세션에 인증된 email 정보 저장
// 응답에 Set-Cookie 헤더 포함
```

---

## 🔄 재발송 정책

> 기존 세션을 재사용하지 않는다. 항상 새 세션으로 시작한다.

```
재발송 요청
  → Rate Limit 검사 (60초 이내 재요청 여부)
  → 기존 PENDING 세션 → CANCELLED 처리
  → 새 verificationKey 발급
  → 새 OTP 생성
  → 새 세션으로 다시 인증 진행
```

이 방식을 권장하는 이유: 가장 단순하고 운영 혼선이 적다.

---

## 📝 입력 검증 정책

### 이메일

- `trim()` 적용
- lowercase 정규화
- 이메일 형식 정규식 검증

### OTP code

- 반드시 `String` 타입으로 수신
- 숫자 6자리 정규식 적용 (`^\d{6}$`)
- 정수형으로 파싱하지 않음 (앞자리 0 손실 방지)

---

## 📬 메일 발송 정책

**제목**
```
Your verification code
```

**본문**
```
Verification code: 348271
This code expires in 5 minutes.
If you did not request this, please ignore this email.
```

- 링크 방식이 아닌 **숫자 직접 입력 방식**으로 구현
- 메일 발송 실패 시 인증 세션 저장 롤백 또는 예외 응답 처리 필요

---

## 🧪 테스트 우선 원칙 및 시나리오

비즈니스 로직 구현 **전에 테스트를 먼저 작성**한다.

### RequestEmailVerificationUseCase 테스트

- 정상 이메일 입력 시 verificationKey가 생성되는지 확인
- OTP 해시가 DB에 저장되는지 확인
- 메일 발송이 호출되는지 확인
- 60초 이내 재요청 시 실패하는지 확인
- 잘못된 이메일 형식이면 실패하는지 확인

### ConfirmEmailVerificationUseCase 테스트

- 올바른 code 입력 시 VERIFIED 처리되는지 확인
- 인증 성공 시 세션이 생성되는지 확인
- 만료된 요청은 실패하는지 확인
- code 불일치 시 `attemptCount`가 증가하는지 확인
- 최대 시도 횟수 초과 시 FAILED 처리되는지 확인
- 이미 VERIFIED 상태는 재검증되지 않는지 확인

### OtpHashProvider 테스트

- 동일 입력에 대해 동일 해시가 생성되는지 확인
- 다른 code 입력 시 다른 해시가 생성되는지 확인

### OtpGenerator 테스트

- 6자리 숫자 문자열이 생성되는지 확인
- 앞자리 0이 허용되는지 확인 (`"000123"` 형태)

---

## ⚠️ 구현 제약

| 항목 | 내용 |
|------|------|
| OTP 원문 저장 | DB 저장 절대 금지 |
| OTP 생성기 | `SecureRandom` 강제 (`Random` 금지) |
| code 타입 | 반드시 `String` 처리 |
| 검증 방식 | 해시 비교 방식 사용 |
| Mock 사용 | 테스트 코드 이외 Mock 사용 금지 |
| 원칙 | SOLID + Clean Architecture |
| 코드 스타일 | Clean Code |
| 환경 | Java 21, Spring Boot 3.x, Gradle, SQLite |

---

## 🚀 구현 우선 항목

> 초기 버전은 아래 항목만 구현한다

- 이메일 인증 요청 API
- 이메일 인증 확인 API
- OTP 생성 (SecureRandom)
- OTP 해시 저장 (SHA-256)
- 시도 횟수 제한
- 만료 정책
- 재발송 제한 (60초)
- 서버 세션 발급

---

## 📌 추후 확장 계획

- 시간당 발송 횟수 제한 강화
- 관리자용 인증 이력 조회 API
- IP 기반 Rate Limit 추가
- 메일 발송 재시도 정책 (비동기 큐)

---

## 📄 산출물 요구사항

| 항목 | 설명 |
|------|------|
| 클래스 목록 | 레이어별 전체 클래스 및 책임 |
| 책임 분리 설명 | 각 컴포넌트 역할 정의 |
| API 명세 | Request / Response 포함 |
| 테스트 코드 | 시나리오별 JUnit 5 기반 |
| 구현 코드 | Clean Architecture 기반 |
| 메일 발송 연동 포인트 | MailSender 구현 설명 |
| 운영 주의사항 | 배포 및 운영 시 고려사항 |
