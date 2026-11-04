package com.example.slotbooking.booking;

import com.example.slotbooking.client.RouteClient.ArrivalWindow;
import com.example.slotbooking.slot.Slot;
import java.time.Instant;

public record BookingResponse(
        long bookingId,
        String orderId,
        long slotId,
        BookingState state,
        Instant startAt,
        Instant endAt,
        long estimatedArrivalWindowMinutes) {

    static BookingResponse from(Booking booking, Slot slot, ArrivalWindow arrivalWindow) {
        return new BookingResponse(
                booking.bookingId(),
                booking.orderId(),
                booking.slotId(),
                booking.state(),
                slot.startAt(),
                slot.endAt(),
                arrivalWindow.width().toMinutes());
    }
}
