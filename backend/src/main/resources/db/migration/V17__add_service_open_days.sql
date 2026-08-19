CREATE TABLE service_open_days (
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    day_of_week VARCHAR(9) NOT NULL,
    PRIMARY KEY (service_id, day_of_week),
    CONSTRAINT chk_service_open_day CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

INSERT INTO service_open_days (service_id, day_of_week)
SELECT id, day_of_week
FROM services
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY'), ('SATURDAY'), ('SUNDAY')) AS open_days(day_of_week);
