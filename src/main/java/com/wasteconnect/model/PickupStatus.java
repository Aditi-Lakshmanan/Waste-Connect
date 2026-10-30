package com.wasteconnect.model;

/**
 * Lifecycle status for custom scheduled citizen pickup requests.
 */
public enum PickupStatus {
    PENDING("Pending Scheduled Slot", "#E65100"),
    ACCEPTED("Accepted by Collector", "#1976D2"),
    COMPLETED("Completed & Logged", "#2E7D32"),
    CANCELLED("Cancelled", "#757575");

    private final String label;
    private final String colorHex;

    PickupStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getColorHex() {
        return colorHex;
    }
}
