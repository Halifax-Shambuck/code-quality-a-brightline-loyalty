package com.brightline.loyalty.domain;

public class SilverTier implements Tier {
    private static final double MULTIPLIER = 1.5;

    @Override
    public int applyTo(int basePoints) {
        // SilverTier: fractional points are dropped, never rounded up.
        return Math.toIntExact((long) (basePoints * MULTIPLIER));
    }
}
