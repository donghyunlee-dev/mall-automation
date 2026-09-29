package com.sfood.automation.goods.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 실제 고도몰 상품 폼 HTML을 가져와서 SfoodAdminFormCollector가 모든 필드를 빠짐없이 수집하는지 검증한다.
 *
 * 검증 대상:
 * - 정적 form 필드 (input/select/textarea)
 * - JS script 태그에서 동적 삽입되는 필드 (imageDB[*] 등)
 * - goods_image() 호출로 생성되는 FILE / TEXT 필드
 *
 * 실행: LIVE_SFOOD_ADMIN=true ./gradlew test --tests "*FormCoverageTest" --rerun
 */
@SpringBootTest(properties = {"app.batch.scheduler.auto-start=false"})
class SfoodAdminFormCoverageTest {

    private static final Path REPORT_PATH = Path.of("/tmp/sfood_form_coverage_report.txt");
    private static final Path HTML_PATH    = Path.of("/tmp/sfood_form_raw.html");
    private static final Path DATABASE_PATH = Path.of("build/tmp/coverage-test.db");

    // 독립 ground-truth 추출용 패턴 (collector 코드와 별개로 작성)
    private static final Pattern GT_INPUT_TAG  = Pattern.compile("<input\\b[^>]*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern GT_ATTR_NAME  = Pattern.compile("\\bname=\"([^\"]+)\"");
    private static final Pattern GT_ATTR_TYPE  = Pattern.compile("\\btype=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern GT_CHECKED    = Pattern.compile("\\bchecked\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GT_DISABLED   = Pattern.compile("\\bdisabled\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GT_IMG_3ARG   = Pattern.compile(
        "goods_image\\('([^']+)',\\s*'[^']*',\\s*'([^']*)'\\s*\\)");
    private static final Pattern GT_IMG_2ARG   = Pattern.compile(
        "goods_image\\('([^']+)',\\s*'[^']*'\\s*\\)");

    private static final Set<String> SKIP_TYPES  = Set.of("submit", "button", "reset", "image");
    private static final Set<String> FB_FIELD_IDS = Set.of("imageFbGoods", "imageFbGoodsURL");

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry registry) throws Exception {
        Files.deleteIfExists(DATABASE_PATH);
        registry.add("app.database.path", () -> DATABASE_PATH.toString());
    }

    @Autowired private SfoodAdminSessionManager sessionManager;
    @Autowired private SfoodAdminFormCollector   formCollector;
    @Autowired private SfoodAdminProperties      properties;

