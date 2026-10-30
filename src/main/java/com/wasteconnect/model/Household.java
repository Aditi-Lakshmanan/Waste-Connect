package com.wasteconnect.model;

import java.util.Objects;

/**
 * Entity representing a registered domestic residential unit within a zone.
 */
public class Household {
    private String id;
    private String zoneId;
    private String citizenId;
    private String address;
    private Location location;

    public Household(String id, String zoneId, String citizenId, String address, Location location) {
        this.id = Objects.requireNonNull(id);
        this.zoneId = Objects.requireNonNull(zoneId);
        this.citizenId = citizenId;
        this.address = Objects.requireNonNull(address);
        this.location = Objects.requireNonNull(location);
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

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
