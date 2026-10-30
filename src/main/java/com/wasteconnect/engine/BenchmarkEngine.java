package com.wasteconnect.engine;

import com.wasteconnect.model.*;
import com.wasteconnect.pattern.FixedScheduleStrategy;
import com.wasteconnect.pattern.PriorityStrategy;
import com.wasteconnect.pattern.RoutingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Novelty Engine: Benchmark Engine.
 * Rigorously compares Traditional Fixed-Schedule routing against WasteConnect's
 * Priority-Based Dynamic routing on identical snapshot telemetry.
 */
public class BenchmarkEngine {

    /**
     * Executes both strategies on a snapshot of candidate bins and returns side-by-side BenchmarkResult records.
     */
    public BenchmarkComparison runBenchmark(Zone zone, List<Bin> candidateBins, Vehicle vehicle) {
        if (candidateBins == null || candidateBins.isEmpty()) {
            throw new IllegalArgumentException("Candidate bins list cannot be empty for benchmarking.");
        }

        // Create independent deep clones of the candidate bins snapshot
        List<Bin> snapshot1 = cloneBins(candidateBins);
        List<Bin> snapshot2 = cloneBins(candidateBins);

        // 1. Run Fixed Schedule Strategy
        RoutingStrategy fixedStrategy = new FixedScheduleStrategy();
        long startFixed = System.currentTimeMillis();
        List<RouteStop> fixedStops = fixedStrategy.calculateRoute(zone, snapshot1, vehicle, zone.getCenterLocation());
        double fixedDistance = fixedStrategy.calculateTotalDistance(fixedStops, zone.getCenterLocation());
        long timeFixed = System.currentTimeMillis() - startFixed;

        // 2. Run Priority-Based Dynamic Strategy
        RoutingStrategy priorityStrategy = new PriorityStrategy(35.0);
        long startPriority = System.currentTimeMillis();
        List<RouteStop> priorityStops = priorityStrategy.calculateRoute(zone, snapshot2, vehicle, zone.getCenterLocation());
        double priorityDistance = priorityStrategy.calculateTotalDistance(priorityStops, zone.getCenterLocation());
        long timePriority = System.currentTimeMillis() - startPriority;

        // Evaluate Metrics
        BenchmarkResult fixedResult = evaluateStrategy("Fixed-Schedule (Traditional)", fixedStops, candidateBins, fixedDistance, timeFixed);
        BenchmarkResult priorityResult = evaluateStrategy("Priority-Based (WasteConnect)", priorityStops, candidateBins, priorityDistance, timePriority);

        return new BenchmarkComparison(fixedResult, priorityResult);
    }

    private BenchmarkResult evaluateStrategy(String strategyName, List<RouteStop> stops, List<Bin> totalBins, double distanceKm, long execTimeMs) {
        Set<String> collectedBinIds = stops.stream().map(RouteStop::getStopId).collect(Collectors.toSet());

        int missedUrgentPickups = 0;
        int overflowsPrevented = 0;
        double totalFillCollected = 0.0;

        for (Bin b : totalBins) {
            boolean wasVisited = collectedBinIds.contains(b.getId());
            boolean isCritical = (b.getCurrentFillPercentage() >= 80.0 || b.getStatus() == BinStatus.OVERFLOW);

            if (isCritical && !wasVisited) {
                missedUrgentPickups++;
            }
            if (isCritical && wasVisited) {
                overflowsPrevented++;
            }
            if (wasVisited) {
                totalFillCollected += b.getCurrentFillPercentage();
            }
        }

        double avgFillAtCollection = stops.isEmpty() ? 0.0 : (totalFillCollected / stops.size());

        return new BenchmarkResult(
                strategyName,
                Math.round(distanceKm * 100.0) / 100.0,
                totalBins.size(),
                stops.size(),
                missedUrgentPickups,
                overflowsPrevented,
                Math.round(avgFillAtCollection * 10.0) / 10.0,
                Math.max(1, execTimeMs)
        );
    }

    private List<Bin> cloneBins(List<Bin> source) {
        List<Bin> list = new ArrayList<>();
        for (Bin b : source) {
            Bin clone = new Bin(
                    b.getId(),
                    b.getBinCode(),
                    b.getZoneId(),
                    b.getLocation(),
                    b.getWasteType(),
                    b.getCapacityLiters(),
                    b.getCurrentFillPercentage(),
                    b.getLastCollectedAt(),
                    b.getStatus(),
                    b.getOpenComplaintsCount()
            );
            list.add(clone);
        }
        return list;
    }

    public record BenchmarkComparison(BenchmarkResult fixedResult, BenchmarkResult priorityResult) {
        public double getDistanceSavedPercentage() {
            if (fixedResult.totalDistanceKm() <= 0) return 0.0;
            double diff = fixedResult.totalDistanceKm() - priorityResult.totalDistanceKm();
            return Math.max(0.0, Math.round((diff / fixedResult.totalDistanceKm() * 100.0) * 10.0) / 10.0);
        }

        public int getMissedPickupsReduction() {
            return Math.max(0, fixedResult.missedPickups() - priorityResult.missedPickups());
        }

        public double getEfficiencyGainPercentage() {
            if (fixedResult.averageFillAtCollection() <= 0) return 0.0;
            double diff = priorityResult.averageFillAtCollection() - fixedResult.averageFillAtCollection();
            return Math.max(0.0, Math.round((diff / fixedResult.averageFillAtCollection() * 100.0) * 10.0) / 10.0);
        }
    }
}
