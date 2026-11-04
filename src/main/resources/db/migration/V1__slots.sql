-- Slots are pre-generated daily per postcode by a separate job (out of scope for this service).
CREATE TABLE slot
(
    slot_id            BIGSERIAL PRIMARY KEY,
    postcode           VARCHAR(16) NOT NULL,
    start_at           TIMESTAMPTZ NOT NULL,
    end_at             TIMESTAMPTZ NOT NULL,
    total_capacity     INTEGER     NOT NULL CHECK (total_capacity >= 0),
    remaining_capacity INTEGER     NOT NULL CHECK (remaining_capacity >= 0),
    CHECK (remaining_capacity <= total_capacity),
    CHECK (end_at > start_at)
);

CREATE INDEX slot_postcode_start_at_idx ON slot (postcode, start_at);
