package com.sfood.automation.goods.application.dto;

import java.util.List;

public record RegisterGoodsListResponse(
    List<RegisterGoodsResponse> items
) {
}
