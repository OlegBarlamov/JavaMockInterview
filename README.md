# Delivery Slot Booking

Each warehouse pre-generates daily delivery slots per postcode with a fixed capacity. Customers book a
delivery slot for an existing order.

## Scope

Three REST endpoints:

- List available slots for a given postcode and date.
- Book a slot for an order.
- Reschedule an existing booking to a different slot.

**Out of scope:** cancellation, slot generation, payment.

## Domain

- **Slot:** `slot_id`, `postcode`, `start_at`, `end_at`, `total_capacity`, `remaining_capacity`. Times are in UTC.
- **Booking:** `booking_id`, `order_id`, `customer_id`, `slot_id`, `state` (one of `ACTIVE`, `RESCHEDULED`, `CANCELLED`).
- **Cutoff:** orders must be placed at least 2 hours before the slot's `start_at`.

### Warehouses

| Code   | Location  | Timezone                 |
|--------|-----------|--------------------------|
| NL-AMS | Amsterdam | `Europe/Amsterdam`, UTC+2 |
| DE-Ber | Berlin    | `Europe/Berlin`, UTC+2    |
| FR-PAR | Paris     | `Europe/Paris`, UTC+2     |

A UK launch (London, `Europe/London`, UTC+1) is planned for late 2026. Each postcode maps to exactly one warehouse.

## Functional requirements

### List slots — `GET /slots?postcode={pc}&date={date}`

- Returns only slots with `remaining_capacity > 0` and `start_at - 2h > now()`.
- `date` is interpreted in the warehouse's local timezone. The response uses UTC for `start_at` and `end_at`.
- Each slot in the response is enriched with the warehouse's `estimated_arrival_window` via `RouteClient`.

### Book — `POST /bookings`, body `{order_id, slot_id}`

- The requesting customer must own the order.
- The order must not already have an `ACTIVE` booking.
- The slot must have remaining capacity and be before its cutoff.
- Atomically: create the booking and decrement the slot's `remaining_capacity` by 1.
- Send a confirmation notification (email + push) via `NotificationClient`.
- Return the booking and the delivery window.

### Reschedule — `PUT /bookings/{id}`, body `{new_slot_id}`

- Booking must exist, be `ACTIVE`, and belong to the caller.
- Atomically: release the old slot (+1), decrement the new slot (−1), update the booking to the new slot.
- Send a reschedule notification.

## Non-functional requirements

- Java 25, Spring Boot, REST, PostgreSQL via jOOQ.
- JUnit 5 + AssertJ for tests, Mockito for mocking.
- Stubbed clients exist and are injected: `OrderClient`, `CustomerClient`, `NotificationClient`, `RouteClient`. All synchronous.
- The request principal includes `customer_id` (set by an upstream auth filter, see `CustomerAuthFilter`).

## Running locally

```bash
export DB_PASSWORD=...   # from your local secrets store
mvn spring-boot:run
```

Requires a local PostgreSQL (`DB_URL`, `DB_USER` can be overridden). Flyway applies the schema on startup.
