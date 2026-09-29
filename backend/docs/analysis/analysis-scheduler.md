# 🗓️ Dynamic Scheduler 설계 분석서

> Spring Boot 기반 Quartz-free 동적 스케줄 아키텍처 분석 및 구현 가이드

---

## 🎯 목적 및 배경

Spring Boot 백엔드 서비스에서 **Quartz 없이** 런타임 스케줄 등록·변경·중지가 가능한 구조를 구현한다.

### 핵심 운영 요구사항

- 관리자 화면을 통해 **운영 중 배치 주기 변경** 가능
- 애플리케이션 **재기동 후 마지막 설정 자동 복원**
- **단일 서버 / 소규모 웹서비스 / SQLite** 기반 운영 전제

---

## ✅ 구현 목표

| 항목 | 내용 |
|------|------|
| 스케줄 엔진 | Spring `TaskScheduler` 기반 (Quartz 제외) |
| 변경 방식 | 런타임 동적 재등록 |
| 설정 저장 | DB(SQLite) 영속화 |
| 재기동 복원 | 시작 시 DB 설정 자동 로드 |
| 중복 실행 방지 | `AtomicBoolean` 기반 단일 서버 락 |
| 실행 로그 | DB 저장 |
| 초기 지원 방식 | Fixed Delay 단일 방식 |

---

## 🚧 범위 정의

### 포함

- 배치 설정 저장 및 CRUD
- 배치 활성화 / 비활성화
- Fixed Delay 기반 동적 재등록
- 서버 시작 시 스케줄 자동 등록 (ApplicationRunner)
- 배치 실행 로그 저장
- 수동 실행 API
- 중복 실행 방지
- 관리자 조회 / 수정 API

### 제외 (추후 확장 대상)

- Quartz 스케줄러
- 분산 락 (Redisson 등)
- 클러스터링 / 복수 인스턴스 동기화
- Cron 표현식 다중 지원
- 실패 알림 (Slack, Email)

---

## 🏗️ 아키텍처 설계

Clean Architecture 원칙을 기반으로 레이어를 분리한다.

### 레이어 구조

```
Presentation Layer
└── BatchScheduleAdminController

Application Layer
├── GetBatchScheduleUseCase
├── UpdateBatchScheduleUseCase
└── RunBatchNowUseCase

Domain Layer
├── BatchScheduleConfig        ← 스케줄 설정 애그리게이트
├── BatchExecutionLog          ← 실행 로그 엔티티
├── BatchSchedulePolicy        ← 도메인 정책 (최솟값 검증 등)
├── BatchJobType               ← Enum (향후 확장용)
└── BatchExecutionStatus       ← Enum: SUCCESS / FAIL / SKIPPED

Infrastructure Layer
├── BatchScheduleConfigRepository
├── BatchExecutionLogRepository
├── DynamicSchedulerManager    ← 핵심 스케줄 관리 컴포넌트
├── TaskSchedulerConfig
├── ProductSyncBatchJob        ← 실제 비즈니스 배치
└── SQLite persistence
```

### 패키지 구조 (권장)

```
com.example.scheduler
├── presentation
│   └── BatchScheduleAdminController
├── application
│   ├── usecase
│   │   ├── GetBatchScheduleUseCase
│   │   ├── UpdateBatchScheduleUseCase
│   │   └── RunBatchNowUseCase
│   └── dto
│       ├── BatchScheduleResponse
│       └── UpdateBatchScheduleRequest
├── domain
│   ├── model
│   │   ├── BatchScheduleConfig
│   │   └── BatchExecutionLog
│   ├── policy
│   │   └── BatchSchedulePolicy
│   └── enums
│       ├── BatchJobType
│       ├── BatchExecutionStatus
│       └── BatchScheduleType
└── infrastructure
    ├── persistence
    │   ├── BatchScheduleConfigRepository
    │   └── BatchExecutionLogRepository
    ├── scheduler
    │   ├── DynamicSchedulerManager
    │   └── TaskSchedulerConfig
    └── job
        ├── ProductSyncBatchJob
        └── BatchExecutionService
```

---

## 📦 도메인 모델 설계

### BatchScheduleConfig

> 배치 스케줄 설정 애그리게이트 루트

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `jobName` | String | 잡 식별자 (UNIQUE) |
| `enabled` | boolean | 활성화 여부 |
| `scheduleType` | BatchScheduleType | 초기: `FIXED_DELAY` 고정 |
| `fixedDelaySeconds` | int | 실행 간격 (최소 60초) |
| `initialDelaySeconds` | int | 초기 지연 시간 (기본 0) |
| `description` | String | 설명 |
| `lastModifiedBy` | String | 마지막 수정자 |
| `createdAt` | LocalDateTime | 생성 일시 |
| `updatedAt` | LocalDateTime | 수정 일시 |

