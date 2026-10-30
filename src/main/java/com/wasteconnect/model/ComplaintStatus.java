package com.wasteconnect.model;

/**
 * Enumeration of citizen complaint workflow lifecycle states.
 */
public enum ComplaintStatus {
    OPEN("Open", "#E65100"),
    ASSIGNED("Assigned", "#1976D2"),
    IN_PROGRESS("In Progress", "#FBC02D"),
    RESOLVED("Resolved", "#388E3C"),
    ESCALATED("Escalated", "#C2185B");

    private final String displayName;
    private final String colorHex;

    ComplaintStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public boolean isTerminal() {
        return this == RESOLVED;
    }
}
