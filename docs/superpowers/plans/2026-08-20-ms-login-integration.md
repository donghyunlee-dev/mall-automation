# MS(AX-Auth) 자동 로그인 연동 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 로그인 화면 진입 시 SFOOD AX-Auth(`https://ax-auth.s-food.ai`)를 통해 Microsoft 계정으로 자동 로그인을 시도하고, 실패(MS 세션 미인식, 검증 실패 등)하면 기존 이메일 OTP 로그인 화면으로 자동 폴백한다.

**Architecture:** AX-Auth의 "리다이렉트 방식" 연동을 사용한다. `/login` 진입 시 프론트가 클릭 없이 `window.location.href`로 AX-Auth 로그인 URL로 전체 페이지 이동(팝업 아님 → 브라우저 팝업 차단 미적용) → 이미 MS 세션이 있으면 즉시 `/auth/callback?login_token=...`으로 복귀, 없으면 MS 로그인 화면 노출 → 콜백 페이지가 `login_token`을 백엔드로 전달 → 백엔드가 `clientSecret`으로 AX-Auth에 서버 사이드 검증 후 이메일을 얻어 기존 세션 발급 로직(`AuthSessionPort`/`SessionIssuer`)을 그대로 재사용해 세션을 발급한다. 검증 실패 시 `sessionStorage` 플래그를 남기고 `/login`으로 되돌아가며, 플래그가 있으면 자동 리다이렉트를 건너뛰고 기존 OTP 화면을 그대로 보여준다(무한 루프 방지).

**Tech Stack:** Spring Boot 3.5 / RestClient (backend), React 19 + Vite + axios + react-router-dom (frontend)

**참고 구현체:** `sfood-it-dev-ax-org/SFOOD-AX-WMS-STOCK-APP` 저장소의 `lib/auth/axAuth.ts`, `app/auth/callback/*`, `docs/ax-auth-login-integration.md` (단, 그 앱은 실패 시 폴백이 없고 수동 재시도 버튼만 제공 — 이 프로젝트는 실패 시 기존 OTP 화면으로 자동 폴백해야 하므로 그 부분만 다르게 설계함)

**AX-Auth 발급 정보:**
- `clientId`: `sfood-mall-automation`
- `clientSecret`: `https://sfm-ssa.s-food.ai` (사용자가 직접 발급/등록한 값 — GitHub Secret/환경변수로만 관리, 코드에 하드코딩 금지. 단, 이 저장소의 기존 관례상 `application.yml`에 `${ENV_VAR:default}` 형태로 로컬 기본값을 두는 패턴을 따른다)
- redirect_uri(로컬): `http://localhost:5173/auth/callback`
- redirect_uri(운영): 실제 배포 도메인 확정 후 AX팀에 별도 등록 필요 (이 계획에서는 로컬 값만 다룸)

---

### Task 0: 로그인 연동 문서 작성

**Files:**
- Create: `backend/docs/product/ms-login-integration.md`

- [ ] **Step 1: 문서 작성**

기존 `backend/docs/product/wms-login-integration.md` 톤에 맞춰 아래 내용을 작성한다.

```markdown
# AX-Auth 기반 MS 자동 로그인 연동 가이드

## 목적

이 문서는 로그인 화면 진입 시 SFOOD AX-Auth(`https://ax-auth.s-food.ai`)를 통해 Microsoft 계정으로 자동 로그인을 시도하고, 실패 시 기존 이메일 OTP 로그인 화면으로 자동 전환하는 방식을 정의한다.

기존 `docs/product/api-spec.md`에 정의된 이메일 OTP 로그인(`/api/auth/email-verifications`, `/api/auth/email-verifications/confirm`)은 그대로 유지된다. 이번 변경은 그 앞단에 MS 자동 로그인 단계를 추가하는 것이다.

## 전체 흐름

로그인은 **팝업이 아니라 전체 페이지 리다이렉트** 방식이다. 브라우저는 클릭 없는 팝업은 차단하지만 자동 페이지 이동은 막지 않는다. 이 특성을 이용해 이미 MS 세션이 있는 사용자는 클릭 없이 로그인이 완료된다.