**제약 조건**
- `jobName` → UNIQUE 보장
- `fixedDelaySeconds` → 최소 60초 이상 (정책 레이어에서 검증)
- `enabled = false` → 스케줄 등록 대상에서 제외

### BatchExecutionLog

> 배치 실행 이력 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `jobName` | String | 잡 식별자 |
| `startedAt` | LocalDateTime | 시작 일시 |
| `endedAt` | LocalDateTime | 종료 일시 |
| `status` | BatchExecutionStatus | SUCCESS / FAIL / SKIPPED |
| `message` | String | 결과 요약 메시지 |
| `triggeredBy` | String | SCHEDULER / MANUAL |
| `durationMs` | Long | 실행 소요 시간 (ms) |
| `createdAt` | LocalDateTime | 저장 일시 |

---

## 🗄️ DB 설계 (SQLite)

### batch_schedule_config

```sql
CREATE TABLE batch_schedule_config (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name             TEXT NOT NULL UNIQUE,
    enabled              INTEGER NOT NULL DEFAULT 1,
    schedule_type        TEXT NOT NULL DEFAULT 'FIXED_DELAY',
    fixed_delay_seconds  INTEGER NOT NULL,
    initial_delay_seconds INTEGER NOT NULL DEFAULT 0,
    description          TEXT,
    last_modified_by     TEXT,
    created_at           TEXT NOT NULL,
    updated_at           TEXT NOT NULL
);
```

### batch_execution_log

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

---

## ⚙️ 핵심 컴포넌트 책임

### DynamicSchedulerManager

> 런타임 스케줄 생명주기 전담 컴포넌트

**내부 상태**
```java
private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
```

**주요 메서드**

| 메서드 | 역할 |
|--------|------|
| `initializeSchedules()` | 서버 시작 시 DB 설정 읽어 자동 등록 |
| `register(BatchScheduleConfig)` | 신규 스케줄 등록 |
| `reschedule(BatchScheduleConfig)` | 기존 취소 후 새 설정으로 재등록 |
| `cancel(String jobName)` | 특정 잡 스케줄 취소 |
| `cancelAll()` | 전체 스케줄 취소 |

### BatchExecutionService

> 실행 전/후 처리 및 중복 방지 담당

**처리 흐름**
```
실행 요청
  → AtomicBoolean.compareAndSet(false, true) 확인
  → false면 → SKIPPED 로그 저장 후 종료
  → true면  → startedAt 기록
            → 배치 잡 호출 (ProductSyncBatchJob)
            → finally: AtomicBoolean 해제
            → 성공/실패 로그 저장
```

**중요**: 스케줄 관리 로직과 비즈니스 로직은 완전히 분리한다.

### ProductSyncBatchJob

> 실제 비즈니스 배치 로직만 포함

- 상품 조회 / 저장 / 변경 이력 처리
- 스케줄 관련 코드 일절 포함하지 않음
- 순수 비즈니스 처리 단위

---

## 🌐 관리자 API 명세

### 스케줄 조회

```
GET /api/admin/batch-schedules/{jobName}
```

**Response**
```json
{
  "jobName": "product-sync",
  "enabled": true,
  "fixedDelaySeconds": 600,
  "running": false,
  "lastStartedAt": "2025-03-30T10:00:00",
  "lastEndedAt": "2025-03-30T10:00:12",
  "lastStatus": "SUCCESS",
  "lastMessage": "상품 512건 동기화 완료",
  "updatedAt": "2025-03-29T09:00:00"
}
```

### 스케줄 수정

```
PUT /api/admin/batch-schedules/{jobName}
```

**Request**
```json
{
  "enabled": true,
  "fixedDelaySeconds": 600,
  "initialDelaySeconds": 10,
  "modifiedBy": "admin@example.com"
}
```

**처리 흐름**
```
입력값 검증 (fixedDelaySeconds ≥ 60)
  → DB 저장
  → 기존 스케줄 취소
  → 새 스케줄 등록
```

### 수동 실행

```
POST /api/admin/batch-schedules/{jobName}/run
```

**처리 규칙**
- 현재 실행 중이면 `SKIPPED` 처리 후 응답
- `triggeredBy = MANUAL` 로그 저장
- 정상 시 즉시 실행 후 결과 응답

---

## 📋 운영 정책

### Fixed Delay 최솟값 정책

