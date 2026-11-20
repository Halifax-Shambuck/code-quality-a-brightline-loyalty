package com.brightline.loyalty.domain;

import java.util.Objects;

public record Member(String id, String name, Tier tier) {
    public Member {
        Objects.requireNonNull(tier, "tier must not be null");
    }
}
