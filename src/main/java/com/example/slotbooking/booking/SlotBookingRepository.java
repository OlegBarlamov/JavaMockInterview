package com.example.slotbooking.booking;

import static com.example.slotbooking.db.Tables.BOOKING;
import static com.example.slotbooking.db.Tables.BOOKING_CUSTOMER_ID;
import static com.example.slotbooking.db.Tables.BOOKING_ID;
import static com.example.slotbooking.db.Tables.BOOKING_ORDER_ID;
import static com.example.slotbooking.db.Tables.BOOKING_SLOT_ID;
import static com.example.slotbooking.db.Tables.BOOKING_STATE;
import static com.example.slotbooking.db.Tables.SLOT;
import static com.example.slotbooking.db.Tables.SLOT_END_AT;
import static com.example.slotbooking.db.Tables.SLOT_ID;
import static com.example.slotbooking.db.Tables.SLOT_POSTCODE;
import static com.example.slotbooking.db.Tables.SLOT_REMAINING_CAPACITY;
import static com.example.slotbooking.db.Tables.SLOT_START_AT;
import static com.example.slotbooking.db.Tables.SLOT_TOTAL_CAPACITY;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
public class SlotBookingRepository {

    private final DSLContext dsl;

    public SlotBookingRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // ---- slots ----

    public List<Slot> findAvailableSlots(String postcode, Instant from, Instant to) {
        return dsl.select()
                .from(SLOT)
                .where(SLOT_POSTCODE.eq(postcode))
                .and(SLOT_START_AT.ge(from))
                .and(SLOT_START_AT.lt(to))
                .and(SLOT_REMAINING_CAPACITY.gt(0))
                .orderBy(SLOT_START_AT)
                .fetch(this::toSlot);
    }

    public Optional<Slot> findSlot(long slotId) {
        return dsl.select()
                .from(SLOT)
                .where(SLOT_ID.eq(slotId))
                .fetchOptional(this::toSlot);
    }

    public Optional<Slot> findSlotForUpdate(long slotId) {
        return dsl.select()
                .from(SLOT)
                .where(SLOT_ID.eq(slotId))
                .forUpdate()
                .fetchOptional(this::toSlot);
    }

    public void updateRemainingCapacity(long slotId, int remainingCapacity) {
        dsl.update(SLOT)
                .set(SLOT_REMAINING_CAPACITY, remainingCapacity)
                .where(SLOT_ID.eq(slotId))
                .execute();
    }

    // ---- bookings ----

    public Optional<Booking> findBooking(long bookingId) {
        return dsl.select()
                .from(BOOKING)
                .where(BOOKING_ID.eq(bookingId))
                .fetchOptional(this::toBooking);
    }

    public Optional<Booking> findActiveBookingByOrderId(String orderId) {
        return dsl.select()
                .from(BOOKING)
                .where(BOOKING_ORDER_ID.eq(orderId))
                .and(BOOKING_STATE.eq(BookingState.ACTIVE.name()))
                .fetchOptional(this::toBooking);
    }

    public Booking insertBooking(String orderId, String customerId, long slotId, BookingState state) {
        long bookingId = dsl.insertInto(BOOKING)
                .set(BOOKING_ORDER_ID, orderId)
                .set(BOOKING_CUSTOMER_ID, customerId)
                .set(BOOKING_SLOT_ID, slotId)
                .set(BOOKING_STATE, state.name())
                .returningResult(BOOKING_ID)
                .fetchOne()
                .value1();
        return new Booking(bookingId, orderId, customerId, slotId, state);
    }

    public void updateBookingSlot(long bookingId, long slotId) {
        dsl.update(BOOKING)
                .set(BOOKING_SLOT_ID, slotId)
                .where(BOOKING_ID.eq(bookingId))
                .execute();
    }

    private Slot toSlot(Record r) {
        return new Slot(
                r.get(SLOT_ID),
                r.get(SLOT_POSTCODE),
                r.get(SLOT_START_AT),
                r.get(SLOT_END_AT),
                r.get(SLOT_TOTAL_CAPACITY),
                r.get(SLOT_REMAINING_CAPACITY));
    }

    private Booking toBooking(Record r) {
        return new Booking(
                r.get(BOOKING_ID),
                r.get(BOOKING_ORDER_ID),
                r.get(BOOKING_CUSTOMER_ID),
                r.get(BOOKING_SLOT_ID),
                BookingState.valueOf(r.get(BOOKING_STATE)));
    }

    public record Slot(long slotId, String postcode, Instant startAt, Instant endAt,
                       int totalCapacity, int remainingCapacity) {
    }

    public record Booking(long bookingId, String orderId, String customerId, long slotId, BookingState state) {
    }

    public enum BookingState {
        ACTIVE, RESCHEDULED, CANCELLED
    }
}
