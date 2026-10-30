package com.wasteconnect.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an environmental complaint raised by a citizen.
 * Implements SLA timeline checks and CSV export.
 */
public class Complaint implements Exportable {
    private String id;
    private String citizenId;
    private ComplaintType complaintType;
    private String description;
    private Location location;
    private String zoneId;
    private ComplaintStatus status;
    private String assignedCollectorId;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;

    public Complaint(String id, String citizenId, ComplaintType complaintType, String description, Location location, String zoneId) {
        this(id, citizenId, complaintType, description, location, zoneId, ComplaintStatus.OPEN, null, LocalDateTime.now(), null, "");
    }

    public Complaint(String id, String citizenId, ComplaintType complaintType, String description, Location location,
                     String zoneId, ComplaintStatus status, String assignedCollectorId,
                     LocalDateTime createdAt, LocalDateTime resolvedAt, String resolutionNotes) {
        this.id = Objects.requireNonNull(id);
        this.citizenId = Objects.requireNonNull(citizenId);
        this.complaintType = Objects.requireNonNull(complaintType);
        this.description = Objects.requireNonNull(description);
        this.location = Objects.requireNonNull(location);
        this.zoneId = Objects.requireNonNull(zoneId);
        this.status = status != null ? status : ComplaintStatus.OPEN;
        this.assignedCollectorId = assignedCollectorId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.resolvedAt = resolvedAt;
        this.resolutionNotes = resolutionNotes != null ? resolutionNotes : "";
    }

    /**
     * Verifies if this complaint has breached its SLA resolution threshold.
     */
    public boolean isSlaBreached() {
        if (status == ComplaintStatus.RESOLVED) {
            return false;
        }
        long hoursElapsed = Duration.between(createdAt, LocalDateTime.now()).toHours();
        return hoursElapsed >= complaintType.getSlaHours();
    }

    public double getHoursElapsed() {
        return Math.max(0.1, Duration.between(createdAt, LocalDateTime.now()).toMinutes() / 60.0);
    }

    public void resolve(String notes) {
        this.status = ComplaintStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes != null ? notes : "Resolved by field operative";
    }

    public void escalate() {
        this.status = ComplaintStatus.ESCALATED;
    }

    // ABSTRACTION: Exportable implementation
    @Override
    public String[] getCsvHeaders() {
        return new String[]{"ComplaintId", "CitizenId", "Type", "Status", "ZoneId", "Latitude", "Longitude", "Created", "Resolved", "SlaBreached"};
    }

    @Override
    public String toCsvRow() {
        return String.format("%s,%s,%s,%s,%s,%.6f,%.6f,%s,%s,%b",
                id, citizenId, complaintType.name(), status.name(), zoneId,
                location.getLatitude(), location.getLongitude(), createdAt, resolvedAt, isSlaBreached());
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

    public ComplaintType getComplaintType() {
        return complaintType;
    }

    public void setComplaintType(ComplaintType complaintType) {
        this.complaintType = complaintType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public String getAssignedCollectorId() {
        return assignedCollectorId;
    }

    public void setAssignedCollectorId(String assignedCollectorId) {
        this.assignedCollectorId = assignedCollectorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
