package com.sfood.automation.member.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SfoodAdminMemberListCollectorTest {

    private final SfoodAdminMemberListCollector collector = new SfoodAdminMemberListCollector();

    private String loadFixture(String filename) throws IOException {
        return new ClassPathResource("fixtures/" + filename)
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void should_collect_all_memNos_from_real_html() throws IOException {
        String html = loadFixture("member_list_page.html");

        List<Long> memNos = collector.collectMemNos(html);

        assertThat(memNos).containsExactlyInAnyOrder(10001L, 10002L);
    }

    @Test
    void should_return_empty_when_no_data_rows() {
        String emptyHtml = """
            <table class="table table-rows">
              <tbody>
                <tr><td class="center" colspan="16">검색된 정보가 없습니다.</td></tr>
              </tbody>
            </table>
            """;

        List<Long> memNos = collector.collectMemNos(emptyHtml);

        assertThat(memNos).isEmpty();
    }

    @Test
    void should_detect_last_page_when_no_data_rows() {
        String emptyHtml = """
            <table class="table table-rows">
              <tbody>
                <tr><td class="center" colspan="16">검색된 정보가 없습니다.</td></tr>
              </tbody>
            </table>
            """;

        assertThat(collector.isLastPage(emptyHtml)).isTrue();
    }

    @Test
    void should_not_be_last_page_when_rows_exist() throws IOException {
        String html = loadFixture("member_list_page.html");

        assertThat(collector.isLastPage(html)).isFalse();
    }

    @Test
    void should_skip_rows_without_data_member_no() {
        String html = """
            <table class="table table-rows">
              <tbody>
                <tr class="center" data-member-no="99999">
                  <td><input type="checkbox" name="chk[]" value="99999"></td>
                </tr>
                <tr class="center">
                  <td>no-attr-row</td>
                </tr>
              </tbody>
            </table>
            """;

        List<Long> memNos = collector.collectMemNos(html);

        assertThat(memNos).containsExactly(99999L);
    }
}
