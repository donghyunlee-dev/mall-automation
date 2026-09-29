package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.common.exception.GoodsNotFoundException;
import com.sfood.automation.common.exception.GoodsSyncBlockValidationException;
import com.sfood.automation.goods.application.dto.GoodsResponse;
import com.sfood.automation.goods.application.dto.UpdateGodoSyncBlockRequest;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateGodoSyncBlockUseCase {

    private final GoodsRepository goodsRepository;

    public UpdateGodoSyncBlockUseCase(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    @Transactional
    public GoodsResponse execute(Integer id, UpdateGodoSyncBlockRequest request, String sessionEmail) {
        var goods = goodsRepository.findById(id).orElseThrow(GoodsNotFoundException::new);
        if ("Y".equals(request.blockedYn()) && !goods.isNormalStatus()) {
            throw new GoodsSyncBlockValidationException("Godo sync block can only be enabled for NORMAL goods");
        }

        if ("Y".equals(request.blockedYn())) {
            goods.enableGodoSyncBlock(sessionEmail);
        } else {
            goods.disableGodoSyncBlock(sessionEmail);
        }

        return GoodsMapper.toResponse(goodsRepository.save(goods));
    }
}
