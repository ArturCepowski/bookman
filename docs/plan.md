# Bookman – Implementation Plan

**Project Status:** Stages 1–2 done  
**Last updated:** 2026-10-14

---

## Overview

Bookman is implemented in 6 stages, each delivering a working, buildable system:
1. **Project setup and authentication** – user can register, log in, and stay logged in
2. **Apartments and apartment management** – user can create apartments and see their list
3. **Reservations and cleaning status** – imports from Booking.com and tracks cleaning progress
4. **Invitations and role management** – allows adding cleaning service members
5. **Notifications and SMS reminders** – sends timely SMS alerts
6. **Localization** – Polish/English support throughout

Each stage includes automated tests for all implemented requirements.

---

## Stage 1: Project setup and authentication

**Status:** ✅ Done
**Objective:** Create working backend and Android projects; users can register and log in.

### Requirements implemented
- **REQ-01:** Registration and login
  - User can create account with email, password, phone (E.164 format)
  - Email is unique (case-insensitive)
  - User stays logged in until logout or session expiry
  - Backend: JWT-based stateless authentication
  - Android: JWT in app-private SharedPreferences, excluded from backup and device transfer

### Implementation checklist

#### Backend (Spring Boot 3.5, Java 21)
- [x] Gradle project with Spring Boot 3.5, Java 21 toolchain
- [x] PostgreSQL 16 + Flyway (`V1__users.sql`, unique index on `lower(email)`)
- [x] `User` entity and repository
- [x] `POST /api/auth/register` (unique e-mail, E.164 phone, password 8–72, language)
- [x] `POST /api/auth/login` (returns JWT + user)
- [x] `GET /api/users/me` (authenticated; used by the app to verify the session)
- [x] Stateless JWT filter, JSON error bodies with stable `code` (`EMAIL_TAKEN`, `INVALID_CREDENTIALS`, …)
- [x] JWT secret only from env `BOOKMAN_JWT_SECRET` (startup fails without it)
- [x] Integration tests on a real PostgreSQL 16 (embedded zonky binaries – Docker is not available
      in the cloud environment, so Testcontainers was replaced)

#### Android (Java 17, MVVM)
- [x] Gradle project (AGP 8.13, compileSdk/targetSdk 35, minSdk 26)
- [x] Retrofit + OkHttp, auth interceptor (adds token, clears session on 401)
- [x] Hilt DI
- [x] `LoginViewModel`, `RegisterViewModel`, `HomeViewModel` (replaced by the apartment list in Stage 2) + `AuthRepository`
- [x] Login, register and placeholder home screens (XML + Material 3, Navigation)
- [x] Token storage in app-private SharedPreferences (EncryptedSharedPreferences is deprecated)
- [x] EN + PL strings for every user-visible text
- [x] HTTPS-only network security config (debug build allows `10.0.2.2` for the local backend)
- Room is not needed for REQ-01; it is introduced when there is data to cache.

### Acceptance criteria verification
| Criterion | Verified by |
|---|---|
| Register with e-mail, password, phone | `AuthApiTest.REQ01_registerReturnsTokenAndStoresUser`, `RegisterViewModelTest.REQ01_*` |
| E-mail unique, case-insensitive, error shown | `AuthApiTest.REQ01_duplicateEmailIsRejectedCaseInsensitively`, `AuthRepositoryImplTest.REQ01_duplicateEmailMapsToEmailTaken`, `RegisterViewModelTest.REQ01_existingEmailShowsError` |
| Phone required, E.164 | `AuthApiTest.REQ01_phoneNumberMustBeE164`, `AuthApiTest.REQ01_phoneNumberIsRequired`, `RegistrationFormTest.REQ01_phoneMustBeE164` |
| Login | `AuthApiTest.REQ01_loginWithCorrectCredentialsReturnsToken`, `AuthApiTest.REQ01_loginWithWrongPasswordOrUnknownEmailFails`, `LoginViewModelTest.REQ01_*` |
| Stays logged in until logout / expiry | `AuthApiTest.REQ01_tokenKeepsUserLoggedIn`, `AuthApiTest.REQ01_expiredSessionIsRejected`, `JwtServiceTest.REQ01_tokenIsValidUntilTtlThenExpires`, `ApartmentListViewModelTest.REQ01_expiredSessionIsReported`, `ApartmentListViewModelTest.REQ01_logoutEndsSession`, `ProfileRepositoryImplTest.REQ01_expiredSessionIsClearedAndReported` |
| Logged-in user sees apartment list | Stage 2: the apartment list (REQ-03) is the start screen for a stored session |

Commands: `cd backend && ./gradlew build` (13 tests), `cd android && ./gradlew assembleDebug testDebugUnitTest lint` (18 tests, lint clean).
Manual (owner): run backend locally, start the debug app on an emulator, register, kill and reopen the app (still logged in), log out.

---

## Stage 2: Apartments and apartment management

**Status:** ✅ Done
**Objective:** Users can create apartments and manage their list and settings.

### Requirements implemented
- **REQ-02:** User profile (phone, language)
- **REQ-03:** Apartment list (shows all apartments where user has a role)
- **REQ-04:** Register an apartment (name, Booking.com link, iCal URL, timezone)
- **REQ-05:** Edit and delete apartment (ADMIN only)
- **NFR-03:** iCal URL returned only to ADMIN

