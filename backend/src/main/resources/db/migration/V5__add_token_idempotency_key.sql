ALTER TABLE tokens ADD COLUMN booking_key VARCHAR(100);
ALTER TABLE tokens ADD CONSTRAINT uq_token_citizen_booking_key UNIQUE (citizen_id, booking_key);
