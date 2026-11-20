package com.brightline.loyalty.points;

public class BasePointsRule {
    private static final int AMOUNT_PER_POINT = 10;

    public int calculateBasePoints(double amount) {
        return Math.toIntExact((long) (amount / AMOUNT_PER_POINT));
    }
}
