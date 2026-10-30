package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.Household;
import com.wasteconnect.model.Location;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HouseholdDaoImpl extends BaseDao<Household, String> implements HouseholdDao {

    @Override
    public Optional<Household> findById(String id) {
        String sql = "SELECT * FROM households WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapHousehold(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query household: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Household> findAll() {
        List<Household> list = new ArrayList<>();
        String sql = "SELECT * FROM households ORDER BY id ASC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapHousehold(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query households", e);
        }
        return list;
    }

    @Override
    public List<Household> findByZone(String zoneId) {
        List<Household> list = new ArrayList<>();
        String sql = "SELECT * FROM households WHERE zone_id = ? ORDER BY id ASC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zoneId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapHousehold(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query households by zone: " + zoneId, e);
        }
        return list;
    }

    @Override
    public List<Household> findPage(int offset, int limit, String search) {
        List<Household> list = new ArrayList<>();
        boolean hasSearch = (search != null && !search.trim().isEmpty());
        String sql = hasSearch ?
                "SELECT * FROM households WHERE address LIKE ? OR id LIKE ? ORDER BY id ASC LIMIT ? OFFSET ?;" :
                "SELECT * FROM households ORDER BY id ASC LIMIT ? OFFSET ?;";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            if (hasSearch) {
                String pattern = "%" + search.trim() + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
                pstmt.setInt(3, limit);
                pstmt.setInt(4, offset);
            } else {
                pstmt.setInt(1, limit);
                pstmt.setInt(2, offset);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapHousehold(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query paginated households", e);
        }
        return list;
    }

    @Override
    public int count(String search) {
        boolean hasSearch = (search != null && !search.trim().isEmpty());
        String sql = hasSearch ?
                "SELECT COUNT(*) FROM households WHERE address LIKE ? OR id LIKE ?;" :
                "SELECT COUNT(*) FROM households;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            if (hasSearch) {
                String pattern = "%" + search.trim() + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count households", e);
        }
        return 0;
    }

    @Override
    public Household save(Household h) {
        String sql = "INSERT INTO households (id, zone_id, citizen_id, address, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, h.getId());
            pstmt.setString(2, h.getZoneId());
            pstmt.setString(3, h.getCitizenId());
            pstmt.setString(4, h.getAddress());
            pstmt.setDouble(5, h.getLocation().getLatitude());
            pstmt.setDouble(6, h.getLocation().getLongitude());
            pstmt.executeUpdate();
            return h;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save household: " + h.getId(), e);
        }
    }

    @Override
    public boolean update(Household h) {
        String sql = "UPDATE households SET zone_id = ?, citizen_id = ?, address = ?, latitude = ?, longitude = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, h.getZoneId());
            pstmt.setString(2, h.getCitizenId());
            pstmt.setString(3, h.getAddress());
            pstmt.setDouble(4, h.getLocation().getLatitude());
            pstmt.setDouble(5, h.getLocation().getLongitude());
            pstmt.setString(6, h.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update household: " + h.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM households WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete household: " + id, e);
        }
    }

    private Household mapHousehold(ResultSet rs) throws SQLException {
        Location loc = new Location(rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("address"));
        return new Household(
                rs.getString("id"),
                rs.getString("zone_id"),
                rs.getString("citizen_id"),
                rs.getString("address"),
                loc
        );
    }
}
