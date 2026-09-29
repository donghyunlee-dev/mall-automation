# Frontend Data Spec

> 작성일: 2026-04-02

## 목적

프론트엔드에서 사용하는 주요 API 데이터 계약과 화면 상태 데이터를 정리한다.

## Auth Data Contracts

### SessionResponse

```ts
interface SessionResponse {
  email: string;
}
```

- `GET /api/auth/session` 성공 시 사용한다.
- 인증된 사용자 이메일을 표시한다.

### OtpRequestResponse

```ts
interface OtpRequestResponse {
  verificationKey: string;
  expiresInSeconds: number;
  message: string;
}
```

### OtpConfirmResponse

```ts
interface OtpConfirmResponse {
  authenticated: boolean;
  email: string;
}
```

### Logout Response

- `POST /api/auth/logout`
- 성공 응답 본문 없음 (`204 No Content`)

## Dashboard Header State

Header는 다음 상태를 사용한다.

- `email`: 현재 로그인 사용자 이메일
- `running`: 배치 수동 실행 중 여부
- `lastRefreshedAt`: 배치 상태 마지막 갱신 시각
- `logoutLoading`: 로그아웃 요청 진행 여부
- `logoutError`: 로그아웃 실패 메시지

## Validation Rules

- 이메일은 빈 문자열이 아닌 값이어야 한다.
- 로그아웃 성공 시 현재 세션은 더 이상 보호 API에 접근할 수 없어야 한다.
- 로그아웃 실패 시 사용자는 현재 화면에 남고 오류 메시지를 확인할 수 있어야 한다.
