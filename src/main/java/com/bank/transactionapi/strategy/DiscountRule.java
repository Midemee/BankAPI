package com.bank.transactionapi.strategy;

import java.math.BigDecimal;

public interface DiscountRule {

    boolean isApplicable(DiscountContext context);

    BigDecimal getDiscountPercentage();
}