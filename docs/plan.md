# Bookman – Implementation Plan

**Project Status:** Stages 1–6 done  
**Last updated:** 2026-10-29

---

## Overview

Bookman is implemented in 6 stages, each delivering a working, buildable system:
1. **Project setup and authentication** – user can register, log in, and stay logged in
2. **Apartments and apartment management** – user can create apartments and see their list
3. **Reservations and cleaning status** – imports from Booking.com and tracks cleaning progress
4. **Invitations and role management** – allows adding cleaning service members
5. **Notifications and reminders** – push reminders about cleanings (Q-24)
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
- [x] Gradle project (AGP 8.13, compileSdk/targetSdk 35 → 36 for Google Play, Q-25; minSdk 26)
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
| Phone optional (Q-24 follow-up), E.164 when given | `AuthApiTest.REQ01_phoneNumberMustBeE164`, `AuthApiTest.REQ01_phoneNumberIsOptional`, `RegistrationFormTest.REQ01_phoneMustBeE164`, `RegistrationFormTest.REQ01_phoneNumberIsOptional`, `RegisterViewModelTest.REQ01_registrationWithoutPhoneNumberIsSent` |
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
| REQ-02 view/edit phone + language, saved on backend; phone can be removed (Q-24 follow-up) | `ProfileApiTest.REQ02_userCanViewAndEditPhoneAndLanguage`, `ProfileApiTest.REQ02_phoneNumberCanBeRemoved`, `ProfileViewModelTest.REQ02_*`, `ProfileRepositoryImplTest.REQ02_*` |
| REQ-02 e-mail cannot be changed | `ProfileApiTest.REQ02_emailCannotBeChanged` |
| REQ-03 list with name + role, different roles per apartment | `ApartmentApiTest.REQ03_listShowsOnlyMyApartmentsWithMyRole`, `ApartmentListViewModelTest.REQ03_showsApartmentsWithRoles` |
| REQ-03 empty state | `ApartmentApiTest.REQ03_newUserHasEmptyList`, `ApartmentListViewModelTest.REQ03_emptyStateOnlyAfterSuccessfulEmptyLoad` |
| REQ-04 any user registers and becomes ADMIN; name 1–60; default time zone | `ApartmentApiTest.REQ04_registrationMakesCallerAdminWithDefaultTimeZone`, `…REQ04_nameMustHave1To60Characters`, `ApartmentFormTest.REQ04_*` |
| REQ-04 invalid/unreachable iCal rejected with message | `ApartmentApiTest.REQ04_unreachableOrInvalidIcalIsRejected`, `HttpCalendarFeedVerifierTest.REQ04_*`, `ApartmentFormViewModelTest.REQ04_unreachableIcalIsShownOnTheIcalField` |
| REQ-04 first import triggered immediately | `ApartmentApiTest.REQ04_registrationTriggersFirstImport` (event published); the import itself is Stage 3 |
| REQ-05 CLEANING_SERVICE does not see edit/delete | `ApartmentDetailViewModelTest.REQ05_onlyAdminCanEdit`, `ReservationListViewModelTest.REQ05_REQ10_cleaningServiceGetsOnlyDetails` |
| REQ-05 backend returns 403 for non-admins | `ApartmentApiTest.REQ05_cleaningServiceCannotEditOrDeleteApartment`, `…REQ05_userWithoutRoleIsForbidden` |
| REQ-05 delete removes roles (reservations/invitations: same cascade, tables come in Stages 3–4) | `ApartmentApiTest.REQ05_deletingApartmentRemovesItsRoles` |
| NFR-03 iCal URL only for ADMIN | `ApartmentApiTest.NFR03_icalUrlIsShownOnlyToAdmin`, `ApartmentDetailViewModelTest.NFR03_*` |

Commands: `cd backend && ./gradlew build` (36 tests), `cd android && ./gradlew assembleDebug testDebugUnitTest lint` (41 tests, lint clean).
Manual (owner): register an apartment with a real Booking.com iCal export link, check the list/role, edit it,
log in as another user (no access), delete it; change phone/language in the profile.

---

## Stage 3: Reservations and cleaning status

**Status:** ✅ Done
**Objective:** Reservations are imported from Booking.com and cleaning status is tracked.

### Requirements implemented
- **REQ-06:** Reservation import (iCal, every 30 min, after registration, pull to refresh)
- **REQ-07:** Reservation list (current/future only, sorted by check-out, status, who took the cleaning)
- **REQ-08:** Cleaning status (`NOT_PLANNED` → `TAKEN` → `DONE`, admin may set any, audit trail)
- **REQ-05 (completed):** deleting an apartment now also removes its reservations

