package com.wasteconnect.model;

/**
 * Enumeration of system user roles governing dashboard rendering and permission sets.
 */
public enum UserRole {
    CITIZEN("Citizen", "Resident managing domestic waste and reporting complaints"),
    COLLECTOR("Collector", "Field operative responsible for driving routes and collecting waste"),
    ADMIN("Administrator", "Municipal supervisor managing zones, assets, routing and analytics"),
    WASTE_PICKER("Waste Picker", "Informal recycling partner collecting dry recyclables");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
