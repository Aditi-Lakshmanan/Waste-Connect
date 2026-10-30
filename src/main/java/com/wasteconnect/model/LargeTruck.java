package com.wasteconnect.model;

/**
 * INHERITANCE: Heavy hydraulic compactor truck for bulk municipal collection.
 */
public class LargeTruck extends Vehicle {

    public LargeTruck(String id, String registrationNumber) {
        super(id, registrationNumber, 8000.0);
    }

    public LargeTruck(String id, String registrationNumber, double capacityKg) {
        super(id, registrationNumber, capacityKg);
    }

    // POLYMORPHISM: 4 km/L fuel efficiency
    @Override
    public double calculateFuelConsumption(double distanceKm) {
        return distanceKm / 4.0;
    }

    // POLYMORPHISM: Restricted on narrow residential alleyways
    @Override
    public boolean canNavigateNarrowStreet() {
        return false;
    }

    @Override
    public VehicleType getVehicleType() {
        return VehicleType.LARGE_TRUCK;
    }
}