### Implementation checklist

#### Backend
- [x] `ReservationSource` interface; v1 `IcalReservationSource` (shared `CalendarFeedClient`: https, no redirects, timeouts, 5 MB cap)
- [x] ical4j relaxed parsing; events without UID/DTSTART/DTEND are skipped with a warning (Q-08);
      parser tested on a **real Booking.com export** (Q-18)
- [x] Flyway `V3__reservations.sql`: `reservation` (unique `(apartment_id, external_id)`, `cancelled_at`,
      optimistic `version`), `reservation_status_change` (who/when/from/to), cascades from `apartment`
- [x] Import is fail-safe: the feed is downloaded first; any error → nothing is written (Q-08)
- [x] Re-import upserts by UID (no duplicates); changed dates are updated; reservations missing from the feed
      that have not ended (check-out ≥ today in the apartment's time zone) are cancelled and hidden (Q-04)
- [x] Scheduler every `PT30M` (`BOOKMAN_IMPORT_INTERVAL`), imports run in parallel on a small pool, one at a time
      per apartment via a PostgreSQL advisory lock (Q-13)
- [x] First import right after apartment registration (after commit)
- [x] `GET /api/apartments/{id}/reservations`, `POST …/reservations/import`,
      `PUT …/reservations/{rid}/cleaning-status`; every endpoint requires a role in the apartment (403 otherwise)
- [x] Status rules in `CleaningStatusPolicy`; stale/concurrent changes → 409 `STATUS_CONFLICT`

#### Android
- [x] Tapping an apartment opens its reservation list; apartment details moved to the screen menu
- [x] Rows: check-in → check-out (localized dates), status, "taken by"; empty state
- [x] Pull to refresh = import from Booking.com, then reload (list still shown if the import fails)
- [x] Buttons by role: cleaning service "Take this cleaning" / "Mark as cleaned"; admin "Change status" dialog
- [x] After registering an apartment the app opens its reservations; after deleting it returns to the list
- Room is still not used (the list is always loaded fresh from the backend).

### Acceptance criteria verification
| Criterion | Verified by |
|---|---|
| REQ-06 import every 30 min + manual (pull to refresh) | `ReservationImportApiTest.REQ06_scheduleRunsEvery30MinutesForEveryApartment`, `…REQ06_anyMemberCanRefreshButStrangersCannot`, `ReservationListViewModelTest.REQ06_pullToRefreshImportsThenReloads`; live `bootRun` check: scheduled import stored the real export |
| REQ-06 each booking → reservation with check-in/check-out | `IcalReservationParserTest.REQ06_realBookingExportBecomesReservationWithCheckInAndCheckOut`, `IcalReservationSourceTest.REQ06_*`, `ReservationImportApiTest.REQ06_importTurnsEachBookingIntoReservation` |
| REQ-06 no duplicates (identity = UID) | `ReservationImportApiTest.REQ06_reimportDoesNotCreateDuplicates`, `…REQ06_changedDatesUpdateTheSameReservation` |
| REQ-06 disappeared + not ended → cancelled and hidden | `ReservationImportApiTest.REQ06_disappearedFutureReservationIsCancelledAndHidden` |
| REQ-06 behind `ReservationSource` | interface + `FakeReservationSource` in all API tests |
| REQ-06 broken feed changes nothing (Q-08) | `ReservationImportApiTest.REQ06_failedDownloadChangesNothing`, `IcalReservationSourceTest.REQ06_unreadableFeedFailsInsteadOfReturningEmpty`, `ReservationListViewModelTest.REQ06_failedImportStillShowsLastKnownList` |
| REQ-04 first import immediately after registration | `ReservationImportApiTest.REQ06_registrationImportsImmediately` |
| REQ-07 current = check-out today or later, sorted by check-out | `ReservationListApiTest.REQ07_showsOnlyCurrentAndFutureSortedByCheckOut` |
| REQ-07 row shows dates, status, who took it | `ReservationListApiTest.REQ07_rowShowsWhoTookTheCleaning`, `ReservationRepositoryImplTest.REQ07_listIsParsed`, `ReservationListViewModelTest.REQ07_*` |
| REQ-07 visible to ADMIN and CLEANING_SERVICE | `ReservationListApiTest.REQ07_visibleToAdminAndCleaningServiceOnly` |
| REQ-08 cleaning service NOT_PLANNED→TAKEN→DONE, responsible recorded | `CleaningStatusApiTest.REQ08_cleaningServiceTakesAndFinishesAndIsRecordedAsResponsible`, `CleaningStatusPolicyTest.REQ08_*`, `CleaningActionsTest.REQ08_*` |
| REQ-08 cleaning service cannot skip or undo someone else's change; may undo its own (Q-28) | `CleaningStatusApiTest.REQ08_cleaningServiceCannotSkipOrUndoSomeoneElsesChange`, `…REQ08_cleaningServiceMayUndoItsOwnChange`, `CleaningStatusPolicyTest.REQ08_cleaningServiceMayUndoOnlyAStatusItSetItself`, `CleaningActionsTest.REQ08_cleaningServiceMayUndoOnlyItsOwnChange` |
| REQ-08 admin sets any status | `CleaningStatusApiTest.REQ08_adminCanSetAnyStatusIncludingRevert` |
| REQ-08 every change stores who and when | `CleaningStatusApiTest.REQ08_everyChangeStoresWhoAndWhen` |
| REQ-08 no role → 403 (read and change) | `CleaningStatusApiTest.REQ08_usersWithoutRoleCannotChangeStatus`, `ReservationListApiTest.REQ07_visibleToAdminAndCleaningServiceOnly` |
| REQ-05 delete removes reservations | `ReservationImportApiTest.REQ05_deletingApartmentRemovesItsReservations` |

Commands: `cd backend && ./gradlew build` (63 tests), `cd android && ./gradlew assembleDebug testDebugUnitTest lint` (54 tests, lint clean).
Manual (owner): open an apartment with a real iCal link → reservations appear (after registration or pull to refresh);
as cleaning service take a cleaning and mark it cleaned; as admin revert it; check "taken by"; block dates in the
Booking.com extranet and pull to refresh to see the new stay (and removal → it disappears).

---

## Stage 4: Invitations and role management

**Status:** ✅ Done
**Objective:** Admins can invite cleaning service members and manage their roles.

### Requirements implemented
- **REQ-09:** Invite cleaning service by e-mail (in-app only, Q-07)
- **REQ-10:** Manage cleaning service members (list, pending invitations, cancel invitation, remove member)
- **REQ-05 (completed):** deleting an apartment now also removes its pending invitations
- **REQ-08 (owner decision Q-20):** extra cleaning status `PRIVATE` for dates the owner blocked by hand

### Implementation checklist

#### Backend
- [x] Flyway `V4__private_cleaning_status.sql` (Q-20) and `V5__invitations.sql`: `invitation` (normalized e-mail,
      role, status PENDING/ACCEPTED/DECLINED/CANCELLED/EXPIRED, inviter, `expires_at` = +14 days), partial unique index =
      one pending invitation per apartment + e-mail (Q-09), cascade from `apartment`
- [x] `POST /api/apartments/{id}/invitations`, `DELETE …/invitations/{iid}`, `GET …/members`,
      `DELETE …/members/{userId}` – ADMIN only (403 otherwise)
- [x] `GET /api/invitations`, `POST /api/invitations/{iid}/accept|decline` – only the user with the invited e-mail
- [x] Accept = role + invitation closed in one transaction (Q-09); expiry checked on every read (no background job needed)
- [x] Errors: `ALREADY_MEMBER`, `INVITATION_PENDING`, `INVITATION_EXPIRED`, `CANNOT_REMOVE_ADMIN`

#### Android
- [x] Apartment details (admin) → **Members**: people with access, pending invitations, invite dialog, cancel, remove (with confirmation)
- [x] Apartment list shows a banner with the number of pending invitations → **Invitations** screen with Accept / Decline
- [x] `PRIVATE` status: admin can "Mark as private"; cleaning service sees "Private" without actions
- Room is still not used.

### Acceptance criteria verification
| Criterion | Verified by |
|---|---|
| REQ-09 admin invites by e-mail; accepted → CLEANING_SERVICE | `InvitationApiTest.REQ09_invitedUserSeesInvitationAndAcceptingGrantsCleaningServiceRole`, `MembersViewModelTest.REQ09_*`, `InvitationsViewModelTest.REQ09_acceptingReportsTheApartmentAndReloads` |
| REQ-09 visible after login, or after registration with that e-mail | `InvitationApiTest.REQ09_invitationAppearsAfterRegisteringWithTheInvitedEmail`, `ApartmentListViewModelTest.REQ09_pendingInvitationsAreCountedForTheBanner` |
| REQ-09 accept or decline | `InvitationApiTest.REQ09_invitedUserCanDecline`, `InvitationsViewModelTest.REQ09_decliningReloads` |
| REQ-09 expires after 14 days | `InvitationApiTest.REQ09_invitationExpiresAfter14Days`, `InvitationsViewModelTest.REQ09_expiredInvitationShowsError` |
| REQ-09 e-mail that already has a role → error | `InvitationApiTest.REQ09_invitingSomeoneWhoAlreadyHasARoleIsAnError`, `MembersViewModelTest.REQ09_existingMemberErrorIsShown` |
| REQ-09 only admin invites; nobody else accepts someone's invitation | `InvitationApiTest.REQ09_onlyAdminCanInvite`, `…REQ09_nobodyElseCanAcceptSomeonesInvitation` |
| REQ-10 admin sees members + pending invitations | `MembersApiTest.REQ10_adminSeesMembersAndPendingInvitations`, `MembersViewModelTest.REQ10_showsMembersAndPendingInvitations`, `MemberRepositoryImplTest.REQ10_*` |
| REQ-10 cancel invitation, remove cleaning service (not the admin) | `MembersApiTest.REQ10_adminCanCancelInvitation`, `…REQ10_adminCanRemoveCleaningServiceButNotTheAdmin` |
| REQ-10 admin only (backend) | `MembersApiTest.REQ10_onlyAdminManagesMembers` |
| REQ-05 delete removes pending invitations | `InvitationApiTest.REQ05_deletingApartmentRemovesPendingInvitations` |
| REQ-08 PRIVATE: admin only, frozen for cleaning service (Q-20) | `CleaningStatusApiTest.REQ08_adminMarksManuallyBlockedDatesPrivateAndCleaningServiceCannotChangeIt`, `CleaningStatusPolicyTest.REQ08_onlyAdminSetsOrLeavesPrivate`, `CleaningActionsTest.REQ08_privateIsFrozenForCleaningService` |

Commands: `cd backend && ./gradlew build` (78 tests), `cd android && ./gradlew assembleDebug testDebugUnitTest lint` (69 tests, lint clean).
Manual (owner): as admin open apartment → ⋮ → Apartment → ⋮ → Members → Invite the second account's e-mail;
log in as that account → banner → Accept → apartment appears as Cleaning service; as admin remove the member;
invite an e-mail without an account, then register with it → invitation appears.

---

## Stage 5: Reminders (push notifications)

**Status:** ✅ Done
**Objective:** Phones of the admin and the cleaning service are reminded about unplanned and unfinished cleanings.
**Owner decision Q-24:** reminders are push notifications to the Bookman app (Firebase Cloud Messaging), not SMS.

### Requirements implemented
- **REQ-11:** "Not planned" reminder – check-out tomorrow, status `NOT_PLANNED`
- **REQ-12:** "Not done" reminder – check-out today, status not `DONE` (and not `PRIVATE`, Q-20)
- **REQ-13 (as changed by Q-24):** sending through `PushSender` (development logger, FCM via env vars, 3 retries)
- **REQ-14 (part):** reminder text in each recipient's profile language

### Implementation checklist

#### Backend
- [x] Flyway `V6__sms_reminders.sql` (reminder hours on `apartment`, `TIME`, NULL = off) and
      `V7__push_notifications.sql` (`device_token`, `reminder_notification` with unique
      `(reservation, recipient, type, day)`, status PENDING/SENT/FAILED/NO_DEVICE) – Q-11, Q-24
- [x] `V8__optional_phone_number.sql` – phone number optional in REQ-01/REQ-02 (owner decision, Q-24 follow-up);
      app labels the field "(optional)" and an empty field removes the number
- [x] `GET/PUT /api/apartments/{id}/notification-settings` (`"HH:MM"` or null), ADMIN only (REQ-05)
- [x] `POST /api/users/me/devices` and `…/devices/unregister` – the app registers its FCM token after login and
      removes it on logout; a token belongs to the last user who logged in on that phone
- [x] `ReminderPlanner` (every minute): due reminders (apartment time zone, within one hour) are queued per recipient
      and reservation with `INSERT … ON CONFLICT DO NOTHING`
- [x] `ReminderDispatcher` (every 10 s): sends to all phones of the recipient; on failure logs and retries after
      2 s / 4 s / 8 s, then `FAILED`; tokens FCM reports as unregistered are deleted; no phone → `NO_DEVICE`
- [x] `PushSender`: `LoggingPushSender` (default) and `FcmPushSender` (`BOOKMAN_PUSH_PROVIDER=fcm`,
      `BOOKMAN_FCM_CREDENTIALS` = path to the service-account JSON)
- [x] Texts in `reminders.properties` / `reminders_pl.properties` (title + apartment name + check-out date)

#### Android
- [x] Firebase Messaging; `google-services.json` is optional and git-ignored – without it the app builds and runs
      without notifications (see CLAUDE.md "Push notifications")
- [x] `PushRegistrar`: registers the token after login, on app start and on token rotation; unregisters on logout
- [x] Notification channel "Cleaning reminders", notifications also shown while the app is open, Android 13+
      notification permission requested once on the apartment list
- [x] Apartment details (admin) → ⋮ **Cleaning reminders**: two switches with 24 h time pickers

### Acceptance criteria verification
| Criterion | Verified by |
|---|---|
| REQ-11 at the set hour, check-out tomorrow, NOT_PLANNED, to all recipients' phones | `ReminderNotificationTest.REQ11_notPlannedReminderAtConfiguredHourForTomorrowsCheckOut`, `…REQ11_everyPhoneOfARecipientGetsIt`, `…REQ11_onlyNotPlannedReservationsAreReminded` |
| REQ-11/12 one per recipient per reservation per day (retry/restart) | `ReminderNotificationTest.REQ11_oneReminderPerRecipientPerReservationPerDayEvenAfterRestart` |
| REQ-11/12 contains apartment name and check-out date | `ReminderNotificationTest.REQ11_notPlannedReminder…`, `…REQ12_notDoneReminder…` |
| REQ-11/12 can be disabled; settings admin-only | `ReminderNotificationTest.REQ11_disabledReminderSendsNothing`, `…REQ11_settingsAreAdminOnlyAndValidated`, `NotificationSettingsViewModelTest.*` |
| REQ-12 check-out today, not DONE | `ReminderNotificationTest.REQ12_notDoneReminderForTodaysCheckOutUnlessDoneOrPrivate` |
| REQ-11/12 in the apartment's time zone | `ReminderNotificationTest.REQ12_hourIsInTheApartmentsTimeZone`, `…REQ11_missedHourIsCaughtUpWithinOneHourOnly` |
| Phones registered per logged-in user (Q-24) | `DeviceApiTest.REQ11_*`, `PushRegistrarTest.REQ11_*`, `DeviceRepositoryImplTest.REQ11_*`, `AuthRepositoryImplTest.REQ11_loginAndLogoutNotifyPushRegistration` |
| REQ-13 development implementation logs | `PushConfigTest.REQ13_developmentDefaultOnlyLogs` |
| REQ-13 production provider via env vars | `PushConfigTest.REQ13_fcmWithoutCredentialsFailsFastAtStartup`, `FcmPushSenderTest.REQ13_*` (local stand-in for FCM) |
| REQ-13 failures logged and retried at most 3 times | `ReminderNotificationTest.REQ13_failedSendIsRetriedAtMostThreeTimes`, `…REQ13_temporaryFailureIsSentOnRetry`, `…REQ13_invalidDeviceTokenIsForgottenAndUserWithoutPhoneIsNotRetried` |
| REQ-14 reminder in the recipient's language | `ReminderNotificationTest.REQ11_notPlannedReminder…` (EN and PL recipient) |

Commands: `cd backend && ./gradlew build` (98 tests), `cd android && ./gradlew assembleDebug testDebugUnitTest lint` (83 tests, lint clean; also builds with a `google-services.json`).
Manual (owner): set up Firebase (CLAUDE.md "Push notifications"), run backend with `BOOKMAN_PUSH_PROVIDER=fcm`,
log in on the emulator (Google Play image), allow notifications, set a reminder to the next minute → notification.
Without Firebase: the backend log shows `Push (not sent, development mode) …`.

---

## Stage 6: Localization (Polish and English)

**Status:** ✅ Done

**Objective:** App and reminders are available in Polish and English.

### Requirements implemented
- **REQ-14:** Polish (pl) and English (en), system language on first start, switch in the app settings applies
  immediately, the language is stored in the profile and used for the user's reminders (push since Q-24)

### Implementation checklist

#### Backend
- [x] `language` (EN/PL) on the user, set at registration and in the profile (Stage 1/2)
- [x] Reminder texts in English and Polish (`reminders*.properties`), chosen per recipient (Stage 5)
- [x] `ReminderTextsTest.REQ14_*`

#### Android
- [x] All user-visible strings in `values/strings.xml` and `values-pl/strings.xml`; lint `HardcodedText` and
      `MissingTranslation` clean; `TranslationsTest` checks both files have the same keys
- [x] First start: the system language decides through resource fallback (Polish system → Polish, otherwise
      English); registration stores the device language (`DeviceLanguage`)
- [x] `AppLanguage` / `AppCompatAppLanguage`: per-app language via `AppCompatDelegate.setApplicationLocales`,
      applied after login/registration (profile language) and after saving the profile – activities are recreated,
      no reinstall; persisted by `AppLocalesMetadataHolderService` (Android ≤ 12) or the system (Android 13+,
      `locales_config.xml` also lists the app in the system's per-app language settings)
- [x] Notification channel name follows the in-app language
- [x] Q-26: sync points and behaviour after logout

### Acceptance criteria verification
| Criterion | Tests |
|---|---|
| First start follows system language | `DeviceLanguageTest.REQ14_polishSystemGivesPolishOtherwiseEnglish`, `RegisterViewModelTest.REQ14_newAccountGetsTheSystemLanguageAndTheAppUsesIt` |
| Switch in settings applies immediately, stored on backend | `ProfileViewModelTest.REQ14_languageChangeInSettingsAppliesImmediately`, `…REQ14_languageIsNotChangedWhenSavingFails`, `ProfileApiTest.REQ02_userCanViewAndEditPhoneAndLanguage` |
| Profile language used after login | `LoginViewModelTest.REQ14_profileLanguageIsAppliedAfterLogin`, `…REQ14_failedLoginKeepsTheLanguage` |
| Both languages complete | `TranslationsTest.REQ14_everyEnglishStringHasAPolishTranslation`, `TranslationsTest.REQ14_languageCodesMapToLocales` |
| Reminders in the user's language | `ReminderTextsTest.REQ14_reminderTextsInEnglishAndPolish`, `ReminderNotificationTest.REQ11_notPlannedReminderAtConfiguredHourForTomorrowsCheckOut` |

Manual (owner): fresh install on a Polish and on an English emulator → login screen in that language; Profile →
switch language → Save → the whole app switches at once; restart the app → language kept; log in on another
device → profile language applied.

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
| REQ-08 | `REQ08_cleaningStatusTransitions`, `REQ08_cleaningServiceMayUndoItsOwnChange` | Integration | backend |
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
| 2. Apartments | ✅ Done | `claude/hopeful-brahmagupta-3cr7bc-stage2` | [#3](https://github.com/ArturCepowski/bookman/pull/3) |
| 3. Reservations | ✅ Done | `claude/hopeful-brahmagupta-3cr7bc` | [#4](https://github.com/ArturCepowski/bookman/pull/4) |
| 4. Invitations | ✅ Done | `claude/hopeful-brahmagupta-3cr7bc` | [#5](https://github.com/ArturCepowski/bookman/pull/5) |
| 5. Notifications | ✅ Done | `claude/hopeful-brahmagupta-3cr7bc` | [#6](https://github.com/ArturCepowski/bookman/pull/6) |
| 6. Localization | ✅ Done | `claude/hopeful-brahmagupta-3cr7bc` | [#7](https://github.com/ArturCepowski/bookman/pull/7) (together with release & deployment) |
| Release & deployment (not a spec stage: SDK 36, Q-25; hosting, Q-06) | ✅ Done | `claude/hopeful-brahmagupta-3cr7bc` | [#7](https://github.com/ArturCepowski/bookman/pull/7) |

**Owner feedback 2026-10-29 (UI only, no new requirements):** Polish UI says "apartament" instead of "mieszkanie";
the reservation screen menu offers *Details* for everyone and *Members*, *Cleaning reminders*, *Delete* for the
ADMIN (moved from the details screen, which keeps *Edit*) – `ReservationListViewModelTest.REQ05_*`.
Second round: simple teal color theme; apartments, reservations and members shown as rounded tiles; cleaning
status and role as colored labels; the cleaning service may **undo a status it set itself** (Q-28, migration
`V9__status_changed_by.sql`, *Undo* button on the reservation tile).

Production runs on an Oracle Always Free VM (`docs/deployment.md`); releases go to the Google Play internal
testing track. Before every Play upload raise `bookmanVersionCode` in `android/gradle.properties`.
