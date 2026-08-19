CREATE UNIQUE INDEX IF NOT EXISTS uq_tokens_booking_key
    ON tokens(citizen_id, booking_key);
