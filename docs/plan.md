# Bookman – Implementation Plan

**Project Status:** Planning phase  
**Last updated:** 2026-10-02

---

## Overview

Bookman is implemented in 5 stages, each delivering a working, buildable system:
1. **Project setup and authentication** – user can register, log in, and stay logged in
2. **Apartments and apartment management** – user can create apartments and see their list
3. **Reservations and cleaning status** – imports from Booking.com and tracks cleaning progress
4. **Invitations and role management** – allows adding cleaning service members
5. **Notifications and SMS reminders** – sends timely SMS alerts
6. **Localization** – Polish/English support throughout

Each stage includes automated tests for all implemented requirements.

---

## Stage 1: Project setup and authentication

**Objective:** Create working backend and Android projects; users can register and log in.

### Requirements implemented
- **REQ-01:** Registration and login
  - User can create account with email, password, phone (E.164 format)
  - Email is unique (case-insensitive)
  - User stays logged in until logout or session expiry
  - Backend: JWT-based stateless authentication
  - Android: store JWT in secure SharedPreferences

### Implementation checklist

#### Backend (Spring Boot 3)
- [ ] Initialize Gradle project with Spring Boot 3, Java 21
- [ ] PostgreSQL 16 setup with Flyway migrations
- [ ] User entity and JPA repository
- [ ] `POST /auth/register` endpoint (validate email uniqueness, phone format)
- [ ] `POST /auth/login` endpoint (return JWT)
- [ ] JWT filter and `@Secured` annotations
- [ ] Integration tests for registration/login (fake DB or TestContainers)

#### Android (Java, MVVM)
- [ ] Initialize Gradle project with Java 17, minSdk 26
- [ ] Retrofit + OkHttp client setup
- [ ] Hilt dependency injection setup
- [ ] User entity and Room database (basic schema)
- [ ] `AuthViewModel` (registration, login, token storage)
- [ ] Login and register screens (XML layouts, Material Components)
- [ ] Secure JWT storage in EncryptedSharedPreferences
- [ ] Unit tests for `AuthViewModel`

### Acceptance criteria verification
- `cd backend && ./gradlew build` passes (includes tests)
- `cd android && ./gradlew assembleDebug testDebugUnitTest lint` passes
- Test coverage: `AuthControllerTest` (REQ-01 registration), `AuthControllerTest` (REQ-01 login)
- Manual: can register with email/password/phone, log in, app stores JWT

---

## Stage 2: Apartments and apartment management

**Objective:** Users can create apartments and manage their list and settings.

### Requirements implemented
- **REQ-02:** User profile (phone, language)
- **REQ-03:** Apartment list (shows all apartments where user has a role)
- **REQ-04:** Register an apartment (name, Booking.com link, iCal URL, timezone)
- **REQ-05:** Edit and delete apartment (ADMIN only)

### Implementation checklist

#### Backend
- [ ] `User` entity: add phone, language (locale) fields
- [ ] `Apartment` entity, `ApartmentRole` enum, `ApartmentUser` join table
- [ ] Apartment repositories (JPA + Spring Data)
- [ ] `PUT /users/{id}` endpoint (profile update)
- [ ] `GET /apartments` endpoint (list for current user)
- [ ] `POST /apartments` endpoint (register apartment, validate iCal URL)
- [ ] `PUT /apartments/{id}` endpoint (ADMIN only)
- [ ] `DELETE /apartments/{id}` endpoint (ADMIN only)
- [ ] Authorization: `@PreAuthorize` checks for ADMIN role in apartment
- [ ] Flyway migration: create tables for User, Apartment, ApartmentUser, ApartmentRole
- [ ] Unit/integration tests for all endpoints and authorization

#### Android
- [ ] `UserViewModel` (profile data, language selection)
- [ ] `Apartment` entity and Room DAO
- [ ] `ApartmentViewModel` (list, register, edit, delete)
- [ ] Profile screen (phone, language picker)
- [ ] Apartment list screen with empty state
- [ ] Register apartment screen (name, link, iCal URL, timezone)
- [ ] Edit apartment dialog (ADMIN only)
- [ ] Delete apartment confirmation dialog
- [ ] Unit tests for ViewModels

