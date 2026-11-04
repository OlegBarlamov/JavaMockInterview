package com.example.slotbooking.client;

/** Synchronous (blocking) client for the Customer service. */
public interface CustomerClient {

    Customer getCustomer(String customerId);

    record Customer(String customerId, String email) {
    }
}
