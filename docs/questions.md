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
**Status:** RESOLVED

**Answer:**
- Use **ical4j** in `/backend` only (the Android app never parses iCal).
- Parse **leniently**: enable `ical4j.parsing.relaxed`, `ical4j.unfolding.relaxed`
  and `ical4j.validation.relaxed` (Booking.com feeds may not be fully RFC 5545 compliant,
  e.g. missing DTSTAMP). Do not run strict validation on imported calendars.
- Be strict only about fields we use: an event without UID, DTSTART or DTEND is skipped
  and logged as a warning; it must not fail the whole import.
- Fail-safe import: if the download fails (non-2xx, timeout, non-iCal content) or the
  calendar cannot be parsed at all, abort the import with NO changes to the database.
  Reservations are marked CANCELLED (REQ-06) only after a fully successful import.
- Add a real, anonymized Booking.com iCal export as a test fixture in
  `backend/src/test/resources/ical/` and base parser tests on it.

### Q-09 (Implementation): Database schema – user-apartment relationships
**Status:** RESOLVED

**Answer:**
- Roles live ONLY in a join table `apartment_member(apartment_id, user_id, role, created_at)`,
  PK `(apartment_id, user_id)` = one role per user per apartment. No role/admin columns on `apartment`.
- `role` is `VARCHAR` + `CHECK (role IN ('ADMIN','CLEANING_SERVICE'))`, mapped with
  `@Enumerated(EnumType.STRING)`. Do not use PostgreSQL ENUM types.
- Index on `apartment_member(user_id)` for the "my apartments" query (REQ-03).
- Exactly one admin per apartment enforced in the DB:
  `CREATE UNIQUE INDEX ... ON apartment_member(apartment_id) WHERE role = 'ADMIN'`.
  The admin row is never deleted except when the apartment is deleted.
- `ON DELETE CASCADE` from `apartment` to `apartment_member`, `invitation`, `reservation` (REQ-05).
- E-mails unique case-insensitively: `CREATE UNIQUE INDEX ... ON app_user(lower(email))`.
- Invitations are a separate table `invitation` (apartment_id, email, role, status, invited_by,
  expires_at), status in PENDING/ACCEPTED/DECLINED/CANCELLED/EXPIRED, at most one PENDING
  invitation per (apartment, lower(email)) via partial unique index. Accepting = one transaction:
  update invitation + insert apartment_member.
- Authorization: every apartment-scoped endpoint looks up `apartment_member` by
  `(apartment_id, current_user_id)`; no row → HTTP 403.

### Q-10 (Implementation): SMS retry strategy
**Status:** RESOLVED

**Answer:**
- Retries are **deferred with exponential backoff** (2s, 4s, 8s) to avoid blocking the scheduler.
- Use Spring's `@Scheduled` + `@Async` with a `TaskExecutor` thread pool for retries.
- On send failure, schedule a retry task with delay using `ScheduledExecutorService.schedule()`.
- After 3 failed attempts, log error and skip (do not retry further); notify via logs.
- Track retry count in `SmsLog` table: `retry_count` column, incremented on each attempt.

### Q-11 (Implementation): Idempotency of SMS sending (REQ-11/12)
**Status:** RESOLVED

**Answer:**
- Track idempotency with table `sms_sent_log(apartment_id, reservation_id, recipient_id, reminder_type, sent_date, created_at)`,
  PK = `(apartment_id, reservation_id, recipient_id, reminder_type, sent_date)`.
- Before sending an SMS for a reminder, check if this tuple exists in today's log.
- If found, skip send (already sent today). If not found, send and insert one log row.
- Query pattern: `SELECT COUNT(*) FROM sms_sent_log WHERE apartment_id=? AND reservation_id=? 
  AND recipient_id=? AND reminder_type=? AND sent_date=CURRENT_DATE`.
- This prevents duplicates on retry, restart, or concurrent scheduler runs.

### Q-12 (REQ-04): Validation of iCal URL
**Status:** RESOLVED

**Answer:**
- Validate URL format on registration: must be HTTP/HTTPS, valid hostname/path.
- **Fetch and parse on registration** (REQ-04): do a test download and parse to catch errors early.
- If download/parse fails, return HTTP 400 with detailed error message to the app.
- If successful, store the URL and trigger first import immediately (REQ-06).
- Re-validate on each periodic import; malformed/unreachable URLs are logged but do not
  stop the app (will retry in next cycle).

### Q-13 (Implementation): Concurrent iCal imports (REQ-06)
**Status:** RESOLVED

**Answer:**
- Imports for multiple apartments run **in parallel** with a fixed thread pool (size = number of CPUs / 2, min 2).
- Use `ExecutorService` (e.g., `Executors.newFixedThreadPool()`) or Spring's `@Async` with custom `TaskExecutor`.
- Prevent duplicate concurrent imports of the same apartment:
  - Distributed lock per apartment_id (use PostgreSQL `SELECT ... FOR UPDATE` or a dedicated lock table).
  - If a lock is held, skip the import for that apartment this cycle (retry next cycle).
- Timeout per import: 30 seconds (configurable). If exceeded, log error and skip.

### Q-14 (REQ-01): Email validation and uniqueness
**Status:** RESOLVED

**Answer:**
- Email validation: practical (not strict RFC 5321). Use Spring's `@Email` validator or similar.
- Normalize on storage: **trim and lowercase** before storing and comparing.
  - In code: `email = email.trim().toLowerCase()` before every DB operation.
  - In DB: `CREATE UNIQUE INDEX ... ON app_user(lower(email))` for case-insensitive uniqueness.
- Reject on registration if a normalized email already exists (case-insensitive comparison).
- Phone validation: strict E.164 format (e.g., `+48600100200`), reject if invalid on registration.

### Q-06 (Infrastructure): Database hosting
**Status:** RESOLVED

**Answer:**
- **Development:** PostgreSQL 16 in Docker Compose (`docker-compose.yml` in repo root).
  - Start: `docker-compose up -d`
  - Connection: `localhost:5432`, user `bookman`, password `bookman_dev_password`, DB `bookman`
  - Backend `.env` points to this during development
  - Data persisted in `postgres_data` volume; `docker-compose down` preserves it
- **Production:** AWS RDS PostgreSQL 16
  - Instance class: `db.t4g.micro` (smallest, sufficient for v1)
  - Multi-AZ: false (can enable later)
  - Backup retention: 7 days (default)
  - Publicly accessible: false (access only from VPC/security group)
  - VPC: same VPC as backend (Spring Boot app will run on EC2 or ECS in same VPC)
  - Connection via endpoint: e.g., `bookman-postgres.c9akciq32.eu-west-1.rds.amazonaws.com:5432`
  - Backend production `.env` reads from environment variable `DATABASE_URL` (or similar)
- Flyway migrations run on backend startup (Spring Boot + Flyway auto-config).

## External/Product decisions (not blocking implementation)

These answers are assumed in the spec but require external decision:

- **Q-05 SMS provider:** Which provider (e.g., SMSAPI.pl, Twilio) and who pays for SMS?
- **Q-07 Invitations by e-mail:** Should invitations trigger email notifications, or is in-app enough?
- **Q-01 Booking.com integration:** Is iCal information (dates only, no guest data) sufficient for v1?
