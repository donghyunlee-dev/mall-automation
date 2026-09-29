package com.sfood.automation.goods.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.SfoodAdminException;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
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
            testGoodsProperties()
        );

        GodoApiResult result = client.restoreToOnSale("1000001371");

        assertThat(result.success()).isTrue();
    }

    @Test
    void should_post_to_correct_save_url() {
        FakeSessionManager sessionManager = new FakeSessionManager()
            .addGoodsFormResponse(GOODS_FORM_HTML)
            .addMultipartResponse(200, SAVE_SUCCESS_HTML)
            .addGoodsFormResponse(VERIFY_FORM_HTML_ON_SALE);
        SfoodAdminGoodsClient client = new SfoodAdminGoodsClient(
            sessionManager,
            new SfoodAdminFormCollector(),
            testGoodsProperties()
        );

        client.restoreToOnSale("1000001371");

        assertThat(sessionManager.lastMultipartUrl)
            .isEqualTo("https://admin.test/goods/goods_ps.php");
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
            testGoodsProperties()
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
            testGoodsProperties()
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
            testGoodsProperties()
        );

        assertThatThrownBy(() -> client.restoreToOnSale("1000001371"))
            .isInstanceOf(SfoodAdminException.class)
            .hasMessageContaining("검증 실패");
    }

    private static SfoodAdminProperties testGoodsProperties() {
        SfoodAdminProperties props = new SfoodAdminProperties();
        props.setBaseUrl("https://admin.test");
        props.setGoodsSavePath("/goods/goods_ps.php");
        return props;
    }

    static final class FakeSessionManager extends SfoodAdminSessionManager {
        private final Deque<String> goodsFormResponses = new ArrayDeque<>();
        private final Deque<RawResponse> multipartResponses = new ArrayDeque<>();
        String lastMultipartUrl;
        List<Map.Entry<String, String>> lastMultipartFields;

        FakeSessionManager() {
            super(fakeSessionProperties(), new FakeHttpSender());
        }

        private static SfoodAdminProperties fakeSessionProperties() {
            SfoodAdminProperties props = new SfoodAdminProperties();
            props.setBaseUrl("https://admin.test");
            props.setManagerId("admin");
            props.setManagerPw("pass");
            props.setLoginPath("/base/login_ps.php");
            props.setGoodsFormPath("/goods/goods_register.php");
            return props;
        }

        FakeSessionManager addGoodsFormResponse(String html) {
            goodsFormResponses.add(html);
            return this;
        }

        FakeSessionManager addMultipartResponse(int status, String body) {
            multipartResponses.add(new RawResponse(status, body));
            return this;
        }

        @Override
        public String fetchGoodsFormPage(String goodsNo) {
            return goodsFormResponses.removeFirst();
        }

        @Override
        public RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields, List<String> emptyFileFieldNames) {
            lastMultipartUrl = url;
            lastMultipartFields = fields;
            return multipartResponses.removeFirst();
        }
    }

    static final class FakeHttpSender implements SfoodAdminSessionManager.HttpSender {
        @Override
        public SfoodAdminSessionManager.RawResponse get(String url) throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public SfoodAdminSessionManager.RawResponse postForm(String url, String body)
            throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public SfoodAdminSessionManager.RawResponse postMultipart(String url, List<Map.Entry<String, String>> fields, List<String> emptyFileFieldNames)
            throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }
    }
}
