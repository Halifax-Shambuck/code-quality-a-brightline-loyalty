package com.brightline.loyalty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlatinumTierTest {

    @ParameterizedTest(name = "PLATINUM: {0} base points gives {1}")
    @CsvSource({"0, 0", "1, 3", "20, 60", "715827882, 2147483646"})
    void platinumTierTriplesBasePoints(int basePoints, int expected) {
        assertEquals(expected, new PlatinumTier().applyTo(basePoints));
    }

    @Test
    void platinumTierRejectsAResultBeyondTheIntRange() {
        assertThrows(ArithmeticException.class, () -> new PlatinumTier().applyTo(715827883));
    }
}
