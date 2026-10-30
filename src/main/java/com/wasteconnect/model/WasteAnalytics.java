package com.wasteconnect.model;

/**
 * CO4: Java record aggregating macro operational indicators across the municipality.
 */
public record WasteAnalytics(
        double totalWetKg,
        double totalDryKg,
        double totalHazardousKg,
        double totalEWasteKg,
        double collectionEfficiencyPct,
        double averageSegregationRatePct,
        double complaintsResolvedPct,
        double estimatedDistanceSavedKm,
        double fuelCostSavedInr
) {}
