package com.wasteconnect.dao;

import com.wasteconnect.model.Notification;

import java.util.List;

public interface NotificationDao extends Repository<Notification, String> {
    List<Notification> findByUser(String userId);
    int countUnread(String userId);
    boolean markAsRead(String notificationId);
    boolean markAllAsRead(String userId);
}
