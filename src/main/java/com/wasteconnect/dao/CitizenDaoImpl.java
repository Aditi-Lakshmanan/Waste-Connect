package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.Citizen;
import com.wasteconnect.model.LeaderboardEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CitizenDaoImpl extends BaseDao<Citizen, String> implements CitizenDao {

    @Override
    public Optional<Citizen> findById(String id) {
        String sql = """
            SELECT u.*, c.streak, c.segregation_score, c.points_balance, c.address AS cit_address,
                   c.phone AS cit_phone, c.zone_id, c.household_id
            FROM users u
            JOIN citizens c ON u.id = c.id
            WHERE u.id = ?;
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCitizen(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query citizen: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Citizen> findAll() {
        List<Citizen> list = new ArrayList<>();
        String sql = """
            SELECT u.*, c.streak, c.segregation_score, c.points_balance, c.address AS cit_address,
                   c.phone AS cit_phone, c.zone_id, c.household_id
            FROM users u
            JOIN citizens c ON u.id = c.id
            ORDER BY u.name ASC;
        """;
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapCitizen(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query citizens list", e);
        }
        return list;
    }

    @Override
    public Citizen save(Citizen citizen) {
        Connection conn = getConnection();
        try {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // Insert into users
                String userSql = "INSERT INTO users (id, name, email, password_hash, role, created_at, active) VALUES (?, ?, ?, ?, ?, ?, ?);";
                try (PreparedStatement uPstmt = conn.prepareStatement(userSql)) {
                    uPstmt.setString(1, citizen.getId());
                    uPstmt.setString(2, citizen.getName());
                    uPstmt.setString(3, citizen.getEmail().toLowerCase());
                    uPstmt.setString(4, citizen.getPasswordHash());
                    uPstmt.setString(5, citizen.getRole().name());
                    uPstmt.setString(6, citizen.getCreatedAt().toString());
                    uPstmt.setInt(7, citizen.isActive() ? 1 : 0);
                    uPstmt.executeUpdate();
                }

                // Insert into citizens
                String citSql = "INSERT INTO citizens (id, streak, segregation_score, points_balance, address, phone, zone_id, household_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
                try (PreparedStatement cPstmt = conn.prepareStatement(citSql)) {
                    cPstmt.setString(1, citizen.getId());
                    cPstmt.setInt(2, citizen.getStreak());
                    cPstmt.setDouble(3, citizen.getSegregationScore());
                    cPstmt.setInt(4, citizen.getPointsBalance());
                    cPstmt.setString(5, citizen.getAddress());
                    cPstmt.setString(6, citizen.getPhone());
                    cPstmt.setString(7, citizen.getZoneId());
                    cPstmt.setString(8, citizen.getHouseholdId());
                    cPstmt.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(autoCommit);
                return citizen;
            } catch (SQLException ex) {
                conn.rollback();
                conn.setAutoCommit(autoCommit);
                throw ex;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save citizen: " + citizen.getEmail(), e);
        }
    }

    @Override
    public boolean update(Citizen citizen) {
        Connection conn = getConnection();
        try {
            String userSql = "UPDATE users SET name = ?, email = ?, active = ? WHERE id = ?;";
            try (PreparedStatement uPstmt = conn.prepareStatement(userSql)) {
                uPstmt.setString(1, citizen.getName());
                uPstmt.setString(2, citizen.getEmail().toLowerCase());
                uPstmt.setInt(3, citizen.isActive() ? 1 : 0);
                uPstmt.setString(4, citizen.getId());
                uPstmt.executeUpdate();
            }

            String citSql = "UPDATE citizens SET streak = ?, segregation_score = ?, points_balance = ?, address = ?, phone = ?, zone_id = ?, household_id = ? WHERE id = ?;";
            try (PreparedStatement cPstmt = conn.prepareStatement(citSql)) {
                cPstmt.setInt(1, citizen.getStreak());
                cPstmt.setDouble(2, citizen.getSegregationScore());
                cPstmt.setInt(3, citizen.getPointsBalance());
                cPstmt.setString(4, citizen.getAddress());
                cPstmt.setString(5, citizen.getPhone());
                cPstmt.setString(6, citizen.getZoneId());
                cPstmt.setString(7, citizen.getHouseholdId());
                cPstmt.setString(8, citizen.getId());
                return cPstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update citizen: " + citizen.getId(), e);
        }
    }

    @Override
    public boolean updateScoreAndPoints(String citizenId, double score, int streak, int points) {
        String sql = "UPDATE citizens SET segregation_score = ?, streak = ?, points_balance = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, score);
            pstmt.setInt(2, streak);
            pstmt.setInt(3, points);
            pstmt.setString(4, citizenId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update score/points for citizen: " + citizenId, e);
        }
    }

    @Override
    public boolean updatePointsOnly(String citizenId, int newBalance) {
        String sql = "UPDATE citizens SET points_balance = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, newBalance);
            pstmt.setString(2, citizenId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update points for citizen: " + citizenId, e);
        }
    }

    @Override
    public Optional<Citizen> findByHousehold(String householdId) {
        String sql = """
            SELECT u.*, c.streak, c.segregation_score, c.points_balance, c.address AS cit_address,
                   c.phone AS cit_phone, c.zone_id, c.household_id
            FROM users u
            JOIN citizens c ON u.id = c.id
            WHERE c.household_id = ?;
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, householdId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCitizen(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query citizen by household: " + householdId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Citizen> findByZone(String zoneId) {
        List<Citizen> list = new ArrayList<>();
        String sql = """
            SELECT u.*, c.streak, c.segregation_score, c.points_balance, c.address AS cit_address,
                   c.phone AS cit_phone, c.zone_id, c.household_id
            FROM users u
            JOIN citizens c ON u.id = c.id
            WHERE c.zone_id = ?
            ORDER BY c.segregation_score DESC;
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zoneId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCitizen(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query citizens by zone: " + zoneId, e);
        }
        return list;
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        String sql = """
            SELECT c.id AS citizen_id, u.name AS citizen_name, COALESCE(z.name, 'All City') AS zone_name,
                   c.points_balance, c.segregation_score, c.streak
            FROM citizens c
            JOIN users u ON c.id = u.id
            LEFT JOIN zones z ON c.zone_id = z.id
            WHERE u.active = 1
            ORDER BY c.segregation_score DESC, c.streak DESC, c.points_balance DESC
            LIMIT ?;
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(
                            rank++,
                            rs.getString("citizen_id"),
                            rs.getString("citizen_name"),
                            rs.getString("zone_name"),
                            rs.getInt("points_balance"),
                            rs.getDouble("segregation_score"),
                            rs.getInt("streak")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query leaderboard", e);
        }
        return entries;
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete citizen: " + id, e);
        }
    }

    private Citizen mapCitizen(ResultSet rs) throws SQLException {
        return new Citizen(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                LocalDateTime.parse(rs.getString("created_at")),
                rs.getInt("active") == 1,
                rs.getInt("streak"),
                rs.getDouble("segregation_score"),
                rs.getInt("points_balance"),
                rs.getString("cit_address"),
                rs.getString("cit_phone"),
                rs.getString("zone_id"),
                rs.getString("household_id")
        );
    }
}
