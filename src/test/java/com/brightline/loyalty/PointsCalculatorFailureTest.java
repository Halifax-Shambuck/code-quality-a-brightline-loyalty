package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PointsCalculatorFailureTest {

    private final PointsCalculator calculator = new PointsCalculator(
            new InMemoryMemberRepository(), new BasePointsRule(), new BigSpenderBonusRule());

    @Test
    void unknownMemberIsRejectedWithItsId() {
        MemberNotFoundException thrown = assertThrows(MemberNotFoundException.class,
                () -> calculator.calculatePoints("unknown", 100));

        assertEquals("Member not found: unknown", thrown.getMessage());
        assertEquals("unknown", thrown.getMemberId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"M1", "m1 "})
    void memberIdsMatchExactly(String id) {
        assertThrows(MemberNotFoundException.class, () -> calculator.calculatePoints(id, 100));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void blankMemberIdIsRejected(String id) {
        InvalidPurchaseException thrown = assertThrows(InvalidPurchaseException.class,
                () -> calculator.calculatePoints(id, 100));

        assertEquals("Member id must not be blank", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -100, Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY})
    void invalidAmountIsRejected(double amount) {
        InvalidPurchaseException thrown = assertThrows(InvalidPurchaseException.class,
                () -> calculator.calculatePoints("m1", amount));

        assertTrue(thrown.getMessage().startsWith("Amount must be a finite number"));
    }

    @Test
    void zeroAmountEarnsNothing() {
        assertEquals(0, calculator.calculatePoints("m1", 0));
    }

    @ParameterizedTest
    @CsvSource({"m1, 1e12", "m2, 1.2e10"})
    void pointsBeyondIntRangeAreRejectedWithTheCause(String id, double amount) {
        InvalidPurchaseException thrown = assertThrows(InvalidPurchaseException.class,
                () -> calculator.calculatePoints(id, amount));

        assertTrue(thrown.getMessage().contains("exceed the int range"));
        assertInstanceOf(ArithmeticException.class, thrown.getCause());
    }

    // 21474835970 / 10 = 2147483597 base points, plus the 50 bonus = Integer.MAX_VALUE
    @Test
    void largestRepresentableTotalIsAccepted() {
        assertEquals(Integer.MAX_VALUE, calculator.calculatePoints("m1", 21474835970.0));
    }

    @Test
    void oneMorePointThanRepresentableIsRejected() {
        assertThrows(InvalidPurchaseException.class,
                () -> calculator.calculatePoints("m1", 21474835980.0));
    }
}
