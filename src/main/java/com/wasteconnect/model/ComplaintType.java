package com.wasteconnect.model;

/**
 * Enumeration of citizen complaint categories with SLA hour limits.
 */
public enum ComplaintType {
    MISSED_PICKUP("Missed Pickup", 4, 1.5),
    OVERFLOWING_BIN("Overflowing Public Bin", 2, 2.0),
    ILLEGAL_DUMPING("Illegal Roadside Dumping", 6, 2.5),
    DAMAGED_BIN("Damaged / Broken Bin", 12, 1.0),
    OTHER("Other Concern", 8, 1.0);

    private final String label;
    private final int slaHours;
    private final double urgencyWeight;

    ComplaintType(String label, int slaHours, double urgencyWeight) {
        this.label = label;
        this.slaHours = slaHours;
        this.urgencyWeight = urgencyWeight;
    }

    public String getLabel() {
        return label;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public double getUrgencyWeight() {
        return urgencyWeight;
    }
}
