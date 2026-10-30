package com.wasteconnect.model;

/**
 * CO4: Java record representing comparative algorithmic benchmark metrics
 * between traditional fixed-schedule and priority-based dynamic routing.
 */
public record BenchmarkResult(
        String strategyName,
        double totalDistanceKm,
        int totalStopsEvaluated,
        int stopsCollected,
        int missedPickups,
        int overflowIncidentsPrevented,
        double averageFillAtCollection,
        long executionTimeMs
) {}
