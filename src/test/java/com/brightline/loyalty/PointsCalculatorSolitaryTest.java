package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointsCalculatorSolitaryTest {

    @Mock
    private MemberRepository repository;

    private PointsCalculator calculator;

    @BeforeEach
    void createCalculator() {
        calculator = new PointsCalculator(repository);
    }

    @Test
    void repositoryFailurePropagatesUnchanged() {
        when(repository.findById("m1")).thenThrow(new IllegalStateException("store unavailable"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> calculator.calculatePoints("m1", 100));

        assertEquals("store unavailable", thrown.getMessage());
    }

    @Test
    void missingMemberIsReportedWithItsId() {
        when(repository.findById("m404")).thenReturn(Optional.empty());

        MemberNotFoundException thrown = assertThrows(MemberNotFoundException.class,
                () -> calculator.calculatePoints("m404", 100));

        assertEquals("Member not found: m404", thrown.getMessage());
    }

    @Test
    void pointsComeFromTheMemberTheRepositoryReturns() {
        Member member = new Member("m9", "Test Member", new GoldTier());
        when(repository.findById("m9")).thenReturn(Optional.of(member));

        assertEquals(40, calculator.calculatePoints("m9", 200));
    }

    @Test
    void invalidAmountIsRejectedBeforeAnyLookup() {
        assertThrows(InvalidPurchaseException.class,
                () -> calculator.calculatePoints("m1", -1));

        verifyNoInteractions(repository);
    }

    @Test
    void aTierDefinedByTheTestIsAppliedWithoutChangingTheCalculator() {
        Member member = new Member("m9", "Test Member", basePoints -> basePoints + 7);
        when(repository.findById("m9")).thenReturn(Optional.of(member));

        assertEquals(27, calculator.calculatePoints("m9", 200));
    }

}
