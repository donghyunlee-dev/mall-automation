package com.sfood.automation.auth.infrastructure.session;

import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionIssuer implements AuthSessionPort {

    @Override
    public void issue(HttpServletRequest request, String email) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, email);
        session.setMaxInactiveInterval(86400);
    }

    @Override
    public void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
