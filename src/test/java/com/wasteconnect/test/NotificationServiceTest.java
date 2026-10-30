package com.wasteconnect.test;

import com.wasteconnect.dao.DatabaseConnection;
import com.wasteconnect.dao.NotificationDao;
import com.wasteconnect.dao.NotificationDaoImpl;
import com.wasteconnect.model.Notification;
import com.wasteconnect.service.NotificationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {
    @BeforeAll
    static void initializeDatabase() {
        DatabaseConnection.getInstance();
    }

    @Test
    void persistsNotificationAndUpdatesUnreadState() throws InterruptedException {
        NotificationDao notificationDao = new NotificationDaoImpl();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        NotificationService service = new NotificationService(notificationDao, executor);
        String title = "Notification test " + UUID.randomUUID();
        Set<String> previousIds = notificationDao.findByUser("ADM001").stream()
                .map(Notification::getId).collect(Collectors.toSet());

        try {
            service.sendNotification("ADM001", title, "Test message");
            service.shutdown();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));

            Notification stored = notificationDao.findByUser("ADM001").stream()
                    .filter(notification -> notification.getTitle().equals(title)).findFirst().orElseThrow();
            assertFalse(stored.isRead());
            assertTrue(notificationDao.countUnread("ADM001") > 0);

            service.markAsRead(stored.getId());
            assertTrue(notificationDao.findById(stored.getId()).orElseThrow().isRead());
        } finally {
            service.shutdown();
            notificationDao.findByUser("ADM001").stream()
                    .filter(notification -> !previousIds.contains(notification.getId()))
                    .filter(notification -> notification.getTitle().equals(title))
                    .forEach(notification -> notificationDao.deleteById(notification.getId()));
        }
    }

}
