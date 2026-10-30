package com.wasteconnect.pattern;

import com.wasteconnect.model.Bin;
import com.wasteconnect.model.Location;
import com.wasteconnect.model.RouteStop;
import com.wasteconnect.model.Vehicle;
import com.wasteconnect.model.Zone;

import java.util.List;

/**
 * CO2 & DESIGN PATTERN: Strategy pattern interface decoupling route calculation algorithms from the execution engine.
 */
public interface RoutingStrategy {

    // POLYMORPHISM: Concrete strategies compute stops via different heuristics
    List<RouteStop> calculateRoute(Zone zone, List<Bin> candidateBins, Vehicle vehicle, Location depotLocation);

    String getStrategyName();

    /**
     * Default calculation of total cumulative route distance including departure from and return to depot.
     */
    default double calculateTotalDistance(List<RouteStop> stops, Location depotLocation) {
        if (stops == null || stops.isEmpty() || depotLocation == null) {
            return 0.0;
        }
        double totalDistance = 0.0;
        Location current = depotLocation;

        for (RouteStop stop : stops) {
            totalDistance += current.distanceTo(stop.getLocation());
            current = stop.getLocation();
        }
        // Return to municipal depot/transfer station
        totalDistance += current.distanceTo(depotLocation);
        return Math.round(totalDistance * 100.0) / 100.0;
    }
}
