package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.Bin;
import com.wasteconnect.model.BinStatus;
import com.wasteconnect.model.Location;
import com.wasteconnect.model.WasteType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BinDaoImpl extends BaseDao<Bin, String> implements BinDao {

    @Override
    public Optional<Bin> findById(String id) {
        String sql = "SELECT * FROM bins WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapBin(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query bin by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Bin> findByCode(String code) {
        String sql = "SELECT * FROM bins WHERE UPPER(bin_code) = UPPER(?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapBin(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query bin by code: " + code, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Bin> findAll() {
        List<Bin> list = new ArrayList<>();
        String sql = "SELECT * FROM bins ORDER BY bin_code ASC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapBin(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query all bins", e);
        }
        return list;
    }

    @Override
    public List<Bin> findByZone(String zoneId) {
        List<Bin> list = new ArrayList<>();
        String sql = "SELECT * FROM bins WHERE zone_id = ? ORDER BY bin_code ASC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zoneId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBin(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query bins by zone: " + zoneId, e);
        }
        return list;
    }

    @Override
    public List<Bin> findByStatus(BinStatus status) {
        List<Bin> list = new ArrayList<>();
        String sql = "SELECT * FROM bins WHERE status = ? ORDER BY fill_percentage DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBin(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query bins by status: " + status, e);
        }
        return list;
    }

    @Override
    public List<Bin> findRequiringPickup(String zoneId, double minPriorityOrFill) {
        List<Bin> list = new ArrayList<>();
        String sql = "SELECT * FROM bins WHERE zone_id = ? AND (fill_percentage >= ? OR status IN ('WARNING', 'OVERFLOW')) ORDER BY fill_percentage DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zoneId);
            pstmt.setDouble(2, minPriorityOrFill);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBin(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query bins requiring pickup in zone: " + zoneId, e);
        }
        return list;
    }

    @Override
    public List<Bin> findPage(int offset, int limit, String zoneId, String wasteType, String search) {
        List<Bin> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM bins WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (zoneId != null && !zoneId.isBlank() && !zoneId.equalsIgnoreCase("ALL")) {
            sql.append("AND zone_id = ? ");
            params.add(zoneId);
        }
        if (wasteType != null && !wasteType.isBlank() && !wasteType.equalsIgnoreCase("ALL")) {
            sql.append("AND waste_type = ? ");
            params.add(wasteType);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (bin_code LIKE ? OR id LIKE ?) ");
            String pat = "%" + search.trim() + "%";
            params.add(pat);
            params.add(pat);
        }
        sql.append("ORDER BY fill_percentage DESC LIMIT ? OFFSET ?;");
        params.add(limit);
        params.add(offset);

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBin(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query paginated bins", e);
        }
        return list;
    }

    @Override
    public int count(String zoneId, String wasteType, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM bins WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (zoneId != null && !zoneId.isBlank() && !zoneId.equalsIgnoreCase("ALL")) {
            sql.append("AND zone_id = ? ");
            params.add(zoneId);
        }
        if (wasteType != null && !wasteType.isBlank() && !wasteType.equalsIgnoreCase("ALL")) {
            sql.append("AND waste_type = ? ");
            params.add(wasteType);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (bin_code LIKE ? OR id LIKE ?) ");
            String pat = "%" + search.trim() + "%";
            params.add(pat);
            params.add(pat);
        }

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count filtered bins", e);
        }
        return 0;
    }

    @Override
    public boolean updateFillLevel(String binId, double fillPercentage, String status) {
        String sql = "UPDATE bins SET fill_percentage = ?, status = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, fillPercentage);
            pstmt.setString(2, status);
            pstmt.setString(3, binId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update fill level for bin: " + binId, e);
        }
    }

    @Override
    public boolean recordCollection(String binId) {
        String sql = "UPDATE bins SET fill_percentage = 0.0, status = 'NORMAL', last_collected_at = ?, open_complaints = 0 WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, LocalDateTime.now().toString());
            pstmt.setString(2, binId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to record collection for bin: " + binId, e);
        }
    }

    @Override
    public void updateBatchFillLevels(List<Bin> bins) {
        if (bins == null || bins.isEmpty()) return;
        String sql = "UPDATE bins SET fill_percentage = ?, status = ? WHERE id = ?;";
        Connection conn = getConnection();
        try {
            boolean ac = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Bin b : bins) {
                    pstmt.setDouble(1, b.getCurrentFillPercentage());
                    pstmt.setString(2, b.getStatus().name());
                    pstmt.setString(3, b.getId());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            } finally {
                conn.setAutoCommit(ac);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute batch update for bins", e);
        }
    }

    @Override
    public Bin save(Bin bin) {
        String sql = "INSERT INTO bins (id, bin_code, zone_id, latitude, longitude, waste_type, capacity_liters, fill_percentage, last_collected_at, status, open_complaints) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, bin.getId());
            pstmt.setString(2, bin.getBinCode());
            pstmt.setString(3, bin.getZoneId());
            pstmt.setDouble(4, bin.getLocation().getLatitude());
            pstmt.setDouble(5, bin.getLocation().getLongitude());
            pstmt.setString(6, bin.getWasteType().name());
            pstmt.setDouble(7, bin.getCapacityLiters());
            pstmt.setDouble(8, bin.getCurrentFillPercentage());
            pstmt.setString(9, bin.getLastCollectedAt().toString());
            pstmt.setString(10, bin.getStatus().name());
            pstmt.setInt(11, bin.getOpenComplaintsCount());
            pstmt.executeUpdate();
            return bin;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save bin: " + bin.getBinCode(), e);
        }
    }

    @Override
    public boolean update(Bin bin) {
        String sql = "UPDATE bins SET bin_code = ?, zone_id = ?, latitude = ?, longitude = ?, waste_type = ?, capacity_liters = ?, fill_percentage = ?, last_collected_at = ?, status = ?, open_complaints = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, bin.getBinCode());
            pstmt.setString(2, bin.getZoneId());
            pstmt.setDouble(3, bin.getLocation().getLatitude());
            pstmt.setDouble(4, bin.getLocation().getLongitude());
            pstmt.setString(5, bin.getWasteType().name());
            pstmt.setDouble(6, bin.getCapacityLiters());
            pstmt.setDouble(7, bin.getCurrentFillPercentage());
            pstmt.setString(8, bin.getLastCollectedAt().toString());
            pstmt.setString(9, bin.getStatus().name());
            pstmt.setInt(10, bin.getOpenComplaintsCount());
            pstmt.setString(11, bin.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update bin: " + bin.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM bins WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete bin: " + id, e);
        }
    }

    private Bin mapBin(ResultSet rs) throws SQLException {
        Location loc = new Location(rs.getDouble("latitude"), rs.getDouble("longitude"), "Bin " + rs.getString("bin_code"));
        WasteType wt = WasteType.valueOf(rs.getString("waste_type"));
        BinStatus status = BinStatus.valueOf(rs.getString("status"));
        LocalDateTime lastCollected = LocalDateTime.parse(rs.getString("last_collected_at"));

        return new Bin(
                rs.getString("id"),
                rs.getString("bin_code"),
                rs.getString("zone_id"),
                loc,
                wt,
                rs.getDouble("capacity_liters"),
                rs.getDouble("fill_percentage"),
                lastCollected,
                status,
                rs.getInt("open_complaints")
        );
    }
}
