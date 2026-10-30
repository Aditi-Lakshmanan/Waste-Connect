package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.Collector;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CollectorDaoImpl extends BaseDao<Collector, String> implements CollectorDao {

    @Override
    public Optional<Collector> findById(String id) {
        String sql = """
            SELECT u.*, c.employee_id, c.assigned_zone_id, c.assigned_vehicle_id, c.rating, c.total_pickups
            FROM users u
            JOIN collectors c ON u.id = c.id
            WHERE u.id = ?;
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCollector(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query collector by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Collector> findByEmployeeId(String employeeId) {
        String sql = """
            SELECT u.*, c.employee_id, c.assigned_zone_id, c.assigned_vehicle_id, c.rating, c.total_pickups
            FROM users u
            JOIN collectors c ON u.id = c.id
            WHERE c.employee_id = ?;
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCollector(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query collector by employeeId: " + employeeId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Collector> findAll() {
        List<Collector> list = new ArrayList<>();
        String sql = """
            SELECT u.*, c.employee_id, c.assigned_zone_id, c.assigned_vehicle_id, c.rating, c.total_pickups
            FROM users u
            JOIN collectors c ON u.id = c.id
            ORDER BY u.name ASC;
        """;
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapCollector(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query collectors", e);
        }
        return list;
    }

    @Override
    public List<Collector> findByZone(String zoneId) {
        List<Collector> list = new ArrayList<>();
        String sql = """
            SELECT u.*, c.employee_id, c.assigned_zone_id, c.assigned_vehicle_id, c.rating, c.total_pickups
            FROM users u
            JOIN collectors c ON u.id = c.id
            WHERE c.assigned_zone_id = ?;
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zoneId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCollector(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query collectors by zone: " + zoneId, e);
        }
        return list;
    }

    @Override
    public Collector save(Collector collector) {
        Connection conn = getConnection();
        try {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String uSql = "INSERT INTO users (id, name, email, password_hash, role, created_at, active) VALUES (?, ?, ?, ?, ?, ?, ?);";
                try (PreparedStatement uPstmt = conn.prepareStatement(uSql)) {
                    uPstmt.setString(1, collector.getId());
                    uPstmt.setString(2, collector.getName());
                    uPstmt.setString(3, collector.getEmail().toLowerCase());
                    uPstmt.setString(4, collector.getPasswordHash());
                    uPstmt.setString(5, collector.getRole().name());
                    uPstmt.setString(6, collector.getCreatedAt().toString());
                    uPstmt.setInt(7, collector.isActive() ? 1 : 0);
                    uPstmt.executeUpdate();
                }

                String cSql = "INSERT INTO collectors (id, employee_id, assigned_zone_id, assigned_vehicle_id, rating, total_pickups) VALUES (?, ?, ?, ?, ?, ?);";
                try (PreparedStatement cPstmt = conn.prepareStatement(cSql)) {
                    cPstmt.setString(1, collector.getId());
                    cPstmt.setString(2, collector.getEmployeeId());
                    cPstmt.setString(3, collector.getAssignedZoneId());
                    cPstmt.setString(4, collector.getAssignedVehicleId());
                    cPstmt.setDouble(5, collector.getRating());
                    cPstmt.setInt(6, collector.getTotalPickupsCompleted());
                    cPstmt.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(autoCommit);
                return collector;
            } catch (SQLException ex) {
                conn.rollback();
                conn.setAutoCommit(autoCommit);
                throw ex;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert collector: " + collector.getEmail(), e);
        }
    }

    @Override
    public boolean update(Collector collector) {
        String uSql = "UPDATE users SET name = ?, email = ?, active = ? WHERE id = ?;";
        String cSql = "UPDATE collectors SET employee_id = ?, assigned_zone_id = ?, assigned_vehicle_id = ?, rating = ?, total_pickups = ? WHERE id = ?;";
        try (PreparedStatement uPstmt = getConnection().prepareStatement(uSql);
             PreparedStatement cPstmt = getConnection().prepareStatement(cSql)) {
            uPstmt.setString(1, collector.getName());
            uPstmt.setString(2, collector.getEmail().toLowerCase());
            uPstmt.setInt(3, collector.isActive() ? 1 : 0);
            uPstmt.setString(4, collector.getId());
            uPstmt.executeUpdate();

            cPstmt.setString(1, collector.getEmployeeId());
            cPstmt.setString(2, collector.getAssignedZoneId());
            cPstmt.setString(3, collector.getAssignedVehicleId());
            cPstmt.setDouble(4, collector.getRating());
            cPstmt.setInt(5, collector.getTotalPickupsCompleted());
            cPstmt.setString(6, collector.getId());
            return cPstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update collector: " + collector.getId(), e);
        }
    }

    @Override
    public boolean incrementPickups(String collectorId) {
        String sql = "UPDATE collectors SET total_pickups = total_pickups + 1 WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, collectorId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to increment pickups for collector: " + collectorId, e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete collector: " + id, e);
        }
    }

    private Collector mapCollector(ResultSet rs) throws SQLException {
        return new Collector(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                LocalDateTime.parse(rs.getString("created_at")),
                rs.getInt("active") == 1,
                rs.getString("employee_id"),
                rs.getString("assigned_zone_id"),
                rs.getString("assigned_vehicle_id"),
                rs.getDouble("rating"),
                rs.getInt("total_pickups")
        );
    }
}
