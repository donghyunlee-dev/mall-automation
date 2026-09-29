package com.sfood.automation.common.exception;

public class WmsScrapingException extends BusinessException {

    public WmsScrapingException() {
        super(ErrorCode.WMS_SCRAPING_ERROR);
    }

    public WmsScrapingException(String message) {
        super(ErrorCode.WMS_SCRAPING_ERROR, message);
    }

    public WmsScrapingException(String message, Throwable cause) {
        super(ErrorCode.WMS_SCRAPING_ERROR, message, cause);
    }
}