    @Test
    void verify_all_form_fields_are_covered() throws IOException {
        assumeTrue(isLiveSfoodAdminEnabled(),
            "Set LIVE_SFOOD_ADMIN=true to run form coverage verification");
        assumeFalse(properties.getTestGoodsCode() == null || properties.getTestGoodsCode().isBlank(),
            "sfood.admin.test-goods-code 가 설정되어 있어야 합니다");

        String goodsCode = properties.getTestGoodsCode();

        // ── 실제 HTML 취득 & 저장 ─────────────────────────────────
        String html = sessionManager.fetchGoodsFormPage(goodsCode);
        Files.writeString(HTML_PATH, html);

        // ── Collector 결과 ────────────────────────────────────────
        SfoodAdminFormCollector.CollectedFormData collected = formCollector.collect(html, goodsCode);
        Set<String> colTextNames = collected.textFields().stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> colFileNames = new LinkedHashSet<>(collected.emptyFileFieldNames());

        // ── Ground-truth 추출 (collector 와 독립된 로직) ──────────
        Document doc  = Jsoup.parse(html);
        Element  form = doc.selectFirst("form#frmGoods");
        assertThat(form).as("frmGoods 폼이 존재해야 한다").isNotNull();

        StringBuilder report = new StringBuilder();
        report.append("=== SfoodAdmin Form Field Coverage Report ===\n")
              .append("goodsCode            : ").append(goodsCode).append('\n')
              .append("collector.textFields : ").append(collected.textFields().size()).append('\n')
              .append("collector.fileFields : ").append(collected.emptyFileFieldNames().size()).append("\n\n");

        List<String> missingText = new ArrayList<>();
        List<String> missingFile = new ArrayList<>();

        // ── [A] 정적 form 필드 ──────────────────────────────────
        report.append("─── [A] 정적 Form 필드 ───────────────────────────────\n");
        for (Element el : form.select("input, select, textarea")) {
            if (el.hasAttr("disabled")) continue;
            String name = el.attr("name");
            if (name.isBlank()) continue;

            String tag  = el.tagName();
            String type = el.attr("type").toLowerCase(Locale.ROOT);

            if (SKIP_TYPES.contains(type)) {
                report.append(fmt("SKIP:type", name, tag + "/" + type));
                continue;
            }
            if ("input".equals(tag) && "file".equals(type)) {
                // 정적 file input: goods_image() 경로로 처리되어야 함
                boolean covered = colFileNames.contains(name) || colTextNames.contains(name);
                if (!covered) {
                    missingFile.add(name + " [static-file]");
                    report.append(fmt("MISS-file", name, tag));
                } else {
                    report.append(fmt("OK-file", name, tag));
                }
                continue;
            }
            if ("input".equals(tag) && ("radio".equals(type) || "checkbox".equals(type))) {
                if (!el.hasAttr("checked")) {
                    report.append(fmt("SKIP:unchecked", name, type));
                    continue;
                }
            }
            if (!colTextNames.contains(name)) {
                missingText.add(name + " [static:" + tag + "/" + type + "]");
                report.append(fmt("MISS-text", name, tag + "/" + type));
            } else {
                report.append(fmt("OK", name, tag + "/" + type));
            }
        }

        // ── [B] JS script 태그에서 동적 삽입된 필드 ──────────────
        report.append("\n─── [B] JS 동적 삽입 필드 (script.data()) ────────────\n");
        // 중복 출력 방지: script 전체에서 한 번씩
        Map<String, String> dynamicSeen = new LinkedHashMap<>();
        for (Element script : form.select("script")) {
            String raw = script.data();
            if (raw.isBlank()) continue;
            String unescaped = raw.replace("\\\"", "\"");
            Matcher m = GT_INPUT_TAG.matcher(unescaped);
            while (m.find()) {
                String tag = m.group();
                if (GT_DISABLED.matcher(tag).find()) continue;
                Matcher nm = GT_ATTR_NAME.matcher(tag);
                if (!nm.find()) continue;
                String name = nm.group(1);
                if (dynamicSeen.containsKey(name)) continue; // 첫 번째 발견만
                Matcher tm = GT_ATTR_TYPE.matcher(tag);
                String type = tm.find() ? tm.group(1).toLowerCase(Locale.ROOT) : "hidden";
                dynamicSeen.put(name, type);

                if (SKIP_TYPES.contains(type)) {
                    report.append(fmt("SKIP:type", name, type));
                    continue;
                }
                if ("file".equals(type)) {
                    boolean covered = colFileNames.contains(name);
                    if (!covered) {
                        missingFile.add(name + " [dynamic-file]");
                        report.append(fmt("MISS-file", name, type));
                    } else {
                        report.append(fmt("OK-file", name, type));
                    }
                    continue;
                }
                if ("checkbox".equals(type) || "radio".equals(type)) {
                    if (!GT_CHECKED.matcher(tag).find()) {
                        report.append(fmt("SKIP:unchecked", name, type));
                        continue;
                    }
                }
                if (!colTextNames.contains(name)) {
                    missingText.add(name + " [dynamic:" + type + "]");
                    report.append(fmt("MISS-text", name, type));
                } else {
                    report.append(fmt("OK", name, type));
                }
            }
        }

        // ── [C] goods_image() 호출 기반 필드 ─────────────────────
        report.append("\n─── [C] goods_image() 필드 ───────────────────────────\n");
        Set<String> imgFileSeen = new LinkedHashSet<>();
        Set<String> imgTextSeen = new LinkedHashSet<>();
        for (Element script : form.select("script")) {
            String raw = script.data();
            if (!raw.contains("goods_image")) continue;
            // 3-arg 먼저 (urlType='y' → TEXT)
            Matcher m3 = GT_IMG_3ARG.matcher(raw);
            while (m3.find()) {
                String fieldId = m3.group(1);
                String urlType = m3.group(2);
                String fname   = fieldName(fieldId);
                if (!imgTextSeen.add(fname)) continue;
                if ("y".equals(urlType)) {
                    if (!colTextNames.contains(fname)) {
                        missingText.add(fname + " [goods_image-3arg-text]");
                        report.append(fmt("MISS-text", fname, "goods_image-3arg urlType=y"));
                    } else {
                        report.append(fmt("OK-text", fname, "goods_image-3arg urlType=y"));
                    }
                } else {
                    report.append(fmt("SKIP:urlType≠y", fname, "goods_image-3arg urlType=" + urlType));
                }
            }
            // 2-arg (→ FILE)
            Matcher m2 = GT_IMG_2ARG.matcher(raw);
            while (m2.find()) {
                String fieldId = m2.group(1);
                String fname   = fieldName(fieldId);
                if (!imgFileSeen.add(fname)) continue;
                if (!colFileNames.contains(fname)) {
                    missingFile.add(fname + " [goods_image-2arg-file]");
                    report.append(fmt("MISS-file", fname, "goods_image-2arg"));
                } else {
                    report.append(fmt("OK-file", fname, "goods_image-2arg"));
                }
            }
        }

        // ── 요약 ──────────────────────────────────────────────────
        report.append("\n═══ 요약 ═══════════════════════════════════════════════\n");
        report.append("누락 text 필드 : ").append(missingText.size()).append('\n');
        missingText.forEach(s -> report.append("  ⚠ MISSING-TEXT: ").append(s).append('\n'));
        report.append("누락 file 필드 : ").append(missingFile.size()).append('\n');
        missingFile.forEach(s -> report.append("  ⚠ MISSING-FILE: ").append(s).append('\n'));
        boolean pass = missingText.isEmpty() && missingFile.isEmpty();
        report.append('\n').append(pass
            ? "✅ PASS — 모든 필드 정상 수집 (누락 없음)"
            : "❌ FAIL — 누락 필드 존재").append('\n');

        Files.writeString(REPORT_PATH, report.toString());
        System.out.println(report);

        assertThat(missingText).as("누락된 text 필드 목록").isEmpty();
        assertThat(missingFile).as("누락된 file 필드 목록").isEmpty();
    }

    private static String fmt(String status, String name, String detail) {
        return "  [%-18s] %-55s (%s)\n".formatted(status, name, detail);
    }

    private static String fieldName(String fieldId) {
        return (FB_FIELD_IDS.contains(fieldId) ? "imageFb" : "image") + "[" + fieldId + "][]";
    }

    private boolean isLiveSfoodAdminEnabled() {
        return Boolean.getBoolean("liveSfoodAdmin")
            || Boolean.parseBoolean(System.getenv("LIVE_SFOOD_ADMIN"));
    }
}
