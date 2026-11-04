package com.example.slotbooking.booking;

import com.example.slotbooking.booking.SlotBookingRepository.Slot;
import com.example.slotbooking.client.RouteClient;
import com.example.slotbooking.client.RouteClient.ArrivalWindow;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/slots")
public class SlotController {

    private static final Duration CUTOFF = Duration.ofHours(2);

    private static final Warehouse AMSTERDAM = new Warehouse("NL-AMS", ZoneOffset.ofHours(2));
    private static final Warehouse BERLIN = new Warehouse("DE-Ber", ZoneOffset.ofHours(2));
    private static final Warehouse PARIS = new Warehouse("FR-PAR", ZoneOffset.ofHours(2));

    // Postcode prefix -> warehouse. TODO: add London when UK launches
    private static final Map<String, Warehouse> WAREHOUSES_BY_PREFIX = Map.of(
            "10", AMSTERDAM,
            "11", AMSTERDAM,
            "12", BERLIN,
            "13", BERLIN,
            "14", BERLIN,
            "75", PARIS);

    private final SlotBookingRepository repository;
    private final RouteClient routeClient;

    public SlotController(SlotBookingRepository repository, RouteClient routeClient) {
        this.repository = repository;
        this.routeClient = routeClient;
    }

    @GetMapping
    public List<SlotResponse> listSlots(
            @RequestParam String postcode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Warehouse warehouse = warehouseFor(postcode);

        // date is in the warehouse's local time, slots are stored in UTC
        Instant dayStart = date.atStartOfDay().toInstant(warehouse.utcOffset());
        Instant dayEnd = dayStart.plus(Duration.ofDays(1));

        return repository.findAvailableSlots(postcode, dayStart, dayEnd).stream()
                .filter(slot -> slot.startAt().minus(CUTOFF).isAfter(Instant.now()))
                .map(slot -> SlotResponse.from(slot, routeClient.getEstimatedArrivalWindow(warehouse.code(), date)))
                .toList();
    }

    static Warehouse warehouseFor(String postcode) {
        return WAREHOUSES_BY_PREFIX.get(postcode.substring(0, 2));
    }

    record Warehouse(String code, ZoneOffset utcOffset) {
    }

    public record SlotResponse(long slotId, String postcode, Instant startAt, Instant endAt,
                               int remainingCapacity, long estimatedArrivalWindowMinutes) {

        static SlotResponse from(Slot slot, ArrivalWindow arrivalWindow) {
            return new SlotResponse(slot.slotId(), slot.postcode(), slot.startAt(), slot.endAt(),
                    slot.remainingCapacity(), arrivalWindow.width().toMinutes());
        }
    }
}
