package com.sfood.automation.common.exception;

public class OtpMailDeliveryException extends BusinessException {

    public OtpMailDeliveryException() {
        super(ErrorCode.OTP_MAIL_DELIVERY_FAILED);
    }
}
