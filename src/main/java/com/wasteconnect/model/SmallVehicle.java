package com.wasteconnect.model;

/**
 * INHERITANCE: Small light commercial vehicle or three-wheeler tipper.
 */
public class SmallVehicle extends Vehicle {

    public SmallVehicle(String id, String registrationNumber) {
        super(id, registrationNumber, 1500.0);
    }

    public SmallVehicle(String id, String registrationNumber, double capacityKg) {
        super(id, registrationNumber, capacityKg);
    }

    // POLYMORPHISM: 12 km/L fuel efficiency
    @Override
    public double calculateFuelConsumption(double distanceKm) {
        return distanceKm / 12.0;
    }

    // POLYMORPHISM: Maneuvers compact residential lanes
    @Override
    public boolean canNavigateNarrowStreet() {
        return true;
    }

    @Override
    public VehicleType getVehicleType() {
        return VehicleType.SMALL_VEHICLE;
    }
}
