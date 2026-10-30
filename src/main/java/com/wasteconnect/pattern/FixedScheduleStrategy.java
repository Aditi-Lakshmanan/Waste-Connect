package com.wasteconnect.pattern;

import com.wasteconnect.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CO2 & DESIGN PATTERN: Concrete strategy implementing traditional fixed-schedule routing.
 * Bins are visited in rigid sequential / alphabetical order irrespective of real-time fill state.
 */
public class FixedScheduleStrategy implements RoutingStrategy {

    @Override
    public List<RouteStop> calculateRoute(Zone zone, List<Bin> candidateBins, Vehicle vehicle, Location depotLocation) {
        if (candidateBins == null || candidateBins.isEmpty()) {
            return List.of();
        }

        double maxPayload = vehicle != null ? vehicle.getCapacityKg() : 5000.0;
        double accumulatedKg = 0.0;
        List<RouteStop> stops = new ArrayList<>();

        // Traditional approach: order rigidly by bin code / geographic sequence
        List<Bin> sortedBins = new ArrayList<>(candidateBins);
        sortedBins.sort(Comparator.comparing(Bin::getBinCode));

        int seq = 1;
        for (Bin bin : sortedBins) {
            double demand = bin.getEstimatedVolume();
            if (accumulatedKg + demand > maxPayload) {
                // Vehicle capacity reached
                break;
            }
            stops.add(new RouteStop(
                    seq++,
                    bin.getId(),
                    bin.getLocation(),
                    demand,
                    bin.calculateScore(),
                    false,
                    "Fixed schedule visit: " + bin.getBinCode()
            ));
            accumulatedKg += demand;
        }
        return stops;
    }

    @Override
    public String getStrategyName() {
        return "Fixed-Schedule (Traditional)";
    }
}
