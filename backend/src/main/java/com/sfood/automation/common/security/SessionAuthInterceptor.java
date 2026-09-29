package com.sfood.automation.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.exception.ErrorCode;
import com.sfood.automation.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_SESSION_KEY = "AUTHENTICATED_EMAIL";

    private static final Set<String> POST_WHITELIST = Set.of(
        "/api/auth/email-verifications",
        "/api/auth/email-verifications/confirm",
        "/api/auth/ms-login/verify"
    );

    private final ObjectMapper objectMapper;

    public SessionAuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (isWhitelisted(request)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            writeUnauthorized(response);
            return false;
        }

        try {
            Object authenticatedEmail = session.getAttribute(AUTH_SESSION_KEY);
            if (authenticatedEmail == null) {
                writeUnauthorized(response);
                return false;
            }
            return true;
        } catch (IllegalStateException exception) {
            writeUnauthorized(response);
            return false;
        }
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod()) && "/actuator/health".equals(request.getRequestURI())) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod()) && "/api/auth/session".equals(request.getRequestURI())) {
            return true;
        }
        return HttpMethod.POST.matches(request.getMethod()) && POST_WHITELIST.contains(request.getRequestURI());
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.from(ErrorCode.AUTHENTICATION_REQUIRED));
    }
}
