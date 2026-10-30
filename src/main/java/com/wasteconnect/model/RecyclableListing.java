package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing dry recyclable materials offered by citizens for waste picker collection.
 */
public class RecyclableListing {
    private String id;
    private String citizenId;
    private WasteType wasteType;
    private double weightKg;
    private String address;
    private Location location;
    private ListingStatus status;
    private String claimedPickerId;
    private LocalDateTime createdAt;
    private LocalDateTime collectedAt;
    private double estimatedValue;

    public RecyclableListing(String id, String citizenId, WasteType wasteType, double weightKg,
                             String address, Location location, double estimatedValue) {
        this(id, citizenId, wasteType, weightKg, address, location, ListingStatus.AVAILABLE, null, LocalDateTime.now(), null, estimatedValue);
    }

    public RecyclableListing(String id, String citizenId, WasteType wasteType, double weightKg,
                             String address, Location location, ListingStatus status,
                             String claimedPickerId, LocalDateTime createdAt, LocalDateTime collectedAt, double estimatedValue) {
        this.id = Objects.requireNonNull(id);
        this.citizenId = Objects.requireNonNull(citizenId);
        this.wasteType = Objects.requireNonNull(wasteType);
        this.weightKg = Math.max(0.1, weightKg);
        this.address = Objects.requireNonNull(address);
        this.location = Objects.requireNonNull(location);
        this.status = status != null ? status : ListingStatus.AVAILABLE;
        this.claimedPickerId = claimedPickerId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.collectedAt = collectedAt;
        this.estimatedValue = estimatedValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public WasteType getWasteType() {
        return wasteType;
    }

    public void setWasteType(WasteType wasteType) {
        this.wasteType = wasteType;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = Math.max(0.1, weightKg);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public String getClaimedPickerId() {
        return claimedPickerId;
    }

    public void setClaimedPickerId(String claimedPickerId) {
        this.claimedPickerId = claimedPickerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public double getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
}
