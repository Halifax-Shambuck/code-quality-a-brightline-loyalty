package com.brightline.loyalty.domain;

public class PlatinumTier implements Tier {
    private static final int MULTIPLIER = 3;

    @Override
    public int applyTo(int basePoints) {
        return Math.multiplyExact(basePoints, MULTIPLIER);
    }
}

