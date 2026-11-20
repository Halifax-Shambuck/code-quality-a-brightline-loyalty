package com.brightline.loyalty.points;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BigSpenderBonusRuleTest {

    private final BigSpenderBonusRule rule = new BigSpenderBonusRule();

    @ParameterizedTest(name = "{0} earns a bonus of {1}")
    @CsvSource({
            "0, 0",
            "499.99, 0",
            "500, 0",
            "500.01, 50",
            "600, 50"
    })
    void bonusAppliesOnlyAboveTheThreshold(double amount, int expected) {
        assertEquals(expected, rule.calculateBonus(amount));
    }
}
