package com.wasteconnect.model;

/**
 * Enumeration of physical smart bin operational statuses.
 */
public enum BinStatus {
    NORMAL("Normal", "#388E3C"),
    WARNING("Warning (>80%)", "#F57C00"),
    OVERFLOW("Overflow (100%)", "#D32F2F"),
    MAINTENANCE("Under Maintenance", "#757575");

    private final String label;
    private final String colorHex;

    BinStatus(String label, String colorHex) {
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
