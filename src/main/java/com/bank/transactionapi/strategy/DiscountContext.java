package com.bank.transactionapi.strategy;

import com.bank.transactionapi.models.Customer;

import java.math.BigDecimal;

public record DiscountContext(Customer customer, BigDecimal amount, long transactionsThisMonth) {

}