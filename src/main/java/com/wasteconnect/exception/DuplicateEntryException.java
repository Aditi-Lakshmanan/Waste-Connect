package com.wasteconnect.exception;

/**
 * INHERITANCE: Thrown when attempting to register or insert an entity that violates unique constraints.
 */
public class DuplicateEntryException extends WasteConnectException {
    public DuplicateEntryException(String message) {
        super(message);
    }
}
