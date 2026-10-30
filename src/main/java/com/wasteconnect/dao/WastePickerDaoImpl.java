package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.WastePicker;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WastePickerDaoImpl extends BaseDao<WastePicker, String> implements WastePickerDao {

    @Override
    public Optional<WastePicker> findById(String id) {
        String sql = """
            SELECT u.*, p.phone, p.vehicle_desc, p.verified, p.total_earnings, p.total_kg_collected
            FROM users u
            JOIN waste_pickers p ON u.id = p.id
            WHERE u.id = ?;
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPicker(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query waste picker: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<WastePicker> findAll() {
        List<WastePicker> list = new ArrayList<>();
        String sql = """
            SELECT u.*, p.phone, p.vehicle_desc, p.verified, p.total_earnings, p.total_kg_collected
            FROM users u
            JOIN waste_pickers p ON u.id = p.id
            ORDER BY u.name ASC;
        """;
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapPicker(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query waste pickers", e);
        }
        return list;
    }

    @Override
    public List<WastePicker> findPendingVerification() {
        List<WastePicker> list = new ArrayList<>();
        String sql = """
            SELECT u.*, p.phone, p.vehicle_desc, p.verified, p.total_earnings, p.total_kg_collected
            FROM users u
            JOIN waste_pickers p ON u.id = p.id
            WHERE p.verified = 0
            ORDER BY u.created_at ASC;
        """;
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapPicker(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query unverified pickers", e);
        }
        return list;
    }

    @Override
    public boolean setVerificationStatus(String pickerId, boolean verified) {
        String sql = "UPDATE waste_pickers SET verified = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, verified ? 1 : 0);
            pstmt.setString(2, pickerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update verification status for picker: " + pickerId, e);
        }
    }

    @Override
    public boolean addEarnings(String pickerId, double amount, double kg) {
        String sql = "UPDATE waste_pickers SET total_earnings = total_earnings + ?, total_kg_collected = total_kg_collected + ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setDouble(2, kg);
            pstmt.setString(3, pickerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to credit earnings for picker: " + pickerId, e);
        }
    }

    @Override
    public WastePicker save(WastePicker picker) {
        Connection conn = getConnection();
        try {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String uSql = "INSERT INTO users (id, name, email, password_hash, role, created_at, active) VALUES (?, ?, ?, ?, ?, ?, ?);";
                try (PreparedStatement uPstmt = conn.prepareStatement(uSql)) {
                    uPstmt.setString(1, picker.getId());
                    uPstmt.setString(2, picker.getName());
                    uPstmt.setString(3, picker.getEmail().toLowerCase());
                    uPstmt.setString(4, picker.getPasswordHash());
                    uPstmt.setString(5, picker.getRole().name());
                    uPstmt.setString(6, picker.getCreatedAt().toString());
                    uPstmt.setInt(7, picker.isActive() ? 1 : 0);
                    uPstmt.executeUpdate();
                }

                String pSql = "INSERT INTO waste_pickers (id, phone, vehicle_desc, verified, total_earnings, total_kg_collected) VALUES (?, ?, ?, ?, ?, ?);";
                try (PreparedStatement pPstmt = conn.prepareStatement(pSql)) {
                    pPstmt.setString(1, picker.getId());
                    pPstmt.setString(2, picker.getPhone());
                    pPstmt.setString(3, picker.getVehicleDesc());
                    pPstmt.setInt(4, picker.isVerified() ? 1 : 0);
                    pPstmt.setDouble(5, picker.getTotalEarnings());
                    pPstmt.setDouble(6, picker.getTotalKgCollected());
                    pPstmt.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(autoCommit);
                return picker;
            } catch (SQLException ex) {
                conn.rollback();
                conn.setAutoCommit(autoCommit);
                throw ex;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert waste picker: " + picker.getEmail(), e);
        }
    }

    @Override
    public boolean update(WastePicker picker) {
        String uSql = "UPDATE users SET name = ?, email = ?, active = ? WHERE id = ?;";
        String pSql = "UPDATE waste_pickers SET phone = ?, vehicle_desc = ?, verified = ?, total_earnings = ?, total_kg_collected = ? WHERE id = ?;";
        try (PreparedStatement uPstmt = getConnection().prepareStatement(uSql);
             PreparedStatement pPstmt = getConnection().prepareStatement(pSql)) {
            uPstmt.setString(1, picker.getName());
            uPstmt.setString(2, picker.getEmail().toLowerCase());
            uPstmt.setInt(3, picker.isActive() ? 1 : 0);
            uPstmt.setString(4, picker.getId());
            uPstmt.executeUpdate();

            pPstmt.setString(1, picker.getPhone());
            pPstmt.setString(2, picker.getVehicleDesc());
            pPstmt.setInt(3, picker.isVerified() ? 1 : 0);
            pPstmt.setDouble(4, picker.getTotalEarnings());
            pPstmt.setDouble(5, picker.getTotalKgCollected());
            pPstmt.setString(6, picker.getId());
            return pPstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update picker: " + picker.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete waste picker: " + id, e);
        }
    }

    private WastePicker mapPicker(ResultSet rs) throws SQLException {
        return new WastePicker(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                LocalDateTime.parse(rs.getString("created_at")),
                rs.getInt("active") == 1,
                rs.getString("phone"),
                rs.getString("vehicle_desc"),
                rs.getInt("verified") == 1,
                rs.getDouble("total_earnings"),
                rs.getDouble("total_kg_collected")
        );
    }
}
