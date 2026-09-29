package com.sfood.automation.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EMAIL_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "Invalid email format"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),
    OTP_EXPIRED(HttpStatus.GONE, "OTP expired"),
    OTP_MISMATCH(HttpStatus.UNAUTHORIZED, "OTP mismatch"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    OTP_ALREADY_USED(HttpStatus.CONFLICT, "OTP already used"),
    OTP_MAIL_DELIVERY_FAILED(HttpStatus.BAD_GATEWAY, "OTP mail delivery failed"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    GOODS_NOT_FOUND(HttpStatus.NOT_FOUND, "Goods not found"),
    DUPLICATE_GOODS(HttpStatus.CONFLICT, "Duplicate goods"),
    GOODS_SYNC_BLOCK_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid goods sync block configuration"),
    GODO_API_ERROR(HttpStatus.BAD_GATEWAY, "Godo API error"),
    WMS_SCRAPING_ERROR(HttpStatus.BAD_GATEWAY, "WMS scraping error"),
    SFOOD_ADMIN_ERROR(HttpStatus.BAD_GATEWAY, "SfoodMall admin error"),
    SCHEDULE_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid schedule configuration"),
    UPLOAD_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Upload session not found"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Member not found"),
    MEMBER_PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "Member partner not found"),
    DUPLICATE_PARTNER_CODE(HttpStatus.CONFLICT, "Duplicate partner code"),
    MEMBER_STATUS_INVALID(HttpStatus.BAD_REQUEST, "Invalid member status"),
    MS_LOGIN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "MS login verification failed"),
    MS_AUTH_GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "AX Auth service error"),
    DASHBOARD_QUERY_INVALID(HttpStatus.BAD_REQUEST, "Invalid dashboard query parameter"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