\`\`\`
1. /login 접속 → 세션 없고, 이전에 MS 로그인 시도가 실패한 적 없으면
   클릭 없이 자동으로 AX-Auth 로그인 URL로 페이지 전체 이동
   GET https://ax-auth.s-food.ai/auth/login/sfood-mall-automation?redirect_uri=.../auth/callback
2. 이미 로그인된 MS 세션이 있으면 즉시 통과, 없으면 MS 로그인 화면 표시 후 로그인
3. AX-Auth가 /auth/callback?login_token=xxx 로 페이지를 다시 이동시킴
4. /auth/callback 페이지가 login_token을 백엔드로 전송
   POST /api/auth/ms-login/verify { "loginToken": "xxx" }
5. 백엔드가 AX-Auth 서버에 login_token을 서버 사이드로 검증
   POST https://ax-auth.s-food.ai/auth/token/verify
   { "clientId": "sfood-mall-automation", "clientSecret": "(서버 전용)", "loginToken": "xxx" }
6. 검증 성공 시 이메일을 얻어 기존 OTP 로그인과 동일한 세션(HttpSession, AUTHENTICATED_EMAIL)을 발급
7. 브라우저가 메인 화면(/goods)으로 이동
8. 검증 실패(MS 로그인 취소, 토큰 만료 등) 시 sessionStorage에 "MS 로그인 시도함" 플래그를 남기고
   /login으로 되돌아감 → 플래그가 있으면 자동 리다이렉트를 건너뛰고 기존 이메일+OTP 화면을 그대로 보여줌
```

## 화면에서 사용자가 겪는 경험

- 회사 PC에 이미 로그인된 MS 계정이 있으면, `/login` 접속 시 클릭 한 번 없이 그대로 메인 화면으로 넘어간다.
- MS 계정 로그인이 안 되어 있으면 MS 로그인 화면으로 이동했다가, 로그인 후 다시 앱으로 돌아온다.
- MS 로그인이 실패/취소되면 자동으로 다시 시도하지 않고, 기존 이메일+OTP 로그인 화면이 그대로 표시된다(무한 리다이렉트 방지를 위한 의도적 설계). 이 화면에서 이메일 인증으로 로그인할 수 있다.
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
```

- [ ] **Step 2: Commit**

```bash
git add backend/docs/product/ms-login-integration.md
git commit -m "docs: MS 자동 로그인 연동 가이드 작성"
```

---

## Backend Tasks

### Task 1: 환경설정 추가 (application.yml, HttpClientConfig)

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/sfood/automation/common/config/HttpClientConfig.java`

- [ ] **Step 1: application.yml에 ax-auth 설정 추가**

`app:` 블록 마지막(`encryption.key` 다음)에 추가:

```yaml
  ax-auth:
    base-url: ${AX_AUTH_BASE_URL:https://ax-auth.s-food.ai}
    client-id: ${AX_AUTH_CLIENT_ID:sfood-mall-automation}
    client-secret: ${AX_AUTH_CLIENT_SECRET:https://sfm-ssa.s-food.ai}
    redirect-uri: ${AX_AUTH_REDIRECT_URI:http://localhost:5173/auth/callback}
```

- [ ] **Step 2: HttpClientConfig에 axAuthRestClient 빈 추가**

`HttpClientConfig.java`의 `godoRestClient` 빈 뒤에 추가:

```java
    @Bean
    @Qualifier("axAuthRestClient")
    RestClient axAuthRestClient(RestClient.Builder builder, @Value("${app.ax-auth.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
```

`@Value` import(`org.springframework.beans.factory.annotation.Value`)를 파일 상단에 추가한다.

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/application.yml backend/src/main/java/com/sfood/automation/common/config/HttpClientConfig.java
git commit -m "feat: AX-Auth 연동을 위한 설정 및 RestClient 빈 추가"
```

---

### Task 2: 에러 코드 / 예외 추가

**Files:**
- Modify: `backend/src/main/java/com/sfood/automation/common/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/sfood/automation/common/exception/MsLoginVerificationFailedException.java`
- Create: `backend/src/main/java/com/sfood/automation/common/exception/MsAuthGatewayException.java`

- [ ] **Step 1: ErrorCode에 항목 추가**

`ErrorCode.java`의 `INTERNAL_SERVER_ERROR` 앞에 추가:

```java
    MS_LOGIN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "MS login verification failed"),
    MS_AUTH_GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "AX Auth service error"),
```

- [ ] **Step 2: MsLoginVerificationFailedException 작성**

```java
package com.sfood.automation.common.exception;

public class MsLoginVerificationFailedException extends BusinessException {

    public MsLoginVerificationFailedException(String reason) {
        super(ErrorCode.MS_LOGIN_VERIFICATION_FAILED, reason != null ? reason : ErrorCode.MS_LOGIN_VERIFICATION_FAILED.defaultMessage());
    }
}
```

- [ ] **Step 3: MsAuthGatewayException 작성**

```java
package com.sfood.automation.common.exception;

