package com.sfood.automation.goods.infrastructure.admin;

import com.sfood.automation.common.exception.SfoodAdminException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SfoodAdminFormCollector {

    private static final Logger log = LoggerFactory.getLogger(SfoodAdminFormCollector.class);

    // <input> 태그 전체를 매칭하는 패턴 (단일 태그, self-closing 포함)
    private static final Pattern INPUT_TAG = Pattern.compile(
        "<input\\b[^>]*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR_NAME = Pattern.compile("\\bname=\"([^\"]+)\"");
    private static final Pattern ATTR_VALUE = Pattern.compile("\\bvalue=\"([^\"]*)\"");

    // goods_image('fieldID', 'addBtnYN') — 2인자: urlType 없음 → FILE input 생성
    private static final Pattern GOODS_IMAGE_2ARG = Pattern.compile(
        "goods_image\\('([^']+)',\\s*'([^']*)'\\s*\\)");
    // goods_image('fieldID', 'addBtnYN', 'urlType') — 3인자: urlType='y' → TEXT input 생성
    private static final Pattern GOODS_IMAGE_3ARG = Pattern.compile(
        "goods_image\\('([^']+)',\\s*'([^']*)',\\s*'([^']*)'\\s*\\)");

    // imageFb 계열 fieldID
    private static final Set<String> FB_FIELD_IDS = Set.of("imageFbGoods", "imageFbGoodsURL");

    /**
     * 폼 수집 결과. textFields는 일반 name=value 쌍, emptyFileFieldNames는 빈 파일 파트로 전송할 필드명.
     */
    public record CollectedFormData(
        List<Map.Entry<String, String>> textFields,
        List<String> emptyFileFieldNames
    ) {}

    public CollectedFormData collect(String html, String expectedGoodsNo) {
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

    private CollectedFormData collectFormFields(Element form) {
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
        fields.addAll(collectJsInjectedImageDbFields(form));

        GoodsImageFields goodsImageFields = collectGoodsImageFields(form);
        fields.addAll(goodsImageFields.textFields());
        log.info("FormCollector: 수집 완료 — textFields={}, emptyFileFields={}",
            fields.size(), goodsImageFields.fileFieldNames().size());

        return new CollectedFormData(fields, goodsImageFields.fileFieldNames());
    }

    /**
     * 고도몰 관리자는 imageDB[*] hidden 필드를 JavaScript .html() 호출로 동적 삽입한다.
     * Jsoup 정적 파싱으로는 수집되지 않으므로 script 텍스트에서 직접 추출한다.
     * 이 필드가 누락되면 서버가 기존 이미지 전체를 삭제한다.
     *
     * 주의: 고도몰 script 태그는 <!--...-- > 주석 래퍼를 사용하므로 Jsoup.parseBodyFragment 대신
     * 정규식으로 직접 추출한다. 주석 래퍼를 사용하면 Jsoup이 내용을 HTML 주석으로 처리해 input을 무시한다.
     */
    private List<Map.Entry<String, String>> collectJsInjectedImageDbFields(Element form) {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        Elements scripts = form.select("script");
        log.info("FormCollector: frmGoods 내 script 태그 수={}", scripts.size());
        for (Element script : scripts) {
            String raw = script.data();
            if (!raw.contains("imageDB")) {
                continue;
            }
            log.info("FormCollector: imageDB 포함 script 발견, data 길이={}", raw.length());
            String unescaped = raw.replace("\\\"", "\"");
            Matcher inputMatcher = INPUT_TAG.matcher(unescaped);
            while (inputMatcher.find()) {
                String tag = inputMatcher.group();
                if (!tag.contains("type=\"hidden\"") || !tag.contains("imageDB")) {
                    continue;
                }
                Matcher nameMatcher = ATTR_NAME.matcher(tag);
                Matcher valueMatcher = ATTR_VALUE.matcher(tag);
                if (nameMatcher.find() && nameMatcher.group(1).contains("imageDB")) {
                    String value = valueMatcher.find() ? valueMatcher.group(1) : "";
                    result.add(entry(nameMatcher.group(1), value));
                }
            }
        }
        log.info("FormCollector: 추출된 imageDB 필드 수={}", result.size());
        return result;
    }

    /**
     * goods_image() JS 함수 호출을 파싱해 이미지 입력 필드를 수집한다.
     *
     * 2인자 goods_image('fieldID', 'addBtnYN') → urlType 없음(undefined) → FILE input 생성
     *   → 브라우저는 빈 파일 파트(filename="", UPLOAD_ERR_NO_FILE)로 전송
     *   → Godo는 이를 "신규 업로드 없음, 기존 이미지 유지"로 해석
     *
     * 3인자 goods_image('fieldID', 'addBtnYN', 'urlType') with urlType='y' → TEXT input 생성
     *   → 브라우저는 빈 문자열 값으로 전송
     */
    private GoodsImageFields collectGoodsImageFields(Element form) {
        List<Map.Entry<String, String>> textFields = new ArrayList<>();
        List<String> fileFieldNames = new ArrayList<>();

        for (Element script : form.select("script")) {
            String raw = script.data();
            if (!raw.contains("goods_image")) {
                continue;
            }
            // 3인자 먼저 처리 (2인자 패턴과 겹치지 않도록)
            Matcher m3 = GOODS_IMAGE_3ARG.matcher(raw);
            while (m3.find()) {
                String fieldId = m3.group(1);
                String urlType = m3.group(3);
                if ("y".equals(urlType)) {
                    textFields.add(entry(fieldName(fieldId), ""));
                }
            }
            // 2인자 (3인자 위치는 이미 3인자 패턴으로 처리됨)
            Matcher m2 = GOODS_IMAGE_2ARG.matcher(raw);
            while (m2.find()) {
                // 3인자 호출의 일부인지 확인: 매칭 끝 이후 문자가 ','이면 3인자
                int end = m2.end();
                if (end < raw.length() && raw.charAt(end - 1) != ')') {
                    continue;
                }
                String fieldId = m2.group(1);
                fileFieldNames.add(fieldName(fieldId));
            }
        }
        log.info("FormCollector: goods_image TEXT={}, FILE={}", textFields.size(), fileFieldNames.size());
        return new GoodsImageFields(textFields, fileFieldNames);
    }

    private String fieldName(String fieldId) {
        String prefix = FB_FIELD_IDS.contains(fieldId) ? "imageFb" : "image";
        return prefix + "[" + fieldId + "][]";
    }

    private record GoodsImageFields(
        List<Map.Entry<String, String>> textFields,
        List<String> fileFieldNames
    ) {}

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
