package com.agrichain.trader;

public class TraderNotFoundException extends RuntimeException {
    public TraderNotFoundException(String message) {
        super(message);
    }
}
