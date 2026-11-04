package com.example.slotbooking.slot;

import com.example.slotbooking.client.RouteClient.ArrivalWindow;
import java.time.Instant;

public record SlotResponse(
        long slotId,
        String postcode,
        Instant startAt,
        Instant endAt,
        int remainingCapacity,
        long estimatedArrivalWindowMinutes) {

    static SlotResponse from(Slot slot, ArrivalWindow arrivalWindow) {
        return new SlotResponse(
                slot.slotId(),
                slot.postcode(),
                slot.startAt(),
                slot.endAt(),
                slot.remainingCapacity(),
                arrivalWindow.width().toMinutes());
    }
}