### Implementation checklist

#### Backend
- [x] `GET/PUT /api/users/me` – phone + language; e-mail is not part of the update request
- [x] Flyway `V2__apartments.sql`: `apartment`, `apartment_member` (PK apartment+user, role CHECK,
      index on `user_id`, partial unique index = one ADMIN per apartment, `ON DELETE CASCADE`) – per Q-09
- [x] `GET /api/apartments` (caller's apartments + role), `GET /api/apartments/{id}`,
      `POST /api/apartments`, `PUT /api/apartments/{id}`, `DELETE /api/apartments/{id}`
- [x] `ApartmentAccess`: every apartment-scoped call looks up the caller's membership; no row or wrong role → 403
- [x] iCal URL check on register/edit (Q-12): `CalendarFeedVerifier` downloads and parses the feed with
      ical4j (relaxed, Q-08); unchanged URL is not re-downloaded on edit
- [x] SSRF guard: only `https` URLs on `booking.com` hosts, no redirects, 10 s timeout, 5 MB limit (Q-17)
- [x] `ApartmentRegisteredEvent` published on registration – Stage 3 starts the first import from it

#### Android
- [x] Apartment list as the main screen after login (name + role, pull to refresh, empty state, register button)
- [x] Apartment details; edit/delete menu only for ADMIN, delete confirmation dialog; iCal link shown only when the backend sends it
- [x] Register/edit apartment form (name 1–60, Booking.com links, time-zone picker defaulting to `Europe/Warsaw`)
- [x] Profile screen (e-mail read-only, phone, language EN/PL saved on the backend)
- [x] Expired session on any screen returns to login
- Room is still not needed (lists are always fetched fresh); revisit if offline access becomes a requirement.

### Acceptance criteria verification
| Criterion | Verified by |
|---|---|
| REQ-02 view/edit phone + language, saved on backend | `ProfileApiTest.REQ02_userCanViewAndEditPhoneAndLanguage`, `ProfileViewModelTest.REQ02_*`, `ProfileRepositoryImplTest.REQ02_*` |
| REQ-02 e-mail cannot be changed | `ProfileApiTest.REQ02_emailCannotBeChanged` |
| REQ-03 list with name + role, different roles per apartment | `ApartmentApiTest.REQ03_listShowsOnlyMyApartmentsWithMyRole`, `ApartmentListViewModelTest.REQ03_showsApartmentsWithRoles` |
| REQ-03 empty state | `ApartmentApiTest.REQ03_newUserHasEmptyList`, `ApartmentListViewModelTest.REQ03_emptyStateOnlyAfterSuccessfulEmptyLoad` |
| REQ-04 any user registers and becomes ADMIN; name 1–60; default time zone | `ApartmentApiTest.REQ04_registrationMakesCallerAdminWithDefaultTimeZone`, `…REQ04_nameMustHave1To60Characters`, `ApartmentFormTest.REQ04_*` |
| REQ-04 invalid/unreachable iCal rejected with message | `ApartmentApiTest.REQ04_unreachableOrInvalidIcalIsRejected`, `HttpCalendarFeedVerifierTest.REQ04_*`, `ApartmentFormViewModelTest.REQ04_unreachableIcalIsShownOnTheIcalField` |
| REQ-04 first import triggered immediately | `ApartmentApiTest.REQ04_registrationTriggersFirstImport` (event published); the import itself is Stage 3 |
| REQ-05 CLEANING_SERVICE does not see edit/delete | `ApartmentDetailViewModelTest.REQ05_cleaningServiceDoesNotGetEditOrDeleteActions` |
| REQ-05 backend returns 403 for non-admins | `ApartmentApiTest.REQ05_cleaningServiceCannotEditOrDeleteApartment`, `…REQ05_userWithoutRoleIsForbidden` |
| REQ-05 delete removes roles (reservations/invitations: same cascade, tables come in Stages 3–4) | `ApartmentApiTest.REQ05_deletingApartmentRemovesItsRoles` |
| NFR-03 iCal URL only for ADMIN | `ApartmentApiTest.NFR03_icalUrlIsShownOnlyToAdmin`, `ApartmentDetailViewModelTest.NFR03_*` |

Commands: `cd backend && ./gradlew build` (36 tests), `cd android && ./gradlew assembleDebug testDebugUnitTest lint` (41 tests, lint clean).
Manual (owner): register an apartment with a real Booking.com iCal export link, check the list/role, edit it,
log in as another user (no access), delete it; change phone/language in the profile.

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
| 1. Authentication | ✅ Done | `claude/hopeful-brahmagupta-3cr7bc` | [#2](https://github.com/ArturCepowski/bookman/pull/2) |
| 2. Apartments | ✅ Done | `claude/hopeful-brahmagupta-3cr7bc-stage2` | Stage 2 PR |
| 3. Reservations | ⏳ Planned | TBD | TBD |
| 4. Invitations | ⏳ Planned | TBD | TBD |
| 5. Notifications | ⏳ Planned | TBD | TBD |
| 6. Localization | ⏳ Planned | TBD | TBD |
