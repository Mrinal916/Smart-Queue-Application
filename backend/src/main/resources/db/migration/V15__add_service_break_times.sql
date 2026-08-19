ALTER TABLE services ADD COLUMN break_start_time TIME;
ALTER TABLE services ADD COLUMN break_end_time TIME;

ALTER TABLE services ADD CONSTRAINT chk_service_break_times CHECK (
    (break_start_time IS NULL AND break_end_time IS NULL)
    OR (start_time < break_start_time AND break_start_time < break_end_time AND break_end_time < end_time)
);
