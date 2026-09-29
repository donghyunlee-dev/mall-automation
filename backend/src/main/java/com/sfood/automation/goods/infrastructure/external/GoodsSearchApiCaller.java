package com.sfood.automation.goods.infrastructure.external;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.goods.domain.model.ApiTransactionLog;
import com.sfood.automation.goods.domain.model.GodoGoodsInfo;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import java.time.Instant;

public class GoodsSearchApiCaller {

    private final GodoTransport godoTransport;
    private final XmlMapper xmlMapper;
    private final ApiTransactionLogRepository apiTransactionLogRepository;

    public GoodsSearchApiCaller(
        GodoTransport godoTransport,
        XmlMapper xmlMapper,
        ApiTransactionLogRepository apiTransactionLogRepository
    ) {
        this.godoTransport = godoTransport;
        this.xmlMapper = xmlMapper;
        this.apiTransactionLogRepository = apiTransactionLogRepository;
    }

    public GodoGoodsInfo call(String url, String requestBody, String godoGoodsCode) {
        ApiTransactionLog saved = apiTransactionLogRepository.save(
            ApiTransactionLog.pending("GOODS_SEARCH", godoGoodsCode, url, requestBody, Instant.now())
        );
        GodoTransportResponse response = godoTransport.postForm(url, requestBody);
        GodoParsedResponse parsed = parse(response.body());
        boolean isSuccess = "000".equals(parsed.code());
        apiTransactionLogRepository.updateResponse(
            saved.id(),
            response.body(),
            parsed.code(),
            parsed.message(),
            isSuccess,
            Instant.now()
        );
        if (!isSuccess) {
            throw new GodoApiException();
        }
        return new GodoGoodsInfo(parsed.goodsNo(), parsed.goodsName(), normalizeSoldOut(parsed.soldOutFl()));
    }

    private GodoParsedResponse parse(String responseBody) {
        try {
            var root = xmlMapper.readTree(responseBody);
            String message = firstText(root, "message");
            if (message.isEmpty()) {
                message = firstText(root, "msg");
            }
            return new GodoParsedResponse(
                firstText(root, "code"),
                message,
                firstText(root, "goodsNo"),
                firstText(root, "goodsNm"),
                firstText(root, "soldOutFl")
            );
        } catch (Exception exception) {
            throw new GodoApiException();
        }
    }

    private String firstText(com.fasterxml.jackson.databind.JsonNode root, String fieldName) {
        return root.findPath(fieldName).asText("");
    }

    private String normalizeSoldOut(String soldOutFl) {
        return "y".equalsIgnoreCase(soldOutFl) ? "Y" : "N";
    }
}
