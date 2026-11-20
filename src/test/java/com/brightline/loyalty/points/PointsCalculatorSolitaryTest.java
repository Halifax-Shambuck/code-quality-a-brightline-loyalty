package com.brightline.loyalty.points;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.brightline.loyalty.domain.GoldTier;
import com.brightline.loyalty.domain.Member;
import com.brightline.loyalty.exception.InvalidPurchaseException;
import com.brightline.loyalty.exception.MemberNotFoundException;
import com.brightline.loyalty.repository.MemberRepository;
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
        calculator = new PointsCalculator(repository, new BasePointsRule(), new BigSpenderBonusRule());
    }


    @Test
    void repositoryFailurePropagatesUnchanged() {
        IllegalStateException failure = new IllegalStateException("store unavailable");
        when(repository.findById("m1")).thenThrow(failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> calculator.calculatePoints("m1", 100));

        assertSame(failure, thrown);
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
        verify(repository).findById("m9");
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

    @Test
    void aTierDefinedByTheTestIsAppliedBeforeTheBonus() {
        Member member = new Member("m9", "Test Member", basePoints -> basePoints + 7);
        when(repository.findById("m9")).thenReturn(Optional.of(member));

        assertEquals(117, calculator.calculatePoints("m9", 600));
    }


}
