package com.sfood.automation.member.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import com.sfood.automation.goods.infrastructure.admin.SfoodAdminProperties;
import com.sfood.automation.goods.infrastructure.admin.SfoodAdminSessionManager;
import com.sfood.automation.member.application.dto.CrawledMember;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SfoodAdminMemberClientTest {

    private SfoodAdminSessionManager sessionManager;
    private SfoodAdminMemberListCollector listCollector;
    private SfoodAdminMemberDetailCollector detailCollector;
    private SfoodAdminProperties properties;
    private SfoodAdminMemberClient client;

    @BeforeEach
    void setUp() {
        sessionManager = mock(SfoodAdminSessionManager.class);
        listCollector = mock(SfoodAdminMemberListCollector.class);
        detailCollector = mock(SfoodAdminMemberDetailCollector.class);
        properties = new SfoodAdminProperties();
        properties.setBaseUrl("https://gdadmin.sfoodmall.co.kr");
        properties.setMemberListPath("/member/member_list.php");
        properties.setMemberDetailPath("/member/member_modify.php");
        client = new SfoodAdminMemberClient(sessionManager, listCollector, detailCollector, properties);
    }

    @Test
    void should_collect_all_pages_until_last() {
        when(sessionManager.fetchMemberListPage(anyString())).thenReturn("<html>page</html>");
        when(listCollector.collectMemNos("<html>page</html>"))
            .thenReturn(List.of(900101L, 900102L))
            .thenReturn(List.of());
        when(listCollector.isLastPage("<html>page</html>"))
            .thenReturn(false)
            .thenReturn(true);

        List<Long> result = client.fetchMemberNos(LocalDate.of(2026, 6, 24));

        assertThat(result).containsExactlyInAnyOrder(900101L, 900102L);
        verify(sessionManager, times(2)).fetchMemberListPage(anyString());
    }

    @Test
    void should_use_unapproved_filter_in_list_url() {
        when(sessionManager.fetchMemberListPage(anyString())).thenReturn("<html>page</html>");
        when(listCollector.isLastPage(anyString())).thenReturn(true);

        client.fetchMemberNos(LocalDate.of(2026, 6, 26));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).fetchMemberListPage(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("appFl=n");
        assertThat(urlCaptor.getValue()).doesNotContain("appFl=y");
    }

    @Test
    void should_fetch_member_detail() {
        String detailHtml = "<html>detail</html>";
        when(sessionManager.fetchMemberDetailPage(anyString())).thenReturn(detailHtml);
        CrawledMember expected = mock(CrawledMember.class);
        when(detailCollector.collect(detailHtml, 900101L)).thenReturn(expected);

        CrawledMember result = client.fetchMemberDetail(900101L);

        assertThat(result).isSameAs(expected);
    }
}
