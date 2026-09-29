package com.sfood.automation.dashboard.application.support;

import com.sfood.automation.common.exception.BusinessException;
import com.sfood.automation.common.exception.ErrorCode;

public class DashboardQueryException extends BusinessException {

    public DashboardQueryException(String message) {
        super(ErrorCode.DASHBOARD_QUERY_INVALID, message);
    }
}