### Acceptance criteria verification
- Backend: `cd backend && ./gradlew build` passes
- Android: `cd android && ./gradlew assembleDebug testDebugUnitTest lint` passes
- Tests: `REQ02_userCanEditPhoneAndLanguage`, `REQ03_apartmentListShowsUserRoles`, `REQ04_registerApartment`, `REQ05_editAndDeleteApartment`
- Manual: create apartment, edit name, delete apartment; see them in list

**Note:** iCal URL validation is implemented (fetch and parse on registration), but actual reservation import is Stage 3.

---

## Stage 3: Reservations and cleaning status

**Objective:** Reservations are imported from Booking.com and cleaning status is tracked.

### Requirements implemented
- **REQ-06:** Reservation import (iCal parsing, periodic every 30 min, pull-to-refresh)
- **REQ-07:** Reservation list (current/future only, sorted by check-out, shows status)
- **REQ-08:** Cleaning status (`NOT_PLANNED` → `TAKEN` → `DONE`, with audit trail)

### Implementation checklist

#### Backend
- [ ] `ReservationSource` interface with iCal implementation
- [ ] `iCal4j` library integration for parsing
- [ ] `Reservation` entity (apartment_id, check_in, check_out, status, ical_uid)
- [ ] `ReservationStatusChange` entity (audit: who, when, old_status, new_status)
- [ ] `@Scheduled` job for periodic import (every 30 minutes)
- [ ] `POST /apartments/{id}/reservations/sync` endpoint (pull-to-refresh)
- [ ] `GET /apartments/{id}/reservations` endpoint (filtered by check-out ≥ today)
- [ ] `PATCH /reservations/{id}/status` endpoint (change status with rules)
- [ ] Duplicate prevention: use iCal UID as idempotency key
- [ ] Cancellation handling: mark disappeared reservations as `CANCELLED`
- [ ] Authorization: both ADMIN and CLEANING_SERVICE can read; CLEANING_SERVICE restricted in status changes
- [ ] Flyway migration: create tables for Reservation, ReservationStatusChange
- [ ] Tests for iCal parsing, duplicate prevention, status transitions, authorization

#### Android
- [ ] `Reservation` entity and Room DAO
- [ ] `ReservationListViewModel` (fetch, pull-to-refresh)
- [ ] `ReservationDetailViewModel` (status change)
- [ ] Reservation list screen (check-in, check-out, status, who took it)
- [ ] Status change UI: CLEANING_SERVICE sees buttons (NOT_PLANNED → TAKEN, TAKEN → DONE)
- [ ] Pull-to-refresh integration
- [ ] Unit tests for ViewModels

### Acceptance criteria verification
- Backend: `cd backend && ./gradlew build` passes
- Android: `cd android && ./gradlew assembleDebug testDebugUnitTest lint` passes
- Tests: `REQ06_importReservations`, `REQ06_noDuplicates`, `REQ06_handleCancellations`, `REQ07_reservationList`, `REQ08_cleaningStatus`, `REQ08_cleaningServiceCannotRevert`
- Manual: add apartment with iCal URL, pull to refresh, see reservations, change status

**Assumption (REQ-06):** iCal URL is validated during apartment registration (Stage 2). Re-validation happens on each import; malformed or unreachable URLs are logged but don't stop the app (retried next cycle).

---

## Stage 4: Invitations and role management

**Objective:** Admins can invite cleaning service members and manage their roles.

### Requirements implemented
- **REQ-09:** Invite cleaning service by email
- **REQ-10:** Manage cleaning service members (list, cancel invitation, remove user)

### Implementation checklist

