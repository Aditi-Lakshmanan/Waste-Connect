package com.wasteconnect.exception;

/**
 * INHERITANCE: Root of the custom exception hierarchy for WasteConnect.
 * All domain-specific and operational exceptions extend this base class.
 */
public class WasteConnectException extends RuntimeException {
    public WasteConnectException(String message) {
        super(message);
    }

    public WasteConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
