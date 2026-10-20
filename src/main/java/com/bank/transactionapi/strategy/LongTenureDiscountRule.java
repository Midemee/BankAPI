package com.bank.transactionapi.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class LongTenureDiscountRule implements DiscountRule {

    private static final BigDecimal PERCENTAGE = new BigDecimal("10");

    @Override
    public boolean isApplicable(DiscountContext ctx) {
        // "over 4 years": joined before the date exactly 4 years ago
        boolean overFourYears = ctx.customer().getDateJoined()
                .isBefore(LocalDate.now().minusYears(4));
        // first three transactions of the month => 0, 1 or 2 already made
        return overFourYears && ctx.transactionsThisMonth() < 3;
    }

    @Override
    public BigDecimal getDiscountPercentage() {
        return PERCENTAGE;
    }
}