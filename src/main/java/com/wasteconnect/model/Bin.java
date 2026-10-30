package com.wasteconnect.model;

import com.wasteconnect.exception.BinOverflowException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Entity representing an IoT-enabled smart waste bin.
 * ABSTRACTION: Implements Routable, Scorable, Exportable.
 * ENCAPSULATION: Rejects fill levels outside 0.0 to 100.0.
 */
public class Bin implements Routable, Scorable, Exportable {
    private String id;
    private String binCode;
    private String zoneId;
    private Location location;
    private WasteType wasteType;
    private double capacityLiters;
    private double currentFillPercentage; // 0.0 to 100.0
    private LocalDateTime lastCollectedAt;
    private BinStatus status;
    private int openComplaintsCount;
    private double calculatedPriority;

    public Bin(String id, String binCode, String zoneId, Location location, WasteType wasteType, double capacityLiters) {
        this(id, binCode, zoneId, location, wasteType, capacityLiters, 0.0, LocalDateTime.now().minusDays(1), BinStatus.NORMAL, 0);
    }

    public Bin(String id, String binCode, String zoneId, Location location, WasteType wasteType,
               double capacityLiters, double currentFillPercentage, LocalDateTime lastCollectedAt,
               BinStatus status, int openComplaintsCount) {
        this.id = Objects.requireNonNull(id);
        this.binCode = Objects.requireNonNull(binCode);
        this.zoneId = Objects.requireNonNull(zoneId);
        this.location = Objects.requireNonNull(location);
        this.wasteType = Objects.requireNonNull(wasteType);
        this.capacityLiters = Math.max(50.0, capacityLiters);
        this.lastCollectedAt = lastCollectedAt != null ? lastCollectedAt : LocalDateTime.now();
        this.status = status != null ? status : BinStatus.NORMAL;
        this.openComplaintsCount = Math.max(0, openComplaintsCount);
        if (currentFillPercentage < 0.0 || currentFillPercentage > 100.0) {
            throw new IllegalArgumentException(String.format("Bin fill level must be between 0.0 and 100.0%%. Received: %.2f", currentFillPercentage));
        }
        this.currentFillPercentage = currentFillPercentage;
        updateStatusFromFillLevel();
    }

    // ENCAPSULATION: Validated setter ensuring strict adherence to physical limits [0, 100]
    public void setFillLevel(double fillPercentage) {
        if (fillPercentage < 0.0 || fillPercentage > 100.0) {
            throw new IllegalArgumentException(String.format("Bin fill level must be between 0.0 and 100.0%%. Received: %.2f", fillPercentage));
        }
        this.currentFillPercentage = fillPercentage;
        updateStatusFromFillLevel();
        if (fillPercentage >= 100.0) {
            // Signal overflow event
            throw new BinOverflowException(this.binCode, fillPercentage);
        }
    }

    /**
     * Internal state transition based on current capacity utilization.
     */
    private void updateStatusFromFillLevel() {
        if (this.currentFillPercentage >= 100.0) {
            this.status = BinStatus.OVERFLOW;
        } else if (this.currentFillPercentage >= 80.0) {
            this.status = BinStatus.WARNING;
        } else if (this.status != BinStatus.MAINTENANCE) {
            this.status = BinStatus.NORMAL;
        }
    }

    public void emptyBin() {
        this.currentFillPercentage = 0.0;
        this.lastCollectedAt = LocalDateTime.now();
        this.status = BinStatus.NORMAL;
        this.openComplaintsCount = 0;
    }

    public double getDaysSinceLastPickup() {
        if (lastCollectedAt == null) return 1.0;
        long hours = ChronoUnit.HOURS.between(lastCollectedAt, LocalDateTime.now());
        return Math.max(0.1, hours / 24.0);
    }

    // ABSTRACTION: Routable implementation
    @Override
    public String getId() {
        return id;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public double getPriorityScore() {
        return calculatedPriority > 0 ? calculatedPriority : calculateScore();
    }

    public void setCalculatedPriority(double priority) {
        this.calculatedPriority = priority;
    }

    @Override
    public double getEstimatedVolume() {
        // Density approx: Wet=0.4 kg/L, Dry=0.15 kg/L, E-Waste=0.5 kg/L, Hazardous=0.3 kg/L
        double density;
        switch (wasteType) {
            case WET -> density = 0.40;
            case DRY -> density = 0.15;
            case HAZARDOUS -> density = 0.30;
            case E_WASTE -> density = 0.50;
            default -> density = 0.25;
        }
        return (capacityLiters * (currentFillPercentage / 100.0)) * density;
    }

    // ABSTRACTION: Scorable implementation
    @Override
    public double calculateScore() {
        // Priority Score Formula: w1*fillLevel + w2*daysSinceLastPickup + w3*openComplaints + w4*wasteUrgency
        double score = (0.45 * currentFillPercentage)
                + (0.25 * Math.min(10.0, getDaysSinceLastPickup() * 20.0))
                + (0.15 * Math.min(10.0, openComplaintsCount * 25.0))
                + (0.15 * (wasteType.getUrgencyWeight() * 30.0));
        this.calculatedPriority = Math.max(0.0, Math.min(100.0, score));
        return this.calculatedPriority;
    }

    @Override
    public double getScore() {
        return calculatedPriority;
    }

    // ABSTRACTION: Exportable implementation
    @Override
    public String[] getCsvHeaders() {
        return new String[]{"BinId", "BinCode", "ZoneId", "Latitude", "Longitude", "WasteType", "CapacityL", "FillLevel%", "Status", "DaysSincePickup"};
    }

    @Override
    public String toCsvRow() {
        return String.format("%s,%s,%s,%.6f,%.6f,%s,%.1f,%.1f,%s,%.2f",
                id, binCode, zoneId, location.getLatitude(), location.getLongitude(),
                wasteType.name(), capacityLiters, currentFillPercentage, status.name(), getDaysSinceLastPickup());
    }

    // Getters and Setters
    public void setId(String id) {
        this.id = id;
    }

    public String getBinCode() {
        return binCode;
    }

    public void setBinCode(String binCode) {
        this.binCode = binCode;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public WasteType getWasteType() {
        return wasteType;
    }

    public void setWasteType(WasteType wasteType) {
        this.wasteType = wasteType;
    }

    public double getCapacityLiters() {
        return capacityLiters;
    }

    public void setCapacityLiters(double capacityLiters) {
        this.capacityLiters = Math.max(50.0, capacityLiters);
    }

    public double getCurrentFillPercentage() {
        return currentFillPercentage;
    }

    public LocalDateTime getLastCollectedAt() {
        return lastCollectedAt;
    }

    public void setLastCollectedAt(LocalDateTime lastCollectedAt) {
        this.lastCollectedAt = lastCollectedAt;
    }

    public BinStatus getStatus() {
        return status;
    }

    public void setStatus(BinStatus status) {
        this.status = status;
    }

    public int getOpenComplaintsCount() {
        return openComplaintsCount;
    }

    public void setOpenComplaintsCount(int openComplaintsCount) {
        this.openComplaintsCount = Math.max(0, openComplaintsCount);
    }

    @Override
    public String toString() {
        return String.format("%s (%s - %.0f%%)", binCode, wasteType.getDisplayName(), currentFillPercentage);
    }
}