#### Backend
- [ ] `Invitation` entity (apartment_id, invited_email, created_at, expires_at, accepted/declined)
- [ ] Invitation expiry: 14 days (scheduled job to mark expired)
- [ ] `POST /apartments/{id}/invitations` endpoint (ADMIN only)
- [ ] `GET /apartments/{id}/members` endpoint (list users and pending invitations)
- [ ] `PATCH /invitations/{id}/accept` endpoint
- [ ] `PATCH /invitations/{id}/decline` endpoint
- [ ] `DELETE /apartments/{id}/members/{userId}` endpoint (ADMIN only, cannot remove self)
- [ ] `DELETE /apartments/{id}/invitations/{id}` endpoint (ADMIN only, cancel invitation)
- [ ] Validation: cannot invite existing member or self
- [ ] Flyway migration: create Invitation table
- [ ] Tests for invitation lifecycle, expiry, authorization

#### Android
- [ ] `Invitation` entity and Room DAO
- [ ] `MembersViewModel` (list users, pending invitations, invite new)
- [ ] Members list screen (ADMIN only)
- [ ] Invite dialog (email input, validation)
- [ ] Accept/decline invitation screen (shown in app on login if user has pending invitations)
- [ ] Unit tests for ViewModels

### Acceptance criteria verification
- Backend: `cd backend && ./gradlew build` passes
- Android: `cd android && ./gradlew assembleDebug testDebugUnitTest lint` passes
- Tests: `REQ09_inviteCleaningService`, `REQ09_invitationExpires`, `REQ10_manageMembersAndInvitations`
- Manual: invite email, log in as invitee, see invitation, accept/decline

---

## Stage 5: Notifications and SMS reminders

**Objective:** SMS reminders are sent for upcoming and unfinished cleaning.

### Requirements implemented
- **REQ-11:** "Not planned" SMS (check-out = tomorrow, status = NOT_PLANNED)
- **REQ-12:** "Not done" SMS (check-out = today, status ≠ DONE)
- **REQ-13:** SMS sending (interface-based, dev logs, production provider via env vars)

### Implementation checklist

#### Backend
- [ ] `SmsSender` interface (dev and production implementations)
- [ ] SMS provider configuration (environment variables for API key, account)
- [ ] `SmsLog` entity (apartment_id, reservation_id, recipient_id, reminder_type, sent_at)
- [ ] `@Scheduled` jobs for REQ-11 and REQ-12 (run at configured times)
- [ ] Idempotency check: don't send duplicate SMS same day (check SmsLog)
- [ ] Retry mechanism: up to 3 retries on failure with exponential backoff
- [ ] SMS templates with apartment name and check-out date
- [ ] Localization: SMS text in user's language (from user profile)
- [ ] `GET /apartments/{id}/notification-settings` endpoint (read)
- [ ] `PUT /apartments/{id}/notification-settings` endpoint (update times, ADMIN only)
- [ ] Flyway migration: create SmsLog, NotificationSettings tables
- [ ] Tests for reminder scheduling, idempotency, retries, localization

#### Android
- [ ] Notification settings screen (ADMIN only)
- [ ] Time picker for REQ-11 and REQ-12 reminder hours
- [ ] Enable/disable toggles for each reminder type
- [ ] Display current settings

### Acceptance criteria verification
- Backend: `cd backend && ./gradlew build` passes
- Android: `cd android && ./gradlew assembleDebug testDebugUnitTest lint` passes
- Tests: `REQ11_notPlannedSms`, `REQ12_notDoneSms`, `REQ13_smsSending`, `REQ13_smsRetry`, `REQ13_noSmsDuplicates`
- Manual: set reminder time, observe logs (dev mode), verify idempotency

**Assumption (REQ-13):** Retries are deferred with exponential backoff (2s, 4s, 8s) to avoid blocking the scheduler.

---

## Stage 6: Localization (Polish and English)

**Objective:** App and SMS are available in Polish and English.

### Requirements implemented
- **REQ-14:** Polish (pl) and English (en) support with system language detection

### Implementation checklist

#### Backend
- [ ] SMS templates: Polish and English versions
- [ ] Locale detection in SMS job based on user's language preference
- [ ] Database: language field in User entity (set during registration/profile update)
- [ ] Tests for SMS generation in both languages

