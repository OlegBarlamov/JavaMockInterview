package com.example.slotbooking.slot;

import com.example.slotbooking.client.RouteClient;
import com.example.slotbooking.warehouse.Warehouse;
import com.example.slotbooking.warehouse.WarehouseResolver;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SlotService {

    private static final Duration CUTOFF = Duration.ofHours(2);

    private final SlotRepository slotRepository;
    private final WarehouseResolver warehouseResolver;
    private final RouteClient routeClient;

    public SlotService(SlotRepository slotRepository, WarehouseResolver warehouseResolver, RouteClient routeClient) {
        this.slotRepository = slotRepository;
        this.warehouseResolver = warehouseResolver;
        this.routeClient = routeClient;
    }

    public List<SlotResponse> listAvailableSlots(String postcode, LocalDate date) {
        Warehouse warehouse = warehouseResolver.resolve(postcode);

        // date is in the warehouse's local time, slots are stored in UTC
        Instant dayStart = date.atStartOfDay().toInstant(warehouse.utcOffset());
        Instant dayEnd = dayStart.plus(Duration.ofDays(1));

        return slotRepository.findAvailable(postcode, dayStart, dayEnd).stream()
                .filter(slot -> slot.startAt().minus(CUTOFF).isAfter(Instant.now()))
                .map(slot -> SlotResponse.from(slot, routeClient.getEstimatedArrivalWindow(warehouse.code(), date)))
                .toList();
    }
}
