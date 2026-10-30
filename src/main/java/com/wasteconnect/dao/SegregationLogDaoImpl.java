package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.SegregationLog;
import com.wasteconnect.model.SegregationQuality;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SegregationLogDaoImpl extends BaseDao<SegregationLog, String> implements SegregationLogDao {

    @Override
    public Optional<SegregationLog> findById(String id) {
        String sql = "SELECT * FROM segregation_logs WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query segregation log by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<SegregationLog> findAll() {
        return findRecent(1000);
    }

    @Override
    public List<SegregationLog> findByCitizen(String citizenId) {
        List<SegregationLog> list = new ArrayList<>();
        String sql = "SELECT * FROM segregation_logs WHERE citizen_id = ? ORDER BY logged_at DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, citizenId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query segregation logs for citizen: " + citizenId, e);
        }
        return list;
    }

    @Override
    public List<SegregationLog> findRecent(int limit) {
        List<SegregationLog> list = new ArrayList<>();
        String sql = "SELECT * FROM segregation_logs ORDER BY logged_at DESC LIMIT ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query recent segregation logs", e);
        }
        return list;
    }

    @Override
    public SegregationLog save(SegregationLog log) {
        String sql = "INSERT INTO segregation_logs (id, citizen_id, collector_id, bin_id, quality, weight_kg, points_awarded, logged_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, log.getId());
            pstmt.setString(2, log.getCitizenId());
            pstmt.setString(3, log.getCollectorId());
            pstmt.setString(4, log.getBinId());
            pstmt.setString(5, log.getQuality().name());
            pstmt.setDouble(6, log.getWeightKg());
            pstmt.setInt(7, log.getPointsAwarded());
            pstmt.setString(8, log.getLoggedAt().toString());
            pstmt.executeUpdate();
            return log;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save segregation log: " + log.getId(), e);
        }
    }

    @Override
    public boolean update(SegregationLog log) {
        return false; // Logs are immutable append-only audit entries
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM segregation_logs WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete segregation log: " + id, e);
        }
    }

    private SegregationLog mapLog(ResultSet rs) throws SQLException {
        return new SegregationLog(
                rs.getString("id"),
                rs.getString("citizen_id"),
                rs.getString("collector_id"),
                rs.getString("bin_id"),
                SegregationQuality.valueOf(rs.getString("quality")),
                rs.getDouble("weight_kg"),
                rs.getInt("points_awarded"),
                LocalDateTime.parse(rs.getString("logged_at"))
        );
    }
}
