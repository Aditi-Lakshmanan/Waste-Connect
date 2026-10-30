package com.wasteconnect.model;

import java.util.Objects;

/**
 * ENCAPSULATION: Immutable value object representing an ordered stop along an optimized collection route.
 */
public final class RouteStop {
    private final int sequence;
    private final String stopId;
    private final Location location;
    private final double demandKg;
    private final double priorityScore;
    private final boolean collected;
    private final String notes;

    public RouteStop(int sequence, String stopId, Location location, double demandKg, double priorityScore, boolean collected, String notes) {
        if (sequence < 0) {
            throw new IllegalArgumentException("Sequence must be non-negative: " + sequence);
        }
        this.sequence = sequence;
        this.stopId = Objects.requireNonNull(stopId, "Stop ID cannot be null");
        this.location = Objects.requireNonNull(location, "Location cannot be null");
        this.demandKg = Math.max(0.0, demandKg);
        this.priorityScore = priorityScore;
        this.collected = collected;
        this.notes = notes == null ? "" : notes;
    }

    public int getSequence() {
        return sequence;
    }

    public String getStopId() {
        return stopId;
    }

    public Location getLocation() {
        return location;
    }

    public double getDemandKg() {
        return demandKg;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public boolean isCollected() {
        return collected;
    }

    public String getNotes() {
        return notes;
    }

    public RouteStop withCollected(boolean isCollected) {
        return new RouteStop(this.sequence, this.stopId, this.location, this.demandKg, this.priorityScore, isCollected, this.notes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteStop routeStop)) return false;
        return sequence == routeStop.sequence &&
                Double.compare(routeStop.demandKg, demandKg) == 0 &&
                Double.compare(routeStop.priorityScore, priorityScore) == 0 &&
                collected == routeStop.collected &&
                Objects.equals(stopId, routeStop.stopId) &&
                Objects.equals(location, routeStop.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, stopId, location, demandKg, priorityScore, collected);
    }

    @Override
    public String toString() {
        return String.format("#%d - %s [%.1f kg, Score: %.1f, Collected: %b]", sequence, stopId, demandKg, priorityScore, collected);
    }
}
