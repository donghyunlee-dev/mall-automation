package com.sfood.automation.common.exception;

public class GoodsSyncBlockValidationException extends BusinessException {

    public GoodsSyncBlockValidationException(String message) {
        super(ErrorCode.GOODS_SYNC_BLOCK_VALIDATION_ERROR, message);
    }
}
