package com.wasteconnect.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing an optimized waste collection route assigned to a vehicle and collector.
 * ENCAPSULATION: Maintains defensive copy of route stops.
 */
public class Route {
    private String id;
    private String zoneId;
    private String collectorId;
    private String vehicleId;
    private LocalDate routeDate;
    private String status; // PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
    private double totalDistanceKm;
    private double estimatedTimeHours;
    private String strategyUsed;
    private final List<RouteStop> stops = new ArrayList<>();

    public Route(String id, String zoneId, String collectorId, String vehicleId, LocalDate routeDate, String strategyUsed) {
        this.id = Objects.requireNonNull(id);
        this.zoneId = Objects.requireNonNull(zoneId);
        this.collectorId = collectorId;
        this.vehicleId = vehicleId;
        this.routeDate = routeDate != null ? routeDate : LocalDate.now();
        this.strategyUsed = strategyUsed != null ? strategyUsed : "PRIORITY";
        this.status = "PLANNED";
        this.totalDistanceKm = 0.0;
        this.estimatedTimeHours = 0.0;
    }

    public void addStop(RouteStop stop) {
        if (stop != null) {
            stops.add(stop);
        }
    }

    public void setStops(List<RouteStop> newStops) {
        stops.clear();
        if (newStops != null) {
            stops.addAll(newStops);
        }
    }

    // ENCAPSULATION: Return unmodifiable view of stops collection to prevent direct tampering
    public List<RouteStop> getStops() {
        return Collections.unmodifiableList(new ArrayList<>(stops));
    }

    public int getStopsCount() {
        return stops.size();
    }

    public int getCompletedStopsCount() {
        return (int) stops.stream().filter(RouteStop::isCollected).count();
    }

    public double getProgressPercentage() {
        if (stops.isEmpty()) return 100.0;
        return (double) getCompletedStopsCount() / stops.size() * 100.0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getRouteDate() {
        return routeDate;
    }

    public void setRouteDate(LocalDate routeDate) {
        this.routeDate = routeDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public double getEstimatedTimeHours() {
        return estimatedTimeHours;
    }

    public void setEstimatedTimeHours(double estimatedTimeHours) {
        this.estimatedTimeHours = estimatedTimeHours;
    }

    public String getStrategyUsed() {
        return strategyUsed;
    }

    public void setStrategyUsed(String strategyUsed) {
        this.strategyUsed = strategyUsed;
    }
}