public class MsAuthGatewayException extends BusinessException {

    public MsAuthGatewayException() {
        super(ErrorCode.MS_AUTH_GATEWAY_ERROR);
    }
}
```

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/sfood/automation/common/exception/
git commit -m "feat: MS 로그인 검증 실패/게이트웨이 오류 예외 추가"
```

---

### Task 3: AX-Auth 검증 포트 및 RestClient 어댑터

**Files:**
- Create: `backend/src/main/java/com/sfood/automation/auth/application/port/AxAuthVerifyPort.java`
- Create: `backend/src/main/java/com/sfood/automation/auth/application/port/AxAuthVerifyResult.java`
- Create: `backend/src/main/java/com/sfood/automation/auth/infrastructure/axauth/AxAuthRestClientTransport.java`

이 프로젝트는 외부 API 포트(`OtpMailPort`, `AuthSessionPort` 등)를 인터페이스로 두고, RestClient 기반 구현체는 얇게 유지하며 유닛 테스트는 유스케이스 단에서 포트를 fake로 대체하는 방식을 쓴다(`ConfirmEmailVerificationUseCaseTest`가 `FakeSessionPort`를 쓰는 것과 동일 패턴). `AxAuthRestClientTransport` 자체는 별도 유닛 테스트 없이 얇게 작성한다(기존 `WmsRestClientTransport`, `GodoRestClientTransport`와 동일 관례).

- [ ] **Step 1: AxAuthVerifyResult 레코드 작성**

```java
package com.sfood.automation.auth.application.port;

public record AxAuthVerifyResult(boolean valid, String email, String reason) {
}
```

- [ ] **Step 2: AxAuthVerifyPort 인터페이스 작성**

```java
package com.sfood.automation.auth.application.port;

public interface AxAuthVerifyPort {
    AxAuthVerifyResult verify(String loginToken);
}
```

- [ ] **Step 3: AxAuthRestClientTransport 작성**

