package com.example.slotbooking.booking;

import com.example.slotbooking.booking.SlotBookingRepository.Booking;
import com.example.slotbooking.booking.SlotBookingRepository.BookingState;
import com.example.slotbooking.booking.SlotBookingRepository.Slot;
import com.example.slotbooking.client.CustomerClient;
import com.example.slotbooking.client.CustomerClient.Customer;
import com.example.slotbooking.client.NotificationClient;
import com.example.slotbooking.client.OrderClient;
import com.example.slotbooking.client.OrderClient.Order;
import com.example.slotbooking.client.RouteClient;
import com.example.slotbooking.client.RouteClient.ArrivalWindow;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private SlotBookingRepository repository;
    @Autowired
    private OrderClient orderClient;
    @Autowired
    private CustomerClient customerClient;
    @Autowired
    private NotificationClient notificationClient;
    @Autowired
    private RouteClient routeClient;

    @PostMapping
    public BookingResponse book(@RequestBody BookingRequest request) {
        return createBooking(request.customerId(), request.orderId(), request.slotId());
    }

    @Transactional
    public BookingResponse createBooking(String customerId, String orderId, long slotId) {
        Order order = orderClient.findOrder(orderId)
                .orElseThrow(() -> new RuntimeException("Order " + orderId + " not found"));
        if (!order.customerId().equals(customerId)) {
            throw new RuntimeException("Order " + orderId + " does not belong to customer " + customerId);
        }
        if (repository.findActiveBookingByOrderId(orderId).isPresent()) {
            throw new RuntimeException("Order " + orderId + " already has an active booking");
        }

        Slot slot = repository.findSlot(slotId).get();
        if (slot.remainingCapacity() <= 0) {
            throw new RuntimeException("Slot " + slotId + " is full");
        }
        if (Instant.now().isAfter(slot.startAt().minus(2, ChronoUnit.HOURS))) {
            throw new RuntimeException("Slot " + slotId + " is past its cutoff");
        }

        repository.updateRemainingCapacity(slotId, slot.remainingCapacity() - 1);
        Booking booking = repository.insertBooking(orderId, customerId, slotId, BookingState.ACTIVE);

        sendConfirmation(customerId, booking, slot);

        return BookingResponse.from(booking, slot, arrivalWindowFor(slot));
    }

    @PutMapping("/{id}")
    @Transactional
    public BookingResponse reschedule(@PathVariable long id, @RequestBody RescheduleRequest request) {
        long newSlotId = request.newSlotId();
        log.info("Rescheduling booking {} to slot {}", id, newSlotId);

        Booking booking = repository.findBooking(id)
                .orElseThrow(() -> new RuntimeException("Booking " + id + " not found"));
        if (booking.state() != BookingState.ACTIVE) {
            throw new RuntimeException("Only active bookings can be rescheduled");
        }

        // lock both slots so their capacity can't change under us
        Slot oldSlot = repository.findSlotForUpdate(booking.slotId()).get();
        Slot newSlot = repository.findSlotForUpdate(newSlotId).get();
        if (newSlot.remainingCapacity() <= 0) {
            throw new RuntimeException("Slot " + newSlotId + " is full");
        }

        repository.updateRemainingCapacity(oldSlot.slotId(), oldSlot.remainingCapacity() + 1);
        repository.updateRemainingCapacity(newSlot.slotId(), newSlot.remainingCapacity() - 1);
        repository.updateBookingSlot(id, newSlotId);

        Customer customer = customerClient.getCustomer(booking.customerId());
        notificationClient.sendEmail(customer.email(), "Your delivery has been rescheduled",
                "Your new delivery slot is " + newSlot.startAt() + " - " + newSlot.endAt());
        notificationClient.sendPush(booking.customerId(), "Your delivery has been rescheduled");

        return BookingResponse.from(booking, newSlot, arrivalWindowFor(newSlot));
    }

    private void sendConfirmation(String customerId, Booking booking, Slot slot) {
        Customer customer = customerClient.getCustomer(customerId);
        log.info("Sending booking confirmation to {} for booking {}", customer.email(), booking.bookingId());
        String message = "Your delivery is booked for " + slot.startAt() + " - " + slot.endAt();
        notificationClient.sendEmail(customer.email(), "Your delivery is booked", message);
        notificationClient.sendPush(customerId, message);
    }

    private ArrivalWindow arrivalWindowFor(Slot slot) {
        String warehouseCode = SlotController.warehouseFor(slot.postcode()).code();
        return routeClient.getEstimatedArrivalWindow(warehouseCode, LocalDate.ofInstant(slot.startAt(), ZoneId.systemDefault()));
    }

    public record BookingRequest(String orderId, long slotId, String customerId) {
    }

    public record RescheduleRequest(long newSlotId) {
    }

    public record BookingResponse(long bookingId, String orderId, long slotId, BookingState state,
                                  Instant startAt, Instant endAt, long estimatedArrivalWindowMinutes) {

        static BookingResponse from(Booking booking, Slot slot, ArrivalWindow arrivalWindow) {
            return new BookingResponse(booking.bookingId(), booking.orderId(), booking.slotId(), booking.state(),
                    slot.startAt(), slot.endAt(), arrivalWindow.width().toMinutes());
        }
    }
}
