package com.brightline.loyalty.exception;

public class MemberNotFoundException extends RuntimeException {
    private final String memberId;

    public MemberNotFoundException(String memberId) {
        super("Member not found: " + memberId);
        this.memberId = memberId;
    }

    public String getMemberId() {
        return memberId;
    }
}
