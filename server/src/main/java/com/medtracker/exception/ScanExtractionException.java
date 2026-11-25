package com.medtracker.exception;

public class ScanExtractionException extends RuntimeException {

    public ScanExtractionException(String message) {
        super(message);
    }

    public ScanExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
