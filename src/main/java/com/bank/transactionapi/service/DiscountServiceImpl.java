package com.bank.transactionapi.service;

import com.bank.transactionapi.models.Customer;
import com.bank.transactionapi.strategy.DiscountContext;
import com.bank.transactionapi.strategy.DiscountRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {
    private final List<DiscountRule> rules;

    @Override
    public BigDecimal calculateDiscount(Customer customer, BigDecimal amount, long transactionsThisMonth){
        DiscountContext context = new DiscountContext(customer, amount, transactionsThisMonth);
        BigDecimal bestPercentage = rules.stream()
                .filter(rule -> rule.isApplicable(context))
                .map(DiscountRule::getDiscountPercentage)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return amount.multiply(bestPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
