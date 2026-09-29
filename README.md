# SFOOD-MALL-AUTOMATION

에쓰푸드몰 상품 판매 상태를 자동으로 동기화하고 운영자가 직접 관리할 수 있도록 만든 내부 운영 도구 프로젝트입니다. 저장된 상품 기준값, 고도몰 상태, WMS 재고, 배치 실행 결과를 기준으로 판매 상태를 관리합니다.

루트 프로젝트는 `backend`와 `frontend` 두 워크스페이스로 나뉘어 있으며, 각 영역은 문서 주도 방식으로 관리됩니다.

## 구성

### Backend

- 기술 스택: Java 21, Spring Boot 3.5, Spring Data JPA, SQLite, PostgreSQL runtime driver, JUnit 5
- 주요 역할:
  - 이메일 OTP 인증과 세션 발급
  - 상품 조회/등록/수정/삭제
  - 공통 판매 상태 기준 관리
  - 고도몰 상태 강제 동기화 및 전송 차단 설정
  - WMS 재고 수집 및 상태 판단
  - 배치 스케줄 조회, 수정, 수동 실행
  - Slack 오류 알림 및 실행 로그 관리

### Frontend

- 기술 스택: React 19, TypeScript, Vite, SCSS Modules, Axios, Vitest
- 주요 역할:
  - OTP 로그인 화면
  - 상단 탭 네비게이션 (상품 관리 / 회원 관리)
  - 대시보드 요약 카드
  - 상품 검색, 필터, 정렬, 페이지네이션
  - 상품 추가/수정/삭제, 엑셀 업로드
  - 배치 실행 및 간격 변경
  - 상태 변경 이력 확인
  - Teams 웹훅 알림 전송
  - 회원 목록 조회 (서버 페이징·검색·필터), 회원 상세, 회원 엑셀 업로드, 파트너 코드 수정

## 디렉터리

```text
.
├── backend/
│   ├── src/
│   └── docs/
└── frontend/
    ├── src/
    └── docs/
```

## 빠른 시작

### 1. 요구 사항

- Java 21
- Node.js 20 이상
- npm

### 2. 백엔드 실행

```bash
cd backend
./gradlew test
./gradlew bootRun
```

기본적으로 SQLite를 사용하며, 애플리케이션은 `backend/src/main/resources/application.yml` 기준으로 동작합니다. 실제 운영 또는 공유 개발 환경에서는 아래 값을 환경 변수로 반드시 덮어써야 합니다.

- `DB_PATH`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `GODO_BASE_URL`
- `GODO_PARTNER_KEY`
- `GODO_KEY`
- `WMS_LOGIN_URL`
- `WMS_STOCK_URL`
- `WMS_USER_ID`
- `WMS_PASSWORD`
- `WMS_CENTER_CODE`
- `WMS_WAREHOUSE_CODE`
- `SLACK_WEBHOOK_URL`
- `OTP_SERVER_SECRET`

### 3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run build
npm run lint
npm run dev
```

프론트엔드는 기본적으로 `VITE_API_BASE_URL`을 사용해 백엔드 API를 호출합니다.

예시:

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_DASHBOARD_URL=http://localhost:5173
```

## 핵심 API 범위

현재 코드 기준으로 다음 API 그룹이 구현되어 있습니다.

- 인증: `/api/auth/email-verifications`, `/api/auth/email-verifications/confirm`, `/api/auth/session`, `/api/auth/me`, `/api/auth/logout`
- 상품: `/api/goods`, `/api/goods/{id}`, `/api/goods/status-logs`, `/api/goods/godo-status/force-sync`
- 배치: `/api/admin/batch-schedules/{jobName}`, `/api/admin/batch-schedules/{jobName}/run`
- 회원: `/api/members`, `/api/members/summary`, `/api/members/{memNo}`, `/api/members/{memNo}/partner`, `/api/members/uploads`

세부 계약은 각 제품 문서를 확인하면 됩니다.

- Backend API 문서: [backend/docs/product/api-spec.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/product/api-spec.md)
- Frontend API 가이드: [frontend/docs/guide/api-guide.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/docs/guide/api-guide.md)

## 문서 기준 작업 방식

이 저장소는 코드보다 문서를 먼저 정리하는 방식을 전제로 합니다.

### Backend 작업 기준

작업 전 우선 확인:

- [backend/AGENTS.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/AGENTS.md)
- [backend/docs/guide/spec-driven-workflow.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/guide/spec-driven-workflow.md)
- [backend/docs/guide/development-rules.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/guide/development-rules.md)
- [backend/docs/guide/testing-strategy.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/guide/testing-strategy.md)
- [backend/docs/guide/done-criteria.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/guide/done-criteria.md)
- [backend/docs/product/prd.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/product/prd.md)

비사소한 작업은 아래 구조를 유지합니다.

```text
backend/docs/tasks/{task-id}/
├── spec.md
├── plan.md
├── tasks.md
├── test-result.md
└── result.md
```

### Frontend 작업 기준

작업 전 우선 확인:

- [frontend/CLAUDE.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/CLAUDE.md)
- [frontend/docs/guide/development-rules.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/docs/guide/development-rules.md)
- [frontend/docs/guide/testing-strategy.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/docs/guide/testing-strategy.md)
- [frontend/docs/guide/done-criteria.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/docs/guide/done-criteria.md)
- [frontend/docs/product/prd.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/docs/product/prd.md)

프론트엔드는 이터레이션 중심으로 관리하며 다음 산출물을 남깁니다.

```text
frontend/docs/plan/iteration/iteration-XX-plan.md
frontend/docs/test/results/iteration-XX-test-result.md
frontend/docs/reports/iteration-XX-result.md
```

## 검증 기준

### Backend

```bash
cd backend
./gradlew test
```

### Frontend

```bash
cd frontend
npm run build
npm run lint
npm run test
```

문서상 기준은 백엔드는 테스트 결과 문서, 프론트엔드는 빌드/린트와 이터레이션 결과 문서까지 포함해야 완료로 간주합니다.

## 참고 문서

- Backend 제품 요구사항: [backend/docs/product/prd.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/product/prd.md)
- Backend 아키텍처: [backend/docs/product/architectures.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/product/architectures.md)
- Backend 데이터 명세: [backend/docs/product/data-spec.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/product/data-spec.md)
- Frontend 제품 요구사항: [frontend/docs/product/prd.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/docs/product/prd.md)
- Frontend 아키텍처: [frontend/docs/product/architectures.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/docs/product/architectures.md)
- Frontend 디자인 기준: [frontend/docs/design/page.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/frontend/docs/design/page.md)

## 주의 사항

- 고도몰 품절 상태 변경 API는 문서상 사전 승인 없이 실제 호출하면 안 됩니다.
- 저장소 규칙상 민감한 설정은 환경 변수로 주입해야 하며, 공유 환경에서는 로컬 기본값 사용을 피해야 합니다.
- 현재 작업 트리는 완전히 깨끗하지 않을 수 있으므로, 문서나 코드 수정 전 `git status` 확인을 권장합니다.
