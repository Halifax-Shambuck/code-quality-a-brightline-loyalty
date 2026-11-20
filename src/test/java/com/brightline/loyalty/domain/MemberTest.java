package com.brightline.loyalty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MemberTest {
    @Test
    void rejectsANullTier() {
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> new Member("m9", "Test Member", null));

        assertEquals("tier must not be null", thrown.getMessage());
    }
}
