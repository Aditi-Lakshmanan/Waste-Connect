package com.wasteconnect.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an on-demand domestic pickup request raised by a citizen.
 */
public class PickupRequest {
    private String id;
    private String citizenId;
    private WasteType wasteType;
    private double approximateWeightKg;
    private LocalDate preferredSlot;
    private String address;
    private PickupStatus status;
    private LocalDateTime createdAt;
    private String assignedCollectorId;

    public PickupRequest(String id, String citizenId, WasteType wasteType, double approximateWeightKg,
                         LocalDate preferredSlot, String address) {
        this(id, citizenId, wasteType, approximateWeightKg, preferredSlot, address, PickupStatus.PENDING, LocalDateTime.now(), null);
    }

    public PickupRequest(String id, String citizenId, WasteType wasteType, double approximateWeightKg,
                         LocalDate preferredSlot, String address, PickupStatus status,
                         LocalDateTime createdAt, String assignedCollectorId) {
        this.id = Objects.requireNonNull(id);
        this.citizenId = Objects.requireNonNull(citizenId);
        this.wasteType = Objects.requireNonNull(wasteType);
        this.approximateWeightKg = Math.max(0.1, approximateWeightKg);
        this.preferredSlot = preferredSlot != null ? preferredSlot : LocalDate.now().plusDays(1);
        this.address = Objects.requireNonNull(address);
        this.status = status != null ? status : PickupStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.assignedCollectorId = assignedCollectorId;
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

    public double getApproximateWeightKg() {
        return approximateWeightKg;
    }

    public void setApproximateWeightKg(double approximateWeightKg) {
        this.approximateWeightKg = Math.max(0.1, approximateWeightKg);
    }

    public LocalDate getPreferredSlot() {
        return preferredSlot;
    }

    public void setPreferredSlot(LocalDate preferredSlot) {
        this.preferredSlot = preferredSlot;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public PickupStatus getStatus() {
        return status;
    }

    public void setStatus(PickupStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAssignedCollectorId() {
        return assignedCollectorId;
    }

    public void setAssignedCollectorId(String assignedCollectorId) {
        this.assignedCollectorId = assignedCollectorId;
    }
}
