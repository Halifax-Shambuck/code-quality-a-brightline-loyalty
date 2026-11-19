package com.brightline.loyalty;

public class GoldTier implements Tier {
    private static final int MULTIPLIER = 2;

    @Override
    public int applyTo(int basePoints) {
        return basePoints * MULTIPLIER;
    }
}
