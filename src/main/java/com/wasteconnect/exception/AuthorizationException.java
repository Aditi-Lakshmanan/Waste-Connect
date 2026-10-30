package com.wasteconnect.exception;

/**
 * Thrown when a user attempts an operation they are not authorized to perform.
 */
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) {
        super(message);
    }
}
