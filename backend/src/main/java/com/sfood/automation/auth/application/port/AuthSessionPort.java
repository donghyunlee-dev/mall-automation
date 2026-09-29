package com.sfood.automation.auth.application.port;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthSessionPort {
    void issue(HttpServletRequest request, String email);

    void invalidate(HttpServletRequest request);
}
