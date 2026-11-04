package com.example.slotbooking.client;

/** Synchronous (blocking) client for the Notification service. */
public interface NotificationClient {

    void sendEmail(String to, String subject, String body);

    void sendPush(String customerId, String message);
}
