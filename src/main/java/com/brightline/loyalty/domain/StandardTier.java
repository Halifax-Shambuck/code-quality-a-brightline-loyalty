package com.brightline.loyalty.domain;

public class StandardTier implements Tier {

    @Override
    public int applyTo(int basePoints) {
        return basePoints;
    }
}
