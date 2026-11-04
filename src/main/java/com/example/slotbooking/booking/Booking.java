package com.example.slotbooking.booking;

public record Booking(long bookingId, String orderId, String customerId, long slotId, BookingState state) {
}
