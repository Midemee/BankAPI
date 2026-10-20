package com.bank.transactionapi.repositories;

import com.bank.transactionapi.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

}