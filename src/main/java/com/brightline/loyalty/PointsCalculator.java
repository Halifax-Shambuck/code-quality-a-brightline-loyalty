package com.brightline.loyalty;

public class PointsCalculator {
    private final MemberRepository repository = new InMemoryMemberRepository();

    public int calculatePoints(String memberId, double amount) {
        validatePurchase(memberId, amount);

        Member member = repository.findById(memberId).orElseThrow(() -> new MemberNotFoundException(memberId));

        int points;

        try {
            points = member.getTier().applyTo(Math.toIntExact((long) (amount / 10)));

            if (amount > 500) {
                points = Math.addExact(points, 50);
            }
        } catch (ArithmeticException e) {
            throw new InvalidPurchaseException(
                    "Points for amount " + amount + " exceed the int range", e);
        }

        System.out.println("Awarded " + points + " points to " + member.getName());

        return points;
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
