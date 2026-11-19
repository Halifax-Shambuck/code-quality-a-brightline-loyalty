package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Negative, NaN, infinite and overflowing amounts, and unknown ids, are not pinned here.
// They are known defects that the refactor changes on purpose.
class PointsCalculatorCharacterizationTest {

    private final PointsCalculator calculator = new PointsCalculator();

    @ParameterizedTest(name = "STANDARD member m1 spending {0} earns {1}")
    @CsvSource({
            "0,      0",
            "9.99,   0",
            "10,     1",
            "19.99,  1",
            "100,    10",
            "499.99, 49",
            "500,    50",
            "500.01, 100",
            "501,    100",
            "600,    110"
    })
    void standardMemberPoints(double amount, int expectedPoints) {
        assertEquals(expectedPoints, calculator.calculatePoints("m1", amount));
    }

    @ParameterizedTest(name = "SILVER member m3 spending {0} earns {1}")
    @CsvSource({
            "10,  1",
            "30,  4",
            "50,  7",
            "200, 30",
            "600, 140"
    })
    void silverMemberPoints(double amount, int expectedPoints) {
        assertEquals(expectedPoints, calculator.calculatePoints("m3", amount));
    }

    @ParameterizedTest(name = "GOLD member m2 spending {0} earns {1}")
    @CsvSource({
            "250, 50",
            "499.99, 98",
            "600, 170"
    })
    void goldMemberPoints(double amount, int expectedPoints) {
        assertEquals(expectedPoints, calculator.calculatePoints("m2", amount));
    }

    // Pins the println that is being moved out of the calculator. Delete with it.
    @Test
    void printsAwardedLineForKnownMember() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            calculator.calculatePoints("m1", 100);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals("Awarded 10 points to Kofi Mensah" + System.lineSeparator(),
                captured.toString());
    }
}
