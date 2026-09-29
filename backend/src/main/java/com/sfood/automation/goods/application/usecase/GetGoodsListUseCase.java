package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.goods.application.dto.GoodsListResponse;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import org.springframework.stereotype.Service;

@Service
public class GetGoodsListUseCase {

    private final GoodsRepository goodsRepository;

    public GetGoodsListUseCase(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    public GoodsListResponse execute(String soldOutYn, String onlyMallYn, String status, String keyword) {
        return new GoodsListResponse(
            goodsRepository.findAll(soldOutYn, onlyMallYn, status, keyword).stream()
                .map(GoodsMapper::toListItemResponse)
                .toList()
        );
    }
}
