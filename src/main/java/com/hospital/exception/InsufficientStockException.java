package com.hospital.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String medication, int required, int available) {
        super(String.format("Insufficient stock for '%s': required %d, available %d",
                medication, required, available));
    }
}
