package com.sfood.automation.member.infrastructure.admin;

import com.sfood.automation.member.application.dto.CrawledMember;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class SfoodAdminMemberDetailCollector {

    public CrawledMember collect(String html, Long memNo) {
        Document doc = Jsoup.parse(html);

        String emailAddr = inputVal(doc, "#emailAddress");
        String emailDomain = inputVal(doc, "#emailDomain");
        String email = (emailAddr != null && emailDomain != null)
            ? emailAddr + "@" + emailDomain : null;

        String address = prefixZoneCode(inputVal(doc, "input[name=zonecode]"),
            combine(inputVal(doc, "input[name=address]"), inputVal(doc, "input[name=addressSub]")));
        String comAddress = prefixZoneCode(inputVal(doc, "input[name=comZonecode]"),
            combine(inputVal(doc, "input[name=comAddress]"), inputVal(doc, "input[name=comAddressSub]")));

        Elements interestChecked = doc.select("input[name=\"interest[]\"][checked]");
        String interest = interestChecked.isEmpty() ? null
            : interestChecked.stream().map(Element::val).collect(Collectors.joining(","));

        return new CrawledMember(
            memNo,
            inputVal(doc, "input[name=memId]"),
            intVal(selectVal(doc, "select[name=groupSno]")),
            radioVal(doc, "memberFl"),
            radioVal(doc, "appFl"),
            null,
            tdAfterTh(doc, "회원가입일"),
            blankToNull(tdAfterTh(doc, "가입경로")),
            tdAfterTh(doc, "최종로그인일"),
            inputVal(doc, "input[name=memNm]"),
            inputVal(doc, "input[name=nickNm]"),
            email,
            inputVal(doc, "input[name=cellPhone]"),
            inputVal(doc, "input[name=phone]"),
            address,
            radioVal(doc, "sexFl"),
            inputVal(doc, "input[name=birthDt]"),
            radioVal(doc, "marriFl"),
            zeroDateToNull(inputVal(doc, "input[name=marriDate]")),
            checkboxFl(doc, "maillingFl"),
            checkboxFl(doc, "smsFl"),
            selectVal(doc, "select[name=job]"),
            interest,
            blankToNull(inputVal(doc, "input[name=recommId]")),
            parseAmount(tdAfterTh(doc, "구매금액")),
            parseAmount(tdAfterTh(doc, "마일리지")),
            parseAmount(tdAfterTh(doc, "예치금")),
            parseCount(tdAfterTh(doc, "방문횟수")),
            blankToNull(inputVal(doc, "input[name=fax]")),
            intVal(radioVal(doc, "expirationFl")),
            blankToNull(textareaVal(doc, "memo")),
            radioVal(doc, "ex1"),
            radioVal(doc, "ex2"),
            blankToNull(inputVal(doc, "input[name=ex3]")),
            blankToNull(inputVal(doc, "input[name=company]")),
            blankToNull(inputVal(doc, "input[name=fullBusiNo]")),
            blankToNull(inputVal(doc, "input[name=ceo]")),
            blankToNull(inputVal(doc, "input[name=service]")),
            blankToNull(inputVal(doc, "input[name=item]")),
            comAddress
        );
    }

    private String inputVal(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        if (el == null) return null;
        String v = el.val().trim();
        return v.isEmpty() ? null : v;
    }

    private String selectVal(Document doc, String selector) {
        Element el = doc.selectFirst(selector + " option[selected]");
        if (el == null) {
            // fallback: select element itself
            Element select = doc.selectFirst(selector);
            if (select == null) return null;
            String v = select.val().trim();
            return v.isEmpty() ? null : v;
        }
        String v = el.val().trim();
        return v.isEmpty() ? null : v;
    }

    private String textareaVal(Document doc, String name) {
        Element el = doc.selectFirst("textarea[name=" + name + "]");
        if (el == null) return null;
        String v = el.text().trim();
        return v.isEmpty() ? null : v;
    }

    private String radioVal(Document doc, String name) {
        Element el = doc.selectFirst("input[name=" + name + "][checked]");
        return el != null ? el.val() : null;
    }

    private String checkboxFl(Document doc, String name) {
        Element el = doc.selectFirst("input[name=" + name + "]");
        return (el != null && el.hasAttr("checked")) ? "y" : "n";
    }

    private String tdAfterTh(Document doc, String thText) {
        return doc.select("th").stream()
            .filter(th -> th.text().trim().equals(thText))
            .findFirst()
            .map(th -> {
                Element td = th.nextElementSibling();
                if (td == null || !td.tagName().equals("td")) return null;
                String text = td.text().trim();
                return text.isEmpty() ? null : text;
            })
            .orElse(null);
    }

    private Double parseAmount(String text) {
        if (text == null || text.isBlank()) return null;
        String digits = text.replaceAll("[^0-9.]", "");
        if (digits.isBlank()) return null;
        try { return Double.parseDouble(digits); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseCount(String text) {
        if (text == null || text.isBlank()) return null;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try { return Integer.parseInt(digits); } catch (NumberFormatException e) { return null; }
    }

    private Integer intVal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return null; }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String zeroDateToNull(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.matches("[0\\-: ]+")) return null;
        return value;
    }

    private String combine(String part1, String part2) {
        if (part1 == null || part1.isBlank()) return null;
        if (part2 == null || part2.isBlank()) return part1;
        return part1 + " " + part2;
    }

    private String prefixZoneCode(String zoneCode, String address) {
        if (address == null) return null;
        if (zoneCode == null || zoneCode.isBlank()) return address;
        return zoneCode + " " + address;
    }
}
