package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * INHERITANCE: Citizen is a specialized User residing in a household and participating in waste segregation.
 * ENCAPSULATION: Point operations strictly validate amounts and prevent negative balances or invalid updates.
 */
public class Citizen extends User implements Scorable {
    private int streak;
    private double segregationScore; // 0.0 to 100.0
    private int pointsBalance;
    private String address;
    private String phone;
    private String zoneId;
    private String householdId;

    public Citizen(String id, String name, String email, String passwordHash, String address, String phone, String zoneId) {
        super(id, name, email, passwordHash, UserRole.CITIZEN);
        this.address = address;
        this.phone = phone;
        this.zoneId = zoneId;
        this.streak = 0;
        this.segregationScore = 100.0;
        this.pointsBalance = 50; // Welcome bonus points
    }

    public Citizen(String id, String name, String email, String passwordHash, LocalDateTime createdAt, boolean active,
                   int streak, double segregationScore, int pointsBalance, String address, String phone, String zoneId, String householdId) {
        super(id, name, email, passwordHash, UserRole.CITIZEN, createdAt, active);
        this.streak = Math.max(0, streak);
        setSegregationScore(segregationScore);
        this.pointsBalance = Math.max(0, pointsBalance);
        this.address = address;
        this.phone = phone;
        this.zoneId = zoneId;
        this.householdId = householdId;
    }

    // POLYMORPHISM: Dynamic dispatch returns Citizen specific dashboard view
    @Override
    public String getDashboardView() {
        return "CITIZEN_DASHBOARD";
    }

    // POLYMORPHISM: Granular citizen-level permissions
    @Override
    public Set<String> getPermissions() {
        // ENCAPSULATION: Defensive copy returned to prevent external state tampering
        return Collections.unmodifiableSet(new HashSet<>(Set.of(
                "RAISE_COMPLAINT", "VIEW_COMPLAINT", "REQUEST_PICKUP",
                "REDEEM_POINTS", "LIST_RECYCLABLES", "VIEW_LEADERBOARD"
        )));
    }

    // ENCAPSULATION: Validated mutators for points
    public void addPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Cannot add negative reward points: " + points);
        }
        this.pointsBalance += points;
    }

    public void deductPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Cannot deduct negative reward points: " + points);
        }
        if (points > this.pointsBalance) {
            throw new IllegalStateException("Insufficient reward points balance. Available: " + this.pointsBalance + ", Requested: " + points);
        }
        this.pointsBalance -= points;
    }

    public void recordSegregationEvaluation(SegregationQuality quality) {
        if (quality == SegregationQuality.GOOD) {
            this.streak += 1;
            addPoints(quality.getBonusPoints());
            // Rolling exponential average update
            this.segregationScore = Math.min(100.0, this.segregationScore * 0.85 + 100.0 * 0.15);
        } else if (quality == SegregationQuality.PARTIAL) {
            addPoints(quality.getBonusPoints());
            this.segregationScore = this.segregationScore * 0.90 + 50.0 * 0.10;
        } else {
            // Mixed waste: streak reset
            this.streak = 0;
            if (this.pointsBalance >= Math.abs(quality.getBonusPoints())) {
                this.pointsBalance += quality.getBonusPoints(); // negative bonus
            }
            this.segregationScore = Math.max(0.0, this.segregationScore * 0.80);
        }
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = Math.max(0, streak);
    }

    public double getSegregationScore() {
        return segregationScore;
    }

    public void setSegregationScore(double segregationScore) {
        if (segregationScore < 0.0 || segregationScore > 100.0) {
            throw new IllegalArgumentException("Segregation score must be between 0.0 and 100.0, got: " + segregationScore);
        }
        this.segregationScore = segregationScore;
    }

    public int getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(int pointsBalance) {
        if (pointsBalance < 0) {
            throw new IllegalArgumentException("Points balance cannot be negative");
        }
        this.pointsBalance = pointsBalance;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(String householdId) {
        this.householdId = householdId;
    }

    // ABSTRACTION: Scorable implementation
    @Override
    public double calculateScore() {
        return (this.segregationScore * 0.7) + (Math.min(10, this.streak) * 3.0);
    }

    @Override
    public double getScore() {
        return this.segregationScore;
    }
}
