package com.sfood.automation.member.infrastructure.admin;

import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class SfoodAdminMemberListCollector {

    private static final String ROW_SELECTOR = "table.table-rows tbody tr[data-member-no]";

    public List<Long> collectMemNos(String html) {
        Document doc = Jsoup.parse(html);
        Elements rows = doc.select(ROW_SELECTOR);
        return rows.stream()
            .map(this::parseMemNo)
            .filter(memNo -> memNo != null)
            .toList();
    }

    public boolean isLastPage(String html) {
        Document doc = Jsoup.parse(html);
        return doc.select(ROW_SELECTOR).isEmpty();
    }

    private Long parseMemNo(Element row) {
        String attr = row.attr("data-member-no");
        if (attr == null || attr.isBlank()) return null;
        try {
            return Long.parseLong(attr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
