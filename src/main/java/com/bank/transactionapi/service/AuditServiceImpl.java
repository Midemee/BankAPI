package com.bank.transactionapi.service;

import com.bank.transactionapi.audit.AuditLog;
import com.bank.transactionapi.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final int MAX_MESSAGE_LENGTH = 1000;   // matches AuditLog.errorMessage

    private final AuditLogRepository auditLogRepository;

    @Override
    public void record(String httpMethod, String endpoint, int statusCode, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setHttpMethod(httpMethod);
            auditLog.setEndpoint(endpoint);
            auditLog.setStatusCode(statusCode);
            auditLog.setSuccess(statusCode >= 200 && statusCode < 300);
            auditLog.setErrorMessage(truncate(errorMessage));
            auditLogRepository.save(auditLog);
        } catch (RuntimeException e) {
            // An audit failure must never turn a good request into a failed one
            log.error("Could not write audit log for {} {}", httpMethod, endpoint, e);
        }
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH);
    }
}