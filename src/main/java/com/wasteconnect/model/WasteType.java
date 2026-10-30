package com.wasteconnect.model;

/**
 * CO4: Enum with fields, constructor, and methods defining waste classifications,
 * operational urgency weights, visual cues, and default base points.
 */
public enum WasteType {
    WET("Wet Waste", "Organic and biodegradable kitchen waste", 1.2, "#2E7D32", 5),
    DRY("Dry Waste", "Recyclable paper, plastic, metal and cardboard", 1.0, "#0288D1", 10),
    HAZARDOUS("Hazardous Waste", "Sanitary, medical, paint and chemical waste", 2.0, "#D32F2F", 0),
    E_WASTE("Electronic Waste", "Discarded electronics, batteries and wiring", 1.5, "#7B1FA2", 20);

    private final String displayName;
    private final String description;
    private final double urgencyWeight;
    private final String colorHex;
    private final int basePointsPerKg;

    WasteType(String displayName, String description, double urgencyWeight, String colorHex, int basePointsPerKg) {
        this.displayName = displayName;
        this.description = description;
        this.urgencyWeight = urgencyWeight;
        this.colorHex = colorHex;
        this.basePointsPerKg = basePointsPerKg;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getUrgencyWeight() {
        return urgencyWeight;
    }

    public String getColorHex() {
        return colorHex;
    }

    public int getBasePointsPerKg() {
        return basePointsPerKg;
    }
}
