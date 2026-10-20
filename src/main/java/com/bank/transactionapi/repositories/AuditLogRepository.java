package com.bank.transactionapi.repositories;

import com.bank.transactionapi.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}