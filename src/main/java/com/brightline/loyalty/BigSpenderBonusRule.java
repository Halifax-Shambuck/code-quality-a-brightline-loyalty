package com.brightline.loyalty;

public class BigSpenderBonusRule {
    private static final double THRESHOLD = 500;
    private static final int BONUS_POINTS = 50;

    public int calculateBonus(double amount) {
        return amount > THRESHOLD ? BONUS_POINTS : 0;
    }
}
