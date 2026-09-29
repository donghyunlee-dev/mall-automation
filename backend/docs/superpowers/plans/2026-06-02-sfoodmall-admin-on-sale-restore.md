# 에쓰푸드몰 관리자 스크래핑을 통한 정상 전환 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 품절→정상(ON_SALE) 전환 시 기존 고도몰 REST API 대신 에쓰푸드몰 관리자 페이지 전체 FormData 제출 방식으로 변경하되, 정상→품절(OUT_OF_STOCK) 전환은 기존 API를 유지한다.

**Architecture:** 새 포트 `SfoodAdminGoodsPort` + 구현체 `SfoodAdminGoodsClient`를 추가한다. `SfoodAdminGoodsClient`는 관리자 로그인 → 상품 폼 수집 → soldOutFl 교체 → multipart POST → 저장 후 검증 흐름을 수행한다. `SyncGodoStatusUseCase`와 `ForceSyncGodoStatusUseCase`는 정상 전환 시 새 포트를 사용하도록 수정한다.

**Tech Stack:** Java 21, Spring Boot 3.5, JDK HttpClient + CookieManager (쿠키 세션 관리), Jsoup 1.18 (HTML 폼 파싱), AssertJ + JUnit 5 (테스트)

---

## 파일 구조 (신규/수정)

| 파일 | 역할 |
|---|---|
| `build.gradle` | jsoup 의존성 추가 |
| `application.yml` | `app.sfood-admin.*` 설정 키 추가 |
| `common/exception/SfoodAdminException.java` | 관리자 스크래핑 전용 런타임 예외 |
| `goods/domain/port/SfoodAdminGoodsPort.java` | 정상 전환 포트 인터페이스 |
| `goods/infrastructure/admin/SfoodAdminProperties.java` | ConfigurationProperties |
| `goods/infrastructure/admin/SfoodAdminSessionManager.java` | CookieManager + 로그인/재로그인 |
| `goods/infrastructure/admin/SfoodAdminFormCollector.java` | Jsoup 기반 전체 FormData 수집 |
| `goods/infrastructure/admin/SfoodAdminGoodsClient.java` | SfoodAdminGoodsPort 구현체 (메인 오케스트레이터) |
| `goods/application/usecase/SyncGodoStatusUseCase.java` | 정상 전환 시 새 포트 사용으로 변경 |
| `goods/application/usecase/ForceSyncGodoStatusUseCase.java` | 동일 |
| `common/config/RequiredEnvironmentValidator.java` | sfood-admin 필수 키 추가 |

---

## Task 1: jsoup 의존성 추가 + SfoodAdminProperties + SfoodAdminException

**Files:**
- Modify: `backend/build.gradle`
- Create: `backend/src/main/java/com/sfood/automation/common/exception/SfoodAdminException.java`
- Create: `backend/src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminProperties.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: build.gradle에 jsoup 추가**

`dependencies` 블록에 아래 줄을 추가한다.

```groovy
implementation 'org.jsoup:jsoup:1.18.3'
```

- [ ] **Step 2: SfoodAdminException 생성**

```java
package com.sfood.automation.common.exception;

public class SfoodAdminException extends RuntimeException {

    public SfoodAdminException(String message) {
        super(message);
    }