#### Android
- [ ] `res/values/strings.xml` (English)
- [ ] `res/values-pl/strings.xml` (Polish)
- [ ] All UI strings moved to resources (no hardcoded text)
- [ ] Language picker in settings (tied to UserProfile)
- [ ] System language detection on first launch
- [ ] Apply language change immediately (re-create activity or use LocalizationUtil)
- [ ] Unit tests for language switching

### Acceptance criteria verification
- Backend: `cd backend && ./gradlew build` passes
- Android: `cd android && ./gradlew assembleDebug testDebugUnitTest lint` passes
- Tests: `REQ14_polishAndEnglish`, `REQ14_systemLanguageDetection`
- Manual: change system language, restart app, see Polish/English; change in-app language, see immediate change

---

## Testing strategy

For **every implemented REQ**, there is at least one automated test with the REQ ID in its name:

| REQ | Test name (examples) | Type | Location |
|-----|----------------------|------|----------|
| REQ-01 | `REQ01_registration`, `REQ01_login` | Integration | backend |
| REQ-02 | `REQ02_userCanEditPhoneAndLanguage` | Unit | backend + Android |
| REQ-03 | `REQ03_apartmentListShowsUserRoles` | Integration | backend |
| REQ-04 | `REQ04_registerApartment`, `REQ04_rejectBadIcalUrl` | Integration | backend |
| REQ-05 | `REQ05_adminCanEditDeleteApartment`, `REQ05_cleaningServiceCannotEdit` | Integration | backend |
| REQ-06 | `REQ06_importReservations`, `REQ06_noDuplicates`, `REQ06_handleCancellations` | Integration | backend |
| REQ-07 | `REQ07_reservationList` | Integration | backend |
| REQ-08 | `REQ08_cleaningStatusTransitions`, `REQ08_cleaningServiceCannotRevert` | Integration | backend |
| REQ-09 | `REQ09_inviteCleaningService`, `REQ09_invitationExpires` | Integration | backend |
| REQ-10 | `REQ10_manageMembersAndInvitations` | Integration | backend |
| REQ-11 | `REQ11_notPlannedSms` | Integration | backend |
| REQ-12 | `REQ12_notDoneSms` | Integration | backend |
| REQ-13 | `REQ13_smsSending`, `REQ13_smsRetry`, `REQ13_noSmsDuplicates` | Integration | backend |
| REQ-14 | `REQ14_polishAndEnglish`, `REQ14_systemLanguageDetection` | Unit/Integration | Android + backend |

---

## Branch and PR strategy

Each stage is developed on a dedicated feature branch and merged via a pull request:

- **Stage 1:** Branch `stage/auth`, PR title `feat(REQ-01): registration and login`
- **Stage 2:** Branch `stage/apartments`, PR title `feat(REQ-02/03/04/05): apartments and management`
- **Stage 3:** Branch `stage/reservations`, PR title `feat(REQ-06/07/08): reservations and cleaning status`
- **Stage 4:** Branch `stage/invitations`, PR title `feat(REQ-09/10): invitations and role management`
- **Stage 5:** Branch `stage/notifications`, PR title `feat(REQ-11/12/13): SMS notifications`
- **Stage 6:** Branch `stage/localization`, PR title `feat(REQ-14): Polish and English localization`

After each stage is merged:
- Update this file to reflect completion
- Update `docs/questions.md` if new ambiguities are discovered

---

## Current status

| Stage | Status | Branch | PR |
|-------|--------|--------|-----|
| 1. Authentication | 📋 Planning | TBD | TBD |
| 2. Apartments | ⏳ Planned | TBD | TBD |
| 3. Reservations | ⏳ Planned | TBD | TBD |
| 4. Invitations | ⏳ Planned | TBD | TBD |
| 5. Notifications | ⏳ Planned | TBD | TBD |
| 6. Localization | ⏳ Planned | TBD | TBD |
