package com.sfood.automation.common.exception;

public class GoodsNotFoundException extends BusinessException {

    public GoodsNotFoundException() {
        super(ErrorCode.GOODS_NOT_FOUND);
    }
}
