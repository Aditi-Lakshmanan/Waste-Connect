package com.wasteconnect.test;

import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.util.PasswordHasher;
import com.wasteconnect.util.Result;
import com.wasteconnect.util.Validators;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilTest {

    @Test
    @DisplayName("Util: PasswordHasher correctly hashes and verifies passwords using SHA-256 + Salt")
    void testPasswordHashingAndVerification() {
        String plain = "Secret@123";
        String hashed = PasswordHasher.hashPassword(plain);

        assertNotNull(hashed);
        assertTrue(hashed.contains("$"), "Hashed password should be in format salt$hash");

        assertTrue(PasswordHasher.verifyPassword(plain, hashed), "Original password must verify successfully");
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", hashed), "Incorrect password must fail verification");
        assertFalse(PasswordHasher.verifyPassword("", hashed));
        assertFalse(PasswordHasher.verifyPassword(null, hashed));
    }

    @Test
    @DisplayName("Util: Validators correctly enforce email, phone, and coordinate standards")
    void testInputValidators() {
        // Valid inputs
        assertTrue(Validators.isValidEmail("citizen@wasteconnect.com"));
        assertTrue(Validators.isValidEmail("user.name+tag@sub.domain.org"));
        assertTrue(Validators.isValidPassword("StrongPass123"));

        // Invalid inputs
        assertFalse(Validators.isValidEmail("invalid-email"));
        assertFalse(Validators.isValidEmail("user@domain"));
        assertFalse(Validators.isValidPassword("123"));

        assertThrows(ValidationException.class, () -> Validators.validateEmail("bad-email"));
        assertThrows(ValidationException.class, () -> Validators.validatePasswordStrength("123"));
        assertThrows(ValidationException.class, () -> Validators.validateCoordinates(95.0, 77.0));
        assertThrows(ValidationException.class, () -> Validators.validateCoordinates(12.0, 200.0));
        assertThrows(ValidationException.class, () -> Validators.validateNonBlank("   ", "Field"));
        assertThrows(ValidationException.class, () -> Validators.validatePositive(-10.0, "Weight"));
        assertThrows(ValidationException.class, () -> Validators.validatePhone("invalid-phone-abc"));
    }

    @Test
    @DisplayName("Util: Generic Result<T> handles success and failure transformations")
    void testGenericResultContainer() {
        Result<Integer> success = Result.success(42);
        assertTrue(success.isSuccess());
        assertEquals(42, success.getData());

        Result<String> mapped = success.map(n -> "Value: " + n);
        assertTrue(mapped.isSuccess());
        assertEquals("Value: 42", mapped.getData());

        Result<Integer> failure = Result.failure("Database error occurred");
        assertTrue(failure.isFailure());
        assertEquals("Database error occurred", failure.getErrorMessage());
        assertThrows(IllegalStateException.class, failure::getData);
    }
}
