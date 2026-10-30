package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ComplaintDaoImpl extends BaseDao<Complaint, String> implements ComplaintDao {

    @Override
    public Optional<Complaint> findById(String id) {
        String sql = "SELECT * FROM complaints WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapComplaint(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query complaint by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Complaint> findAll() {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints ORDER BY created_at DESC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapComplaint(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query complaints", e);
        }
        return list;
    }

    @Override
    public List<Complaint> findByCitizen(String citizenId) {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints WHERE citizen_id = ? ORDER BY created_at DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, citizenId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapComplaint(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query complaints by citizen: " + citizenId, e);
        }
        return list;
    }

    @Override
    public List<Complaint> findByCollector(String collectorId) {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints WHERE assigned_collector_id = ? AND status != 'RESOLVED' ORDER BY created_at ASC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, collectorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapComplaint(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query complaints for collector: " + collectorId, e);
        }
        return list;
    }

    @Override
    public List<Complaint> findByZone(String zoneId) {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints WHERE zone_id = ? ORDER BY created_at DESC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zoneId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapComplaint(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query complaints by zone: " + zoneId, e);
        }
        return list;
    }

    @Override
    public List<Complaint> findUnresolved() {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints WHERE status != 'RESOLVED' ORDER BY created_at ASC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapComplaint(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query unresolved complaints", e);
        }
        return list;
    }

    @Override
    public List<Complaint> findSlaBreached() {
        List<Complaint> list = new ArrayList<>();
        List<Complaint> unresolved = findUnresolved();
        for (Complaint c : unresolved) {
            if (c.isSlaBreached() && c.getStatus() != ComplaintStatus.ESCALATED) {
                list.add(c);
            }
        }
        return list;
    }

    @Override
    public boolean updateStatus(String complaintId, ComplaintStatus status, String resolutionNotes) {
        String sql = (status == ComplaintStatus.RESOLVED) ?
                "UPDATE complaints SET status = ?, resolved_at = ?, resolution_notes = ? WHERE id = ?;" :
                "UPDATE complaints SET status = ?, resolution_notes = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            if (status == ComplaintStatus.RESOLVED) {
                pstmt.setString(1, status.name());
                pstmt.setString(2, LocalDateTime.now().toString());
                pstmt.setString(3, resolutionNotes != null ? resolutionNotes : "");
                pstmt.setString(4, complaintId);
            } else {
                pstmt.setString(1, status.name());
                pstmt.setString(2, resolutionNotes != null ? resolutionNotes : "");
                pstmt.setString(3, complaintId);
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update status for complaint: " + complaintId, e);
        }
    }

    @Override
    public boolean assignCollector(String complaintId, String collectorId) {
        String sql = "UPDATE complaints SET assigned_collector_id = ?, status = CASE WHEN status = 'ESCALATED' THEN 'ESCALATED' ELSE 'ASSIGNED' END WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, collectorId);
            pstmt.setString(2, complaintId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to assign collector to complaint: " + complaintId, e);
        }
    }

    @Override
    public List<ComplaintEvent> findEventsForComplaint(String complaintId) {
        List<ComplaintEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM complaint_events WHERE complaint_id = ? ORDER BY timestamp ASC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, complaintId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ComplaintStatus from = rs.getString("from_status") != null ? ComplaintStatus.valueOf(rs.getString("from_status")) : null;
                    ComplaintStatus to = ComplaintStatus.valueOf(rs.getString("to_status"));
                    list.add(new ComplaintEvent(
                            rs.getString("id"),
                            rs.getString("complaint_id"),
                            from,
                            to,
                            LocalDateTime.parse(rs.getString("timestamp")),
                            rs.getString("remarks")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query events for complaint: " + complaintId, e);
        }
        return list;
    }

    @Override
    public void logEvent(ComplaintEvent event) {
        String sql = "INSERT INTO complaint_events (id, complaint_id, from_status, to_status, timestamp, remarks) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, event.getId());
            pstmt.setString(2, event.getComplaintId());
            pstmt.setString(3, event.getFromStatus() != null ? event.getFromStatus().name() : null);
            pstmt.setString(4, event.getToStatus().name());
            pstmt.setString(5, event.getTimestamp().toString());
            pstmt.setString(6, event.getRemarks());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to log complaint event", e);
        }
    }

    @Override
    public Complaint save(Complaint c) {
        String sql = "INSERT INTO complaints (id, citizen_id, complaint_type, description, latitude, longitude, zone_id, status, assigned_collector_id, created_at, resolved_at, resolution_notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, c.getId());
            pstmt.setString(2, c.getCitizenId());
            pstmt.setString(3, c.getComplaintType().name());
            pstmt.setString(4, c.getDescription());
            pstmt.setDouble(5, c.getLocation().getLatitude());
            pstmt.setDouble(6, c.getLocation().getLongitude());
            pstmt.setString(7, c.getZoneId());
            pstmt.setString(8, c.getStatus().name());
            pstmt.setString(9, c.getAssignedCollectorId());
            pstmt.setString(10, c.getCreatedAt().toString());
            pstmt.setString(11, c.getResolvedAt() != null ? c.getResolvedAt().toString() : null);
            pstmt.setString(12, c.getResolutionNotes());
            pstmt.executeUpdate();
            return c;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save complaint: " + c.getId(), e);
        }
    }

    @Override
    public boolean update(Complaint c) {
        String sql = "UPDATE complaints SET complaint_type = ?, description = ?, status = ?, assigned_collector_id = ?, resolved_at = ?, resolution_notes = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, c.getComplaintType().name());
            pstmt.setString(2, c.getDescription());
            pstmt.setString(3, c.getStatus().name());
            pstmt.setString(4, c.getAssignedCollectorId());
            pstmt.setString(5, c.getResolvedAt() != null ? c.getResolvedAt().toString() : null);
            pstmt.setString(6, c.getResolutionNotes());
            pstmt.setString(7, c.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update complaint: " + c.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM complaints WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete complaint: " + id, e);
        }
    }

    private Complaint mapComplaint(ResultSet rs) throws SQLException {
        Location loc = new Location(rs.getDouble("latitude"), rs.getDouble("longitude"), "Complaint location");
        ComplaintType type = ComplaintType.valueOf(rs.getString("complaint_type"));
        ComplaintStatus status = ComplaintStatus.valueOf(rs.getString("status"));
        LocalDateTime created = LocalDateTime.parse(rs.getString("created_at"));
        LocalDateTime resolved = rs.getString("resolved_at") != null ? LocalDateTime.parse(rs.getString("resolved_at")) : null;

        return new Complaint(
                rs.getString("id"),
                rs.getString("citizen_id"),
                type,
                rs.getString("description"),
                loc,
                rs.getString("zone_id"),
                status,
                rs.getString("assigned_collector_id"),
                created,
                resolved,
                rs.getString("resolution_notes")
        );
    }
}
