package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.common.exception.DuplicateGoodsException;
import com.sfood.automation.goods.application.dto.RegisterGoodsRequest;
import com.sfood.automation.goods.application.dto.RegisterGoodsListResponse;
import com.sfood.automation.goods.application.dto.RegisterGoodsResponse;
import com.sfood.automation.goods.domain.enums.GoodsScope;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.port.CommonStatusConfigRepository;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterGoodsUseCase {

    private final GoodsRepository goodsRepository;
    private final CommonStatusConfigRepository commonStatusConfigRepository;
    private final GodoApiPort godoApiPort;

    public RegisterGoodsUseCase(
        GoodsRepository goodsRepository,
        CommonStatusConfigRepository commonStatusConfigRepository,
        GodoApiPort godoApiPort
    ) {
        this.goodsRepository = goodsRepository;
        this.commonStatusConfigRepository = commonStatusConfigRepository;
        this.godoApiPort = godoApiPort;
    }

    @Transactional
    public RegisterGoodsListResponse execute(List<RegisterGoodsRequest> requests, String sessionEmail) {
        return new RegisterGoodsListResponse(
            requests.stream()
                .map(request -> register(request, sessionEmail))
                .toList()
        );
    }

    private RegisterGoodsResponse register(RegisterGoodsRequest request, String sessionEmail) {
        var existingByGodo = goodsRepository.findByGodoCode(request.godoGoodsCode());
        if (existingByGodo.isPresent()) {
            Goods goods = existingByGodo.get();
            if (goods.getStatus() == com.sfood.automation.goods.domain.enums.GoodsStatus.HIDE) {
                goods.restore();
                var godoGoodsInfo = godoApiPort.searchGoods(request.godoGoodsCode());
                applyGodoSearchResult(goods, request.godoGoodsCode(), godoGoodsInfo);
                Goods restored = goodsRepository.save(goods);
                return new RegisterGoodsResponse(restored.getId(), restored.getGodoGoodsCode(), restored.getErpGoodsCode(), restored.getStatus().name(), restored.getMessage());
            }
            throw new DuplicateGoodsException();
        }

        Goods goods = Goods.create(request.godoGoodsCode(), request.erpGoodsCode(), request.goodsName(), request.onlyMallYn(), sessionEmail);
        var godoGoodsInfo = godoApiPort.searchGoods(request.godoGoodsCode());
        applyGodoSearchResult(goods, request.godoGoodsCode(), godoGoodsInfo);
        var scope = "Y".equals(request.onlyMallYn()) ? GoodsScope.EXCLUSIVE : GoodsScope.GENERAL;
        var config = commonStatusConfigRepository.findByScope(scope).orElseThrow();
        goods.initializeCriteria(config.outOfStockQty(), config.safetyRate(), config.remainValidDays());
        Goods saved = goodsRepository.save(goods);
        return new RegisterGoodsResponse(saved.getId(), saved.getGodoGoodsCode(), saved.getErpGoodsCode(), saved.getStatus().name(), saved.getMessage());
    }

    private void applyGodoSearchResult(Goods goods, String requestedGodoGoodsCode, com.sfood.automation.goods.domain.model.GodoGoodsInfo godoGoodsInfo) {
        if (godoGoodsInfo.matches(requestedGodoGoodsCode)) {
            goods.applyGodoSyncResult(godoGoodsInfo.soldOutYn());
            return;
        }
        goods.clearGodoSyncResult();
    }
}
