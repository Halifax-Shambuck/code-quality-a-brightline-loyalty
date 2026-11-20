package com.brightline.loyalty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class TierTest {

    @ParameterizedTest(name = "STANDARD: {0} base points gives {1}")
    @CsvSource({"0, 0", "1, 1", "60, 60"})
    void standardTierKeepsBasePoints(int basePoints, int expected) {
        assertEquals(expected, new StandardTier().applyTo(basePoints));
    }

    @ParameterizedTest(name = "SILVER: {0} base points gives {1}")
    @CsvSource({"0, 0", "1, 1", "3, 4", "5, 7", "20, 30", "1431655765, 2147483647"})
    void silverTierMultipliesByOnePointFiveAndRoundsDown(int basePoints, int expected) {
        assertEquals(expected, new SilverTier().applyTo(basePoints));
    }

    @ParameterizedTest(name = "GOLD: {0} base points gives {1}")
    @CsvSource({"0, 0", "1, 2", "25, 50", "1073741823, 2147483646"})
    void goldTierDoublesBasePoints(int basePoints, int expected) {
        assertEquals(expected, new GoldTier().applyTo(basePoints));
    }

    @Test
    void silverTierRejectsAResultBeyondTheIntRange() {
        assertThrows(ArithmeticException.class, () -> new SilverTier().applyTo(1431655766));
    }

    @Test
    void goldTierRejectsAResultBeyondTheIntRange() {
        assertThrows(ArithmeticException.class, () -> new GoldTier().applyTo(1073741824));
    }
}
