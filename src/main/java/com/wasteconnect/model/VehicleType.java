package com.wasteconnect.model;

/**
 * Categorization of municipal collection vehicles.
 */
public enum VehicleType {
    SMALL_VEHICLE("Small Vehicle / Auto Tipper", 1500.0, 12.0, "Narrow alleyways and residential lanes"),
    LARGE_TRUCK("Large Hydraulic Compactor", 8000.0, 4.0, "Arterial roads, commercial bins and transfer stations");

    private final String displayName;
    private final double capacityKg;
    private final double fuelEfficiencyKmPerL;
    private final String typicalUsage;

    VehicleType(String displayName, double capacityKg, double fuelEfficiencyKmPerL, String typicalUsage) {
        this.displayName = displayName;
        this.capacityKg = capacityKg;
        this.fuelEfficiencyKmPerL = fuelEfficiencyKmPerL;
        this.typicalUsage = typicalUsage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getCapacityKg() {
        return capacityKg;
    }

    public double getFuelEfficiencyKmPerL() {
        return fuelEfficiencyKmPerL;
    }

    public String getTypicalUsage() {
        return typicalUsage;
    }
}
