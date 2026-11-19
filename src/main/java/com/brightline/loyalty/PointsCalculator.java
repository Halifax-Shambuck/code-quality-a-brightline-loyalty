package com.brightline.loyalty;

public class PointsCalculator {

    // The calculator makes its own repository. Convenient, but it means callers
    // and tests cannot give it a different one.
    private final MemberRepository repository = new InMemoryMemberRepository();

    public int calculatePoints(String memberId, double amount) {
        try {
            Member member = repository.findById(memberId);

            // base points: one point for every GHS 10 spent
            int points = (int) (amount / 10);

            points = member.getTier().applyTo(points);

            // loyalty bonus for big spenders
            if (amount > 500) {
                points = points + 50;
            }

            System.out.println("Awarded " + points + " points to " + member.getName());
            return points;
        } catch (Exception e) {
            // something went wrong (for example the member was not found) - award nothing
            return 0;
        }
    }
}
