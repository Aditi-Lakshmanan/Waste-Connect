package com.wasteconnect.model;

/**
 * CO4: Java record representing ranked civic standing on the residential segregation leaderboard.
 */
public record LeaderboardEntry(
        int rank,
        String citizenId,
        String citizenName,
        String zoneName,
        int points,
        double segregationScore,
        int streak
) {}
