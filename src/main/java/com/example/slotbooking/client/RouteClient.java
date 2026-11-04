package com.example.slotbooking.client;

import java.time.Duration;
import java.time.LocalDate;

/** Synchronous (blocking) client for the Route planning service. */
public interface RouteClient {

    /**
     * How precisely a warehouse can promise the courier's arrival within a slot on a given (local) date,
     * e.g. a 20-minute window.
     */
    ArrivalWindow getEstimatedArrivalWindow(String warehouseCode, LocalDate date);

    record ArrivalWindow(Duration width) {
    }
}
