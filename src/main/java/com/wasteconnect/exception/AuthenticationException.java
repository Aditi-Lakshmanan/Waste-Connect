package com.wasteconnect.exception;

/**
 * INHERITANCE: Specific exception for credential verification and authorization failures.
 */
public class AuthenticationException extends WasteConnectException {
    public AuthenticationException(String message) {
        super(message);
    }
}
