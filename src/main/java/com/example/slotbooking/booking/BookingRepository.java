package com.example.slotbooking.booking;

import static com.example.slotbooking.db.Tables.BOOKING;
import static com.example.slotbooking.db.Tables.BOOKING_CUSTOMER_ID;
import static com.example.slotbooking.db.Tables.BOOKING_ID;
import static com.example.slotbooking.db.Tables.BOOKING_ORDER_ID;
import static com.example.slotbooking.db.Tables.BOOKING_SLOT_ID;
import static com.example.slotbooking.db.Tables.BOOKING_STATE;

import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
public class BookingRepository {

    private final DSLContext dsl;

    public BookingRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Booking> findById(long bookingId) {
        return dsl.select()
                .from(BOOKING)
                .where(BOOKING_ID.eq(bookingId))
                .fetchOptional(this::toBooking);
    }

    public Optional<Booking> findActiveByOrderId(String orderId) {
        return dsl.select()
                .from(BOOKING)
                .where(BOOKING_ORDER_ID.eq(orderId))
                .and(BOOKING_STATE.eq(BookingState.ACTIVE.name()))
                .fetchOptional(this::toBooking);
    }

    public Booking insert(String orderId, String customerId, long slotId, BookingState state) {
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

    public void updateSlot(long bookingId, long slotId) {
        dsl.update(BOOKING)
                .set(BOOKING_SLOT_ID, slotId)
                .where(BOOKING_ID.eq(bookingId))
                .execute();
    }

    private Booking toBooking(Record r) {
        return new Booking(
                r.get(BOOKING_ID),
                r.get(BOOKING_ORDER_ID),
                r.get(BOOKING_CUSTOMER_ID),
                r.get(BOOKING_SLOT_ID),
                BookingState.valueOf(r.get(BOOKING_STATE)));
    }
}
