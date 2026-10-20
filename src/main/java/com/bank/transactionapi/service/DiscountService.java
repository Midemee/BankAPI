package com.bank.transactionapi.service;

import com.bank.transactionapi.models.Customer;

import java.math.BigDecimal;

public interface DiscountService {
    BigDecimal calculateDiscount(Customer customer, BigDecimal amount, long transactionsThisMonth);
}
