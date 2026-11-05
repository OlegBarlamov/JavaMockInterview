CREATE TABLE booking
(
    booking_id  BIGSERIAL PRIMARY KEY,
    order_id    VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    slot_id     BIGINT      NOT NULL REFERENCES slot (slot_id),
    state       VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX booking_order_id_idx ON booking (order_id);
