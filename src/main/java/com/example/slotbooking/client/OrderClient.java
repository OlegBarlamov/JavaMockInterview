package com.example.slotbooking.client;

import java.util.Optional;

/** Synchronous (blocking) client for the Order service. */
public interface OrderClient {

    Optional<Order> findOrder(String orderId);

    record Order(String orderId, String customerId, String deliveryPostcode, OrderStatus status) {
    }

    enum OrderStatus {
        OPEN, CANCELLED, DELIVERED
    }
}
