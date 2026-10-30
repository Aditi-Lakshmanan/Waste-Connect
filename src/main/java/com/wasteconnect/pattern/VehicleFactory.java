package com.wasteconnect.pattern;

import com.wasteconnect.model.LargeTruck;
import com.wasteconnect.model.SmallVehicle;
import com.wasteconnect.model.Vehicle;
import com.wasteconnect.model.VehicleType;

import java.util.UUID;

/**
 * CO2 & DESIGN PATTERN: Factory pattern for generating municipal fleet vehicles.
 */
public final class VehicleFactory {

    private VehicleFactory() {}

    public static Vehicle createVehicle(VehicleType type, String regNum, double capacityKg) {
        String id = "VEH-" + UUID.randomUUID().toString().substring(0, 8);
        return switch (type) {
            case LARGE_TRUCK -> new LargeTruck(id, regNum, capacityKg);
            case SMALL_VEHICLE -> new SmallVehicle(id, regNum, capacityKg);
        };
    }
}
