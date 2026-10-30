package com.wasteconnect.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for cryptographic password hashing using salted SHA-256 (MessageDigest).
 */
public final class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    /**
     * Generates a cryptographically secure random base64 salt.
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes password with provided salt using SHA-256.
     * Returns string in format: salt$hash
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            String hashBase64 = Base64.getEncoder().encodeToString(hashedBytes);
            return salt + "$" + hashBase64;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in current runtime environment", e);
        }
    }

    /**
     * Hashes password by generating a fresh random salt.
     */
    public static String hashPassword(String password) {
        String salt = generateSalt();
        return hashPassword(password, salt);
    }

    /**
     * Verifies plain text password against stored "salt$hash" string.
     */
    public static boolean verifyPassword(String password, String storedHashWithSalt) {
        if (password == null || password.isEmpty() || storedHashWithSalt == null) {
            return false;
        }
        String[] parts = storedHashWithSalt.split("\\$");
        if (parts.length != 2) {
            return false;
        }
        String salt = parts[0];
        String computed = hashPassword(password, salt);
        return computed.equals(storedHashWithSalt);
    }
}
