package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.ListingStatus;
import com.wasteconnect.model.Location;
import com.wasteconnect.model.RecyclableListing;
import com.wasteconnect.model.WasteType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecyclableListingDaoImpl extends BaseDao<RecyclableListing, String> implements RecyclableListingDao {

    @Override
    public Optional<RecyclableListing> findById(String id) {
        String sql = "SELECT * FROM recyclable_listings WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query recyclable listing: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<RecyclableListing> findAll() {
        List<RecyclableListing> list = new ArrayList<>();
        String sql = "SELECT * FROM recyclable_listings ORDER BY created_at DESC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapListing(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query recyclable listings", e);
        }
        return list;
    }

    @Override
    public List<RecyclableListing> findByStatus(ListingStatus status) {
        List<RecyclableListing> list = new ArrayList<>();
        String sql = "SELECT * FROM recyclable_listings WHERE status = ? ORDER BY created_at DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query listings by status: " + status, e);
        }
        return list;
    }

    @Override
    public List<RecyclableListing> findByCitizen(String citizenId) {
        List<RecyclableListing> list = new ArrayList<>();
        String sql = "SELECT * FROM recyclable_listings WHERE citizen_id = ? ORDER BY created_at DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, citizenId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query listings for citizen: " + citizenId, e);
        }
        return list;
    }

    @Override
    public List<RecyclableListing> findByPicker(String pickerId) {
        List<RecyclableListing> list = new ArrayList<>();
        String sql = "SELECT * FROM recyclable_listings WHERE claimed_picker_id = ? ORDER BY created_at DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, pickerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query listings for picker: " + pickerId, e);
        }
        return list;
    }

    @Override
    public boolean claimListing(String listingId, String pickerId) {
        String sql = "UPDATE recyclable_listings SET status = 'CLAIMED', claimed_picker_id = ? WHERE id = ? AND status = 'AVAILABLE';";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, pickerId);
            pstmt.setString(2, listingId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to claim recyclable listing: " + listingId, e);
        }
    }

    @Override
    public boolean completeCollection(String listingId, String pickerId, double actualWeight, double finalEarnings) {
        String sql = "UPDATE recyclable_listings SET status = 'COLLECTED', weight_kg = ?, estimated_value = ?, collected_at = ? WHERE id = ? AND claimed_picker_id = ? AND status = 'CLAIMED';";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, actualWeight);
            pstmt.setDouble(2, finalEarnings);
            pstmt.setString(3, LocalDateTime.now().toString());
            pstmt.setString(4, listingId);
            pstmt.setString(5, pickerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to complete collection for listing: " + listingId, e);
        }
    }

    @Override
    public RecyclableListing save(RecyclableListing l) {
        String sql = "INSERT INTO recyclable_listings (id, citizen_id, waste_type, weight_kg, address, latitude, longitude, status, claimed_picker_id, created_at, collected_at, estimated_value) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, l.getId());
            pstmt.setString(2, l.getCitizenId());
            pstmt.setString(3, l.getWasteType().name());
            pstmt.setDouble(4, l.getWeightKg());
            pstmt.setString(5, l.getAddress());
            pstmt.setDouble(6, l.getLocation().getLatitude());
            pstmt.setDouble(7, l.getLocation().getLongitude());
            pstmt.setString(8, l.getStatus().name());
            pstmt.setString(9, l.getClaimedPickerId());
            pstmt.setString(10, l.getCreatedAt().toString());
            pstmt.setString(11, l.getCollectedAt() != null ? l.getCollectedAt().toString() : null);
            pstmt.setDouble(12, l.getEstimatedValue());
            pstmt.executeUpdate();
            return l;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save recyclable listing: " + l.getId(), e);
        }
    }

    @Override
    public boolean update(RecyclableListing l) {
        String sql = "UPDATE recyclable_listings SET waste_type = ?, weight_kg = ?, address = ?, latitude = ?, longitude = ?, status = ?, claimed_picker_id = ?, collected_at = ?, estimated_value = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, l.getWasteType().name());
            pstmt.setDouble(2, l.getWeightKg());
            pstmt.setString(3, l.getAddress());
            pstmt.setDouble(4, l.getLocation().getLatitude());
            pstmt.setDouble(5, l.getLocation().getLongitude());
            pstmt.setString(6, l.getStatus().name());
            pstmt.setString(7, l.getClaimedPickerId());
            pstmt.setString(8, l.getCollectedAt() != null ? l.getCollectedAt().toString() : null);
            pstmt.setDouble(9, l.getEstimatedValue());
            pstmt.setString(10, l.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update recyclable listing: " + l.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM recyclable_listings WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete recyclable listing: " + id, e);
        }
    }

    private RecyclableListing mapListing(ResultSet rs) throws SQLException {
        Location loc = new Location(rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("address"));
        LocalDateTime collected = rs.getString("collected_at") != null ? LocalDateTime.parse(rs.getString("collected_at")) : null;

        return new RecyclableListing(
                rs.getString("id"),
                rs.getString("citizen_id"),
                WasteType.valueOf(rs.getString("waste_type")),
                rs.getDouble("weight_kg"),
                rs.getString("address"),
                loc,
                ListingStatus.valueOf(rs.getString("status")),
                rs.getString("claimed_picker_id"),
                LocalDateTime.parse(rs.getString("created_at")),
                collected,
                rs.getDouble("estimated_value")
        );
    }
}