```java
package com.sfood.automation.auth.infrastructure.axauth;

import com.sfood.automation.auth.application.port.AxAuthVerifyPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyResult;
import com.sfood.automation.common.exception.MsAuthGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AxAuthRestClientTransport implements AxAuthVerifyPort {

    private static final Logger log = LoggerFactory.getLogger(AxAuthRestClientTransport.class);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public AxAuthRestClientTransport(
        @Qualifier("axAuthRestClient") RestClient restClient,
        @Value("${app.ax-auth.client-id}") String clientId,
        @Value("${app.ax-auth.client-secret}") String clientSecret
    ) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public AxAuthVerifyResult verify(String loginToken) {
        try {
            return restClient.post()
                .uri("/auth/token/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VerifyRequest(clientId, clientSecret, loginToken))
                .retrieve()
                .body(AxAuthVerifyResult.class);
        } catch (RestClientException exception) {
            log.error("AX-Auth token verify call failed", exception);
            throw new MsAuthGatewayException();
        }
    }

    private record VerifyRequest(String clientId, String clientSecret, String loginToken) {
    }
}
```

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/sfood/automation/auth/application/port/AxAuthVerifyPort.java backend/src/main/java/com/sfood/automation/auth/application/port/AxAuthVerifyResult.java backend/src/main/java/com/sfood/automation/auth/infrastructure/axauth/
git commit -m "feat: AX-Auth 토큰 검증 포트/RestClient 어댑터 추가"
```

---

### Task 4: ConfirmMsLoginUseCase (TDD)

**Files:**
- Create: `backend/src/test/java/com/sfood/automation/auth/application/usecase/ConfirmMsLoginUseCaseTest.java`
- Create: `backend/src/main/java/com/sfood/automation/auth/application/usecase/ConfirmMsLoginUseCase.java`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.sfood.automation.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyResult;
import com.sfood.automation.auth.domain.model.UserAccount;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.auth.domain.policy.OtpPolicy;
import com.sfood.automation.common.exception.EmailFormatException;
import com.sfood.automation.common.exception.MsLoginVerificationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ConfirmMsLoginUseCaseTest {

    @Test
    void should_issue_session_when_ax_auth_verifies_valid_email() {
        FakeAxAuthVerifyPort axAuthVerifyPort = new FakeAxAuthVerifyPort(new AxAuthVerifyResult(true, "user@s-food.com", null));
        FakeSessionPort sessionPort = new FakeSessionPort();
        FakeUserAccountRepository userAccountRepository = new FakeUserAccountRepository();
        ConfirmMsLoginUseCase useCase = new ConfirmMsLoginUseCase(
            axAuthVerifyPort, new OtpPolicy(), sessionPort, userAccountRepository
        );

        var response = useCase.execute("login-token", new MockHttpServletRequest());

        assertThat(response.authenticated()).isTrue();
        assertThat(response.email()).isEqualTo("user@s-food.com");
        assertThat(sessionPort.issuedEmail).isEqualTo("user@s-food.com");
        assertThat(userAccountRepository.saved.getEmail()).isEqualTo("user@s-food.com");
    }

    @Test
    void should_throw_when_ax_auth_reports_invalid_token() {
        FakeAxAuthVerifyPort axAuthVerifyPort = new FakeAxAuthVerifyPort(new AxAuthVerifyResult(false, null, "TOKEN_EXPIRED"));
        ConfirmMsLoginUseCase useCase = new ConfirmMsLoginUseCase(
            axAuthVerifyPort, new OtpPolicy(), new FakeSessionPort(), new FakeUserAccountRepository()
        );

        assertThatThrownBy(() -> useCase.execute("login-token", new MockHttpServletRequest()))
            .isInstanceOf(MsLoginVerificationFailedException.class)
            .hasMessage("TOKEN_EXPIRED");
    }

    @Test
    void should_reject_when_email_domain_is_not_s_food_com() {
        FakeAxAuthVerifyPort axAuthVerifyPort = new FakeAxAuthVerifyPort(new AxAuthVerifyResult(true, "user@other.com", null));
        ConfirmMsLoginUseCase useCase = new ConfirmMsLoginUseCase(
            axAuthVerifyPort, new OtpPolicy(), new FakeSessionPort(), new FakeUserAccountRepository()
        );

        assertThatThrownBy(() -> useCase.execute("login-token", new MockHttpServletRequest()))
            .isInstanceOf(EmailFormatException.class);
    }

    private record FakeAxAuthVerifyPort(AxAuthVerifyResult result) implements AxAuthVerifyPort {
        @Override
        public AxAuthVerifyResult verify(String loginToken) {
            return result;
        }
    }

    private static class FakeSessionPort implements AuthSessionPort {
        private String issuedEmail;
        @Override public void issue(HttpServletRequest request, String email) { this.issuedEmail = email; }
        @Override public void invalidate(HttpServletRequest request) { }
    }

    private static class FakeUserAccountRepository implements UserAccountRepository {
        private UserAccount saved;

        @Override
        public UserAccount save(UserAccount userAccount) {
            this.saved = userAccount;
            return userAccount;
        }

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return Optional.ofNullable(saved);
        }

        @Override
        public List<UserAccount> findAllEmailNotificationEnabled() {
            return List.of();
        }
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `./gradlew test --tests "com.sfood.automation.auth.application.usecase.ConfirmMsLoginUseCaseTest"`
Expected: FAIL (컴파일 오류 — `ConfirmMsLoginUseCase` 클래스 없음)

- [ ] **Step 3: ConfirmMsLoginUseCase 구현**

```java
package com.sfood.automation.auth.application.usecase;

