package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.common.exception.GoodsNotFoundException;
import com.sfood.automation.goods.application.dto.GoodsResponse;
import com.sfood.automation.goods.application.dto.UpdateGoodsRequest;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateGoodsUseCase {

    private final GoodsRepository goodsRepository;

    public UpdateGoodsUseCase(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    @Transactional
    public GoodsResponse execute(Integer id, UpdateGoodsRequest request, String sessionEmail) {
        var goods = goodsRepository.findById(id).orElseThrow(GoodsNotFoundException::new);
        goods.update(
            request.goodsName(),
            request.erpGoodsCode(),
            request.onlyMallYn(),
            request.availableQty(),
            request.safetyRate(),
            request.remainValidDays(),
            sessionEmail
        );
        var saved = goodsRepository.save(goods);
        return GoodsMapper.toResponse(saved);
    }
}
