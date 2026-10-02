# Bookman – working rules for Claude

## Source of truth
- `docs/spec.md` is the specification. Do not implement features that are not in the spec.
- Every requirement has an ID (`REQ-xx`) and acceptance criteria. Work is always traceable to these IDs.
- If something in the spec is ambiguous or missing, do NOT invent product behaviour silently:
  - add the question to `docs/questions.md` (with the related REQ ID),
  - take the simplest reasonable assumption and mark it in code with `// ASSUMPTION(REQ-xx): ...`.
- `docs/plan.md` is the implementation plan. After finishing a stage, update its status there.
- Never edit `docs/spec.md` yourself. Propose spec changes in `docs/questions.md`.

## Repository layout
```
/android    Android client (Gradle project)
/backend    Server application (Gradle project)
/docs       spec.md, plan.md, questions.md
```

## Stack

### Android (`/android`)
- Language: **Java only** (Java 17 language level). No Kotlin source files.
  Gradle build scripts may use the Kotlin DSL (`*.gradle.kts`), application code is Java.
- UI: Android Views with XML layouts + Material Components (Jetpack Compose is Kotlin-only, do not use it).
- Architecture: MVVM – `ViewModel` + `LiveData`, repository layer, single-activity with Navigation component.
- Networking: Retrofit + OkHttp + Gson/Moshi.
- Local cache: Room.
- DI: Hilt.
- minSdk 26, targetSdk/compileSdk 35.
- Localization: all user-visible strings in `res/values/strings.xml` (English, default) and
  `res/values-pl/strings.xml` (Polish). No hard-coded user-visible strings in Java or XML layouts.

### Backend (`/backend`)
- Java 21, Spring Boot 3, Gradle.
- PostgreSQL 16, schema migrations with Flyway.
- Spring Security with JWT (stateless API).
- Scheduling with Spring `@Scheduled` (or Quartz if persistence of jobs is required).
- External integrations behind interfaces (e.g. `ReservationSource`, `SmsSender`) so they can be replaced and mocked.

## Testing and definition of done
A stage is done only when:
- `cd android && ./gradlew assembleDebug testDebugUnitTest lint` passes,
- `cd backend && ./gradlew build` passes (includes tests),
- every implemented REQ has at least one automated test whose name contains the REQ ID
  (e.g. `REQ07_cleaningServiceCannotEditApartment`),
- `docs/plan.md` is updated.

Notes:
- There is no Android emulator in the cloud environment. Do not try to start one.
  Verify Android logic with JVM unit tests (Robolectric is allowed); UI is verified manually by the owner.
- External services (Booking.com, SMS provider) must never be called from tests – use fakes.

## Code conventions
- Small, focused classes; no business logic in Activities/Fragments or REST controllers.
- Authorization rules (who may do what) are enforced on the **backend**. The Android app may hide
  actions, but must never be the only place where a permission is checked.
- Secrets (API keys, passwords, tokens) never go into the repository. Use environment variables.

## Git
- Commit messages in English, Conventional Commits, with REQ IDs: `feat(REQ-04): reservation list screen`.
- One stage of `docs/plan.md` = one branch = one pull request.
