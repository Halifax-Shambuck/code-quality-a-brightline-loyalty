package com.brightline.loyalty.exception;

public class InvalidPurchaseException extends RuntimeException {

    public InvalidPurchaseException(String message) {
        super(message);
    }

    public InvalidPurchaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
