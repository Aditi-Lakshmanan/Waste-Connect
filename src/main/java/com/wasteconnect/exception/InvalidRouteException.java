package com.wasteconnect.exception;

/**
 * INHERITANCE: Specific domain exception when route planning constraints are violated.
 */
public class InvalidRouteException extends WasteConnectException {
    public InvalidRouteException(String message) {
        super(message);
    }
}
