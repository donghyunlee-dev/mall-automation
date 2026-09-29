package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.common.exception.GoodsNotFoundException;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteGoodsUseCase {

    private final GoodsRepository goodsRepository;

    public DeleteGoodsUseCase(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    @Transactional
    public void execute(Integer id) {
        var goods = goodsRepository.findById(id).orElseThrow(GoodsNotFoundException::new);
        if ("Y".equals(goods.getGodoSyncYn())) {
            goods.hide();
            goodsRepository.save(goods);
            return;
        }
        goodsRepository.delete(goods);
    }
}
