package com.brightline.loyalty;

public class StandardTier implements Tier {

    @Override
    public int applyTo(int basePoints) {
        return basePoints;
    }
}
