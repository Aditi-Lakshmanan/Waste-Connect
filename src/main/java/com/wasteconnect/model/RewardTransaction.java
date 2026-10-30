package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an atomic credit or debit to a citizen's environmental reward wallet.
 */
public class RewardTransaction {
    public enum Type {
        EARNED, REDEEMED, WELCOME_BONUS, PENALTY
    }

    private String id;
    private String citizenId;
    private Type type;
    private int points; // positive for EARNED/BONUS, negative for REDEEMED/PENALTY
    private String description;
    private LocalDateTime timestamp;

    public RewardTransaction(String id, String citizenId, Type type, int points, String description) {
        this(id, citizenId, type, points, description, LocalDateTime.now());
    }

    public RewardTransaction(String id, String citizenId, Type type, int points, String description, LocalDateTime timestamp) {
        this.id = Objects.requireNonNull(id);
        this.citizenId = Objects.requireNonNull(citizenId);
        this.type = Objects.requireNonNull(type);
        this.points = points;
        this.description = Objects.requireNonNull(description);
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public Type getType() {
        return type;
    }

    public int getPoints() {
        return points;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
