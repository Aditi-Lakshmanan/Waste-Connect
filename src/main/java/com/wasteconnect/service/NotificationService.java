package com.wasteconnect.service;

import com.wasteconnect.dao.NotificationDao;
import com.wasteconnect.dao.NotificationDaoImpl;
import com.wasteconnect.model.Notification;
import com.wasteconnect.pattern.EventBus;
import com.wasteconnect.util.AppLogger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CO4: Asynchronous notification service utilizing ExecutorService for non-blocking notification dispatch.
 */
public class NotificationService {
    private final NotificationDao notificationDao;
    private final ExecutorService dispatchPool;

    public NotificationService() {
        this(new NotificationDaoImpl(), Executors.newFixedThreadPool(2));
    }

    public NotificationService(NotificationDao notificationDao, ExecutorService dispatchPool) {
        this.notificationDao = notificationDao;
        this.dispatchPool = dispatchPool;
    }

    /**
     * Dispatches notification asynchronously off the caller thread.
     */
    public void sendNotification(String userId, String title, String message) {
        if (userId == null || title == null) return;
        dispatchPool.submit(() -> {
            try {
                String id = "NTF-" + UUID.randomUUID().toString().substring(0, 8);
                Notification notif = new Notification(id, userId, title, message);
                notificationDao.save(notif);
                EventBus.getInstance().publish(new EventBus.NotificationCreated(id, userId));
                AppLogger.info("Notification dispatched to user " + userId + ": " + title);
            } catch (Exception e) {
                AppLogger.warning("Failed to dispatch notification: " + e.getMessage());
            }
        });
    }

    public List<Notification> getUserNotifications(String userId) {
        return notificationDao.findByUser(userId);
    }

    public int getUnreadCount(String userId) {
        return notificationDao.countUnread(userId);
    }

    public void markAsRead(String notificationId) {
        notificationDao.markAsRead(notificationId);
    }

    public void markAllAsRead(String userId) {
        notificationDao.markAllAsRead(userId);
    }

    public void shutdown() {
        dispatchPool.shutdown();
    }
}
