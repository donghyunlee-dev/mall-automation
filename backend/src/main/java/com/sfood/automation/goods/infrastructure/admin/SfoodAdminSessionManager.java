package com.sfood.automation.goods.infrastructure.admin;

import com.sfood.automation.common.admin.SfoodAdminHttpClient;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(SfoodAdminProperties.class)
public class SfoodAdminSessionManager implements SfoodAdminHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SfoodAdminSessionManager.class);
    private static final String LOGIN_SUCCESS_MARKER = "관리자 접속이 승인되었습니다.";
    private static final String GOODS_FORM_MARKER = "frmGoods";
    private static final String MEMBER_LIST_MARKER = "table-rows";
    private static final String MEMBER_DETAIL_MARKER = "memNm";

    private final String baseUrl;
    private final String managerId;
    private final String managerPw;
    private final String loginPath;
    private final String goodsFormPath;
    private final HttpSender httpSender;

    @Autowired
    public SfoodAdminSessionManager(SfoodAdminProperties properties) {
        this.baseUrl = properties.getBaseUrl();
        this.managerId = properties.getManagerId();
        this.managerPw = properties.getManagerPw();
        this.loginPath = properties.getLoginPath();
        this.goodsFormPath = properties.getGoodsFormPath();
        this.httpSender = new JdkHttpSender(createHttpClient());
    }

    SfoodAdminSessionManager(SfoodAdminProperties properties, HttpSender httpSender) {
        this.baseUrl = properties.getBaseUrl();
        this.managerId = properties.getManagerId();
        this.managerPw = properties.getManagerPw();
        this.loginPath = properties.getLoginPath();
        this.goodsFormPath = properties.getGoodsFormPath();
        this.httpSender = httpSender;
    }

    @Override
    public boolean login() {
        String formBody = "mode=login"
            + "&returnUrl=" + encode("/")
            + "&managerId=" + encode(managerId)
            + "&managerPw=" + encode(managerPw);
        try {
            RawResponse response = httpSender.postForm(baseUrl + loginPath, formBody);
            boolean success = response.body().contains(LOGIN_SUCCESS_MARKER);
            if (success) {
                log.info("SfoodAdmin login succeeded: managerId=[MASKED]");
            } else {
                log.warn("SfoodAdmin login failed: managerId=[MASKED]");
            }
            return success;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("관리자 로그인 요청 실패", exception);
        } catch (IOException exception) {
            throw new SfoodAdminException("관리자 로그인 요청 실패", exception);
        }
    }

    @Override
    public String fetchPage(String url) {
        try {
            RawResponse response = httpSender.get(url);
            if (response.statusCode() >= 400) {
                throw new SfoodAdminException("페이지 조회 실패: status=" + response.statusCode() + ", url=" + url);
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("페이지 조회 요청 실패: url=" + url, exception);
        } catch (IOException exception) {
            throw new SfoodAdminException("페이지 조회 요청 실패: url=" + url, exception);
        }
    }

    public String fetchGoodsFormPage(String goodsNo) {
        String url = baseUrl + goodsFormPath + "?popupMode=yes&goodsNo=" + goodsNo;
        try {
            RawResponse response = httpSender.get(url);
            if (!response.body().contains(GOODS_FORM_MARKER)) {
                log.info("SfoodAdmin goods form not found, attempting login: goodsNo={}", goodsNo);
                if (!login()) {
                    throw new SfoodAdminException("관리자 로그인 실패: goodsNo=" + goodsNo);
                }
                response = httpSender.get(url);
            }
            if (!response.body().contains(GOODS_FORM_MARKER)) {
                throw new SfoodAdminException("상품 폼 없음: goodsNo=" + goodsNo);
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("상품 폼 페이지 요청 실패: goodsNo=" + goodsNo, exception);
        } catch (IOException exception) {
            throw new SfoodAdminException("상품 폼 페이지 요청 실패: goodsNo=" + goodsNo, exception);
        }
    }

    public String fetchMemberListPage(String url) {
        try {
            RawResponse response = httpSender.get(url);
            if (!response.body().contains(MEMBER_LIST_MARKER)) {
                log.info("SfoodAdmin member list not found, attempting login: url={}", url);
                if (!login()) {
                    throw new SfoodAdminException("관리자 로그인 실패: url=" + url);
                }
                response = httpSender.get(url);
            }
            if (!response.body().contains(MEMBER_LIST_MARKER)) {
                throw new SfoodAdminException("회원 목록 없음: url=" + url);
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("회원 목록 페이지 요청 실패: url=" + url, exception);
        } catch (IOException exception) {
            throw new SfoodAdminException("회원 목록 페이지 요청 실패: url=" + url, exception);
        }
    }

    public String fetchMemberDetailPage(String url) {
        try {
            RawResponse response = httpSender.get(url);
            if (!response.body().contains(MEMBER_DETAIL_MARKER)) {
                log.info("SfoodAdmin member detail not found, attempting login: url={}", url);
                if (!login()) {
                    throw new SfoodAdminException("관리자 로그인 실패: url=" + url);
                }
                response = httpSender.get(url);
            }
            if (!response.body().contains(MEMBER_DETAIL_MARKER)) {
                throw new SfoodAdminException("회원 상세 없음: url=" + url);
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("회원 상세 페이지 요청 실패: url=" + url, exception);
        } catch (IOException exception) {
            throw new SfoodAdminException("회원 상세 페이지 요청 실패: url=" + url, exception);
        }
    }

    public RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields,
                                     List<String> emptyFileFieldNames) {
        try {
            return httpSender.postMultipart(url, fields, emptyFileFieldNames);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SfoodAdminException("multipart POST 실패: url=" + url, exception);
        } catch (IOException exception) {
            throw new SfoodAdminException("multipart POST 실패: url=" + url, exception);
        }
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
        RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields,
                                  List<String> emptyFileFieldNames) throws IOException, InterruptedException;
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
        public RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields,
                                         List<String> emptyFileFieldNames) throws IOException, InterruptedException {
            String boundary = "----SfoodAdminBoundary" + UUID.randomUUID().toString().replace("-", "");
            String body = buildMultipartBody(fields, emptyFileFieldNames, boundary);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new RawResponse(response.statusCode(), response.body());
        }

        private String buildMultipartBody(List<Map.Entry<String, String>> fields,
                                          List<String> emptyFileFieldNames, String boundary) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : fields) {
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n");
                sb.append("\r\n");
                sb.append(entry.getValue() == null ? "" : entry.getValue()).append("\r\n");
            }
            // 빈 파일 파트: 브라우저의 빈 file input 전송과 동일
            // Godo goods_ps.php는 UPLOAD_ERR_NO_FILE(error=4)을 "신규 업로드 없음, 기존 이미지 유지"로 처리한다
            for (String name : emptyFileFieldNames) {
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"; filename=\"\"\r\n");
                sb.append("Content-Type: application/octet-stream\r\n");
                sb.append("\r\n");
                sb.append("\r\n");
            }
            sb.append("--").append(boundary).append("--\r\n");
            return sb.toString();
        }
    }
}
