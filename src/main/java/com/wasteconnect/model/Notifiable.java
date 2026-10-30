package com.wasteconnect.model;

/**
 * ABSTRACTION: Interface representing any recipient capable of receiving operational or status notifications.
 */
public interface Notifiable {
    String getRecipientEmail();
    String getRecipientName();
    void notifyUser(String title, String message);
}
