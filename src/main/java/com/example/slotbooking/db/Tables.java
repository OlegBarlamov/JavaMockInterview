package com.example.slotbooking.db;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.time.Instant;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

/**
 * Hand-written jOOQ table references (instead of code generation) to keep the project self-contained.
 */
public final class Tables {

    public static final Table<Record> SLOT = table(name("slot"));
    public static final Field<Long> SLOT_ID = field(name("slot", "slot_id"), SQLDataType.BIGINT);
    public static final Field<String> SLOT_POSTCODE = field(name("slot", "postcode"), SQLDataType.VARCHAR);
    public static final Field<Instant> SLOT_START_AT = field(name("slot", "start_at"), SQLDataType.INSTANT);
    public static final Field<Instant> SLOT_END_AT = field(name("slot", "end_at"), SQLDataType.INSTANT);
    public static final Field<Integer> SLOT_TOTAL_CAPACITY = field(name("slot", "total_capacity"), SQLDataType.INTEGER);
    public static final Field<Integer> SLOT_REMAINING_CAPACITY = field(name("slot", "remaining_capacity"), SQLDataType.INTEGER);

    private Tables() {
    }
}
