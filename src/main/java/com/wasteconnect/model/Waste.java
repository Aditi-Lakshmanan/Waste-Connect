package com.wasteconnect.model;

import java.util.Objects;

/**
 * ABSTRACTION: Abstract base class representing waste material.
 * Defines contract for polymorphic reward point calculation and environmental handling procedures.
 * ENCAPSULATION: Validates weight and preserves waste category classification.
 */
public abstract class Waste {
    private String id;
    private WasteType type;
    private double weightKg;

    public Waste(String id, WasteType type, double weightKg) {
        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
        this.type = Objects.requireNonNull(type, "Waste type cannot be null");
        setWeightKg(weightKg);
    }

    // POLYMORPHISM: Subclasses calculate rewards dynamically based on commodity economics and environmental impact
    public abstract int calculateRewardPoints(double kg);

    // POLYMORPHISM: Subclasses supply specialized handling guidelines for collectors and sorting facilities
    public abstract String getHandlingInstructions();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public WasteType getType() {
        return type;
    }

    protected void setType(WasteType type) {
        this.type = Objects.requireNonNull(type);
    }

    public double getWeightKg() {
        return weightKg;
    }

    // ENCAPSULATION: Rejects negative weight measurements
    public void setWeightKg(double weightKg) {
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("Waste weight cannot be negative: " + weightKg);
        }
        this.weightKg = weightKg;
    }

    @Override
    public String toString() {
        return String.format("%s [id=%s, type=%s, weight=%.2f kg]", getClass().getSimpleName(), id, type, weightKg);
    }
}
