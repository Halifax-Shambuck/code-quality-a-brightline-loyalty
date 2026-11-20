package com.brightline.loyalty.points;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.brightline.loyalty.repository.InMemoryMemberRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PointsCalculatorBoundaryTest {

    private final PointsCalculator calculator = new PointsCalculator(
            new InMemoryMemberRepository(), new BasePointsRule(), new BigSpenderBonusRule());

    @ParameterizedTest(name = "{0} spending {1} earns {2}")
    @CsvSource({
            "m1, 499, 49",
            "m1, 500, 50",
            "m1, 501, 100",
            "m3, 499, 73",
            "m3, 500, 75",
            "m3, 501, 125",
            "m2, 499, 98",
            "m2, 500, 100",
            "m2, 501, 150"
    })
    void bonusBoundaryForEveryTier(String memberId, double amount, int expected) {
        assertEquals(expected, calculator.calculatePoints(memberId, amount));
    }

    @ParameterizedTest(name = "{0} spending {1} earns {2}")
    @CsvSource({
            "m3, 20, 3",
            "m3, 40, 6",
            "m3, 70, 10",
            "m1, 0, 0",
            "m2, 0, 0",
            "m3, 0, 0",
            "m1, -0.0, 0"
    })
    void roundingAndZeroEdges(String memberId, double amount, int expected) {
        assertEquals(expected, calculator.calculatePoints(memberId, amount));
    }
}
