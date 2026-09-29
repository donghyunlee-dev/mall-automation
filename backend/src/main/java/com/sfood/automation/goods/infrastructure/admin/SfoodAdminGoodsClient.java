package com.sfood.automation.goods.infrastructure.admin;

import com.sfood.automation.common.exception.SfoodAdminException;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(SfoodAdminProperties.class)
public class SfoodAdminGoodsClient implements SfoodAdminGoodsPort {

    private static final Logger log = LoggerFactory.getLogger(SfoodAdminGoodsClient.class);
    private static final String SAVE_SUCCESS_MARKER = "저장이 완료되었습니다.";

    private final SfoodAdminSessionManager sessionManager;
    private final SfoodAdminFormCollector formCollector;
    private final String baseUrl;
    private final String goodsSavePath;

    @Autowired
    public SfoodAdminGoodsClient(
        SfoodAdminSessionManager sessionManager,
        SfoodAdminFormCollector formCollector,
        SfoodAdminProperties properties
    ) {
        this.sessionManager = sessionManager;
        this.formCollector = formCollector;
        this.baseUrl = properties.getBaseUrl();
        this.goodsSavePath = properties.getGoodsSavePath();
    }

    @Override
    public GodoApiResult restoreToOnSale(String godoGoodsCode) {
        log.info("SfoodAdmin restoreToOnSale started: godoGoodsCode={}", godoGoodsCode);

        String formHtml = sessionManager.fetchGoodsFormPage(godoGoodsCode);
        SfoodAdminFormCollector.CollectedFormData formData = formCollector.collect(formHtml, godoGoodsCode);
        List<Map.Entry<String, String>> submittableData = replaceTargetFields(formData.textFields());

        String saveUrl = baseUrl + goodsSavePath;
        SfoodAdminSessionManager.RawResponse saveResponse =
            sessionManager.postMultipart(saveUrl, submittableData, formData.emptyFileFieldNames());

        log.info("SfoodAdmin save response status={}", saveResponse.statusCode());

        if (!saveResponse.body().contains(SAVE_SUCCESS_MARKER)) {
            log.warn("SfoodAdmin save failed: godoGoodsCode={} responseSnippet={}", godoGoodsCode, snippet(saveResponse.body()));
            throw new SfoodAdminException("저장 실패: godoGoodsCode=" + godoGoodsCode);
        }

        verifySoldOutRestored(godoGoodsCode);

        log.info("SfoodAdmin restoreToOnSale succeeded: godoGoodsCode={}", godoGoodsCode);
        return new GodoApiResult(true, "SUCCESS", "정상 전환 완료");
    }

    private List<Map.Entry<String, String>> replaceTargetFields(List<Map.Entry<String, String>> formData) {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : formData) {
            if ("soldOutFl".equals(entry.getKey()) || "modDtUse".equals(entry.getKey())) {
                continue;
            }
            result.add(entry);
        }
        result.add(Map.entry("soldOutFl", "n"));
        result.add(Map.entry("modDtUse", "y"));
        return result;
    }

    private void verifySoldOutRestored(String godoGoodsCode) {
        String verifyHtml = sessionManager.fetchGoodsFormPage(godoGoodsCode);
        List<Map.Entry<String, String>> verifyData = formCollector.collect(verifyHtml, godoGoodsCode).textFields();
        String soldOutFl = verifyData.stream()
            .filter(e -> "soldOutFl".equals(e.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
        if (!"n".equals(soldOutFl)) {
            log.error("SfoodAdmin verify failed: godoGoodsCode={} soldOutFl={}", godoGoodsCode, soldOutFl);
            throw new SfoodAdminException("검증 실패: godoGoodsCode=" + godoGoodsCode + " soldOutFl=" + soldOutFl);
        }
    }

    private String snippet(String body) {
        return body == null ? "" : body.substring(0, Math.min(200, body.length()));
    }
}
