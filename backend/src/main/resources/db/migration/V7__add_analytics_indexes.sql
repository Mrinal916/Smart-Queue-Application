-- PostgreSQL reporting paths: date/status aggregation and filtered reports.
CREATE INDEX IF NOT EXISTS idx_tokens_queue_date_status_office ON tokens(queue_date, status, office_id);
CREATE INDEX IF NOT EXISTS idx_tokens_queue_date_counter ON tokens(queue_date, counter_id);
CREATE INDEX IF NOT EXISTS idx_queue_history_performed_by_created ON queue_history(performed_by, created_at);
