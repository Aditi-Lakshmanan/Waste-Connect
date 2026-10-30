package com.wasteconnect.util;

import com.wasteconnect.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Defensive validation utility enforcing business integrity rules across forms and entity layers.
 */
public final class Validators {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{7,15}$");

    private Validators() {}

    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Invalid email format: " + email);
        }
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static void validateCoordinates(double lat, double lng) {
        if (lat < -90.0 || lat > 90.0) {
            throw new ValidationException("Latitude must be between -90 and 90. Received: " + lat);
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new ValidationException("Longitude must be between -180 and 180. Received: " + lng);
        }
    }

    public static void validateNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty or blank.");
        }
    }

    public static void validatePositive(double value, String fieldName) {
        if (value <= 0.0) {
            throw new ValidationException(fieldName + " must be strictly positive. Received: " + value);
        }
    }

    public static void validatePhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("Invalid phone number format: " + phone);
        }
    }
}
