package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Audit log recording collector verification of household waste segregation compliance.
 */
public class SegregationLog {
    private String id;
    private String citizenId;
    private String collectorId;
    private String binId;
    private SegregationQuality quality;
    private double weightKg;
    private int pointsAwarded;
    private LocalDateTime loggedAt;

    public SegregationLog(String id, String citizenId, String collectorId, String binId,
                          SegregationQuality quality, double weightKg, int pointsAwarded) {
        this(id, citizenId, collectorId, binId, quality, weightKg, pointsAwarded, LocalDateTime.now());
    }

    public SegregationLog(String id, String citizenId, String collectorId, String binId,
                          SegregationQuality quality, double weightKg, int pointsAwarded, LocalDateTime loggedAt) {
        this.id = Objects.requireNonNull(id);
        this.citizenId = Objects.requireNonNull(citizenId);
        this.collectorId = Objects.requireNonNull(collectorId);
        this.binId = binId;
        this.quality = Objects.requireNonNull(quality);
        this.weightKg = Math.max(0.0, weightKg);
        this.pointsAwarded = pointsAwarded;
        this.loggedAt = loggedAt != null ? loggedAt : LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public String getBinId() {
        return binId;
    }

    public SegregationQuality getQuality() {
        return quality;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
}
