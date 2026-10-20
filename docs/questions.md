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

### Q-15 (REQ-01): Password policy, session length, logout
**Status:** RESOLVED – confirmed by the owner on 2026-10-15 (as implemented below)

The spec says the user "stays logged in until they log out or the session expires" but defines no
expiry, password rules or server-side logout. Decision:
- Password: 8–72 characters, no composition rules (72 = bcrypt input limit).
- Session: JWT valid for **30 days** (`BOOKMAN_JWT_TTL`), no refresh token; after expiry the app
  returns to the login screen.
- Logout is client-side (token deleted from the phone). A stolen token stays valid until expiry;
  server-side revocation would need a token blacklist or short-lived tokens + refresh tokens.

30 days is accepted; server-side logout/revocation is not needed in v1.

### Q-16 (NFR-02, Q-06): Production API address
**Status:** OPEN

Release builds use `https://bookman.invalid/` as a placeholder base URL until the AWS hosting has a
domain + TLS certificate. Build with `-PbookmanApiUrl=https://<real-host>/`. Which domain will the API use?

### Q-17 (REQ-04, NFR-03): Which calendar/listing URLs are accepted
**Status:** RESOLVED – confirmed by the owner on 2026-10-15

The backend downloads the iCal URL entered by the user. Without restrictions this allows SSRF – e.g. making
the server on AWS fetch `http://169.254.169.254/` (instance metadata with credentials).
Decision: both the listing link and the iCal link must be **https** URLs on
`booking.com` or its subdomains (e.g. `ical.booking.com`), default port, no redirects followed.
The host list is configurable (`bookman.apartment.allowed-hosts`) so other channels can be added later.
No other domains need to be accepted in v1.

### Q-18 (REQ-06, Q-08): Real iCal fixture
**Status:** RESOLVED – the owner provided a real export on 2026-10-20

`backend/src/test/resources/ical/booking-export-real.ics` is the real Booking.com export (only the event UID was
replaced). Parser and HTTP-source tests use it. `booking-export-multi.ics` is a synthetic feed in exactly the same
format with extra cases (missing UID / DTEND) that must be skipped.

### Q-19 (REQ-02, REQ-14): Language change takes effect where?
**Status:** INFO

In Stage 2 the language chosen in the profile is saved on the backend (used for SMS, REQ-14). Switching the
app UI language immediately is part of Stage 6 (REQ-14); until then the UI follows the system language.

### Q-20 (REQ-06, Q-01, Q-04): Bookings vs. manually closed dates; reservations that come back
**Status:** RESOLVED (owner decisions 2026-10-20)

1. Booking.com marks **every** blocked period the same way (`SUMMARY:CLOSED - Not available`), so the app cannot
   tell guest bookings from dates the owner closed by hand. **Decision:** every event is imported as a reservation,
   and there is an extra cleaning status **`PRIVATE`** that only the apartment's ADMIN can set. The cleaning service
   cannot change a PRIVATE reservation (it sees it as "Private" without actions). Only the ADMIN can set it back.
   *Proposed spec change for REQ-08:* add `PRIVATE` to the cleaning statuses with the rules above.
   *Impact on Stage 5:* PRIVATE reservations must not trigger "not planned" / "not done" SMS (REQ-11/12).
2. **Decision (confirmed):** a reservation that was cancelled (disappeared from the feed) and later reappears with
   the same UID becomes active again; its cleaning status is kept.

### Q-21 (REQ-08): Details of the status rules
**Status:** RESOLVED (owner decision 2026-10-20)

- Any CLEANING_SERVICE member may mark a TAKEN cleaning as DONE, even if a colleague took it. The responsible
  person stays the one who took it.
- When an ADMIN sets TAKEN, the admin is recorded as responsible; NOT_PLANNED and PRIVATE clear the responsible person.
- Setting the status a reservation already has is rejected with 409 – in practice someone else was faster, and the
  app reloads the list.

### Q-22 (REQ-09, REQ-10): Invitation details
**Status:** INFO – implemented as follows, tell me if anything should differ

- Invitations are in-app only, no e-mail is sent (assumption from Q-07 in the spec). The invited person must log in
  or register with exactly the invited e-mail (case-insensitive).
- An expired invitation can simply be sent again; a second invitation while one is pending is rejected.
- The admin cannot be removed (spec 6: single admin, no transfer). A removed cleaning service member keeps being shown
  as "taken by" on reservations they took earlier (history is not rewritten).

### Q-23 (REQ-11, REQ-12, REQ-13): Reminder details (now push, see Q-24)
**Status:** OPEN – implemented with assumptions, please confirm

- Reminders are **off** until the admin sets an hour (the spec gives no default). `ASSUMPTION(REQ-11)`
- If the backend is down at the reminder minute, the SMS is still sent when it comes back **within one hour**;
  later than that it is skipped for that day. `ASSUMPTION(REQ-11)`
- Delivery is "at least once": if the server crashes exactly between FCM accepting a notification and the database
  recording it, that one notification may arrive twice. Everything else (retries, restarts, a second scheduler run)
  is deduplicated by the database.

### Q-24 (REQ-11, REQ-12, REQ-13, spec 6): Push notifications instead of SMS
**Status:** RESOLVED – owner decision 2026-10-20 · **proposed spec change**

The owner decided that reminders are delivered as **push notifications to the Bookman app** via Firebase Cloud
Messaging (FCM), and that **SMS support is removed**. Proposed spec changes:
- REQ-11 / REQ-12: "an SMS is sent" → "a push notification is sent to the phones of the recipients"; acceptance
  criteria unchanged (one per recipient per reservation per day, apartment name + check-out date).
- REQ-13 "SMS sending" → "Push sending" through a `PushSender` interface: a development implementation logs
  messages, the production provider (FCM) is configured via environment variables, failed sends are logged and
  retried at most 3 times.
- Section 6 "Out of scope": remove "Push notifications (SMS only)".
- Q-05 (SMS provider) is no longer needed.
Consequences: a recipient gets reminders only on phones where they are logged in to Bookman and allowed notifications
(Android 13+ asks once). Reminders for a user without such a phone are recorded as `NO_DEVICE`.
Open follow-up: REQ-01 still requires a phone number "needed for REQ-11/12" – it is no longer used. Keep it required,
make it optional, or remove it?

## External/Product decisions (not blocking implementation)

These answers are assumed in the spec but require external decision:

- **Q-05 SMS provider:** obsolete – reminders are push notifications (Q-24).
- **Q-07 Invitations by e-mail:** Should invitations trigger email notifications, or is in-app enough? (v1: in-app only, see Q-22)
- **Q-01 Booking.com integration:** Is iCal information (dates only, no guest data) sufficient for v1?
