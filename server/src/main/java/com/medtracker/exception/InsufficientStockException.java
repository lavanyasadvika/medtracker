package com.medtracker.exception;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final Integer available;

    public InsufficientStockException(String message) {
        super(message);
        this.available = null;
    }

    public InsufficientStockException(String drugName, Integer requested, Integer available) {
        super(String.format("Insufficient stock for '%s'. Requested: %s, Available: %s",
                drugName, requested, available));
        this.available = available;
    }
}
