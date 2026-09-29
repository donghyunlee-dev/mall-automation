package com.sfood.automation.common.exception;

public class MsAuthGatewayException extends BusinessException {

    public MsAuthGatewayException() {
        super(ErrorCode.MS_AUTH_GATEWAY_ERROR);
    }
}
