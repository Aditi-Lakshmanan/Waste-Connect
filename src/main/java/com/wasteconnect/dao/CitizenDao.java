package com.wasteconnect.dao;

import com.wasteconnect.model.Citizen;
import com.wasteconnect.model.LeaderboardEntry;

import java.util.List;
import java.util.Optional;

public interface CitizenDao extends Repository<Citizen, String> {
    List<LeaderboardEntry> getLeaderboard(int limit);
    boolean updateScoreAndPoints(String citizenId, double score, int streak, int points);
    boolean updatePointsOnly(String citizenId, int newBalance);
    Optional<Citizen> findByHousehold(String householdId);
    List<Citizen> findByZone(String zoneId);
}
