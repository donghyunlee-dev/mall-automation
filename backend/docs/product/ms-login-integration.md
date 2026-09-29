# AX-Auth 기반 MS 로그인 연동 가이드

## 목적

이 문서는 로그인 화면에 "Microsoft 계정으로 로그인" 버튼을 추가해, 클릭 시 SFOOD AX-Auth(`https://ax-auth.s-food.ai`)를 통해 Microsoft 계정으로 로그인하는 방식을 정의한다. 기존 이메일 OTP 로그인은 그 아래 보조 수단으로 유지된다.

기존 `docs/product/api-spec.md`에 정의된 이메일 OTP 로그인(`/api/auth/email-verifications`, `/api/auth/email-verifications/confirm`)은 그대로 유지된다. 이번 변경은 로그인 화면에 MS 로그인 진입점을 추가하는 것이다.

> **참고**: 최초 설계는 로그인 화면 진입 시 MS 로그인을 자동으로 시도하는 방식이었으나, 로그아웃 후에도 MS 브라우저 세션이 남아있어 자동으로 즉시 재로그인되어 "로그아웃이 안 되는" 문제가 있어 **버튼 클릭 방식으로 변경**했다.

## 전체 흐름

```
1. /login 화면에서 사용자가 "Microsoft 계정으로 로그인" 버튼을 클릭
2. 브라우저가 AX-Auth 로그인 URL로 페이지 전체 이동
   GET https://ax-auth.s-food.ai/auth/login/sfood-mall-automation?redirect_uri=.../auth/callback
3. 이미 로그인된 MS 세션이 있으면 즉시 통과, 없으면 MS 로그인 화면 표시 후 로그인
4. AX-Auth가 /auth/callback?login_token=xxx 로 페이지를 다시 이동시킴
5. /auth/callback 페이지가 login_token을 백엔드로 전송
   POST /api/auth/ms-login/verify { "loginToken": "xxx" }
6. 백엔드가 AX-Auth 서버에 login_token을 서버 사이드로 검증
   POST https://ax-auth.s-food.ai/auth/token/verify
   { "clientId": "sfood-mall-automation", "clientSecret": "(서버 전용)", "loginToken": "xxx" }
7. 검증 성공 시 이메일을 얻어 기존 OTP 로그인과 동일한 세션(HttpSession, AUTHENTICATED_EMAIL)을 발급
8. 브라우저가 메인 화면(/goods)으로 이동
9. 검증 실패(MS 로그인 취소, 토큰 만료 등) 또는 콜백에 login_token이 없으면 /login으로 되돌아가
   기존 이메일+OTP 로그인 화면을 그대로 보여줌 (자동 재시도 없음)
```

## 화면에서 사용자가 겪는 경험

- 로그인 화면 상단에 "Microsoft 계정으로 로그인" 버튼이 눈에 띄게 표시된다.
- 버튼을 클릭하면 MS 로그인 화면으로 이동한다. 이미 로그인된 MS 세션이 있으면 즉시 통과되고, 없으면 MS 로그인 후 앱으로 돌아온다.
- MS 로그인이 실패/취소되면 자동으로 다시 시도하지 않고 `/login` 화면(버튼 + 이메일 OTP 폼)이 그대로 표시된다. 다시 시도하려면 버튼을 다시 클릭하면 된다.
- 로그아웃 후에는 MS 자동 재로그인이 발생하지 않는다 — 버튼을 다시 클릭해야 로그인이 진행된다.
- 로그인 이후 화면은 기존과 동일하다.

## API 명세

### POST /api/auth/ms-login/verify

| 항목 | 내용 |
|---|---|
| 인증 필요 | 아니오 (세션 발급 전 호출되므로 `SessionAuthInterceptor` 화이트리스트에 등록) |
| Request | `{ "loginToken": "string" }` |
| Response (성공, 200) | `{ "authenticated": true, "email": "user@s-food.com" }` |
| Response (검증 실패, 401) | `{ "code": "MS_LOGIN_VERIFICATION_FAILED", "message": "<AX-Auth reason>", "timestamp": "..." }` |
| Response (AX-Auth 통신 장애, 502) | `{ "code": "MS_AUTH_GATEWAY_ERROR", "message": "...", "timestamp": "..." }` |

이메일 도메인은 기존 OTP와 동일하게 `@s-food.com`만 허용한다(`OtpPolicy.validateEmailDomain` 재사용). AX-Auth가 다른 도메인 이메일을 반환하면 `EMAIL_FORMAT_INVALID`(400)로 거부된다.

## 환경변수

| 이름 | 설명 | 위치 |
|---|---|---|
| `AX_AUTH_BASE_URL` | AX-Auth 서비스 Base URL | backend |
| `AX_AUTH_CLIENT_ID` | 등록된 clientId (`sfood-mall-automation`) | backend + frontend |
| `AX_AUTH_CLIENT_SECRET` | 발급받은 clientSecret — **서버 전용, 프론트에 노출 금지** | backend only |
| `AX_AUTH_REDIRECT_URI` | 로그인 완료 후 돌아올 콜백 URL | backend(기본값) + frontend |
| `VITE_AX_AUTH_BASE_URL` | AX-Auth 서비스 Base URL | frontend |
| `VITE_AX_CLIENT_ID` | 등록된 clientId | frontend |
| `VITE_AX_REDIRECT_URI` | 콜백 URL (프론트 라우트) | frontend |

## 운영 관점에서 알아야 할 것

- **clientSecret이 유출/재발급되면** 환경변수(`AX_AUTH_CLIENT_SECRET`)를 갱신하고 재배포해야 한다.
- **AX-Auth 서비스 장애 시** MS 자동 로그인 단계가 실패하지만, 기존 이메일 OTP 로그인으로 자동 폴백되므로 서비스 전체 로그인 불가로는 이어지지 않는다.
- **운영 도메인 배포 시** AX팀에 운영 `redirect_uri`를 별도로 등록 요청해야 한다(현재는 로컬 `http://localhost:5173/auth/callback`만 등록됨).
- `login_token`은 3분 TTL, 1회용이다. 콜백 수신 즉시 검증하며, 재사용/재시도하지 않는다.

## 관련 문서

- 이메일 OTP 로그인 API 자체 명세: `docs/product/api-spec.md`
- AX-Auth 연동 원본 가이드: `https://ax-auth.s-food.ai/demo/guide`, `sfood-it-dev-ax-org/SFOOD-AX-AUTH` 저장소 `docs/guide/integration.md`
- 이번 변경의 구현 계획: `docs/superpowers/plans/2026-08-20-ms-login-integration.md`

문의: donghyun.lee@s-food.com
