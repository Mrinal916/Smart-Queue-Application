UPDATE tokens
SET status = 'WAITING'
WHERE status IN ('BOOKED', 'CHECKED_IN');
