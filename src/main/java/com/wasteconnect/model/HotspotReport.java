package com.wasteconnect.model;

/**
 * CO4: Java record representing an immutable Data Transfer Object (DTO)
 * for chronic garbage vulnerable points detected by the hotspot engine.
 */
public record HotspotReport(
        String zoneId,
        String zoneName,
        double latitude,
        double longitude,
        int complaintCount,
        int overflowCount,
        double riskScore, // 0.0 to 100.0
        String riskLevel, // LOW, MEDIUM, CRITICAL
        String recommendation
) {}
