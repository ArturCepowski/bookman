# Bookman – Specification

Version: 0.1 (draft)
Status: open questions at the end must be resolved before implementing the affected REQs.

## 1. Purpose

Bookman helps owners of short-term rental apartments listed on Booking.com coordinate the cleaning
service. The system imports reservations of each apartment and lets the cleaning service mark
for each reservation whether cleaning is planned and done. SMS reminders are sent when cleaning
is not planned or not done in time.

## 2. Glossary

| Term | Meaning |
|---|---|
| User | A person with an account, identified by e-mail address. |
| Apartment (object) | A rental unit registered in Bookman and linked to one Booking.com listing. |
| Role | A user's role for a specific apartment: `ADMIN` or `CLEANING_SERVICE`. |
| Reservation | A stay imported from Booking.com for an apartment (check-in date, check-out date). |
| Cleaning status | Status of a reservation from the cleaning point of view: `NOT_PLANNED`, `TAKEN`, `DONE`. |

## 3. Architecture overview

- **Android app** – the only user interface.
- **Backend server** – required because data is shared between users, roles must be enforced
  centrally, reservations must be synchronized periodically and SMS must be sent at fixed hours
  even when no phone has the app open.
- **Reservation source** – Booking.com. See REQ-06 for the integration method.
- **SMS provider** – external SMS gateway (provider to be chosen, see Q-05).

## 4. Requirements

### 4.1 Accounts

**REQ-01 Registration and login**
A person who installed the app can create an account with e-mail, password and phone number,
and log in.
Acceptance criteria:
- e-mail is unique (case-insensitive); registering an existing e-mail shows an error,
- phone number is stored in E.164 format (e.g. `+48600100200`) and is required (needed for REQ-11/12),
- after login the user stays logged in until they log out or the session expires,
- a logged-in user sees the list of apartments in which they have any role (REQ-03).

**REQ-02 User profile**
A user can view and edit their phone number and preferred language (REQ-14).
Acceptance criteria:
- e-mail cannot be changed in v1,
- changes are saved on the backend.

### 4.2 Apartments and roles

**REQ-03 Apartment list**
The main screen shows all apartments where the user has a role, with the apartment name and the
user's role for it.
Acceptance criteria:
- a user can have different roles in different apartments,
- empty state: message encouraging the user to register an apartment.

**REQ-04 Register an apartment**
Any user can register an apartment and automatically becomes its `ADMIN`.
Required data:
- custom name (1–60 characters),
- Booking.com listing link (URL of the property on booking.com),
- data required by the reservation import method (REQ-06) – in v1: the Booking.com **calendar export (iCal) URL**,
- time zone (default `Europe/Warsaw`).
Acceptance criteria:
- invalid or unreachable iCal URL is rejected with an error message,
- after successful registration the first reservation import (REQ-06) is triggered immediately.

**REQ-05 Edit and delete an apartment**
Only an `ADMIN` of the apartment can edit its data (name, links, time zone, notification settings
from REQ-11/12) and delete it.
Acceptance criteria:
- a `CLEANING_SERVICE` user does not see edit/delete actions,
- the backend rejects such requests from non-admins with HTTP 403,
- deleting an apartment removes its reservations, roles and pending invitations.

### 4.3 Reservations

**REQ-06 Reservation import**
The backend imports reservations of each apartment from Booking.com.
v1 method: periodic download and parsing of the apartment's iCal calendar export.
Acceptance criteria:
- import runs every 30 minutes for every apartment and can be triggered manually from the app (pull to refresh),
- each imported booking becomes a reservation with check-in and check-out date,
- re-import does not create duplicates (reservation identity = iCal event `UID`),
- a reservation that disappears from the feed and has not ended yet is marked `CANCELLED`
  (see Q-04) and is hidden from the default list,
- the import is implemented behind a `ReservationSource` interface so that another method
  (e.g. Booking.com Connectivity API) can be added later without changing the rest of the system.

**REQ-07 Reservation list**
A separate screen per apartment shows current and future reservations.
Acceptance criteria:
- "current" = check-out date is today or later; past reservations are not shown in v1,
- sorted by check-out date ascending,
- each row shows check-in date, check-out date, cleaning status and, if `TAKEN` or `DONE`,
  who took the cleaning,
- visible to both `ADMIN` and `CLEANING_SERVICE` of the apartment.

