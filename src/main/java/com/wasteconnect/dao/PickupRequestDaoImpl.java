package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.PickupRequest;
import com.wasteconnect.model.PickupStatus;
import com.wasteconnect.model.WasteType;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PickupRequestDaoImpl extends BaseDao<PickupRequest, String> implements PickupRequestDao {

    @Override
    public Optional<PickupRequest> findById(String id) {
        String sql = "SELECT * FROM pickups WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPickup(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query pickup request by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<PickupRequest> findAll() {
        List<PickupRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM pickups ORDER BY created_at DESC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapPickup(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query pickup requests", e);
        }
        return list;
    }

    @Override
    public List<PickupRequest> findByCitizen(String citizenId) {
        List<PickupRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM pickups WHERE citizen_id = ? ORDER BY created_at DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, citizenId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPickup(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query pickups for citizen: " + citizenId, e);
        }
        return list;
    }

    @Override
    public List<PickupRequest> findPending() {
        List<PickupRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM pickups WHERE status = 'PENDING' ORDER BY preferred_slot ASC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapPickup(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query pending pickups", e);
        }
        return list;
    }

    @Override
    public boolean updateStatus(String id, PickupStatus status, String collectorId) {
        String sql = "UPDATE pickups SET status = ?, assigned_collector_id = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, collectorId);
            pstmt.setString(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update status for pickup: " + id, e);
        }
    }

    @Override
    public PickupRequest save(PickupRequest p) {
        String sql = "INSERT INTO pickups (id, citizen_id, waste_type, weight_kg, preferred_slot, address, status, created_at, assigned_collector_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, p.getId());
            pstmt.setString(2, p.getCitizenId());
            pstmt.setString(3, p.getWasteType().name());
            pstmt.setDouble(4, p.getApproximateWeightKg());
            pstmt.setString(5, p.getPreferredSlot().toString());
            pstmt.setString(6, p.getAddress());
            pstmt.setString(7, p.getStatus().name());
            pstmt.setString(8, p.getCreatedAt().toString());
            pstmt.setString(9, p.getAssignedCollectorId());
            pstmt.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save pickup request: " + p.getId(), e);
        }
    }

    @Override
    public boolean update(PickupRequest p) {
        String sql = "UPDATE pickups SET waste_type = ?, weight_kg = ?, preferred_slot = ?, address = ?, status = ?, assigned_collector_id = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, p.getWasteType().name());
            pstmt.setDouble(2, p.getApproximateWeightKg());
            pstmt.setString(3, p.getPreferredSlot().toString());
            pstmt.setString(4, p.getAddress());
            pstmt.setString(5, p.getStatus().name());
            pstmt.setString(6, p.getAssignedCollectorId());
            pstmt.setString(7, p.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update pickup request: " + p.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM pickups WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete pickup request: " + id, e);
        }
    }

    private PickupRequest mapPickup(ResultSet rs) throws SQLException {
        return new PickupRequest(
                rs.getString("id"),
                rs.getString("citizen_id"),
                WasteType.valueOf(rs.getString("waste_type")),
                rs.getDouble("weight_kg"),
                LocalDate.parse(rs.getString("preferred_slot")),
                rs.getString("address"),
                PickupStatus.valueOf(rs.getString("status")),
                LocalDateTime.parse(rs.getString("created_at")),
                rs.getString("assigned_collector_id")
        );
    }
}
