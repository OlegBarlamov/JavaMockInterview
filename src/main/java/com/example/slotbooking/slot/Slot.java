package com.example.slotbooking.slot;

import java.time.Instant;

public record Slot(
        long slotId,
        String postcode,
        Instant startAt,
        Instant endAt,
        int totalCapacity,
        int remainingCapacity) {
}
