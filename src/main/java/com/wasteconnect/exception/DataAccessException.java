package com.wasteconnect.exception;

/**
 * INHERITANCE: Wraps lower-level database or persistence errors.
 */
public class DataAccessException extends WasteConnectException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
