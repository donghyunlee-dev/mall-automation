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

        var fields = collector.collect(html, "1000001371").textFields();

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

        var fields = collector.collect(html, "1000001371").textFields();

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

        var fields = collector.collect(html, "1000001371").textFields();

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

        var fields = collector.collect(html, "1000001371").textFields();

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

        var fields = collector.collect(html, "1000001371").textFields();

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

        var fields = collector.collect(html, "1000001371").textFields();

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

        var fields = collector.collect(html, "1000001371").textFields();

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
    void should_collect_imageDB_fields_from_js_injected_script() {
        // 고도몰은 imageDB[*] hidden 필드를 JS .html()로 동적 삽입하므로 script 태그에서 추출해야 한다
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <div id="imageMagnifyPreView0"></div>
              <script type="text/javascript">
                $('#imageMagnifyPreView0').html(' <input type=\\"hidden\\" name=\\"imageDB[sno][]\\" value=\\"7286\\" /> <input type=\\"hidden\\" name=\\"imageDB[imageName][]\\" value=\\"1000001371_magnify_03.png\\" /> <input type=\\"hidden\\" name=\\"imageDB[imageKind][]\\" value=\\"magnify\\" /> <input type=\\"hidden\\" name=\\"imageDB[imageCode][magnify0]\\" value=\\"7286\\" />');
              </script>
            </form></html>
            """;

        var fields = collector.collect(html, "1000001371").textFields();

        assertThat(fields).contains(Map.entry("imageDB[sno][]", "7286"));
        assertThat(fields).contains(Map.entry("imageDB[imageName][]", "1000001371_magnify_03.png"));
        assertThat(fields).contains(Map.entry("imageDB[imageKind][]", "magnify"));
        assertThat(fields).contains(Map.entry("imageDB[imageCode][magnify0]", "7286"));
    }

    @Test
    void should_collect_imageDB_fields_when_script_uses_html_comment_wrapper() {
        // 실제 고도몰 스크립트는 <!-- --> 래퍼를 사용한다.
        // Jsoup.parseBodyFragment는 이 내용을 HTML 주석으로 처리해 input을 무시하므로 정규식으로 추출해야 한다.
        String html = """
            <html><form id="frmGoods">
              <input name="goodsNo" value="1000001371">
              <input name="mode" value="modify">
              <div id="imageMainPreView0"></div>
              <script type="text/javascript">
              <!--
                $('#imageMainPreView0').html(' <input type=\\"hidden\\" name=\\"imageDB[sno][]\\" value=\\"7284\\" /> <input type=\\"hidden\\" name=\\"imageDB[imageName][]\\" value=\\"1000001371_main_07.png\\" /> <input type=\\"checkbox\\" name=\\"imageDB[imageDelFl][main0]\\" value=\\"y\\" />');
              // -->
              </script>
            </form></html>
            """;

        var fields = collector.collect(html, "1000001371").textFields();

        assertThat(fields).contains(Map.entry("imageDB[sno][]", "7284"));
        assertThat(fields).contains(Map.entry("imageDB[imageName][]", "1000001371_main_07.png"));
        // checkbox(imageDelFl)는 unchecked이므로 포함되지 않아야 한다
        assertThat(fields).noneMatch(e -> "imageDB[imageDelFl][main0]".equals(e.getKey()));
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

        var fields = collector.collect(html, "1000001371").textFields();

        long count = fields.stream().filter(e -> "optionSno[]".equals(e.getKey())).count();
        assertThat(count).isEqualTo(2);
    }
}
