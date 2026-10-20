package com.bank.transactionapi.repositories;

import com.bank.transactionapi.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySourceAccountAccountNumberOrderByCreatedAtDesc(String accountNumber);

    @Query("""
           SELECT COUNT(t) FROM Transaction t
           WHERE t.sourceAccount.customer.customerId = :customerId
             AND t.status = com.bank.transactionapi.enums.TransactionStatus.SUCCESS
             AND t.createdAt >= :start AND t.createdAt < :end
           """)
    long countByCustomerInPeriod(@Param("customerId") Long customerId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);
}