package com.sfood.automation.dashboard.infrastructure.external;

public class GodoOrderResponseParseException extends RuntimeException {

    public GodoOrderResponseParseException(Throwable cause) {
        super("고도몰 주문조회 응답 파싱에 실패했습니다.", cause);
    }
}
