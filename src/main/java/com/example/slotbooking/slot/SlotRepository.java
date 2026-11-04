package com.example.slotbooking.slot;

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
public class SlotRepository {

    private final DSLContext dsl;

    public SlotRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Slot> findAvailable(String postcode, Instant from, Instant to) {
        return dsl.select()
                .from(SLOT)
                .where(SLOT_POSTCODE.eq(postcode))
                .and(SLOT_START_AT.ge(from))
                .and(SLOT_START_AT.lt(to))
                .and(SLOT_REMAINING_CAPACITY.gt(0))
                .orderBy(SLOT_START_AT)
                .fetch(this::toSlot);
    }

    public Optional<Slot> findById(long slotId) {
        return dsl.select()
                .from(SLOT)
                .where(SLOT_ID.eq(slotId))
                .fetchOptional(this::toSlot);
    }

    public Optional<Slot> findByIdForUpdate(long slotId) {
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

    private Slot toSlot(Record r) {
        return new Slot(
                r.get(SLOT_ID),
                r.get(SLOT_POSTCODE),
                r.get(SLOT_START_AT),
                r.get(SLOT_END_AT),
                r.get(SLOT_TOTAL_CAPACITY),
                r.get(SLOT_REMAINING_CAPACITY));
    }
}
