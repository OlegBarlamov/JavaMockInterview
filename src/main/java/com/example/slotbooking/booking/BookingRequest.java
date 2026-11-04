package com.example.slotbooking.booking;

public record BookingRequest(String orderId, long slotId, String customerId) {
}
