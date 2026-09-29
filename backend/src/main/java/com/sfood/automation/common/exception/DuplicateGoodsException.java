package com.sfood.automation.common.exception;

public class DuplicateGoodsException extends BusinessException {

    public DuplicateGoodsException() {
        super(ErrorCode.DUPLICATE_GOODS);
    }

    public DuplicateGoodsException(String message) {
        super(ErrorCode.DUPLICATE_GOODS, message);
    }
}
