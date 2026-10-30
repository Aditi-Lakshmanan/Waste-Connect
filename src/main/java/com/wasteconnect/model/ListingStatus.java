package com.wasteconnect.model;

/**
 * Status of dry recyclable listings submitted by citizens for informal waste pickers.
 */
public enum ListingStatus {
    AVAILABLE("Available for Pickup", "#1976D2"),
    CLAIMED("Claimed by Picker", "#F57C00"),
    COLLECTED("Collected & Paid", "#2E7D32"),
    CANCELLED("Cancelled", "#757575");

    private final String label;
    private final String colorHex;

    ListingStatus(String label, String colorHex) {
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
