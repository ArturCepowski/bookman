# Bookman – Open Questions

## Implementation-specific questions

### Q-02 (REQ-08): Status change rules – ANSWERED
**Status:** Clarified based on spec section 4.3 and requirements.

- `CLEANING_SERVICE` users can only change: `NOT_PLANNED → TAKEN → DONE` (one direction only)
- `ADMIN` users can set any status and can revert changes
- `CLEANING_SERVICE` cannot revert `DONE` or `TAKEN`
- Every status change is logged with user identity and timestamp

### Q-03 (REQ-11/12): Reminder timing – ANSWERED
**Status:** Clarified from spec section 4.5.

- "Not planned" reminder (REQ-11): sent for reservations with check-out date = **tomorrow** and status `NOT_PLANNED`
- "Not done" reminder (REQ-12): sent for reservations with check-out date = **today** and status ≠ `DONE`
- Timing is in apartment's time zone (REQ-04 specifies time zone setting)
- Recipients: all `ADMIN` and `CLEANING_SERVICE` users of the apartment

### Q-04 (REQ-06): Cancelled reservations – ANSWERED
**Status:** Clarified from spec section 4.3.

- When a reservation disappears from iCal feed and has not yet ended, mark it `CANCELLED`
- `CANCELLED` reservations are **hidden** from the default list in v1
- Treat `CANCELLED` as past (do not send reminders for them)

### Q-08 (Implementation): iCal parsing library
**Status:** TBD

- Which library should be used for iCal parsing? (e.g., `ical4j` for JVM)
- Should we validate iCal format strictly or be lenient with common variations?

### Q-09 (Implementation): Database schema – user-apartment relationships
**Status:** TBD

- How to handle the many-to-many relationship between users and apartments (roles)?
- Should roles be stored in separate table or on apartment row (assumes separate table for scalability)?

### Q-10 (Implementation): SMS retry strategy
**Status:** TBD

- REQ-13 says "failed sends are retried at most 3 times" – should retries be:
  - Immediate/synchronous in the same job?
  - Deferred with exponential backoff using a queue/task scheduler?
  - Recommended: exponential backoff with jitter to avoid thundering herd

### Q-11 (Implementation): Idempotency of SMS sending (REQ-11/12)
**Status:** TBD

- "one SMS per recipient per reservation per day" – how to ensure idempotency?
  - Recommended: track (apartment_id, reservation_id, recipient_id, date, reminder_type) tuple
  - Only send if this tuple has not been processed in the current day

### Q-12 (REQ-04): Validation of iCal URL
**Status:** TBD

- Should we validate the URL format (HTTP/HTTPS, valid domain)?
- Should we attempt to fetch and parse it during registration (REQ-04) to catch errors early?
- Recommended: fetch + parse on registration to provide immediate feedback

### Q-13 (Implementation): Concurrent iCal imports (REQ-06)
**Status:** TBD

- Can imports for multiple apartments run in parallel, or should they be sequential?
- Recommended: parallel with a thread pool to avoid bottlenecks

### Q-14 (REQ-01): Email validation and uniqueness
**Status:** TBD

- Email validation: strict RFC 5321 or practical (good enough)?
- Uniqueness: case-insensitive (as stated), but how to handle edge cases (trailing spaces)?
- Recommended: trim and lowercase before storing and comparing

## External/Product decisions (not blocking implementation)

These answers are assumed in the spec but require external decision:

- **Q-05 SMS provider:** Which provider (e.g., SMSAPI.pl, Twilio) and who pays for SMS?
- **Q-06 Hosting:** Where will the backend run?
- **Q-07 Invitations by e-mail:** Should invitations trigger email notifications, or is in-app enough?
- **Q-01 Booking.com integration:** Is iCal information (dates only, no guest data) sufficient for v1?
