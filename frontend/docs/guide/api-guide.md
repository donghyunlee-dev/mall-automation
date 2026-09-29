# API_GUIDE — Frontend API 연동 가이드

Backend API 명세는 `docs/product/` 폴더에 동현님이 작성한 문서를 참고한다.
이 문서는 프론트엔드 연동에 필요한 Axios 설정과 공통 규칙만 정의한다.

---

## 환경변수 설정

`.env` 파일:
```
VITE_API_BASE_URL=http://localhost:8080
```

`.env.production` 파일:
```
VITE_API_BASE_URL=http://{EC2_HOST}:{PORT}
```

---

## Axios 인스턴스 (`src/api/client.ts`)

```typescript
import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,  // 세션 쿠키 자동 포함 — 필수
  timeout: 10_000,
});

// 401 수신 시 로그인으로 자동 이동
// 단, 인증 API 자체는 제외 (무한 리다이렉트 방지)
client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (
      error.response?.status === 401 &&
      !error.config.url?.includes('/api/auth/')
    ) {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;
```

---

## API 호출 규칙

- 모든 API 호출은 `src/api/` 함수를 통해서만 한다 — 컴포넌트/훅에서 직접 `client` 호출 금지
- 모든 요청에 `withCredentials: true` 적용 — 위 인스턴스를 쓰면 자동 적용됨
- 에러 처리는 인터셉터(공통)와 각 API 함수(개별) 두 곳에서 나눠서 처리
  - 공통: 401 → 로그인 리다이렉트
  - 개별: 400, 409, 410, 429 등 → 화면에 에러 메시지 표시

---

## 외부 서비스 HTTP 요청 (Power Automate)

Teams 알림은 백엔드 API가 아닌 Power Automate HTTP 트리거 URL로 전송한다. 이 요청은 세션 쿠키가 필요 없고 외부 서비스로 나가므로 `client`(Axios 인스턴스)를 사용하지 않고 `fetch`로 직접 호출한다.

```typescript
// src/api/teams.ts
const sendTeamsMessage = async (payload: TeamsPayload): Promise<void> => {
  await fetch(import.meta.env.VITE_TEAMS_WEBHOOK_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
};
```

- `VITE_TEAMS_WEBHOOK_URL` 환경변수 필수 — 미설정 시 알림 발송 스킵
- `client` 인스턴스 사용 금지 (401 인터셉터가 외부 URL에 오작동할 수 있음)
- 발송 실패 시 오류를 조용히 삼키지 않고 콘솔에 기록

---

## 세션 쿠키 동작 방식

Spring Session + HttpOnly Cookie 기반. 프론트에서 별도로 토큰을 저장하거나 헤더에 붙일 필요 없다.

```
로그인 성공
  └─ 백엔드: Set-Cookie: SESSION=xxx; HttpOnly; Path=/
  └─ 브라우저: 쿠키 자동 저장 (withCredentials: true 덕분)

이후 모든 API 요청
  └─ 브라우저: 쿠키 자동 포함
  └─ 백엔드: 세션으로 사용자 식별
```
