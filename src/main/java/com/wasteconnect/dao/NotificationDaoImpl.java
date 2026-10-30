package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationDaoImpl extends BaseDao<Notification, String> implements NotificationDao {

    @Override
    public Optional<Notification> findById(String id) {
        String sql = "SELECT * FROM notifications WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapNotif(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query notification by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Notification> findAll() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications ORDER BY created_at DESC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapNotif(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query all notifications", e);
        }
        return list;
    }

    @Override
    public List<Notification> findByUser(String userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapNotif(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query notifications for user: " + userId, e);
        }
        return list;
    }

    @Override
    public int countUnread(String userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read = 0;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count unread notifications for user: " + userId, e);
        }
        return 0;
    }

    @Override
    public boolean markAsRead(String notificationId) {
        String sql = "UPDATE notifications SET read = 1 WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark notification as read: " + notificationId, e);
        }
    }

    @Override
    public boolean markAllAsRead(String userId) {
        String sql = "UPDATE notifications SET read = 1 WHERE user_id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark all notifications as read for user: " + userId, e);
        }
    }

    @Override
    public Notification save(Notification n) {
        String sql = "INSERT INTO notifications (id, user_id, title, message, read, created_at) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, n.getId());
            pstmt.setString(2, n.getUserId());
            pstmt.setString(3, n.getTitle());
            pstmt.setString(4, n.getMessage());
            pstmt.setInt(5, n.isRead() ? 1 : 0);
            pstmt.setString(6, n.getCreatedAt().toString());
            pstmt.executeUpdate();
            return n;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save notification: " + n.getId(), e);
        }
    }

    @Override
    public boolean update(Notification n) {
        String sql = "UPDATE notifications SET read = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, n.isRead() ? 1 : 0);
            pstmt.setString(2, n.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update notification: " + n.getId(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM notifications WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete notification: " + id, e);
        }
    }

    private Notification mapNotif(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getInt("read") == 1,
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
