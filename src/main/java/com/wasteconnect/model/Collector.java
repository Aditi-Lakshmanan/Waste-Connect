package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * INHERITANCE: Collector is a municipal field employee operating collection vehicles and recording bin lifts.
 */
public class Collector extends User {
    private String employeeId;
    private String assignedZoneId;
    private String assignedVehicleId;
    private double rating; // 1.0 to 5.0
    private int totalPickupsCompleted;

    public Collector(String id, String name, String email, String passwordHash, String employeeId, String assignedZoneId, String assignedVehicleId) {
        super(id, name, email, passwordHash, UserRole.COLLECTOR);
        this.employeeId = employeeId;
        this.assignedZoneId = assignedZoneId;
        this.assignedVehicleId = assignedVehicleId;
        this.rating = 5.0;
        this.totalPickupsCompleted = 0;
    }

    public Collector(String id, String name, String email, String passwordHash, LocalDateTime createdAt, boolean active,
                     String employeeId, String assignedZoneId, String assignedVehicleId, double rating, int totalPickupsCompleted) {
        super(id, name, email, passwordHash, UserRole.COLLECTOR, createdAt, active);
        this.employeeId = employeeId;
        this.assignedZoneId = assignedZoneId;
        this.assignedVehicleId = assignedVehicleId;
        this.rating = Math.max(1.0, Math.min(5.0, rating));
        this.totalPickupsCompleted = Math.max(0, totalPickupsCompleted);
    }

    // POLYMORPHISM: Return Collector specific dashboard view
    @Override
    public String getDashboardView() {
        return "COLLECTOR_DASHBOARD";
    }

    // POLYMORPHISM: Collector permissions
    @Override
    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(new HashSet<>(Set.of(
                "VIEW_ASSIGNED_ROUTE", "RECORD_COLLECTION", "RESOLVE_COMPLAINT", "REPORT_INCIDENT"
        )));
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getAssignedZoneId() {
        return assignedZoneId;
    }

    public void setAssignedZoneId(String assignedZoneId) {
        this.assignedZoneId = assignedZoneId;
    }

    public String getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public void setAssignedVehicleId(String assignedVehicleId) {
        this.assignedVehicleId = assignedVehicleId;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = Math.max(1.0, Math.min(5.0, rating));
    }

    public int getTotalPickupsCompleted() {
        return totalPickupsCompleted;
    }

    public void incrementCompletedPickups() {
        this.totalPickupsCompleted++;
    }

    public void setTotalPickupsCompleted(int totalPickupsCompleted) {
        this.totalPickupsCompleted = Math.max(0, totalPickupsCompleted);
    }
}
