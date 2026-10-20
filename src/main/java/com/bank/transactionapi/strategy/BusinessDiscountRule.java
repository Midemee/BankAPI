package com.bank.transactionapi.strategy;

import com.bank.transactionapi.enums.CustomerType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BusinessDiscountRule implements DiscountRule {

    private static final BigDecimal THRESHOLD = new BigDecimal("150000");
    private static final BigDecimal PERCENTAGE = new BigDecimal("27");

    @Override
    public boolean isApplicable(DiscountContext ctx) {
        return ctx.customer().getCustomerType() == CustomerType.BUSINESS
                && ctx.amount().compareTo(THRESHOLD) > 0
                && ctx.transactionsThisMonth() >= 3;   // starts from the 4th
    }

    @Override
    public BigDecimal getDiscountPercentage() {
        return PERCENTAGE;
    }
}