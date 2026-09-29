package com.sfood.automation.goods.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.SfoodAdminException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SfoodAdminSessionManagerTest {

    @Test
    void should_detect_login_success_by_message_in_response() {
        FakeHttpSender sender = new FakeHttpSender()
            .addResponse(200, "<html>관리자 접속이 승인되었습니다.</html>");
        SfoodAdminSessionManager manager = new SfoodAdminSessionManager(
            testProperties("https://admin.test", "admin", "pass"), sender
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
            testProperties("https://admin.test", "admin", "wrong"), sender
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
            testProperties("https://admin.test", "admin", "pass"), sender
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
            testProperties("https://admin.test", "admin", "pass"), sender
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
            testProperties("https://admin.test", "admin", "wrong"), sender
        );

        assertThatThrownBy(() -> manager.fetchGoodsFormPage("1000001371"))
            .isInstanceOf(SfoodAdminException.class)
            .hasMessageContaining("로그인");
    }

    private static SfoodAdminProperties testProperties(String baseUrl, String managerId, String managerPw) {
        SfoodAdminProperties props = new SfoodAdminProperties();
        props.setBaseUrl(baseUrl);
        props.setManagerId(managerId);
        props.setManagerPw(managerPw);
        props.setLoginPath("/base/login_ps.php");
        props.setGoodsFormPath("/goods/goods_register.php");
        return props;
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
        public SfoodAdminSessionManager.RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields, List<String> emptyFileFieldNames)
            throws IOException, InterruptedException {
            lastRequestedUrl = url;
            requestCount++;
            return new SfoodAdminSessionManager.RawResponse(statuses.removeFirst(), bodies.removeFirst());
        }
    }
}