- `fixedDelaySeconds` 최소 **60초** 이상 강제
- 미만 입력 시 400 Bad Request 응답

### 중복 실행 방지 정책

- 이미 실행 중인 경우 새 실행 요청을 무시
- 실행 로그에 `SKIPPED` 상태로 기록

### 실패 처리 정책

- 배치 실패 시 스케줄 자체는 유지 (자동 취소 없음)
- DB에는 요약 메시지만 저장
- 전체 스택 트레이스는 Application 로그(파일)에만 기록

### 재기동 복원 정책

- `enabled = true` 인 설정만 자동 등록
- 특정 잡 등록 실패 시 **해당 잡만 실패 처리** (전체 기동 중단 없음)
- `ApplicationRunner` 또는 `@PostConstruct` 에서 초기화 수행

---

## 🔄 전체 실행 흐름

```
[서버 시작]
  ApplicationRunner.run()
    └─ DynamicSchedulerManager.initializeSchedules()
         └─ BatchScheduleConfigRepository.findAllEnabled()
              └─ 각 config에 대해 register(config) 호출
                   └─ TaskScheduler.scheduleWithFixedDelay(task, delay)

[스케줄 실행 시점]
  TaskScheduler trigger
    └─ BatchExecutionService.execute(jobName, SCHEDULER)
         ├─ AtomicBoolean.compareAndSet(false, true) 확인
         ├─ (실행 중이면 SKIPPED 로그 후 종료)
         ├─ ProductSyncBatchJob.run()
         └─ finally: AtomicBoolean 해제 + 실행 로그 저장

[관리자 수정 요청]
  PUT /api/admin/batch-schedules/{jobName}
    └─ UpdateBatchScheduleUseCase.execute()
         ├─ BatchSchedulePolicy.validate()
         ├─ BatchScheduleConfigRepository.save()
         └─ DynamicSchedulerManager.reschedule(config)
              ├─ cancel(jobName)
              └─ register(config)
```

---

## 🧪 테스트 우선 원칙 및 시나리오

비즈니스 로직 구현 **전에 테스트를 먼저 작성**한다.

### DynamicSchedulerManager 테스트

- `enabled = true` 설정이 등록되는지 확인
- `enabled = false` 설정은 등록되지 않는지 확인
- 기존 스케줄 취소 후 재등록이 정상 처리되는지 확인

### BatchExecutionService 테스트

- 실행 중이 아닐 때 정상 실행되는지 확인
- 실행 중일 때 SKIPPED 처리되는지 확인
- 성공 시 SUCCESS 로그 저장 여부 확인
- 실패 시 FAIL 로그 저장 여부 확인

### UpdateBatchScheduleUseCase 테스트

- 설정 변경 후 DB에 반영되는지 확인
- 변경 후 `reschedule()` 호출되는지 확인
- `fixedDelaySeconds < 60` 입력 시 거부되는지 확인

### Startup Initialization 테스트

- 서버 시작 시 `enabled` 설정이 자동 등록되는지 확인
- 특정 잡 등록 실패 시 해당 잡만 실패 처리되는지 확인

---

## ⚠️ 구현 제약

| 항목 | 내용 |
|------|------|
| 프레임워크 | 불필요한 의존성 추가 금지 |
| 스케줄러 | Quartz 사용 금지 |
| Mock 사용 | 테스트 코드 이외 Mock 사용 금지 |
| 원칙 | SOLID + Clean Architecture |
| 코드 스타일 | Clean Code |
| 환경 | Java 21, Spring Boot 3.x, Gradle, SQLite |

---

## 🚀 초기 구현 우선 항목

> 초기 버전은 아래 항목만 구현한다

- 단일 Job (product-sync)
- Fixed Delay 방식만 지원
- DB 설정 저장 (SQLite)
- 동적 재등록
- 실행 로그 저장
- 수동 실행 API
- 중복 실행 방지

---

## 📌 추후 확장 계획

- Cron 표현식 지원
- 복수 Job 관리
- 상태 대시보드 UI
- 실패 알림 연동 (Slack / Email)
- 분산 환경 지원 (Redis 기반 락)

---

## 📄 산출물 요구사항

| 항목 | 설명 |
|------|------|
| 클래스 목록 | 레이어별 전체 클래스 및 책임 |
| 책임 분리 설명 | 각 컴포넌트 역할 정의 |
| API 명세 | Request / Response 포함 |
| 테스트 코드 | 시나리오별 JUnit 5 기반 |
| 구현 코드 | Clean Architecture 기반 |
| 실행 방법 | 로컬 구동 가이드 |
| 운영 주의사항 | 배포 및 운영 시 고려사항 |
