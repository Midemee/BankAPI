package com.bank.transactionapi.service;

import com.bank.transactionapi.audit.AuditLog;
import com.bank.transactionapi.repositories.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditServiceImplTest {

    @Autowired private AuditService auditService;
    @Autowired private AuditLogRepository auditLogRepository;

    @Test
    void record_successfulRequest_isMarkedSuccess() {
        auditService.record("POST", "/api/v1/transactions/transfer", 201, null);

        AuditLog saved = onlyEntry();
        assertThat(saved.getHttpMethod()).isEqualTo("POST");
        assertThat(saved.getEndpoint()).isEqualTo("/api/v1/transactions/transfer");
        assertThat(saved.getStatusCode()).isEqualTo(201);
        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getErrorMessage()).isNull();
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void record_failedRequest_isMarkedFailedWithMessage() {
        auditService.record("GET", "/api/v1/accounts/999/transactions", 404, "Account not found: 999");

        AuditLog saved = onlyEntry();
        assertThat(saved.getStatusCode()).isEqualTo(404);
        assertThat(saved.isSuccess()).isFalse();
        assertThat(saved.getErrorMessage()).isEqualTo("Account not found: 999");
    }

    @Test
    void record_veryLongErrorMessage_isTruncatedTo1000Characters() {
        auditService.record("POST", "/api/v1/transactions/airtime", 500, "x".repeat(5000));

        assertThat(onlyEntry().getErrorMessage()).hasSize(1000);
    }

    @Test
    void record_everyCallIsStored() {
        auditService.record("POST", "/api/v1/transactions/transfer", 201, null);
        auditService.record("POST", "/api/v1/transactions/transfer", 400, "Validation failed");
        auditService.record("GET", "/api/v1/accounts/1/transactions", 200, null);

        assertThat(auditLogRepository.findAll()).hasSize(3);
    }

    private AuditLog onlyEntry() {
        List<AuditLog> all = auditLogRepository.findAll();
        assertThat(all).hasSize(1);
        return all.get(0);
    }
}