package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Data Access Object implementation for Users, assembling polymorphic User subtypes.
 */
public class UserDaoImpl extends BaseDao<User, String> implements UserDao {

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error querying user by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error querying user by email: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User u = mapUser(rs);
                if (u != null) {
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching all users", e);
        }
        return list;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (id, name, email, password_hash, role, created_at, active) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getEmail().toLowerCase());
            pstmt.setString(4, user.getPasswordHash());
            pstmt.setString(5, user.getRole().name());
            pstmt.setString(6, user.getCreatedAt().toString());
            pstmt.setInt(7, user.isActive() ? 1 : 0);
            pstmt.executeUpdate();
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert user: " + user.getEmail(), e);
        }
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, active = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail().toLowerCase());
            pstmt.setInt(3, user.isActive() ? 1 : 0);
            pstmt.setString(4, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update user: " + user.getId(), e);
        }
    }

    @Override
    public boolean updatePassword(String id, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, passwordHash);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update password for user: " + id, e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user: " + id, e);
        }
    }

    // POLYMORPHISM: Factory dispatch to instantiate concrete subclass based on UserRole enum
    private User mapUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String hash = rs.getString("password_hash");
        UserRole role = UserRole.valueOf(rs.getString("role"));
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        boolean active = rs.getInt("active") == 1;

        switch (role) {
            case CITIZEN -> {
                String subSql = "SELECT * FROM citizens WHERE id = ?;";
                try (PreparedStatement pstmt = getConnection().prepareStatement(subSql)) {
                    pstmt.setString(1, id);
                    try (ResultSet subRs = pstmt.executeQuery()) {
                        if (subRs.next()) {
                            return new Citizen(id, name, email, hash, createdAt, active,
                                    subRs.getInt("streak"),
                                    subRs.getDouble("segregation_score"),
                                    subRs.getInt("points_balance"),
                                    subRs.getString("address"),
                                    subRs.getString("phone"),
                                    subRs.getString("zone_id"),
                                    subRs.getString("household_id"));
                        }
                    }
                }
                return new Citizen(id, name, email, hash, "Default Address", "", "Z1");
            }
            case COLLECTOR -> {
                String subSql = "SELECT * FROM collectors WHERE id = ?;";
                try (PreparedStatement pstmt = getConnection().prepareStatement(subSql)) {
                    pstmt.setString(1, id);
                    try (ResultSet subRs = pstmt.executeQuery()) {
                        if (subRs.next()) {
                            return new Collector(id, name, email, hash, createdAt, active,
                                    subRs.getString("employee_id"),
                                    subRs.getString("assigned_zone_id"),
                                    subRs.getString("assigned_vehicle_id"),
                                    subRs.getDouble("rating"),
                                    subRs.getInt("total_pickups"));
                        }
                    }
                }
                return new Collector(id, name, email, hash, "EMP-TEMP", "Z1", "V1");
            }
            case ADMIN -> {
                String subSql = "SELECT * FROM admins WHERE id = ?;";
                try (PreparedStatement pstmt = getConnection().prepareStatement(subSql)) {
                    pstmt.setString(1, id);
                    try (ResultSet subRs = pstmt.executeQuery()) {
                        if (subRs.next()) {
                            return new Admin(id, name, email, hash, createdAt, active,
                                    subRs.getString("department"),
                                    subRs.getInt("access_level"));
                        }
                    }
                }
                return new Admin(id, name, email, hash, "Municipal Dept", 1);
            }
            case WASTE_PICKER -> {
                String subSql = "SELECT * FROM waste_pickers WHERE id = ?;";
                try (PreparedStatement pstmt = getConnection().prepareStatement(subSql)) {
                    pstmt.setString(1, id);
                    try (ResultSet subRs = pstmt.executeQuery()) {
                        if (subRs.next()) {
                            return new WastePicker(id, name, email, hash, createdAt, active,
                                    subRs.getString("phone"),
                                    subRs.getString("vehicle_desc"),
                                    subRs.getInt("verified") == 1,
                                    subRs.getDouble("total_earnings"),
                                    subRs.getDouble("total_kg_collected"));
                        }
                    }
                }
                return new WastePicker(id, name, email, hash, "", "");
            }
        }
        return null;
    }
}
