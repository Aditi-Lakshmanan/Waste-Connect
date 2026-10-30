package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an asynchronous notification message for system users.
 */
public class Notification {
    private String id;
    private String userId;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification(String id, String userId, String title, String message) {
        this(id, userId, title, message, false, LocalDateTime.now());
    }

    public Notification(String id, String userId, String title, String message, boolean read, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.title = Objects.requireNonNull(title);
        this.message = Objects.requireNonNull(message);
        this.read = read;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