import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyResult;
import com.sfood.automation.auth.domain.model.UserAccount;
import com.sfood.automation.auth.domain.policy.OtpPolicy;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.common.exception.MsLoginVerificationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmMsLoginUseCase {

    private final AxAuthVerifyPort axAuthVerifyPort;
    private final OtpPolicy otpPolicy;
    private final AuthSessionPort authSessionPort;
    private final UserAccountRepository userAccountRepository;

    public ConfirmMsLoginUseCase(
        AxAuthVerifyPort axAuthVerifyPort,
        OtpPolicy otpPolicy,
        AuthSessionPort authSessionPort,
        UserAccountRepository userAccountRepository
    ) {
        this.axAuthVerifyPort = axAuthVerifyPort;
        this.otpPolicy = otpPolicy;
        this.authSessionPort = authSessionPort;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public ConfirmVerificationResponse execute(String loginToken, HttpServletRequest request) {
        AxAuthVerifyResult result = axAuthVerifyPort.verify(loginToken);
        if (!result.valid()) {
            throw new MsLoginVerificationFailedException(result.reason());
        }
        otpPolicy.validateEmailDomain(result.email());
        upsertUserAccount(result.email());
        authSessionPort.issue(request, result.email());
        return new ConfirmVerificationResponse(true, result.email());
    }

    private void upsertUserAccount(String email) {
        Instant now = Instant.now();
        var userAccount = userAccountRepository.findByEmail(email)
            .orElseGet(() -> UserAccount.create(email, now));
        userAccount.markLoggedIn(now);
        userAccountRepository.save(userAccount);
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `./gradlew test --tests "com.sfood.automation.auth.application.usecase.ConfirmMsLoginUseCaseTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/sfood/automation/auth/application/usecase/ConfirmMsLoginUseCase.java backend/src/test/java/com/sfood/automation/auth/application/usecase/ConfirmMsLoginUseCaseTest.java
git commit -m "feat: MS 로그인 검증 유스케이스 추가"
```

---

### Task 5: MsLoginController + 화이트리스트 등록 (TDD)

**Files:**
- Create: `backend/src/main/java/com/sfood/automation/auth/application/dto/MsLoginVerifyRequest.java`
- Create: `backend/src/main/java/com/sfood/automation/auth/presentation/MsLoginController.java`
- Modify: `backend/src/main/java/com/sfood/automation/common/security/SessionAuthInterceptor.java`
- Create: `backend/src/test/java/com/sfood/automation/auth/presentation/MsLoginControllerTest.java`

- [ ] **Step 1: MsLoginVerifyRequest DTO 작성**

```java
package com.sfood.automation.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record MsLoginVerifyRequest(@NotBlank String loginToken) {
}
```

- [ ] **Step 2: 컨트롤러 테스트 작성**

`EmailVerificationControllerTest`와 동일한 `@WebMvcTest` 패턴을 따른다.

```java
package com.sfood.automation.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.usecase.ConfirmMsLoginUseCase;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.exception.MsLoginVerificationFailedException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MsLoginController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, com.sfood.automation.common.security.SessionAuthInterceptor.class})
class MsLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfirmMsLoginUseCase confirmMsLoginUseCase;

    @Test
    void should_return_bad_request_when_login_token_missing() throws Exception {
        mockMvc.perform(post("/api/auth/ms-login/verify")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_ok_when_verification_succeeds() throws Exception {
        when(confirmMsLoginUseCase.execute(eq("token-abc"), any()))
            .thenReturn(new ConfirmVerificationResponse(true, "user@s-food.com"));

        mockMvc.perform(post("/api/auth/ms-login/verify")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("loginToken", "token-abc"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.email").value("user@s-food.com"));
    }

    @Test
    void should_return_unauthorized_when_verification_fails() throws Exception {
        when(confirmMsLoginUseCase.execute(eq("token-abc"), any()))
            .thenThrow(new MsLoginVerificationFailedException("TOKEN_EXPIRED"));

        mockMvc.perform(post("/api/auth/ms-login/verify")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("loginToken", "token-abc"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("MS_LOGIN_VERIFICATION_FAILED"))
            .andExpect(jsonPath("$.message").value("TOKEN_EXPIRED"));
    }
}
```

- [ ] **Step 3: 테스트 실행하여 실패 확인**

Run: `./gradlew test --tests "com.sfood.automation.auth.presentation.MsLoginControllerTest"`
Expected: FAIL (컴파일 오류 — `MsLoginController` 없음, 또는 401 — 화이트리스트 미등록)

- [ ] **Step 4: MsLoginController 구현**

```java
package com.sfood.automation.auth.presentation;

import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.dto.MsLoginVerifyRequest;
import com.sfood.automation.auth.application.usecase.ConfirmMsLoginUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/ms-login")
public class MsLoginController {

    private final ConfirmMsLoginUseCase confirmMsLoginUseCase;

    public MsLoginController(ConfirmMsLoginUseCase confirmMsLoginUseCase) {
        this.confirmMsLoginUseCase = confirmMsLoginUseCase;
    }

    @PostMapping("/verify")
    public ResponseEntity<ConfirmVerificationResponse> verify(
        @Valid @RequestBody MsLoginVerifyRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(confirmMsLoginUseCase.execute(request.loginToken(), httpServletRequest));
    }
}
```

- [ ] **Step 5: SessionAuthInterceptor 화이트리스트에 등록**

`SessionAuthInterceptor.java`의 `POST_WHITELIST`를 수정:

```java
    private static final Set<String> POST_WHITELIST = Set.of(
        "/api/auth/email-verifications",
        "/api/auth/email-verifications/confirm",
        "/api/auth/ms-login/verify"
    );
```

- [ ] **Step 6: 테스트 실행하여 통과 확인**

Run: `./gradlew test --tests "com.sfood.automation.auth.presentation.MsLoginControllerTest"`
Expected: PASS (3 tests)

- [ ] **Step 7: 전체 백엔드 테스트 실행**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL (기존 테스트 회귀 없음)

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/sfood/automation/auth/application/dto/MsLoginVerifyRequest.java backend/src/main/java/com/sfood/automation/auth/presentation/MsLoginController.java backend/src/main/java/com/sfood/automation/common/security/SessionAuthInterceptor.java backend/src/test/java/com/sfood/automation/auth/presentation/MsLoginControllerTest.java
git commit -m "feat: MS 로그인 검증 API 엔드포인트 추가"
```

---

## Frontend Tasks

### Task 6: 환경변수 및 MS 로그인 유틸

**Files:**
- Modify: `frontend/.env.local.example`
- Modify: `frontend/.env.production.example`
- Modify: `frontend/.env.local` (로컬 개발용, git 미추적 파일 — 존재 시에만 값 추가)
- Create: `frontend/src/utils/msAuth.ts`
- Create: `frontend/src/utils/msAuth.test.ts`

- [ ] **Step 1: .env.local.example에 값 추가**

```
VITE_API_BASE_URL=http://localhost:8080
VITE_DASHBOARD_URL=http://localhost:5173
VITE_TEAMS_WEBHOOK_URL=
VITE_AX_AUTH_BASE_URL=https://ax-auth.s-food.ai
VITE_AX_CLIENT_ID=sfood-mall-automation
VITE_AX_REDIRECT_URI=http://localhost:5173/auth/callback
```

- [ ] **Step 2: .env.production.example에 값 추가**

```
VITE_API_BASE_URL=
VITE_DASHBOARD_URL=https://app.example.com
VITE_TEAMS_WEBHOOK_URL=
VITE_AX_AUTH_BASE_URL=https://ax-auth.s-food.ai
VITE_AX_CLIENT_ID=sfood-mall-automation
VITE_AX_REDIRECT_URI=https://app.example.com/auth/callback
```

- [ ] **Step 3: 실패 테스트 작성 (msAuth.test.ts)**

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { redirectToMicrosoftLogin, MS_LOGIN_ATTEMPTED_KEY } from './msAuth';

describe('redirectToMicrosoftLogin', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_AX_AUTH_BASE_URL', 'https://ax-auth.s-food.ai');
    vi.stubEnv('VITE_AX_CLIENT_ID', 'sfood-mall-automation');
    vi.stubEnv('VITE_AX_REDIRECT_URI', 'http://localhost:5173/auth/callback');
    delete (window as unknown as { location: unknown }).location;
    (window as unknown as { location: { href: string } }).location = { href: '' };
  });

  it('builds the AX-Auth login URL and navigates to it', () => {
    redirectToMicrosoftLogin();

    expect(window.location.href).toBe(
      'https://ax-auth.s-food.ai/auth/login/sfood-mall-automation?redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Fauth%2Fcallback',
    );
  });

  it('throws when required env vars are missing', () => {
    vi.stubEnv('VITE_AX_CLIENT_ID', '');

    expect(() => redirectToMicrosoftLogin()).toThrow();
  });

  it('exposes a stable sessionStorage flag key', () => {
    expect(MS_LOGIN_ATTEMPTED_KEY).toBe('sfood_ms_login_attempted');
  });
});
```

- [ ] **Step 4: 테스트 실행하여 실패 확인**

Run: `npm run test -- msAuth.test.ts`
Expected: FAIL (모듈 `./msAuth` 없음)

- [ ] **Step 5: msAuth.ts 구현**

```typescript
export const MS_LOGIN_ATTEMPTED_KEY = 'sfood_ms_login_attempted';

export function redirectToMicrosoftLogin(): void {
  const clientId = import.meta.env.VITE_AX_CLIENT_ID;
  const authBaseUrl = import.meta.env.VITE_AX_AUTH_BASE_URL;
  const redirectUri = import.meta.env.VITE_AX_REDIRECT_URI;
  if (!clientId || !authBaseUrl || !redirectUri) {
    throw new Error('AX-Auth 환경변수가 설정되지 않았습니다.');
  }
  const params = new URLSearchParams({ redirect_uri: redirectUri });
  window.location.href = `${authBaseUrl}/auth/login/${clientId}?${params.toString()}`;
}
```

- [ ] **Step 6: 테스트 실행하여 통과 확인**

Run: `npm run test -- msAuth.test.ts`
Expected: PASS (3 tests)

- [ ] **Step 7: Commit**

```bash
git add frontend/.env.local.example frontend/.env.production.example frontend/src/utils/msAuth.ts frontend/src/utils/msAuth.test.ts
git commit -m "feat: AX-Auth MS 로그인 리다이렉트 유틸 추가"
```

---

### Task 7: 백엔드 검증 API 호출 함수 추가

**Files:**
- Modify: `frontend/src/types/auth.ts`
- Modify: `frontend/src/api/auth.ts`

- [ ] **Step 1: 타입 추가**

`frontend/src/types/auth.ts` 끝에 추가:

```typescript
export interface MsLoginVerifyBody {
  loginToken: string;
}
```

- [ ] **Step 2: API 함수 추가**

`frontend/src/api/auth.ts`에 `import` 목록에 `MsLoginVerifyBody` 추가 후, 파일 끝에 추가:

```typescript
export const verifyMsLogin = (body: MsLoginVerifyBody): Promise<OtpConfirmResponse> =>
  client.post<OtpConfirmResponse>('/api/auth/ms-login/verify', body).then((r) => r.data);
```

- [ ] **Step 3: 타입 체크**

Run: `npm run build`
Expected: 빌드 성공 (타입 오류 없음)

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/auth.ts frontend/src/api/auth.ts
git commit -m "feat: MS 로그인 검증 API 호출 함수 추가"
```

---

### Task 8: AuthCallbackPage + 라우팅

**Files:**
- Create: `frontend/src/pages/AuthCallbackPage.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: AuthCallbackPage 작성**

```tsx
import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { verifyMsLogin } from '../api/auth';
import { MS_LOGIN_ATTEMPTED_KEY } from '../utils/msAuth';
import styles from './LoginPage.module.scss';

const AuthCallbackPage = () => {
  const [searchParams] = useSearchParams();
  const ranRef = useRef(false);

  useEffect(() => {
    if (ranRef.current) return;
    ranRef.current = true;

    const loginToken = searchParams.get('login_token');
    if (!loginToken) {
      sessionStorage.setItem(MS_LOGIN_ATTEMPTED_KEY, '1');
      window.location.href = '/login';
      return;
    }

    verifyMsLogin({ loginToken })
      .then(() => {
        window.location.href = '/';
      })
      .catch(() => {
        sessionStorage.setItem(MS_LOGIN_ATTEMPTED_KEY, '1');
        window.location.href = '/login';
      });
  }, [searchParams]);

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <p role="status">로그인 처리 중...</p>
      </main>
    </div>
  );
};

export default AuthCallbackPage;
```

- [ ] **Step 2: App.tsx에 라우트 추가**

`frontend/src/App.tsx` 상단 import에 추가:

```tsx
import AuthCallbackPage from './pages/AuthCallbackPage';
```

`<Route path="/login" element={<LoginPage />} />` 다음 줄에 추가:

```tsx
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
```

- [ ] **Step 3: 빌드 확인**

Run: `npm run build`
Expected: 빌드 성공

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/AuthCallbackPage.tsx frontend/src/App.tsx
git commit -m "feat: MS 로그인 콜백 페이지 및 라우트 추가"
```

---

### Task 9: LoginPage 진입 시 MS 자동 로그인 시도 (useAuth.ts)

**Files:**
- Modify: `frontend/src/hooks/useAuth.ts`
- Modify: `frontend/src/hooks/useAuth.test.ts` (존재 시) 또는 신규 테스트 작성

- [ ] **Step 1: 기존 세션 확인 useEffect를 확인하는 실패 테스트 작성**

`frontend/src/hooks/useAuth.ts`에 대한 기존 테스트 파일이 있는지 먼저 확인한다:

Run: `ls frontend/src/hooks/useAuth.test.ts* 2>/dev/null || echo "no existing test"`

기존 테스트가 없다면 아래 신규 테스트를 작성한다 (`@testing-library/react`의 `renderHook` 사용, 이 프로젝트의 다른 훅 테스트 — 예: `useUserNotifications.test.tsx` — 스타일을 따른다. 해당 파일을 먼저 읽고 mocking 패턴을 맞출 것).

```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import useAuth from './useAuth';
import * as authApi from '../api/auth';
import * as msAuth from '../utils/msAuth';

vi.mock('../api/auth');
vi.mock('../utils/msAuth');

describe('useAuth - MS auto login', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.mocked(msAuth).MS_LOGIN_ATTEMPTED_KEY = 'sfood_ms_login_attempted';
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('redirects to Microsoft login when session check fails and no prior attempt flag exists', async () => {
    vi.mocked(authApi.getSession).mockRejectedValue(new Error('401'));
    const redirectSpy = vi.mocked(msAuth.redirectToMicrosoftLogin).mockImplementation(() => {});

    renderHook(() => useAuth());

    await waitFor(() => expect(redirectSpy).toHaveBeenCalled());
  });

  it('skips redirect and shows OTP screen when attempt flag is already set', async () => {
    sessionStorage.setItem('sfood_ms_login_attempted', '1');
    vi.mocked(authApi.getSession).mockRejectedValue(new Error('401'));
    const redirectSpy = vi.mocked(msAuth.redirectToMicrosoftLogin);

    const { result } = renderHook(() => useAuth());

    await waitFor(() => expect(result.current.sessionChecked).toBe(true));
    expect(redirectSpy).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `npm run test -- useAuth.test.ts`
Expected: FAIL (`redirectToMicrosoftLogin`이 호출되지 않음 — 아직 미구현)

- [ ] **Step 3: useAuth.ts 수정**

`frontend/src/hooks/useAuth.ts` 상단 import에 추가:

```typescript
import { redirectToMicrosoftLogin, MS_LOGIN_ATTEMPTED_KEY } from '../utils/msAuth';
```

기존 세션 확인 `useEffect` (다음 블록)를 교체:

```typescript
  // 세션 확인 (마운트 시)
  useEffect(() => {
    getSession()
      .then(() => {
        window.location.href = '/';
      })
      .catch(() => {
        if (sessionStorage.getItem(MS_LOGIN_ATTEMPTED_KEY)) {
          setState((prev) => ({ ...prev, sessionChecked: true }));
          return;
        }
        try {
          redirectToMicrosoftLogin();
        } catch {
          setState((prev) => ({ ...prev, sessionChecked: true }));
        }
      });
  }, []);
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `npm run test -- useAuth.test.ts`
Expected: PASS (2 tests)

- [ ] **Step 5: 전체 프론트엔드 테스트 및 빌드 확인**

Run: `npm run test && npm run build`
Expected: 모든 테스트 통과, 빌드 성공

- [ ] **Step 6: Commit**

```bash
git add frontend/src/hooks/useAuth.ts frontend/src/hooks/useAuth.test.ts
git commit -m "feat: 로그인 화면 진입 시 MS 자동 로그인 시도 및 실패 시 OTP 폴백"
```

---

### Task 10: 수동 브라우저 확인 (E2E 스모크 체크)

**Files:** 없음 (검증 전용 태스크)

- [ ] **Step 1: 로컬 백엔드/프론트 기동**

Run:
```bash
cd backend && ./gradlew bootRun &
cd frontend && npm run dev
```

- [ ] **Step 2: AX팀에 로컬 redirect_uri 등록 확인**

`http://localhost:5173/auth/callback`이 AX-Auth 쪽에 `sfood-mall-automation` clientId로 등록되어 있는지 확인한다(등록 안 되어 있으면 `redirect_uri_mismatch` 류 오류 발생 — AX팀 문의 필요).

- [ ] **Step 3: 브라우저 시크릿 창(MS 미로그인 상태)에서 `/login` 접속**

Expected: MS 로그인 화면으로 자동 이동 → 로그인 취소 시 `/login`으로 돌아와 기존 이메일+OTP 화면이 표시됨(무한 리다이렉트 없음)

- [ ] **Step 4: 회사 MS 계정으로 이미 로그인된 브라우저에서 `/login` 접속**

Expected: 클릭 없이 `/auth/callback` → `/goods`로 자동 이동

- [ ] **Step 5: 결과를 사용자에게 보고**

자동화 테스트로 커버되지 않는 실제 MS 로그인 UX(리다이렉트 체감 속도, MS 로그인 화면 노출 여부)는 수동 확인이 필요하다는 점을 보고서에 명시한다.

---

## Self-Review 요약

- **Spec coverage:** MS 자동 로그인(리다이렉트) → Task 6,9 / 콜백 검증 → Task 8 / 백엔드 서버사이드 검증(clientSecret) → Task 3,4,5 / 실패 시 기존 OTP 폴백 → Task 8,9 / 문서화 → Task 0. 모두 커버됨.
- **화이트리스트 누락 방지:** Task 5에서 `SessionAuthInterceptor.POST_WHITELIST`에 새 엔드포인트를 반드시 추가하도록 명시함 — 누락 시 401로 항상 실패하므로 중요.
- **무한 리다이렉트 방지:** `sessionStorage` 플래그(`MS_LOGIN_ATTEMPTED_KEY`)를 useAuth와 AuthCallbackPage 양쪽에서 동일한 키로 사용하도록 통일함.
