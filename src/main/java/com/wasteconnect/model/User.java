package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * ABSTRACTION: Abstract base class representing any authenticated entity in the WasteConnect ecosystem.
 * Defines contract for role-specific dashboard views and permission matrix.
 * ENCAPSULATION: All internal states are private with validated mutators; password hash is safeguarded.
 */
public abstract class User implements Notifiable {
    // ENCAPSULATION: Private fields
    private String id;
    private String name;
    private String email;
    private String passwordHash;
    private UserRole role;
    private LocalDateTime createdAt;
    private boolean active;

    public User(String id, String name, String email, String passwordHash, UserRole role) {
        this(id, name, email, passwordHash, role, LocalDateTime.now(), true);
    }

    public User(String id, String name, String email, String passwordHash, UserRole role, LocalDateTime createdAt, boolean active) {
        setId(id);
        setName(name);
        setEmail(email);
        setPasswordHash(passwordHash);
        this.role = Objects.requireNonNull(role, "User role cannot be null");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.active = active;
    }

    // POLYMORPHISM: Dynamic dispatch allows each subclass to specify its designated UI dashboard view identifier
    public abstract String getDashboardView();

    // POLYMORPHISM: Subclasses grant specialized granular permissions
    public abstract Set<String> getPermissions();

    // ENCAPSULATION: Validated setters and safe getters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("User name must be at least 2 characters long");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        this.passwordHash = passwordHash.trim();
    }

    public UserRole getRole() {
        return role;
    }

    protected void setRole(UserRole role) {
        this.role = Objects.requireNonNull(role, "Role cannot be null");
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // ABSTRACTION: Notifiable contract implementation
    @Override
    public String getRecipientEmail() {
        return email;
    }

    @Override
    public String getRecipientName() {
        return name;
    }

    @Override
    public void notifyUser(String title, String message) {
        // Concrete logging / dispatch hook for notification delivery
        System.out.printf("[NOTIFICATION to %s <%s>] %s: %s%n", name, email, title, message);
    }

    // ENCAPSULATION: Never expose sensitive passwordHash in string representations
    @Override
    public String toString() {
        return String.format("%s[id=%s, name='%s', email='%s', role=%s, active=%b]",
                getClass().getSimpleName(), id, name, email, role, active);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
