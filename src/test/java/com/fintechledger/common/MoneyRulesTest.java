package com.fintechledger.common;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyRulesTest {
    @Test void acceptsPositiveAmountsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("500.00"), MoneyRules.requirePositiveScale2(new BigDecimal("500.00")));
    }
    @Test void rejectsZeroNegativeAndExcessPrecision() {
        assertThrows(IllegalArgumentException.class, () -> MoneyRules.requirePositiveScale2(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> MoneyRules.requirePositiveScale2(new BigDecimal("-1.00")));
        assertThrows(IllegalArgumentException.class, () -> MoneyRules.requirePositiveScale2(new BigDecimal("1.001")));
    }
    @Test void comparesDebitAndCreditTotalsByNumericValue() {
        assertTrue(MoneyRules.isBalanced(new BigDecimal("500"), new BigDecimal("500.00")));
        assertFalse(MoneyRules.isBalanced(new BigDecimal("499.99"), new BigDecimal("500.00")));
    }
}
