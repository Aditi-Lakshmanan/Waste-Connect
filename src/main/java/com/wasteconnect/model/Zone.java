package com.wasteconnect.model;

import java.util.Objects;

/**
 * Entity representing a municipal ward / operational collection sector.
 */
public class Zone {
    private String id;
    private String name;
    private String code;
    private double centerLat;
    private double centerLng;
    private boolean marketArea; // Commercial market areas have faster waste accumulation
    private double targetFillLimit; // default 80.0%

    public Zone(String id, String name, String code, double centerLat, double centerLng, boolean marketArea) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.code = Objects.requireNonNull(code);
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.marketArea = marketArea;
        this.targetFillLimit = 80.0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(double centerLat) {
        this.centerLat = centerLat;
    }

    public double getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(double centerLng) {
        this.centerLng = centerLng;
    }

    public boolean isMarketArea() {
        return marketArea;
    }

    public void setMarketArea(boolean marketArea) {
        this.marketArea = marketArea;
    }

    public double getTargetFillLimit() {
        return targetFillLimit;
    }

    public void setTargetFillLimit(double targetFillLimit) {
        this.targetFillLimit = targetFillLimit;
    }

    public Location getCenterLocation() {
        return new Location(centerLat, centerLng, name + " Center");
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, code);
    }
}
