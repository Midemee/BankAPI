package com.bank.transactionapi.audit;

import com.bank.transactionapi.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    /** GlobalExceptionHandler stores the error text here, because handled exceptions never reach afterCompletion. */
    public static final String ERROR_MESSAGE_ATTRIBUTE = "audit.errorMessage";

    private final AuditService auditService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String errorMessage = null;
        if (request.getAttribute(ERROR_MESSAGE_ATTRIBUTE) instanceof String message) {
            errorMessage = message;
        } else if (ex != null) {
            errorMessage = ex.getMessage();
        }
        auditService.record(request.getMethod(), request.getRequestURI(), response.getStatus(), errorMessage);
    }
}