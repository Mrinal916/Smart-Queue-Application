# Changelog

## Unreleased

### Added
- Citizen active-token and paginated booking-history APIs.
- Live service queue status/current-serving API.
- Signed QR payload generation and QR validation endpoints.
- Token idempotency keys, dynamic wait-time calculation, and WAITING token lifecycle state.

### Milestone 5
- Completed citizen booking, cancellation, active token, booking history, live queue, current-serving, wait-time, and secure QR validation APIs.
- Verified local PostgreSQL and Redis connectivity with a successful application startup.

### Milestone 6
- Added JWT-secured STOMP/SockJS messaging with after-commit queue and counter updates.
- Added office/service topics and citizen-specific notification routing.

### Milestone 7 (in progress)
- Added PostgreSQL-only dashboard, reporting, statistics, and performance analytics APIs.
- Added CSV/XLSX report and performance exports, reporting indexes, and counter status-history persistence.
