package com.fintechledger.common;

import java.math.BigDecimal;

public final class MoneyRules {
    private MoneyRules() {}
    public static BigDecimal requirePositiveScale2(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.scale() > 2)
            throw new IllegalArgumentException("Amount must be positive and have at most two decimal places");
        return value;
    }
    public static boolean isBalanced(BigDecimal debits, BigDecimal credits) {
        return debits != null && credits != null && debits.compareTo(credits) == 0;
    }
}
