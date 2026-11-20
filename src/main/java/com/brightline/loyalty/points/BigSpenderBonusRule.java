package com.brightline.loyalty.points;

public class BigSpenderBonusRule {
    private static final double SPEND_THRESHOLD = 500;
    private static final int BONUS_POINTS = 50;

    // BigSpenderBonusRule: the bonus starts strictly above the threshold, so exactly 500 earns none.
    public int calculateBonus(double amount) {
        return amount > SPEND_THRESHOLD ? BONUS_POINTS : 0;
    }
}
