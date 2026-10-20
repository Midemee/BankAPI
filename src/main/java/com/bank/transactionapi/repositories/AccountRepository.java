package com.bank.transactionapi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bank.transactionapi.models.Account;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
}
