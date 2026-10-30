package com.wasteconnect.model;

import java.util.Objects;

/**
 * ABSTRACTION: Abstract base class representing municipal fleet vehicles.
 * Encapsulates registration number, payload capacities, and status.
 */
public abstract class Vehicle {
    private String id;
    private String registrationNumber;
    private double capacityKg;
    private double currentPayloadKg;
    private boolean active;

    public Vehicle(String id, String registrationNumber, double capacityKg) {
        this.id = Objects.requireNonNull(id);
        this.registrationNumber = Objects.requireNonNull(registrationNumber);
        setCapacityKg(capacityKg);
        this.currentPayloadKg = 0.0;
        this.active = true;
    }

    // POLYMORPHISM: Subclasses declare distinct operational fuel efficiencies
    public abstract double calculateFuelConsumption(double distanceKm);

    // POLYMORPHISM: Physical maneuverability differs by chassis size
    public abstract boolean canNavigateNarrowStreet();

    public abstract VehicleType getVehicleType();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public double getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(double capacityKg) {
        if (capacityKg <= 0.0) {
            throw new IllegalArgumentException("Capacity must be positive: " + capacityKg);
        }
        this.capacityKg = capacityKg;
    }

    public double getCurrentPayloadKg() {
        return currentPayloadKg;
    }

    public void loadPayload(double weightKg) {
        if (weightKg < 0) {
            throw new IllegalArgumentException("Cannot load negative weight: " + weightKg);
        }
        if (this.currentPayloadKg + weightKg > this.capacityKg) {
            throw new IllegalStateException("Exceeds vehicle max capacity of " + capacityKg + " kg!");
        }
        this.currentPayloadKg += weightKg;
    }

    public void resetPayload() {
        this.currentPayloadKg = 0.0;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("%s [%s - %s] Load: %.1f / %.1f kg",
                getClass().getSimpleName(), id, registrationNumber, currentPayloadKg, capacityKg);
    }
}