    public SfoodAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 3: SfoodAdminProperties 생성**

```java
package com.sfood.automation.goods.infrastructure.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sfood-admin")
public class SfoodAdminProperties {

    private String baseUrl;
    private String managerId;
    private String managerPw;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public String getManagerPw() { return managerPw; }
    public void setManagerPw(String managerPw) { this.managerPw = managerPw; }
}
```

- [ ] **Step 4: application.yml에 sfood-admin 설정 추가**

`app:` 섹션 끝에 아래를 추가한다.

```yaml
  sfood-admin:
    base-url: ${SFOOD_ADMIN_BASE_URL:https://gdadmin.sfoodmall.co.kr}
    manager-id: ${SFOOD_ADMIN_MANAGER_ID:}
    manager-pw: ${SFOOD_ADMIN_MANAGER_PW:}
```

- [ ] **Step 5: 빌드 확인**

```bash
cd /home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add build.gradle src/main/resources/application.yml \
  src/main/java/com/sfood/automation/common/exception/SfoodAdminException.java \
  src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminProperties.java
git commit -m "feat: add jsoup dependency, SfoodAdminProperties, SfoodAdminException"
```

---

## Task 2: SfoodAdminGoodsPort 인터페이스 생성

**Files:**
- Create: `backend/src/main/java/com/sfood/automation/goods/domain/port/SfoodAdminGoodsPort.java`

- [ ] **Step 1: 인터페이스 생성**

```java
package com.sfood.automation.goods.domain.port;

import com.sfood.automation.goods.domain.model.GodoApiResult;

public interface SfoodAdminGoodsPort {
    GodoApiResult restoreToOnSale(String godoGoodsCode);
}
```

- [ ] **Step 2: 빌드 확인**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/sfood/automation/goods/domain/port/SfoodAdminGoodsPort.java
git commit -m "feat: add SfoodAdminGoodsPort interface for on-sale restore"
```

---

## Task 3: SfoodAdminSessionManager 생성 + 테스트

**Files:**
- Create: `backend/src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminSessionManager.java`
- Create: `backend/src/test/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminSessionManagerTest.java`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.sfood.automation.goods.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.SfoodAdminException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SfoodAdminSessionManagerTest {

    @Test
    void should_detect_login_success_by_message_in_response() {
        FakeHttpSender sender = new FakeHttpSender()
            .addResponse(200, "<html>관리자 접속이 승인되었습니다.</html>");
        SfoodAdminSessionManager manager = new SfoodAdminSessionManager(
            "https://admin.test", "admin", "pass", sender
        );

        boolean result = manager.login();

        assertThat(result).isTrue();
        assertThat(sender.lastRequestedUrl).contains("/base/login_ps.php");
    }

    @Test
    void should_detect_login_failure_by_message_in_response() {
        FakeHttpSender sender = new FakeHttpSender()
            .addResponse(200, "<html>쇼핑몰 관리자 정보가 존재하지 않거나 잘못된 정보 입니다.</html>");
        SfoodAdminSessionManager manager = new SfoodAdminSessionManager(
            "https://admin.test", "admin", "wrong", sender
        );

        boolean result = manager.login();

        assertThat(result).isFalse();
    }

    @Test
    void should_detect_goods_page_success_by_form_presence() {
        FakeHttpSender sender = new FakeHttpSender()
            .addResponse(200, """
                <html><form id="frmGoods">
                  <input name="goodsNo" value="1000001371">
                  <input name="mode" value="modify">
                  <input type="radio" name="soldOutFl" value="n" checked>
                </form></html>
                """);
        SfoodAdminSessionManager manager = new SfoodAdminSessionManager(
            "https://admin.test", "admin", "pass", sender
        );

        String html = manager.fetchGoodsFormPage("1000001371");

        assertThat(html).contains("frmGoods");
        assertThat(sender.lastRequestedUrl).contains("goodsNo=1000001371");
    }

    @Test
    void should_relogin_and_retry_when_goods_page_redirects_to_login() {
        FakeHttpSender sender = new FakeHttpSender()
            .addResponse(200, "<html><form id=\"frmLogin\">로그인 페이지</form></html>")
            .addResponse(200, "<html>관리자 접속이 승인되었습니다.</html>")
            .addResponse(200, "<html><form id=\"frmGoods\"><input name=\"goodsNo\" value=\"1000001371\"><input name=\"mode\" value=\"modify\"><input type=\"radio\" name=\"soldOutFl\" value=\"n\" checked></form></html>");
        SfoodAdminSessionManager manager = new SfoodAdminSessionManager(
            "https://admin.test", "admin", "pass", sender
        );

        String html = manager.fetchGoodsFormPage("1000001371");

        assertThat(html).contains("frmGoods");
        assertThat(sender.requestCount).isEqualTo(3);
    }

    @Test
    void should_throw_when_relogin_also_fails() {
        FakeHttpSender sender = new FakeHttpSender()
            .addResponse(200, "<html><form id=\"frmLogin\">로그인 페이지</form></html>")
            .addResponse(200, "<html>쇼핑몰 관리자 정보가 존재하지 않거나 잘못된 정보 입니다.</html>");
        SfoodAdminSessionManager manager = new SfoodAdminSessionManager(
            "https://admin.test", "admin", "wrong", sender
        );

        assertThatThrownBy(() -> manager.fetchGoodsFormPage("1000001371"))
            .isInstanceOf(SfoodAdminException.class)
            .hasMessageContaining("로그인");
    }

    static final class FakeHttpSender implements SfoodAdminSessionManager.HttpSender {
        private final Deque<String> bodies = new ArrayDeque<>();
        private final Deque<Integer> statuses = new ArrayDeque<>();
        String lastRequestedUrl;
        int requestCount;

        FakeHttpSender addResponse(int status, String body) {
            statuses.add(status);
            bodies.add(body);
            return this;
        }

        @Override
        public SfoodAdminSessionManager.RawResponse get(String url) throws IOException, InterruptedException {
            lastRequestedUrl = url;
            requestCount++;
            return new SfoodAdminSessionManager.RawResponse(statuses.removeFirst(), bodies.removeFirst());
        }

        @Override
        public SfoodAdminSessionManager.RawResponse postForm(String url, String body)
            throws IOException, InterruptedException {
            lastRequestedUrl = url;
            requestCount++;
            return new SfoodAdminSessionManager.RawResponse(statuses.removeFirst(), bodies.removeFirst());
        }

        @Override
        public SfoodAdminSessionManager.RawResponse postMultipart(String url, Map<String, String> fields)
            throws IOException, InterruptedException {
            lastRequestedUrl = url;
            requestCount++;
            return new SfoodAdminSessionManager.RawResponse(statuses.removeFirst(), bodies.removeFirst());
        }
    }
}
```

- [ ] **Step 2: 테스트 실행해서 컴파일 에러 확인**

```bash
./gradlew test --tests "*SfoodAdminSessionManagerTest" 2>&1 | head -30
```

Expected: compilation error (SfoodAdminSessionManager not found)

- [ ] **Step 3: SfoodAdminSessionManager 구현**

```java
package com.sfood.automation.goods.infrastructure.admin;

import com.sfood.automation.common.exception.SfoodAdminException;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(SfoodAdminProperties.class)
public class SfoodAdminSessionManager {

    private static final Logger log = LoggerFactory.getLogger(SfoodAdminSessionManager.class);
    private static final String LOGIN_SUCCESS_MARKER = "관리자 접속이 승인되었습니다.";
    private static final String LOGIN_PAGE_MARKER = "frmLogin";
    private static final String GOODS_FORM_MARKER = "frmGoods";

    private final String baseUrl;
    private final String managerId;
    private final String managerPw;
    private final HttpSender httpSender;

    public SfoodAdminSessionManager(SfoodAdminProperties properties) {
        this.baseUrl = properties.getBaseUrl();
        this.managerId = properties.getManagerId();
        this.managerPw = properties.getManagerPw();
        this.httpSender = new JdkHttpSender(createHttpClient());
    }

    SfoodAdminSessionManager(String baseUrl, String managerId, String managerPw, HttpSender httpSender) {
        this.baseUrl = baseUrl;
        this.managerId = managerId;
        this.managerPw = managerPw;
        this.httpSender = httpSender;
    }

    public boolean login() {
        String formBody = "mode=login"
            + "&returnUrl=" + encode("/")
            + "&managerId=" + encode(managerId)
            + "&managerPw=" + encode(managerPw);
        try {
            RawResponse response = httpSender.postForm(baseUrl + "/base/login_ps.php", formBody);
            boolean success = response.body().contains(LOGIN_SUCCESS_MARKER);
            if (success) {
                log.info("SfoodAdmin login succeeded: managerId=[MASKED]");
            } else {
                log.warn("SfoodAdmin login failed: managerId=[MASKED]");
            }
            return success;
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("관리자 로그인 요청 실패", exception);
        }
    }

    public String fetchGoodsFormPage(String goodsNo) {
        String url = baseUrl + "/goods/goods_register.php?popupMode=yes&goodsNo=" + goodsNo;
        try {
            RawResponse response = httpSender.get(url);
            if (isLoginPage(response.body())) {
                log.info("SfoodAdmin session expired, re-logging in: goodsNo={}", goodsNo);
                if (!login()) {
                    throw new SfoodAdminException("관리자 재로그인 실패: goodsNo=" + goodsNo);
                }
                response = httpSender.get(url);
            }
            if (!response.body().contains(GOODS_FORM_MARKER)) {
                throw new SfoodAdminException("상품 폼 없음: goodsNo=" + goodsNo);
            }
            return response.body();
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("상품 폼 페이지 요청 실패: goodsNo=" + goodsNo, exception);
        }
    }

    public RawResponse postMultipart(String url, Map<String, String> fields) {
        try {
            return httpSender.postMultipart(url, fields);
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("multipart POST 실패: url=" + url, exception);
        }
    }

    private boolean isLoginPage(String html) {
        return html.contains(LOGIN_PAGE_MARKER) && !html.contains(GOODS_FORM_MARKER);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static HttpClient createHttpClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public interface HttpSender {
        RawResponse get(String url) throws IOException, InterruptedException;
        RawResponse postForm(String url, String body) throws IOException, InterruptedException;
        RawResponse postMultipart(String url, Map<String, String> fields) throws IOException, InterruptedException;
    }

    public record RawResponse(int statusCode, String body) {
    }

    static final class JdkHttpSender implements HttpSender {
        private final HttpClient httpClient;

        JdkHttpSender(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        public RawResponse get(String url) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new RawResponse(response.statusCode(), response.body());
        }

        @Override
        public RawResponse postForm(String url, String body) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new RawResponse(response.statusCode(), response.body());
        }

        @Override
        public RawResponse postMultipart(String url, Map<String, String> fields) throws IOException, InterruptedException {
            String boundary = "----SfoodAdminBoundary" + UUID.randomUUID().toString().replace("-", "");
            String body = buildMultipartBody(fields, boundary);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new RawResponse(response.statusCode(), response.body());
        }

        private String buildMultipartBody(Map<String, String> fields, String boundary) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n");
                sb.append("\r\n");
                sb.append(entry.getValue() == null ? "" : entry.getValue()).append("\r\n");
            }
            sb.append("--").append(boundary).append("--\r\n");
            return sb.toString();
        }
    }
}
```

> **주의**: `JdkHttpSender.buildMultipartBody`는 `Map<String, String>`을 사용한다. 폼 필드가 동일 이름으로 여러 값을 가질 수 있으므로 Task 4에서 `LinkedHashMap`으로 처리하는 방식을 설계한다. 단순 Map은 중복 키를 지원하지 않으므로 Task 5에서 `postMultipart`의 시그니처를 `List<Map.Entry<String, String>>`으로 변경한다.

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew test --tests "*SfoodAdminSessionManagerTest"
```

Expected: 모든 테스트 PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminSessionManager.java \
  src/test/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminSessionManagerTest.java
git commit -m "feat: add SfoodAdminSessionManager with JDK HttpClient and cookie management"
```

---

## Task 4: SfoodAdminFormCollector 생성 + 테스트

**Files:**
- Create: `backend/src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminFormCollector.java`
- Create: `backend/src/test/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminFormCollectorTest.java`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.sfood.automation.goods.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.SfoodAdminException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SfoodAdminFormCollectorTest {

    private final SfoodAdminFormCollector collector = new SfoodAdminFormCollector();

    @Test
    void should_collect_hidden_inputs() {
        String html = """
            <html><form id="frmGoods">
              <input type="hidden" name="mode" value="modify">
              <input type="hidden" name="goodsNo" value="1000001371">
            </form></html>
            """;

        List<Map.Entry<String, String>> fields = collector.collect(html, "1000001371");

        assertThat(fields).contains(Map.entry("mode", "modify"));
        assertThat(fields).contains(Map.entry("goodsNo", "1000001371"));
    }

    @Test
    void should_collect_only_checked_radio() {
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <input type="radio" name="soldOutFl" value="n" checked>
              <input type="radio" name="soldOutFl" value="y">
            </form></html>
            """;

        List<Map.Entry<String, String>> fields = collector.collect(html, "1000001371");

        long soldOutFlCount = fields.stream().filter(e -> "soldOutFl".equals(e.getKey())).count();
        assertThat(soldOutFlCount).isEqualTo(1);
        assertThat(fields).contains(Map.entry("soldOutFl", "n"));
        assertThat(fields).doesNotContain(Map.entry("soldOutFl", "y"));
    }

    @Test
    void should_collect_checked_checkbox_only() {
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <input type="checkbox" name="flagA" value="y" checked>
              <input type="checkbox" name="flagB" value="y">
            </form></html>
            """;

        List<Map.Entry<String, String>> fields = collector.collect(html, "1000001371");

        assertThat(fields).contains(Map.entry("flagA", "y"));
        assertThat(fields).noneMatch(e -> "flagB".equals(e.getKey()));
    }

    @Test
    void should_collect_select_selected_option() {
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <select name="goodsDisplayFl">
                <option value="y">노출</option>
                <option value="n" selected>미노출</option>
              </select>
            </form></html>
            """;

        List<Map.Entry<String, String>> fields = collector.collect(html, "1000001371");

        assertThat(fields).contains(Map.entry("goodsDisplayFl", "n"));
    }

    @Test
    void should_collect_textarea_text() {
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <textarea name="goodsContent">상품 설명</textarea>
            </form></html>
            """;

        List<Map.Entry<String, String>> fields = collector.collect(html, "1000001371");

        assertThat(fields).contains(Map.entry("goodsContent", "상품 설명"));
    }

    @Test
    void should_exclude_submit_and_file_inputs() {
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <input type="submit" name="submit" value="저장">
              <input type="file" name="image">
              <input type="button" name="btn" value="취소">
            </form></html>
            """;

        List<Map.Entry<String, String>> fields = collector.collect(html, "1000001371");

        assertThat(fields).noneMatch(e -> "submit".equals(e.getKey()));
        assertThat(fields).noneMatch(e -> "image".equals(e.getKey()));
        assertThat(fields).noneMatch(e -> "btn".equals(e.getKey()));
    }

    @Test
    void should_exclude_disabled_controls() {
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <input name="readonlyField" value="x" disabled>
            </form></html>
            """;

        List<Map.Entry<String, String>> fields = collector.collect(html, "1000001371");

        assertThat(fields).noneMatch(e -> "readonlyField".equals(e.getKey()));
    }

    @Test
    void should_validate_goodsNo_in_form_matches_requested() {
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="9999">
              <input name="mode" value="modify">
            </form></html>
            """;

        assertThatThrownBy(() -> collector.collect(html, "1000001371"))
            .isInstanceOf(SfoodAdminException.class)
            .hasMessageContaining("goodsNo");
    }

    @Test
    void should_throw_when_frmGoods_not_found() {
        String html = "<html><body>오류 페이지</body></html>";

        assertThatThrownBy(() -> collector.collect(html, "1000001371"))
            .isInstanceOf(SfoodAdminException.class)
            .hasMessageContaining("frmGoods");
    }

    @Test
    void should_collect_multiple_values_with_same_name() {
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <input type="hidden" name="optionSno[]" value="10">
              <input type="hidden" name="optionSno[]" value="20">
            </form></html>
            """;

        List<Map.Entry<String, String>> fields = collector.collect(html, "1000001371");

        long count = fields.stream().filter(e -> "optionSno[]".equals(e.getKey())).count();
        assertThat(count).isEqualTo(2);
    }
}
```

- [ ] **Step 2: 테스트 실행해서 컴파일 에러 확인**

```bash
./gradlew test --tests "*SfoodAdminFormCollectorTest" 2>&1 | head -20
```

Expected: compilation error (SfoodAdminFormCollector not found)

- [ ] **Step 3: SfoodAdminFormCollector 구현**

```java
package com.sfood.automation.goods.infrastructure.admin;

import com.sfood.automation.common.exception.SfoodAdminException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class SfoodAdminFormCollector {

    public List<Map.Entry<String, String>> collect(String html, String expectedGoodsNo) {
        Document document = Jsoup.parse(html);
        Element form = document.selectFirst("form#frmGoods");
        if (form == null) {
            throw new SfoodAdminException("상품 폼(frmGoods) 없음");
        }
        validateGoodsNo(form, expectedGoodsNo);
        return collectFormFields(form);
    }

    private void validateGoodsNo(Element form, String expectedGoodsNo) {
        Element goodsNoInput = form.selectFirst("input[name=goodsNo]");
        if (goodsNoInput == null || !expectedGoodsNo.equals(goodsNoInput.attr("value"))) {
            throw new SfoodAdminException("goodsNo 불일치: expected=" + expectedGoodsNo);
        }
    }

    private List<Map.Entry<String, String>> collectFormFields(Element form) {
        List<Map.Entry<String, String>> fields = new ArrayList<>();
        Elements controls = form.select("input, select, textarea");

        for (Element el : controls) {
            if (el.hasAttr("disabled")) {
                continue;
            }
            String name = el.attr("name");
            if (name.isBlank()) {
                continue;
            }
            switch (el.tagName()) {
                case "input" -> collectInput(el, name, fields);
                case "textarea" -> fields.add(entry(name, el.text()));
                case "select" -> collectSelect(el, name, fields);
            }
        }
        return fields;
    }

    private void collectInput(Element el, String name, List<Map.Entry<String, String>> fields) {
        String type = el.attr("type").toLowerCase(Locale.ROOT);
        if (List.of("submit", "button", "reset", "file").contains(type)) {
            return;
        }
        if (("radio".equals(type) || "checkbox".equals(type)) && !el.hasAttr("checked")) {
            return;
        }
        fields.add(entry(name, el.attr("value")));
    }

    private void collectSelect(Element el, String name, List<Map.Entry<String, String>> fields) {
        Elements selected = el.select("option[selected]");
        if (selected.isEmpty()) {
            Element first = el.selectFirst("option");
            if (first != null) {
                selected = new Elements(first);
            }
        }
        for (Element option : selected) {
            fields.add(entry(name, option.hasAttr("value") ? option.attr("value") : option.text()));
        }
    }

    private Map.Entry<String, String> entry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
```

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew test --tests "*SfoodAdminFormCollectorTest"
```

Expected: 모든 테스트 PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminFormCollector.java \
  src/test/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminFormCollectorTest.java
git commit -m "feat: add SfoodAdminFormCollector for Jsoup-based full form data collection"
```

---

## Task 5: SfoodAdminGoodsClient 생성 + 테스트

**Files:**
- Create: `backend/src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminGoodsClient.java`
- Create: `backend/src/test/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminGoodsClientTest.java`

> **주의:** Task 3에서 `postMultipart`의 시그니처를 `Map<String, String>`으로 설계했는데, 중복 키 지원을 위해 `List<Map.Entry<String, String>>`으로 변경한다. `SfoodAdminSessionManager`의 `HttpSender` 인터페이스와 `JdkHttpSender.buildMultipartBody` 메서드 시그니처를 먼저 수정해야 한다.

- [ ] **Step 1: SfoodAdminSessionManager의 postMultipart 시그니처 수정**

`SfoodAdminSessionManager.java`에서:
- `HttpSender.postMultipart(String url, Map<String, String> fields)` → `postMultipart(String url, List<Map.Entry<String, String>> fields)`
- `postMultipart(String url, Map<String, String> fields)` 공개 메서드 동일 변경
- `JdkHttpSender.buildMultipartBody(Map<String, String> fields, ...)` → `buildMultipartBody(List<Map.Entry<String, String>> fields, ...)`
- `JdkHttpSender.postMultipart` 내부에서 `for (Map.Entry<String, String> entry : fields.entrySet())` → `for (Map.Entry<String, String> entry : fields)`
- 테스트의 `FakeHttpSender.postMultipart` 시그니처도 동일 변경

```java
// HttpSender 인터페이스 (변경)
RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields)
    throws IOException, InterruptedException;

// SfoodAdminSessionManager의 공개 메서드 (변경)
public RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields) {
    try {
        return httpSender.postMultipart(url, fields);
    } catch (IOException | InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new SfoodAdminException("multipart POST 실패: url=" + url, exception);
    }
}

