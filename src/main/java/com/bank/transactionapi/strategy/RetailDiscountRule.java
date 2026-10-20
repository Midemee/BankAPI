package com.bank.transactionapi.strategy;

import com.bank.transactionapi.enums.CustomerType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetailDiscountRule implements DiscountRule {

    private static final BigDecimal THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal PERCENTAGE = new BigDecimal("18");

    @Override
    public boolean isApplicable(DiscountContext ctx) {
        return ctx.customer().getCustomerType() == CustomerType.RETAIL
                && ctx.amount().compareTo(THRESHOLD) > 0
                && ctx.transactionsThisMonth() >= 3;   // starts from the 4th
    }

    @Override
    public BigDecimal getDiscountPercentage() {
        return PERCENTAGE;
    }
}