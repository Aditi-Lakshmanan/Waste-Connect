package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * INHERITANCE: WastePicker represents informal recycling partners collecting recyclable commodities.
 */
public class WastePicker extends User {
    private String phone;
    private String vehicleDesc;
    private boolean verified;
    private double totalEarnings;
    private double totalKgCollected;

    public WastePicker(String id, String name, String email, String passwordHash, String phone, String vehicleDesc) {
        super(id, name, email, passwordHash, UserRole.WASTE_PICKER);
        this.phone = phone;
        this.vehicleDesc = vehicleDesc;
        this.verified = false; // requires admin approval
        this.totalEarnings = 0.0;
        this.totalKgCollected = 0.0;
    }

    public WastePicker(String id, String name, String email, String passwordHash, LocalDateTime createdAt, boolean active,
                       String phone, String vehicleDesc, boolean verified, double totalEarnings, double totalKgCollected) {
        super(id, name, email, passwordHash, UserRole.WASTE_PICKER, createdAt, active);
        this.phone = phone;
        this.vehicleDesc = vehicleDesc;
        this.verified = verified;
        this.totalEarnings = Math.max(0.0, totalEarnings);
        this.totalKgCollected = Math.max(0.0, totalKgCollected);
    }

    // POLYMORPHISM: Picker dashboard view
    @Override
    public String getDashboardView() {
        return "PICKER_DASHBOARD";
    }

    // POLYMORPHISM: WastePicker permissions
    @Override
    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(new HashSet<>(Set.of(
                "VIEW_RECYCLABLE_LISTINGS", "CLAIM_LISTING", "COMPLETE_COLLECTION", "VIEW_EARNINGS"
        )));
    }

    public void addEarnings(double amount, double kg) {
        if (amount < 0 || kg < 0) {
            throw new IllegalArgumentException("Earnings and weight cannot be negative");
        }
        this.totalEarnings += amount;
        this.totalKgCollected += kg;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getVehicleDesc() {
        return vehicleDesc;
    }

    public void setVehicleDesc(String vehicleDesc) {
        this.vehicleDesc = vehicleDesc;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(double totalEarnings) {
        this.totalEarnings = Math.max(0.0, totalEarnings);
    }

    public double getTotalKgCollected() {
        return totalKgCollected;
    }

    public void setTotalKgCollected(double totalKgCollected) {
        this.totalKgCollected = Math.max(0.0, totalKgCollected);
    }
}
