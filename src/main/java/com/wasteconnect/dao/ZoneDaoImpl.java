package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.Zone;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ZoneDaoImpl extends BaseDao<Zone, String> implements ZoneDao {

    @Override
    public Optional<Zone> findById(String id) {
        String sql = "SELECT * FROM zones WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapZone(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query zone by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Zone> findByCode(String code) {
        String sql = "SELECT * FROM zones WHERE UPPER(code) = UPPER(?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapZone(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query zone by code: " + code, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Zone> findAll() {
        List<Zone> list = new ArrayList<>();
        String sql = "SELECT * FROM zones ORDER BY name ASC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapZone(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query zones", e);
        }
        return list;
    }

    @Override
    public Zone save(Zone zone) {
        String sql = "INSERT INTO zones (id, name, code, center_lat, center_lng, market_area, target_fill_limit) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zone.getId());
            pstmt.setString(2, zone.getName());
            pstmt.setString(3, zone.getCode());
            pstmt.setDouble(4, zone.getCenterLat());
            pstmt.setDouble(5, zone.getCenterLng());
            pstmt.setInt(6, zone.isMarketArea() ? 1 : 0);
            pstmt.setDouble(7, zone.getTargetFillLimit());
            pstmt.executeUpdate();
            return zone;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save zone: " + zone.getName(), e);
        }
    }

    @Override
    public boolean update(Zone zone) {
        String sql = "UPDATE zones SET name = ?, code = ?, center_lat = ?, center_lng = ?, market_area = ?, target_fill_limit = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zone.getName());
            pstmt.setString(2, zone.getCode());
            pstmt.setDouble(3, zone.getCenterLat());
            pstmt.setDouble(4, zone.getCenterLng());
            pstmt.setInt(5, zone.isMarketArea() ? 1 : 0);
            pstmt.setDouble(6, zone.getTargetFillLimit());
            pstmt.setString(7, zone.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update zone: " + zone.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM zones WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete zone: " + id, e);
        }
    }

    private Zone mapZone(ResultSet rs) throws SQLException {
        Zone z = new Zone(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("code"),
                rs.getDouble("center_lat"),
                rs.getDouble("center_lng"),
                rs.getInt("market_area") == 1
        );
        z.setTargetFillLimit(rs.getDouble("target_fill_limit"));
        return z;
    }
}
