package com.wasteconnect.model;

import java.util.Objects;

/**
 * ENCAPSULATION: Immutable value object representing a geographical coordinate with validated boundaries.
 */
public final class Location {
    private final double latitude;
    private final double longitude;
    private final String address;

    public Location(double latitude, double longitude, String address) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees: " + longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = (address == null || address.isBlank()) ? "Unknown Location" : address.trim();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    /**
     * Calculates the great-circle distance between two locations in kilometers using the Haversine formula.
     */
    public double distanceTo(Location other) {
        if (other == null) return 0.0;
        final double R = 6371.0; // Earth's mean radius in km
        double latDistance = Math.toRadians(other.latitude - this.latitude);
        double lonDistance = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location location)) return false;
        return Double.compare(location.latitude, latitude) == 0 &&
               Double.compare(location.longitude, longitude) == 0 &&
               Objects.equals(address, location.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, address);
    }

    @Override
    public String toString() {
        return String.format("%.5f, %.5f (%s)", latitude, longitude, address);
    }
}
