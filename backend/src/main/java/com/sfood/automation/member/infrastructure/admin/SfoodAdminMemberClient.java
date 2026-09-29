package com.sfood.automation.member.infrastructure.admin;

import com.sfood.automation.goods.infrastructure.admin.SfoodAdminProperties;
import com.sfood.automation.goods.infrastructure.admin.SfoodAdminSessionManager;
import com.sfood.automation.member.application.dto.CrawledMember;
import com.sfood.automation.member.domain.port.SfoodAdminMemberPort;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SfoodAdminMemberClient implements SfoodAdminMemberPort {

    private static final Logger log = LoggerFactory.getLogger(SfoodAdminMemberClient.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 500;

    private final SfoodAdminSessionManager sessionManager;
    private final SfoodAdminMemberListCollector listCollector;
    private final SfoodAdminMemberDetailCollector detailCollector;
    private final SfoodAdminProperties properties;

    public SfoodAdminMemberClient(
        SfoodAdminSessionManager sessionManager,
        SfoodAdminMemberListCollector listCollector,
        SfoodAdminMemberDetailCollector detailCollector,
        SfoodAdminProperties properties
    ) {
        this.sessionManager = sessionManager;
        this.listCollector = listCollector;
        this.detailCollector = detailCollector;
        this.properties = properties;
    }

    @Override
    public List<Long> fetchMemberNos(LocalDate date) {
        String dateStr = date.format(DATE_FMT);
        java.util.LinkedHashSet<Long> seen = new java.util.LinkedHashSet<>();
        int page = 1;
        while (true) {
            if (page > MAX_PAGES) {
                log.warn("fetchMemberNos: MAX_PAGES({}) 초과, 강제 종료: date={}", MAX_PAGES, dateStr);
                break;
            }
            String url = buildListUrl(dateStr, page);
            log.info("Fetching member list: page={}, url={}", page, url);
            String html = sessionManager.fetchMemberListPage(url);
            if (listCollector.isLastPage(html)) break;
            List<Long> pageMemNos = listCollector.collectMemNos(html);
            int sizeBefore = seen.size();
            seen.addAll(pageMemNos);
            if (seen.size() == sizeBefore) {
                log.info("fetchMemberNos: 중복 페이지 감지(마지막 페이지 반복), 종료: page={}", page);
                break;
            }
            page++;
        }
        List<Long> result = new ArrayList<>(seen);
        log.info("fetchMemberNos: total={}, date={}", result.size(), dateStr);
        return result;
    }

    @Override
    public CrawledMember fetchMemberDetail(Long memNo) {
        String url = properties.getBaseUrl() + properties.getMemberDetailPath() + "?memNo=" + memNo;
        log.debug("Fetching member detail: memNo={}", memNo);
        String html = sessionManager.fetchMemberDetailPage(url);
        return detailCollector.collect(html, memNo);
    }

    private String buildListUrl(String date, int page) {
        String encodedDate = URLEncoder.encode(date, StandardCharsets.UTF_8);
        // !! PLACEHOLDER: 실제 페이지네이션 파라미터명 브라우저 확인 후 교체 !!
        return properties.getBaseUrl() + properties.getMemberListPath()
            + "?searchFl=y"
            + "&appFl=n"
            + "&entryDt%5B%5D=" + encodedDate
            + "&entryDt%5B%5D=" + encodedDate
            + "&pageNum=" + PAGE_SIZE
            + "&nowPage=" + page;
    }
}
