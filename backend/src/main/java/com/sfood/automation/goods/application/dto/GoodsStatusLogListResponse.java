package com.sfood.automation.goods.application.dto;

import java.util.List;

public record GoodsStatusLogListResponse(
    List<GoodsStatusLogResponse> items
) {
}
