package com.brightline.loyalty.points;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.brightline.loyalty.domain.Member;
import com.brightline.loyalty.domain.PlatinumTier;
import com.brightline.loyalty.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatinumMemberPointsTest {

    @Mock
    private MemberRepository repository;

    @Test
    void platinumMemberEarnsTripleBasePointsPlusFlatBonus() {
        Member member = new Member("p1", "Test Platinum", new PlatinumTier());
        when(repository.findById("p1")).thenReturn(Optional.of(member));
        PointsCalculator calculator = new PointsCalculator(
                repository, new BasePointsRule(), new BigSpenderBonusRule());

        assertEquals(230, calculator.calculatePoints("p1", 600));
    }
}
