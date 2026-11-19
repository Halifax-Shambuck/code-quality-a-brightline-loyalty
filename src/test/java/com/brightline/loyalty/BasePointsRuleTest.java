package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BasePointsRuleTest {

    private final BasePointsRule rule = new BasePointsRule();

    @ParameterizedTest(name = "{0} earns {1} base points")
    @CsvSource({
            "0, 0",
            "9.99, 0",
            "10, 1",
            "19.99, 1",
            "499.99, 49",
            "21474835970, 2147483597"
    })
    void roundsDownToWholePointsPerTenGhs(double amount, int expected) {
        assertEquals(expected, rule.calculateBasePoints(amount));
    }

    @Test
    void rejectsAnAmountWhosePointsExceedTheIntRange() {
        assertThrows(ArithmeticException.class, () -> rule.calculateBasePoints(21474836480.0));
    }
}
