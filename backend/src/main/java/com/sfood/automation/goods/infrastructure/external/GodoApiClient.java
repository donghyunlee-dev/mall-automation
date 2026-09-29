package com.sfood.automation.goods.infrastructure.external;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.model.GodoGoodsInfo;
import com.sfood.automation.goods.domain.port.ApiTransactionLogRepository;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@EnableConfigurationProperties(GodoApiProperties.class)
public class GodoApiClient implements GodoApiPort {

    private final GoodsSearchApiCaller goodsSearchApiCaller;
    private final SoldoutStatusApiCaller soldoutStatusApiCaller;
    private final GodoApiProperties properties;

    public GodoApiClient(
        GodoTransport godoTransport,
        ApiTransactionLogRepository apiTransactionLogRepository,
        GodoApiProperties properties
    ) {
        XmlMapper xmlMapper = new XmlMapper();
        this.goodsSearchApiCaller = new GoodsSearchApiCaller(godoTransport, xmlMapper, apiTransactionLogRepository);
        this.soldoutStatusApiCaller = new SoldoutStatusApiCaller(godoTransport, xmlMapper, apiTransactionLogRepository, duration -> {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        });
        this.properties = properties;
    }

    @Override
    public GodoGoodsInfo searchGoods(String godoGoodsCode) {
        String url = goodsBaseUrl() + "/Goods_Search.php";
        String requestBody = form("goodsNo", godoGoodsCode);
        return goodsSearchApiCaller.call(url, requestBody, godoGoodsCode);
    }

    @Override
    public GodoApiResult updateSoldOutStatus(String godoGoodsCode, String soldOutFl) {
        String url = goodsBaseUrl() + "/Goods_Soldout_Status.php";
        String requestBody = form("goodsNo", godoGoodsCode) + "&soldoutFl=" + soldOutFl;
        return soldoutStatusApiCaller.call(url, requestBody, godoGoodsCode);
    }

    private String goodsBaseUrl() {
        String configured = properties.getBaseUrl();
        if (configured.endsWith("/godomall5/goods")) {
            return configured;
        }
        if (configured.endsWith("/")) {
            return configured + "godomall5/goods";
        }
        return configured + "/godomall5/goods";
    }

    private String form(String key, String value) {
        return "partner_key=" + properties.getPartnerKey()
            + "&key=" + properties.getKey()
            + "&" + key + "=" + value;
    }
}