// JdkHttpSender.buildMultipartBody (변경)
private String buildMultipartBody(List<Map.Entry<String, String>> fields, String boundary) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : fields) {
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n");
        sb.append("\r\n");
        sb.append(entry.getValue() == null ? "" : entry.getValue()).append("\r\n");
    }
    sb.append("--").append(boundary).append("--\r\n");
    return sb.toString();
}

// JdkHttpSender.postMultipart (변경)
@Override
public RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields)
    throws IOException, InterruptedException {
    String boundary = "----SfoodAdminBoundary" + UUID.randomUUID().toString().replace("-", "");
    String body = buildMultipartBody(fields, boundary);
    ...
}
```

테스트의 `FakeHttpSender`:

```java
@Override
public SfoodAdminSessionManager.RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields)
    throws IOException, InterruptedException {
    lastRequestedUrl = url;
    requestCount++;
    return new SfoodAdminSessionManager.RawResponse(statuses.removeFirst(), bodies.removeFirst());
}
```

- [ ] **Step 2: 기존 세션매니저 테스트 확인**

```bash
./gradlew test --tests "*SfoodAdminSessionManagerTest"
```

Expected: 모든 테스트 PASS

- [ ] **Step 3: SfoodAdminGoodsClientTest 작성**

```java
package com.sfood.automation.goods.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.SfoodAdminException;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SfoodAdminGoodsClientTest {

    private static final String GOODS_FORM_HTML = """
        <html><form id="frmGoods">
          <input name="goodsNo" value="1000001371">
          <input name="mode" value="modify">
          <input type="radio" name="soldOutFl" value="n" checked>
          <input type="radio" name="soldOutFl" value="y">
          <input type="hidden" name="modDtUse" value="n">
          <input type="hidden" name="goodsNm" value="테스트상품">
        </form></html>
        """;

    private static final String SAVE_SUCCESS_HTML = "<html>저장이 완료되었습니다.</html>";

    private static final String VERIFY_FORM_HTML_ON_SALE = """
        <html><form id="frmGoods">
          <input name="goodsNo" value="1000001371">
          <input name="mode" value="modify">
          <input type="radio" name="soldOutFl" value="n" checked>
          <input type="radio" name="soldOutFl" value="y">
        </form></html>
        """;

    @Test
    void should_succeed_when_save_and_verify_pass() {
        FakeSessionManager sessionManager = new FakeSessionManager()
            .addGoodsFormResponse(GOODS_FORM_HTML)
            .addMultipartResponse(200, SAVE_SUCCESS_HTML)
            .addGoodsFormResponse(VERIFY_FORM_HTML_ON_SALE);
        SfoodAdminGoodsClient client = new SfoodAdminGoodsClient(
            sessionManager,
            new SfoodAdminFormCollector(),
            "https://admin.test"
        );

        GodoApiResult result = client.restoreToOnSale("1000001371");

        assertThat(result.success()).isTrue();
    }

    @Test
    void should_include_soldOutFl_n_and_modDtUse_y_in_submitted_fields() {
        FakeSessionManager sessionManager = new FakeSessionManager()
            .addGoodsFormResponse(GOODS_FORM_HTML)
            .addMultipartResponse(200, SAVE_SUCCESS_HTML)
            .addGoodsFormResponse(VERIFY_FORM_HTML_ON_SALE);
        SfoodAdminGoodsClient client = new SfoodAdminGoodsClient(
            sessionManager,
            new SfoodAdminFormCollector(),
            "https://admin.test"
        );

        client.restoreToOnSale("1000001371");

        List<Map.Entry<String, String>> submittedFields = sessionManager.lastMultipartFields;
        assertThat(submittedFields).contains(Map.entry("soldOutFl", "n"));
        assertThat(submittedFields).contains(Map.entry("modDtUse", "y"));
        long soldOutFlCount = submittedFields.stream().filter(e -> "soldOutFl".equals(e.getKey())).count();
        assertThat(soldOutFlCount).isEqualTo(1);
        long modDtUseCount = submittedFields.stream().filter(e -> "modDtUse".equals(e.getKey())).count();
        assertThat(modDtUseCount).isEqualTo(1);
    }

    @Test
    void should_fail_when_save_response_does_not_contain_success_message() {
        FakeSessionManager sessionManager = new FakeSessionManager()
            .addGoodsFormResponse(GOODS_FORM_HTML)
            .addMultipartResponse(200, "<html>상품명은 필수 항목 입니다.</html>");
        SfoodAdminGoodsClient client = new SfoodAdminGoodsClient(
            sessionManager,
            new SfoodAdminFormCollector(),
            "https://admin.test"
        );

        assertThatThrownBy(() -> client.restoreToOnSale("1000001371"))
            .isInstanceOf(SfoodAdminException.class)
            .hasMessageContaining("저장 실패");
    }

    @Test
    void should_fail_when_verify_shows_soldOutFl_still_y() {
        String verifyFormWithSoldOut = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <input type="radio" name="soldOutFl" value="n">
              <input type="radio" name="soldOutFl" value="y" checked>
            </form></html>
            """;
        FakeSessionManager sessionManager = new FakeSessionManager()
            .addGoodsFormResponse(GOODS_FORM_HTML)
            .addMultipartResponse(200, SAVE_SUCCESS_HTML)
            .addGoodsFormResponse(verifyFormWithSoldOut);
        SfoodAdminGoodsClient client = new SfoodAdminGoodsClient(
            sessionManager,
            new SfoodAdminFormCollector(),
            "https://admin.test"
        );

        assertThatThrownBy(() -> client.restoreToOnSale("1000001371"))
            .isInstanceOf(SfoodAdminException.class)
            .hasMessageContaining("검증 실패");
    }

    static final class FakeSessionManager extends SfoodAdminSessionManager {
        private final java.util.ArrayDeque<String> goodsFormResponses = new java.util.ArrayDeque<>();
        private final java.util.ArrayDeque<SfoodAdminSessionManager.RawResponse> multipartResponses = new java.util.ArrayDeque<>();
        List<Map.Entry<String, String>> lastMultipartFields;

        FakeSessionManager() {
            super("https://admin.test", "admin", "pass",
                new SfoodAdminSessionManagerTest.FakeHttpSender());
        }

        FakeSessionManager addGoodsFormResponse(String html) {
            goodsFormResponses.add(html);
            return this;
        }

        FakeSessionManager addMultipartResponse(int status, String body) {
            multipartResponses.add(new SfoodAdminSessionManager.RawResponse(status, body));
            return this;
        }

        @Override
        public String fetchGoodsFormPage(String goodsNo) {
            return goodsFormResponses.removeFirst();
        }

        @Override
        public SfoodAdminSessionManager.RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields) {
            lastMultipartFields = fields;
            return multipartResponses.removeFirst();
        }
    }
}
```

- [ ] **Step 4: 테스트 실행해서 컴파일 에러 확인**

```bash
./gradlew test --tests "*SfoodAdminGoodsClientTest" 2>&1 | head -20
```

Expected: compilation error (SfoodAdminGoodsClient not found)

- [ ] **Step 5: SfoodAdminGoodsClient 구현**

```java
package com.sfood.automation.goods.infrastructure.admin;

import com.sfood.automation.common.exception.SfoodAdminException;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(SfoodAdminProperties.class)
public class SfoodAdminGoodsClient implements SfoodAdminGoodsPort {

    private static final Logger log = LoggerFactory.getLogger(SfoodAdminGoodsClient.class);
    private static final String SAVE_SUCCESS_MARKER = "저장이 완료되었습니다.";

    private final SfoodAdminSessionManager sessionManager;
    private final SfoodAdminFormCollector formCollector;
    private final String baseUrl;

    public SfoodAdminGoodsClient(
        SfoodAdminSessionManager sessionManager,
        SfoodAdminFormCollector formCollector,
        SfoodAdminProperties properties
    ) {
        this.sessionManager = sessionManager;
        this.formCollector = formCollector;
        this.baseUrl = properties.getBaseUrl();
    }

    SfoodAdminGoodsClient(
        SfoodAdminSessionManager sessionManager,
        SfoodAdminFormCollector formCollector,
        String baseUrl
    ) {
        this.sessionManager = sessionManager;
        this.formCollector = formCollector;
        this.baseUrl = baseUrl;
    }

    @Override
    public GodoApiResult restoreToOnSale(String godoGoodsCode) {
        log.info("SfoodAdmin restoreToOnSale started: godoGoodsCode={}", godoGoodsCode);

        String formHtml = sessionManager.fetchGoodsFormPage(godoGoodsCode);
        List<Map.Entry<String, String>> formData = formCollector.collect(formHtml, godoGoodsCode);
        List<Map.Entry<String, String>> submittableData = replaceTargetFields(formData);

        String saveUrl = baseUrl + "/goods/goods_ps.php";
        SfoodAdminSessionManager.RawResponse saveResponse = sessionManager.postMultipart(saveUrl, submittableData);

        if (!saveResponse.body().contains(SAVE_SUCCESS_MARKER)) {
            log.warn("SfoodAdmin save failed: godoGoodsCode={} responseSnippet={}", godoGoodsCode, snippet(saveResponse.body()));
            throw new SfoodAdminException("저장 실패: godoGoodsCode=" + godoGoodsCode);
        }

        verifySoldOutRestored(godoGoodsCode);

        log.info("SfoodAdmin restoreToOnSale succeeded: godoGoodsCode={}", godoGoodsCode);
        return new GodoApiResult(true, "SUCCESS", "정상 전환 완료");
    }

    private List<Map.Entry<String, String>> replaceTargetFields(List<Map.Entry<String, String>> formData) {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : formData) {
            if ("soldOutFl".equals(entry.getKey()) || "modDtUse".equals(entry.getKey())) {
                continue;
            }
            result.add(entry);
        }
        result.add(Map.entry("soldOutFl", "n"));
        result.add(Map.entry("modDtUse", "y"));
        return result;
    }

    private void verifySoldOutRestored(String godoGoodsCode) {
        String verifyHtml = sessionManager.fetchGoodsFormPage(godoGoodsCode);
        List<Map.Entry<String, String>> verifyData = formCollector.collect(verifyHtml, godoGoodsCode);
        String soldOutFl = verifyData.stream()
            .filter(e -> "soldOutFl".equals(e.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
        if (!"n".equals(soldOutFl)) {
            log.error("SfoodAdmin verify failed: godoGoodsCode={} soldOutFl={}", godoGoodsCode, soldOutFl);
            throw new SfoodAdminException("검증 실패: godoGoodsCode=" + godoGoodsCode + " soldOutFl=" + soldOutFl);
        }
    }

    private String snippet(String body) {
        return body == null ? "" : body.substring(0, Math.min(200, body.length()));
    }
}
```

- [ ] **Step 6: 테스트 실행**

```bash
./gradlew test --tests "*SfoodAdminGoodsClientTest"
```

Expected: 모든 테스트 PASS

- [ ] **Step 7: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminGoodsClient.java \
  src/main/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminSessionManager.java \
  src/test/java/com/sfood/automation/goods/infrastructure/admin/SfoodAdminGoodsClientTest.java
git commit -m "feat: add SfoodAdminGoodsClient implementing SfoodAdminGoodsPort for on-sale restore"
```

---

## Task 6: SyncGodoStatusUseCase 수정 + 테스트 업데이트

**Files:**
- Modify: `backend/src/main/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCase.java`
- Modify: `backend/src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java`

- [ ] **Step 1: SyncGodoStatusUseCase 수정**

`SyncGodoStatusUseCase`에 `SfoodAdminGoodsPort` 필드와 생성자 파라미터를 추가하고, `applyTransition`에서 정상 전환 시 새 포트를 사용한다.

`SyncGodoStatusUseCase.java`의 필드 및 생성자 변경:

```java
// 추가할 import
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;

// 추가할 필드
private final SfoodAdminGoodsPort sfoodAdminGoodsPort;

// 생성자 파라미터 추가 (기존 파라미터 뒤에)
public SyncGodoStatusUseCase(
    GoodsRepository goodsRepository,
    GodoApiPort godoApiPort,
    GoodsStatusLogRepository goodsStatusLogRepository,
    WmsStockAggregator wmsStockAggregator,
    GoodsSoldOutPolicy goodsSoldOutPolicy,
    SlackNotifier slackNotifier,
    SfoodAdminGoodsPort sfoodAdminGoodsPort
) {
    this.goodsRepository = goodsRepository;
    this.godoApiPort = godoApiPort;
    this.goodsStatusLogRepository = goodsStatusLogRepository;
    this.wmsStockAggregator = wmsStockAggregator;
    this.goodsSoldOutPolicy = goodsSoldOutPolicy;
    this.slackNotifier = slackNotifier;
    this.sfoodAdminGoodsPort = sfoodAdminGoodsPort;
}
```

`applyTransition` 메서드를 아래와 같이 변경한다. 기존 코드에서 `godoApiPort.updateSoldOutStatus(goods.getGodoGoodsCode(), soldOutFl)` 호출 부분을 두 경로로 분기한다:

```java
private int applyTransition(
    Goods goods,
    AggregatedStock aggregatedStock,
    String soldOutFl,
    String soldOutYn,
    String beforeStatus,
    String afterStatus,
    int criteriaQty,
    List<GoodsStatusLog> changedLogs
) {
    try {
        var result = callStatusApi(goods.getGodoGoodsCode(), soldOutFl);
        if (!result.success()) {
            slackNotifier.send(
                "SyncGodoStatusUseCase.applyTransition",
                "Failed to update sold-out status for godoGoodsCode=" + goods.getGodoGoodsCode()
            );
            return 0;
        }
    } catch (GodoApiException | com.sfood.automation.common.exception.SfoodAdminException exception) {
        slackNotifier.send(
            "SyncGodoStatusUseCase.applyTransition",
            "API error for godoGoodsCode=" + goods.getGodoGoodsCode()
        );
        return 0;
    }

    goods.updateSoldOutStatus(soldOutYn, SYSTEM_USER);
    goodsRepository.save(goods);
    changedLogs.add(saveLog(goods, aggregatedStock, beforeStatus, afterStatus, criteriaQty));
    return 1;
}

private GodoApiResult callStatusApi(String godoGoodsCode, String soldOutFl) {
    if ("n".equals(soldOutFl)) {
        return sfoodAdminGoodsPort.restoreToOnSale(godoGoodsCode);
    }
    return godoApiPort.updateSoldOutStatus(godoGoodsCode, soldOutFl);
}
```

- [ ] **Step 2: SyncGodoStatusUseCaseTest 업데이트**

테스트의 `useCase(...)` 팩토리 메서드에 `FakeSfoodAdminGoodsPort`를 추가하고, 정상 전환 테스트를 업데이트한다.

기존 `FakeGodoApiPort`에 기록된 `"10000002:n"` 검증들을 `FakeSfoodAdminGoodsPort`로 이동한다.

테스트 클래스에 아래를 추가한다:

```java
// import 추가
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import com.sfood.automation.common.exception.SfoodAdminException;

// FakeSfoodAdminGoodsPort 내부 클래스 추가
private static final class FakeSfoodAdminGoodsPort implements SfoodAdminGoodsPort {
    final List<String> calls = new ArrayList<>();
    final Map<String, RuntimeException> failures = new HashMap<>();

    @Override
    public GodoApiResult restoreToOnSale(String godoGoodsCode) {
        calls.add(godoGoodsCode);
        RuntimeException failure = failures.get(godoGoodsCode);
        if (failure != null) {
            throw failure;
        }
        return new GodoApiResult(true, "SUCCESS", "정상 전환 완료");
    }

    void failForCode(String godoGoodsCode) {
        failures.put(godoGoodsCode, new SfoodAdminException("admin error"));
    }
}
```

`useCase(...)` 팩토리 메서드를 아래와 같이 변경한다:

```java
private SyncGodoStatusUseCase useCase(
    FakeGoodsRepository goodsRepository,
    FakeGodoApiPort godoApiPort,
    FakeGoodsStatusLogRepository logRepository
) {
    return useCase(goodsRepository, godoApiPort, logRepository, new FakeSfoodAdminGoodsPort());
}

private SyncGodoStatusUseCase useCase(
    FakeGoodsRepository goodsRepository,
    FakeGodoApiPort godoApiPort,
    FakeGoodsStatusLogRepository logRepository,
    FakeSfoodAdminGoodsPort sfoodAdminGoodsPort
) {
    return new SyncGodoStatusUseCase(
        goodsRepository,
        godoApiPort,
        logRepository,
        null,
        new GoodsSoldOutPolicy(),
        org.mockito.Mockito.mock(SlackNotifier.class),
        sfoodAdminGoodsPort
    );
}
```

`"10000002:n"` 기대치를 갖는 테스트들을 업데이트한다:

1. `should_process_out_of_stock_before_on_sale` 테스트:
```java
@Test
void should_process_out_of_stock_before_on_sale() {
    FakeGoodsRepository goodsRepository = new FakeGoodsRepository(
        goods("10000001", "ERP-0001", "N", "N"),
        goods("10000002", "ERP-0002", "N", "Y")
    );
    FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
    FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
    SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort);

    useCase.execute(
        soldOutStocks(stock("ERP-0001", 20, 80)),
        onSaleStocks(stock("ERP-0002", 21, 50))
    );

    assertThat(godoApiPort.calls).containsExactly("10000001:y");
    assertThat(sfoodAdminGoodsPort.calls).containsExactly("10000002");
}
```

2. `should_return_sync_result_counts` 테스트:
```java
@Test
void should_return_sync_result_counts() {
    FakeGoodsRepository goodsRepository = new FakeGoodsRepository(
        goods("10000001", "ERP-0001", "N", "N"),
        goods("10000002", "ERP-0002", "N", "Y")
    );
    FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
    SyncGodoStatusUseCase useCase = useCase(goodsRepository, new FakeGodoApiPort(), new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort);

    SyncResult result = useCase.execute(
        soldOutStocks(stock("ERP-0001", 20, 80)),
        onSaleStocks(stock("ERP-0002", 21, 50))
    );

    assertThat(result.outOfStockCount()).isEqualTo(1);
    assertThat(result.onSaleCount()).isEqualTo(1);
    assertThat(result.changedLogs()).hasSize(2);
}
```

3. `should_recover_on_sale_using_goods_available_qty_after_safety_filtered_aggregation` 테스트:
```java
@Test
void should_recover_on_sale_using_goods_available_qty_after_safety_filtered_aggregation() {
    FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000002", "ERP-0002", "N", "Y"));
    FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
    SyncGodoStatusUseCase useCase = useCase(goodsRepository, new FakeGodoApiPort(), new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort);

    SyncResult result = useCase.execute(
        Map.of(),
        onSaleStocks(stock("ERP-0002", 21, 60))
    );

    assertThat(result.onSaleCount()).isEqualTo(1);
    assertThat(sfoodAdminGoodsPort.calls).containsExactly("10000002");
}
```

4. `should_not_flip_same_goods_back_to_on_sale_within_single_batch` 테스트:
```java
@Test
void should_not_flip_same_goods_back_to_on_sale_within_single_batch() {
    FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N", "N"));
    FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
    FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
    SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort);

    SyncResult result = useCase.execute(
        soldOutStocks(stock("ERP-0001", 20, 80)),
        onSaleStocks(stock("ERP-0001", 20, 80))
    );

    assertThat(result.outOfStockCount()).isEqualTo(1);
    assertThat(result.onSaleCount()).isZero();
    assertThat(godoApiPort.calls).containsExactly("10000001:y");
    assertThat(sfoodAdminGoodsPort.calls).isEmpty();
    assertThat(goodsRepository.savedGoods.getLast().getSoldOutYn()).isEqualTo("Y");
}
```

- [ ] **Step 3: 테스트 실행**

```bash
./gradlew test --tests "*SyncGodoStatusUseCaseTest"
```

Expected: 모든 테스트 PASS

- [ ] **Step 4: 전체 테스트 확인**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCase.java \
  src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java
git commit -m "feat: use SfoodAdminGoodsPort for on-sale restore in SyncGodoStatusUseCase"
```

---

## Task 7: ForceSyncGodoStatusUseCase 수정 + 테스트 업데이트

**Files:**
- Modify: `backend/src/main/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCase.java`
- Modify: `backend/src/test/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCaseTest.java`

- [ ] **Step 1: ForceSyncGodoStatusUseCase 수정**

`SfoodAdminGoodsPort` 필드와 생성자 파라미터 추가:

```java
// 추가할 import
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import com.sfood.automation.common.exception.SfoodAdminException;

// 추가할 필드
private final SfoodAdminGoodsPort sfoodAdminGoodsPort;

// 생성자 변경
public ForceSyncGodoStatusUseCase(
    GoodsRepository goodsRepository,
    GodoApiPort godoApiPort,
    GoodsStatusLogRepository goodsStatusLogRepository,
    SfoodAdminGoodsPort sfoodAdminGoodsPort
) {
    this.goodsRepository = goodsRepository;
    this.godoApiPort = godoApiPort;
    this.goodsStatusLogRepository = goodsStatusLogRepository;
    this.sfoodAdminGoodsPort = sfoodAdminGoodsPort;
}
```

`forceSync` 메서드의 기존 `godoApiPort.updateSoldOutStatus` 호출 블록을 아래로 교체한다:

```java
try {
    var result = callStatusApi(goods.getGodoGoodsCode(), targetSoldOutYn);
    if (!result.success()) {
        log.warn(
            "Force sync failed: godoGoodsCode={} reason=API_NON_SUCCESS resultCode={} resultMessage={}",
            godoGoodsCode,
            result.resultCode(),
            result.resultMessage()
        );
        return new ForceSyncGodoStatusItemResponse(goods.getGodoGoodsCode(), "FAILED", result.resultMessage(), goods.getSoldOutYn());
    }
} catch (GodoApiException | SfoodAdminException exception) {
    log.error(
        "Force sync failed: godoGoodsCode={} reason=API_EXCEPTION targetSoldOutYn={} currentSoldOutYn={}",
        godoGoodsCode,
        targetSoldOutYn,
        goods.getSoldOutYn(),
        exception
    );
    return new ForceSyncGodoStatusItemResponse(goods.getGodoGoodsCode(), "FAILED", "API error", goods.getSoldOutYn());
}
```

`callStatusApi` 헬퍼 메서드 추가:

```java
private GodoApiResult callStatusApi(String godoGoodsCode, String targetSoldOutYn) {
    if ("N".equals(targetSoldOutYn)) {
        return sfoodAdminGoodsPort.restoreToOnSale(godoGoodsCode);
    }
    return godoApiPort.updateSoldOutStatus(godoGoodsCode, soldOutFlag(targetSoldOutYn));
}
```

- [ ] **Step 2: ForceSyncGodoStatusUseCaseTest 업데이트**

`FakeSfoodAdminGoodsPort` 내부 클래스 추가:

```java
// import 추가
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import com.sfood.automation.common.exception.SfoodAdminException;

private static final class FakeSfoodAdminGoodsPort implements SfoodAdminGoodsPort {
    final List<String> calls = new ArrayList<>();
    final Map<String, RuntimeException> failures = new HashMap<>();

    @Override
    public GodoApiResult restoreToOnSale(String godoGoodsCode) {
        calls.add(godoGoodsCode);
        RuntimeException failure = failures.get(godoGoodsCode);
        if (failure != null) {
            throw failure;
        }
        return new GodoApiResult(true, "SUCCESS", "정상 전환 완료");
    }

    void failForCode(String godoGoodsCode) {
        failures.put(godoGoodsCode, new SfoodAdminException("admin error"));
    }
}
```

기존 테스트들의 `new ForceSyncGodoStatusUseCase(goodsRepository, godoApiPort, logRepository)` → `new ForceSyncGodoStatusUseCase(goodsRepository, godoApiPort, logRepository, new FakeSfoodAdminGoodsPort())`로 변경한다.

기존 테스트 `should_force_sync_eligible_goods_and_update_local_state`에서 `targetSoldOutYn="Y"` → `"y"` API를 기대하는 것은 유지 (soldOut 방향은 기존 API). 정상 전환("N")을 테스트하는 케이스 추가:

```java
@Test
void should_use_sfood_admin_when_restoring_to_on_sale() {
    FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "Y"));
    FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
    FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
    FakeGoodsStatusLogRepository logRepository = new FakeGoodsStatusLogRepository();
    ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(
        goodsRepository, godoApiPort, logRepository, sfoodAdminGoodsPort
    );

    ForceSyncGodoStatusResponse response = useCase.execute(
        new ForceSyncGodoStatusRequest(List.of("10000001"), "N"),
        "user@s-food.com"
    );

    assertThat(godoApiPort.calls).isEmpty();
    assertThat(sfoodAdminGoodsPort.calls).containsExactly("10000001");
    assertThat(goodsRepository.savedGoods).hasSize(1);
    assertThat(goodsRepository.savedGoods.get(0).getSoldOutYn()).isEqualTo("N");
    assertThat(logRepository.savedLogs.get(0).beforeStatus()).isEqualTo("OUT_OF_STOCK");
    assertThat(logRepository.savedLogs.get(0).afterStatus()).isEqualTo("ON_SALE");
    assertThat(response.items()).containsExactly(
        new ForceSyncGodoStatusItemResponse("10000001", "SUCCESS", null, "N")
    );
}

@Test
void should_return_failed_when_sfood_admin_throws_on_restore() {
    FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "Y"));
    FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
    sfoodAdminGoodsPort.failForCode("10000001");
    ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(
        goodsRepository, new FakeGodoApiPort(), new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort
    );

    ForceSyncGodoStatusResponse response = useCase.execute(
        new ForceSyncGodoStatusRequest(List.of("10000001"), "N"),
        "user@s-food.com"
    );

    assertThat(goodsRepository.savedGoods).isEmpty();
    assertThat(response.items()).containsExactly(
        new ForceSyncGodoStatusItemResponse("10000001", "FAILED", "API error", "Y")
    );
}
```

- [ ] **Step 3: 테스트 실행**

```bash
./gradlew test --tests "*ForceSyncGodoStatusUseCaseTest"
```

Expected: 모든 테스트 PASS

- [ ] **Step 4: 전체 테스트 확인**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCase.java \
  src/test/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCaseTest.java
git commit -m "feat: use SfoodAdminGoodsPort for on-sale restore in ForceSyncGodoStatusUseCase"
```

---

## Task 8: RequiredEnvironmentValidator에 sfood-admin 키 추가

**Files:**
- Modify: `backend/src/main/java/com/sfood/automation/common/config/RequiredEnvironmentValidator.java`

- [ ] **Step 1: REQUIRED_KEYS에 추가**

기존 `REQUIRED_KEYS` 리스트에 아래 3개를 추가한다:

```java
"app.sfood-admin.base-url",
"app.sfood-admin.manager-id",
"app.sfood-admin.manager-pw"
```

- [ ] **Step 2: application.yml의 빈 기본값 처리 확인**

`RequiredEnvironmentValidator`는 빈 값을 거부한다. application.yml에서:
```yaml
manager-id: ${SFOOD_ADMIN_MANAGER_ID:}
manager-pw: ${SFOOD_ADMIN_MANAGER_PW:}
```

기본값이 빈 문자열이므로 실제 운영에서는 환경변수를 반드시 주입해야 한다. 로컬 개발을 위해 `application-local.yml`에 테스트용 플레이스홀더 값을 추가한다:

```yaml
app:
  sfood-admin:
    manager-id: local-test-admin
    manager-pw: local-test-password
```

- [ ] **Step 3: 기존 RequiredEnvironmentValidator 테스트 확인**

```bash
./gradlew test --tests "*RequiredEnvironment*"
```

Expected: PASS (기존 테스트는 테스트용 application.yml을 별도 사용하므로 영향 없음)

- [ ] **Step 4: 전체 테스트 확인**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/common/config/RequiredEnvironmentValidator.java \
  src/main/resources/application-local.yml
git commit -m "feat: add sfood-admin required environment keys to validator"
```

---

## 자기검토 (Self-Review)

### 스펙 커버리지

| 요구사항 | Task |
|---|---|
| 품절 → 기존 GODO API 유지 | Task 6, 7 (`callStatusApi`에서 "y"는 기존 API) |
| 정상 → 관리자 스크래핑으로 변경 | Task 5, 6, 7 |
| 로그인 세션 재사용 및 재로그인 | Task 3 (`SfoodAdminSessionManager`) |
| 전체 FormData 수집 (브라우저 규칙) | Task 4 (`SfoodAdminFormCollector`) |
| soldOutFl + modDtUse만 덮어쓰기 | Task 5 (`replaceTargetFields`) |
| multipart/form-data POST | Task 3 (`JdkHttpSender.postMultipart`) |
| 저장 성공 메시지 확인 | Task 5 (`SAVE_SUCCESS_MARKER`) |
| 저장 후 조회 검증 | Task 5 (`verifySoldOutRestored`) |
| 배치(`SyncGodoStatusUseCase`) 연동 | Task 6 |
| 컨트롤러(`ForceSyncGodoStatusUseCase`) 연동 | Task 7 |
| 환경변수 필수값 검증 | Task 8 |
| 비밀번호/세션 로그 마스킹 | `SfoodAdminSessionManager` (`[MASKED]` 처리) |

### 타입 일관성

- `SfoodAdminGoodsPort.restoreToOnSale(String)` → `GodoApiResult` : Task 2, 5, 6, 7 모두 일치
- `SfoodAdminSessionManager.postMultipart(String, List<Map.Entry<String,String>>)` : Task 3, 5 일치
- `SfoodAdminFormCollector.collect(String, String)` → `List<Map.Entry<String,String>>` : Task 4, 5 일치
