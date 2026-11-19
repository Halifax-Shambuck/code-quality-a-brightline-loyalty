package com.brightline.loyalty;

public class PointsCalculator {
    private final MemberRepository repository;
    private final BasePointsRule basePointsRule;
    private final BigSpenderBonusRule bonusRule;

    public PointsCalculator(MemberRepository repository, BasePointsRule basePointsRule, BigSpenderBonusRule bonusRule) {
        this.repository = repository;
        this.basePointsRule = basePointsRule;
        this.bonusRule = bonusRule;
    }

    public int calculatePoints(String memberId, double amount) {
        validatePurchase(memberId, amount);

        Member member = findMember(memberId);

        try {
            int points = member.getTier().applyTo(basePointsRule.calculateBasePoints(amount));

            return Math.addExact(points, bonusRule.calculateBonus(amount));
        } catch (ArithmeticException e) {
            throw new InvalidPurchaseException(
                    "Points for amount " + amount + " exceed the int range", e);
        }
    }

    private Member findMember(String memberId) {
        return repository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    private void validatePurchase(String memberId, double amount) {
        if (memberId == null || memberId.isBlank()) {
            throw new InvalidPurchaseException("Member id must not be blank");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0) {
            throw new InvalidPurchaseException(
                    "Amount must be a finite number of zero or more: " + amount);
        }
    }
}
