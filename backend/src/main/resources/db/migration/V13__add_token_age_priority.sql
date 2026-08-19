ALTER TABLE tokens ADD COLUMN age_priority BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE tokens SET age_priority = TRUE WHERE visitor_age >= 54;