**REQ-08 Cleaning status**
Each reservation has a cleaning status: `NOT_PLANNED` (initial), `TAKEN`, `DONE`.
`DONE` means the apartment was cleaned after this stay.
Acceptance criteria:
- a `CLEANING_SERVICE` user can change `NOT_PLANNED → TAKEN` and `TAKEN → DONE`;
  the user who sets `TAKEN` is recorded as the person responsible,
- an `ADMIN` can set any status (including reverting) – see Q-02,
- every status change stores who changed it and when,
- users without a role in the apartment cannot read or change its reservations (HTTP 403).

### 4.4 Invitations

**REQ-09 Invite cleaning service**
An `ADMIN` can invite a person by e-mail; after accepting, that person gets the
`CLEANING_SERVICE` role for this apartment.
Acceptance criteria:
- the invitation is visible in the app of the invited user after they log in with the invited e-mail
  (if they have no account yet, it appears after registration with that e-mail),
- the invited user can accept or decline,
- an invitation expires after 14 days,
- inviting an e-mail that already has a role in this apartment shows an error.

**REQ-10 Manage cleaning service members**
An `ADMIN` sees the list of users with roles in the apartment and pending invitations,
can cancel an invitation and remove a `CLEANING_SERVICE` user from the apartment.

### 4.5 Notifications

Both settings are per apartment, editable by `ADMIN` only, in the apartment's time zone.
Recipients: all `ADMIN` and all `CLEANING_SERVICE` users of the apartment.

**REQ-11 "Not planned" SMS**
Setting: hour (HH:MM) of the "not planned" reminder; can be disabled.
At that hour, for every reservation whose check-out date is **tomorrow** and whose status is
`NOT_PLANNED`, an SMS is sent to the recipients (see Q-03).
Acceptance criteria:
- one SMS per recipient per reservation per day (no duplicates on retry or restart),
- the SMS contains apartment name and check-out date.

**REQ-12 "Not done" SMS**
Setting: hour (HH:MM) of the "not done" reminder; can be disabled.
At that hour, for every reservation whose check-out date is **today** and whose status is not
`DONE`, an SMS is sent to the recipients (see Q-03).
Acceptance criteria: same as REQ-11.

**REQ-13 SMS sending**
SMS are sent through an `SmsSender` interface.
Acceptance criteria:
- a development implementation logs messages instead of sending them,
- the production provider is configured via environment variables (see Q-05),
- failed sends are logged and retried at most 3 times.

### 4.6 Localization

**REQ-14 Polish and English**
The app is available in Polish and English.
Acceptance criteria:
- on first start the language follows the system language (Polish if system is Polish, otherwise English),
- the user can switch the language in the app settings; the change applies immediately without reinstall,
- the chosen language is stored in the user profile and used for that user's SMS texts.

## 5. Non-functional requirements

- **NFR-01** All authorization is enforced on the backend.
- **NFR-02** Communication between app and backend uses HTTPS only.
- **NFR-03** The iCal URL is a secret (it gives access to the calendar): it is stored only on the
  backend and shown in the app only to `ADMIN` users.
- **NFR-04** Bookman never stores the user's Booking.com extranet login or password.

## 6. Out of scope for v1

- Direct integration with the Booking.com Connectivity API (requires Connectivity Partner status).
- Guest data (names, contact details) – not available via iCal.
- Other channels (Airbnb etc.) – but the `ReservationSource` design should allow them later.
- Multiple admins per apartment, transferring admin role.
- iOS / web clients.
- Push notifications (SMS only).

## 7. Open questions

- **Q-01 Booking.com integration.** The original idea mentioned the Booking.com API with login data.
  Booking.com's API is available only to certified Connectivity Partners, so v1 uses the iCal
  calendar export. Is the information available in iCal (dates only) enough?
- **Q-02 Status rules.** Can the admin change reservation status (assumed: yes)? Can the cleaning
  service revert `DONE` or `TAKEN` (assumed: no)?
- **Q-03 Reminder timing.** Which reservations should the reminders cover – assumed:
  "not planned" = check-out tomorrow, "not done" = check-out today. Is that right?
- **Q-04 Cancellations.** When a reservation disappears from Booking.com, should it be hidden
  (assumed) or shown as cancelled?
- **Q-05 SMS provider.** Which provider (e.g. SMSAPI.pl, Twilio) and who pays for SMS?
- **Q-06 Hosting.** Where will the backend run?
- **Q-07 Invitations by e-mail.** Should the invited person also receive an e-mail (requires an
  e-mail sending service), or is the in-app invitation enough for v1 (assumed)?
