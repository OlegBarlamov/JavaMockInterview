package com.example.slotbooking.booking;

import com.example.slotbooking.client.CustomerClient;
import com.example.slotbooking.client.CustomerClient.Customer;
import com.example.slotbooking.client.NotificationClient;
import com.example.slotbooking.client.OrderClient;
import com.example.slotbooking.client.OrderClient.Order;
import com.example.slotbooking.client.RouteClient;
import com.example.slotbooking.client.RouteClient.ArrivalWindow;
import com.example.slotbooking.slot.Slot;
import com.example.slotbooking.slot.SlotRepository;
import com.example.slotbooking.warehouse.Warehouse;
import com.example.slotbooking.warehouse.WarehouseResolver;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private OrderClient orderClient;
    @Autowired
    private CustomerClient customerClient;
    @Autowired
    private NotificationClient notificationClient;
    @Autowired
    private RouteClient routeClient;
    @Autowired
    private WarehouseResolver warehouseResolver;

    public BookingResponse book(BookingRequest request) {
        return createBooking(request.customerId(), request.orderId(), request.slotId());
    }

    @Transactional
    public BookingResponse createBooking(String customerId, String orderId, long slotId) {
        Order order = orderClient.findOrder(orderId)
                .orElseThrow(() -> new RuntimeException("Order " + orderId + " not found"));
        if (!order.customerId().equals(customerId)) {
            throw new RuntimeException("Order " + orderId + " does not belong to customer " + customerId);
        }
        if (bookingRepository.findActiveByOrderId(orderId).isPresent()) {
            throw new RuntimeException("Order " + orderId + " already has an active booking");
        }

        Slot slot = slotRepository.findById(slotId).get();
        if (slot.remainingCapacity() <= 0) {
            throw new RuntimeException("Slot " + slotId + " is full");
        }
        if (Instant.now().isAfter(slot.startAt().minus(2, ChronoUnit.HOURS))) {
            throw new RuntimeException("Slot " + slotId + " is past its cutoff");
        }

        slotRepository.updateRemainingCapacity(slotId, slot.remainingCapacity() - 1);
        Booking booking = bookingRepository.insert(orderId, customerId, slotId, BookingState.ACTIVE);

        // notifications are slow, don't make the customer wait for them
        CompletableFuture.runAsync(() -> sendConfirmation(customerId, booking, slot));

        return BookingResponse.from(booking, slot, arrivalWindowFor(slot));
    }

    @Transactional
    public BookingResponse reschedule(long bookingId, long newSlotId, String customerId) {
        log.info("Customer {} is rescheduling booking {} to slot {}", customerId, bookingId, newSlotId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking " + bookingId + " not found"));
        if (booking.state() != BookingState.ACTIVE) {
            throw new RuntimeException("Only active bookings can be rescheduled");
        }

        // lock both slots so their capacity can't change under us
        Slot oldSlot = slotRepository.findByIdForUpdate(booking.slotId()).get();
        Slot newSlot = slotRepository.findByIdForUpdate(newSlotId).get();
        if (newSlot.remainingCapacity() <= 0) {
            throw new RuntimeException("Slot " + newSlotId + " is full");
        }

        slotRepository.updateRemainingCapacity(oldSlot.slotId(), oldSlot.remainingCapacity() + 1);
        slotRepository.updateRemainingCapacity(newSlot.slotId(), newSlot.remainingCapacity() - 1);
        bookingRepository.updateSlot(bookingId, newSlotId);

        Customer customer = customerClient.getCustomer(customerId);
        notificationClient.sendEmail(customer.email(), "Your delivery has been rescheduled",
                "Your new delivery slot is " + newSlot.startAt() + " - " + newSlot.endAt());
        notificationClient.sendPush(customerId, "Your delivery has been rescheduled");

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
        Warehouse warehouse = warehouseResolver.resolve(slot.postcode());
        return routeClient.getEstimatedArrivalWindow(warehouse.code(), LocalDate.ofInstant(slot.startAt(), ZoneId.systemDefault()));
    }
}
