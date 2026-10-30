package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Audit log entry capturing status transitions along a complaint lifecycle timeline.
 */
public class ComplaintEvent {
    private String id;
    private String complaintId;
    private ComplaintStatus fromStatus;
    private ComplaintStatus toStatus;
    private LocalDateTime timestamp;
    private String remarks;

    public ComplaintEvent(String id, String complaintId, ComplaintStatus fromStatus, ComplaintStatus toStatus, String remarks) {
        this(id, complaintId, fromStatus, toStatus, LocalDateTime.now(), remarks);
    }

    public ComplaintEvent(String id, String complaintId, ComplaintStatus fromStatus, ComplaintStatus toStatus, LocalDateTime timestamp, String remarks) {
        this.id = Objects.requireNonNull(id);
        this.complaintId = Objects.requireNonNull(complaintId);
        this.fromStatus = fromStatus;
        this.toStatus = Objects.requireNonNull(toStatus);
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.remarks = remarks != null ? remarks : "";
    }

    public String getId() {
        return id;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public ComplaintStatus getFromStatus() {
        return fromStatus;
    }

    public ComplaintStatus getToStatus() {
        return toStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getRemarks() {
        return remarks;
    }
}
