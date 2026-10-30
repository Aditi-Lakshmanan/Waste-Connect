package com.wasteconnect.exception;

/**
 * INHERITANCE: Specific exception for user input or business rule validation failures.
 */
public class ValidationException extends WasteConnectException {
    public ValidationException(String message) {
        super(message);
    }
}
