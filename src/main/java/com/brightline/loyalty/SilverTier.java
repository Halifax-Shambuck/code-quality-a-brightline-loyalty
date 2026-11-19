package com.brightline.loyalty;

public class SilverTier implements Tier {
    private static final double MULTIPLIER = 1.5;

    @Override
    public int applyTo(int basePoints) {
        return Math.toIntExact((long) (basePoints * MULTIPLIER));
    }
}
