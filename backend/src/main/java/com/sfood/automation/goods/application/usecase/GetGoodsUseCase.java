package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.common.exception.GoodsNotFoundException;
import com.sfood.automation.goods.application.dto.GoodsResponse;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import org.springframework.stereotype.Service;

@Service
public class GetGoodsUseCase {

    private final GoodsRepository goodsRepository;

    public GetGoodsUseCase(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    public GoodsResponse execute(Integer id) {
        return GoodsMapper.toResponse(goodsRepository.findById(id).orElseThrow(GoodsNotFoundException::new));
    }
}
