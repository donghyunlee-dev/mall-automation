# Architecture — SFOOD-MALL-AUTOMATION-FRONTEND

> 에쓰푸드몰 운영 관리 프론트엔드 (상품 판매 상태 자동화 + 거래처 등록 관리)  
> 참조 문서: api-spec.md / api-spec-member.md  
> 작성일: 2026-04-01

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
│              SFOOD-MALL-AUTOMATION-FRONTEND                           │
│              React 18 / TypeScript / Vite / AWS EC2 + Nginx          │
│                                                                       │
│   ┌──────────────┐  ┌──────────────────────┐  ┌──────────────────┐  │
│   │  LoginPage   │  │  GoodsPage           │  │  MembersPage /   │  │
│   │  (OTP 인증)  │  │  (상품 현황/배치)    │  │  MemberDetailPage│  │
│   └──────────────┘  └──────────────────────┘  └──────────────────┘  │
│   (AuthedLayout: 전역 Header + TabBar 하위에 상품/회원 페이지 배치)   │
│                                                                       │
│   ┌──────────────────────────────────────────────────────────────┐   │
│   │   src/api/  (Axios + 인터셉터)                               │   │
│   │   auth.ts / goods.ts / batch.ts / teams.ts / members.ts      │   │
│   └──────────────────────────────────────────────────────────────┘   │
│                                                                       │
│   ┌──────────────────────────────────────────────────────────────┐   │
│   │   src/hooks/useTeamsNotification.ts                          │   │
│   │   배치 폴링 감지 → 변경 이력 확인 → 알림 메시지 발송         │   │
│   └──────────────────────────────────┬───────────────────────────┘   │
└──────────────────────────┬───────────┼───────────────────────────────┘
                           │           │ HTTPS (HTTP 트리거)
                           │           ▼
                           │  ┌─────────────────────────┐
                           │  │  Power Automate          │
                           │  │  (HTTP 트리거 플로우)    │
                           │  └────────────┬────────────┘
                           │               │
                           │               ▼
                           │  ┌─────────────────────────┐
                           │  │  Microsoft Teams 채팅    │
                           │  │  (채팅 메시지 게시)      │
                           │  └─────────────────────────┘
                           │
                           │ HTTP (세션 쿠키)
                           ▼
              ┌────────────────────────┐
              │  SFOOD-MALL-AUTOMATION │
              │  BACKEND (Spring Boot) │
              └────────────────────────┘
```

---

## 🗂️ 레이어 구조

```
Pages
  └─ 라우팅 단위 컴포넌트 (LoginPage, GoodsPage, MembersPage, MemberDetailPage)
  └─ 훅과 컴포넌트를 조합

Components
  └─ UI 단위 컴포넌트 (Props로 데이터를 받아 렌더링만 수행)
  └─ layout/ : Header, TabBar, AuthedLayout, AvatarDropdown
  └─ dashboard/ : SummaryCards, ProductTable, HistoryTable 등
  └─ members/ : MemberSummaryCards, MemberSearchBar, MemberTable, PartnerCodeEditModal
  └─ modals/ : 상품 추가/수정/삭제/엑셀업로드, 회원 엑셀업로드 모달

Hooks
  └─ 비즈니스 로직 및 상태 관리
  └─ useAuth.ts / useGoods.ts / useBatch.ts / useTeamsNotification.ts
  └─ useMembers.ts / useMemberUpload.ts / useMemberDetail.ts

API
  └─ 외부 통신 전담
  └─ client.ts (Axios 인스턴스, 인터셉터)
  └─ auth.ts / goods.ts / batch.ts / teams.ts (Teams 웹훅 발송) / members.ts

Types
  └─ Props, API 요청/응답 타입 정의
  └─ auth.ts / goods.ts / batch.ts / member.ts

Styles
  └─ CSS Modules (.module.scss) — 컴포넌트 단위
  └─ _variables.scss — 전역 디자인 토큰 (CSS 변수)
  └─ global.scss — 전역 리셋 및 @use
```

---

## 🔄 데이터 흐름

```
사용자 인터랙션
  → Page / Component (이벤트 발생)
  → Hook (상태 업데이트, API 호출 조율)
  → API 함수 (HTTP 요청 생성)
  → client.ts (Axios 인스턴스로 전송)
  → Backend REST API
  → 응답 반환
  → Hook (상태 갱신)
  → Component (리렌더링)
```

---

## 🔐 인증 흐름

```
1. /login 접근
2. 이메일 입력 → OTP 발송 요청 (POST /api/auth/email-verifications)
3. OTP 입력 → 세션 발급 요청 (POST /api/auth/email-verifications/confirm)
4. 백엔드: Set-Cookie: SESSION=xxx; HttpOnly
5. 이후 모든 요청: 브라우저가 쿠키 자동 포함 (withCredentials: true)
6. 401 수신 시 /login 자동 리다이렉트 (client.ts 인터셉터)
```

---

## 🌐 라우팅

| 경로 | 컴포넌트 | 인증 필요 |
|---|---|---|
| `/login` | `LoginPage` | 없음 |
| `/goods` | `GoodsPage` | 있음 |
| `/members` | `MembersPage` | 있음 |
| `/members/:memNo` | `MemberDetailPage` | 있음 |
| `/` | → `/goods` 리다이렉트 | 있음 |

- 보호 경로는 `AuthedLayout`(전역 Header + TabBar) 하위에 배치
- 세션 없이 보호 경로 접근 시 `/login`으로 리다이렉트
- 로그인 성공 시 `/`(→ `/goods`)으로 이동

---

## 🚀 배포 구조

```
개발
  → npm run build → dist/

배포
  → AWS EC2 (Nginx)
  → Nginx: dist/ 정적 파일 서빙
  → Nginx: /api/* 요청을 Backend(Spring Boot)로 프록시
```

### 환경변수

| 파일 | 용도 |
|---|---|
| `.env` | 로컬 개발 (`VITE_API_BASE_URL=http://localhost:8080`) |
| `.env.production` | 운영 배포 (`VITE_API_BASE_URL=http://{EC2_HOST}:{PORT}`) |

**Teams 웹훅 환경변수**

| 변수명 | 설명 |
|---|---|
| `VITE_TEAMS_WEBHOOK_URL` | Power Automate HTTP 트리거 URL |

- 연동 방식: Power Automate HTTP 트리거 → Teams 채팅(1:1 또는 그룹 채팅)에 메시지 게시
- URL은 코드에 하드코딩하지 않고 환경변수로만 관리 (GitHub 커밋 금지)
- Iteration 07 개발 전 Power Automate 플로우 생성 및 URL 발급 필요 (김동현 담당)

---

## 🧰 기술 스택

| 항목 | 선택 | 비고 |
|---|---|---|
| 프레임워크 | React 18 | |
| 언어 | TypeScript | strict 모드 |
| 빌드 도구 | Vite | |
| 스타일 | CSS Modules + SCSS | 전역 변수는 `_variables.scss` |
| HTTP | Axios | 인터셉터로 인증/에러 공통 처리 |
| 배포 | AWS EC2 + Nginx | Vite build → dist 서빙 |
