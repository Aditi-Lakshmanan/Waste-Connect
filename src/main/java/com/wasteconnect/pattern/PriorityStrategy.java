package com.wasteconnect.pattern;

import com.wasteconnect.model.*;

import java.util.*;

/**
 * CO2 & DESIGN PATTERN: Concrete strategy implementing dynamic priority-based routing.
 * Evaluates IoT bin fill levels, complaints, urgency, and optimizes stops using a PriorityQueue nearest-neighbor heuristic.
 */
public class PriorityStrategy implements RoutingStrategy {
    private final double minPriorityThreshold;

    public PriorityStrategy() {
        this(35.0); // Default score cutoff
    }

    public PriorityStrategy(double minPriorityThreshold) {
        this.minPriorityThreshold = minPriorityThreshold;
    }

    @Override
    public List<RouteStop> calculateRoute(Zone zone, List<Bin> candidateBins, Vehicle vehicle, Location depotLocation) {
        if (candidateBins == null || candidateBins.isEmpty()) {
            return List.of();
        }

        double maxPayload = vehicle != null ? vehicle.getCapacityKg() : 5000.0;
        Location start = (depotLocation != null) ? depotLocation : (zone != null ? zone.getCenterLocation() : new Location(12.9716, 77.5946, "Depot"));

        // 1. Filter bins needing urgent collection (high priority or fill level >= 60% or overflow/warning)
        List<Bin> eligibleBins = new ArrayList<>();
        for (Bin bin : candidateBins) {
            double score = bin.calculateScore();
            if (score >= minPriorityThreshold || bin.getCurrentFillPercentage() >= 60.0 || bin.getStatus() == BinStatus.OVERFLOW || bin.getStatus() == BinStatus.WARNING) {
                eligibleBins.add(bin);
            }
        }

        // If no urgent bins, select the top 20 highest filled bins
        if (eligibleBins.isEmpty()) {
            List<Bin> sortedByFill = new ArrayList<>(candidateBins);
            sortedByFill.sort((b1, b2) -> Double.compare(b2.getCurrentFillPercentage(), b1.getCurrentFillPercentage()));
            eligibleBins.addAll(sortedByFill.subList(0, Math.min(20, sortedByFill.size())));
        }

        // 2. Greedy PriorityQueue Nearest-Neighbor optimization
        // Balances shortest distance with highest priority score
        List<RouteStop> stops = new ArrayList<>();
        Set<String> visitedBinIds = new HashSet<>();
        Location currentLocation = start;
        double accumulatedWeight = 0.0;
        int seq = 1;

        while (visitedBinIds.size() < eligibleBins.size()) {
            // Find best next stop minimizing (distance / (priorityScore + 1))
            final Location currentRef = currentLocation;
            PriorityQueue<Bin> pq = new PriorityQueue<>((b1, b2) -> {
                double dist1 = currentRef.distanceTo(b1.getLocation());
                double dist2 = currentRef.distanceTo(b2.getLocation());
                double metric1 = dist1 / (b1.getPriorityScore() * 0.5 + 1.0);
                double metric2 = dist2 / (b2.getPriorityScore() * 0.5 + 1.0);
                return Double.compare(metric1, metric2);
            });

            for (Bin bin : eligibleBins) {
                if (!visitedBinIds.contains(bin.getId())) {
                    pq.add(bin);
                }
            }

            if (pq.isEmpty()) break;
            Bin nextBest = pq.poll();
            double demand = nextBest.getEstimatedVolume();

            if (accumulatedWeight + demand > maxPayload && !stops.isEmpty()) {
                // Vehicle capacity limit reached
                break;
            }

            visitedBinIds.add(nextBest.getId());
            stops.add(new RouteStop(
                    seq++,
                    nextBest.getId(),
                    nextBest.getLocation(),
                    demand,
                    nextBest.getPriorityScore(),
                    false,
                    String.format("Priority Stop (Fill: %.0f%%, Score: %.1f)", nextBest.getCurrentFillPercentage(), nextBest.getPriorityScore())
            ));
            accumulatedWeight += demand;
            currentLocation = nextBest.getLocation();
        }

        return stops;
    }

    @Override
    public String getStrategyName() {
        return "Priority-Based (Smart)";
    }
}
