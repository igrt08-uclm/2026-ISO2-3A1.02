# Plan – C6 Patient portal Design (#186)

> Plan for the Design discipline of Component C6 (*Patient portal*: UC01 and UC04), following
> [AGENTS.md](../../AGENTS.md) (D-025, D-046, D-047, D-048) and [doc/plans/TEMPLATE.md](TEMPLATE.md).
> Covers every domain rule, validation, limit, contract, DTO, screen flow, and user-facing text owned by Design
> (*Decision ownership*, `AGENTS.md`) so Implementation and Testing can work without ambiguity.

| | |
|---|---|
| **Component issue** | #186 *C6. Patient portal* (`Refs #186`; no separate design issue) · branch `feature/c6-design` |
| **Use cases & versions** | UC01 (#187, #188, #189; FR01, C6 `1.0.0`), UC04 (#190, #191, #192; FR04, C6 `1.1.0`) · Product `v0.6.0` · Milestone 4 · *(UC03/UC05 deferred to It8, D-011, D45)* |
| **Specification & analysis** | #186 (§6, §9, §10), #187–#192, [AN-C6-DECISIONS.md](../analysis/AN-C6-DECISIONS.md) (D45–D55), [UC01-patient-appointments-sequence.puml](../analysis/UC01-patient-appointments-sequence.puml), [UC04-patient-feedback-sequence.puml](../analysis/UC04-patient-feedback-sequence.puml) |
| **Design sources & precedent** | [doc/design/README.md](../design/README.md), [doc/plans/C5-design.md](C5-design.md), `doc/design/common/{_style,shared-contracts,C1-common,C2-common,C5-common}.puml`, `DECISIONS.md` (D-008, D-011, D-015, D-016, D-022, D-023, D-025, D-027, D-029, D-030, D-032, D-036, D-041, D-046, D-047, D-048) |
| **Plan approved by** | Marcos Loro (25/11/2026) |

---

## Questions and answers (Step 2)

All open items from `#186` §10, [AN-C6-DECISIONS.md](../analysis/AN-C6-DECISIONS.md) (D46–D55), and cross-component inspection of C1–C5 Java code — confirmed by Marcos (25/11/2026):

- **Q1 – Delivery order and PlantUML files (D-046, D-047)?**
  **A:** One plan (`doc/plans/C6-design.md`) and one branch (`feature/c6-design`, linked `Refs #186`, D-048), delivering modular PlantUML files (`doc/design/common/C6-common.puml`, `doc/design/C6-server-design.puml`, `doc/design/C6-client-design.puml`) plus `doc/plans/C6-context.md` (D-047) in **two sequential steps**: **Step 1 = UC01**, **Step 2 = UC04**. UC03 and UC05 (`getCompletedProcedureHistory`, `getClinicalGuidelines`) stay excluded (D-011, D45).
- **Q2 – Service decomposition and `requester` vs `patientId` (`#186` §6, D48)?**
  **A:** **2 cohesive services**, 1-to-1 with Analysis `Control_*` and component versions:
  - `IPatientAppointmentService` / `PatientAppointmentService` → `Control_PatientAppointments` (UC01, C6 `1.0.0`)
  - `IPatientFeedbackService` / `PatientFeedbackService` → `Control_PatientFeedback` (UC04, C6 `1.1.0`)
  Every method takes `requester : String` first and resolves the linked patient on the server — **never** accepting a client-supplied `patientId` (`#189`, D48).
- **Q3 – Linking `UserAccount` (C1) ↔ `Patient` (C3) (D48)?**
  **A:** **Option A:** Add nullable `-patientId : String` to `UserAccount` (C1 `common`) and `+getLinkedPatientId(username : String) : String` to `IRoleManagementService` (realized via a D-036 PATCH on C1 + `common`). Keep the existing 5-arg `UserAccount` constructor delegating with `patientId = null` so zero existing C1–C5 tests break. Keeps C1 decoupled from C3 and avoids needing `MANAGE_PATIENTS` on C3. If a caller has `USE_PATIENT_PORTAL` but `getLinkedPatientId(requester)` is `null`/blank, throw `ValidationException`.
- **Q4 – Cross-component D-036 patches on finished C1, C2, C4, C5 (OUTSIDE this design PR) & updating `C1-common.puml` / `C5-common.puml`?**
  **A:** Update `doc/design/common/C1-common.puml`, `doc/design/C1-server-design.puml`, `doc/design/common/C5-common.puml`, and `doc/design/C5-server-design.puml` in this PR so `C6-server-design.puml` renders the exact patched contracts via `!includesub` (D-046). The corresponding Java code patches stay in their own D-036 PATCH PRs approved by their component owners (listed in *Required D-036 patches* below).
- **Q5 – UC01 view DTO and `RescheduleRequest` details (D50, D52)?**
  **A:**
  - Add non-persisted derived view `PatientAppointmentView(appointment, testTypeName, preparation, hasPendingRescheduleRequest)` so both `IU_PatientAppointments` and `IU_PatientFeedback` depend only on their own C6 service. UC01 lists only **upcoming non-cancelled** (`BOOKED`/`RESCHEDULED`, `start.isAfter(now)`) appointments.
  - `RescheduleRequest(requestId, appointmentId, patientId, requestedAt, preferredDates, reason, status)` with `preferredDates : String` (free text, non-blank) and `RescheduleRequestStatus { PENDING, HANDLED }`. Reject a second `PENDING` request for the same appointment.
  - Expose `+getPendingRescheduleRequests(requester : String) : List<RescheduleRequest>` and `+markRescheduleRequestHandled(requester : String, requestId : String) : RescheduleRequest` (requiring `MANAGE_APPOINTMENTS`) on `IPatientAppointmentService` for C5's future patch (`#186` §9).
- **Q6 – UC04 `IncidentReport`, `SatisfactionSurvey`, cancelled appointments, and read methods (D54, D55)?**
  **A:**
  - `SatisfactionSurvey.ratings` is `Map<SurveyAspect, Integer>` with `enum SurveyAspect { CARE_QUALITY, WAITING_TIME, COMMUNICATION, FACILITIES }`, scale `1..5`, all 4 aspects required; `comments : String` optional (normalized to `""` when `null`).
  - Surveys are allowed only for **past, non-cancelled** (`BOOKED`/`RESCHEDULED`, `start.isBefore(now)`) appointments of the linked patient that do not already have a survey (one per appointment, immutable once sent). Incident reports are allowed for **any** appointment of the linked patient (past, upcoming, or cancelled; multiple reports allowed; immutable once sent).
  - Keep `IPatientFeedbackService` minimal to the 3 UC04 operations (`getMyAppointments`, `submitIncidentReport`, `submitSatisfactionSurvey`).
- **Q7 – Process & branch (`AGENTS.md` Rule 7, D-041)?**
  **A:** Marcos moved `#186` to *In progress* on the project board, explicitly instructed creating `feature/c6-design` from `develop`, approved all defaults and this plan (25/11/2026), and installed `plantuml` CLI (`1.2026.8`) so `.png` previews are generated directly via `plantuml -tpng` (D-046).

---

## Required D-036 follow-up patches on finished components (code PRs outside this PR)

Per D-036, finished components are patched in their own branches/PRs approved by their component owners before C6 implementation runs end-to-end:

| Component | Patch scope (code) | Exact change | Breaking impact |
|---|---|---|---|
| **C1** (`server-c1-access`, `common`) | Owner: Ignacio | 1) Add `USE_PATIENT_PORTAL` to `InMemoryPermissionRepository` and grant it to `PATIENT` in `InMemoryRoleRepository` (D46). 2) Add nullable `patientId : String` to `UserAccount` (keep 5-arg constructor defaulting to `null` + add 6-arg constructor) and `getLinkedPatientId(username : String) : String` to `IRoleManagementService` / `RoleManagementService` (D48). 3) Link seeded `jfernandez` → `"P-0001"` (and `mgarcia` → `"P-0002"`) in `InMemoryUserAccountRepository`. | Zero — 5-arg `UserAccount` constructor preserved |
| **C2** (`server-c2-configuration`) | C2 owner | Add `USE_PATIENT_PORTAL` to `SystemParameterService.checkReadPermission` so patients can read `TestType` name and preparation instructions for UC01/UC04. | Zero — no interface change |
| **C4** (`server-c4-orders`) | C4 owner | Add `USE_PATIENT_PORTAL` to `ClinicalOrderService.checkSchedulingPermission` so `markSchedulingNeedPending` succeeds when invoked during patient cancellation (D53). | Zero — no interface change |
| **C5** (`server-c5-appointments`, `common`) | C5 owner | 1) Add `confirmedByPatient : boolean` to `Appointment` (keep 9-arg constructor defaulting to `false` + add 10-arg constructor; reset to `false` on reschedule in `AppointmentService.rescheduleAppointment`, D51). 2) Add `confirmAppointment(requester : String, appointmentId : String) : Appointment` to `IAppointmentService` / `AppointmentService` (idempotent if already confirmed; rejects past/cancelled). 3) Allow `USE_PATIENT_PORTAL` in `AppointmentService` on `getAppointmentsOfPatient`, `confirmAppointment`, and `cancelAppointment`. 4) *(Recommended for UC04 demo)* Seed at least one past `BOOKED` appointment for `"P-0001"` in `AppointmentSeedData` so UC04 satisfaction surveys can be exercised out of the box. | Zero — 9-arg `Appointment` constructor preserved |

---

## Files

| File | Action | What |
|---|---|---|
| `doc/plans/C6-design.md` | create | This design plan, domain rules, user-facing texts, PlantUML specs, and step log |
| `doc/plans/C6-context.md` | create | C6 component context summary from `CONTEXT-TEMPLATE.md` (D-047) |
| `doc/design/common/C1-common.puml` | update | Add `-patientId : String` to `UserAccount` and `+getLinkedPatientId(username : String) : String` to `IRoleManagementService` (D48) |
| `doc/design/C1-server-design.puml` | update | Add `+getLinkedPatientId(username : String) : String` to `RoleManagementService` |
| `doc/design/common/C5-common.puml` | update | Add `-confirmedByPatient : boolean` to `Appointment` and `+confirmAppointment(requester : String, appointmentId : String) : Appointment` to `IAppointmentService` (D51) |
| `doc/design/C5-server-design.puml` | update | Add `+confirmAppointment(requester : String, appointmentId : String) : Appointment` to `AppointmentService` |
| `doc/design/common/C6-common.puml` | create | Single-source-of-truth `!startsub`/`!endsub` definitions for C6 domain types and contracts (UC01 in Step 1, UC04 in Step 2) |
| `doc/design/C6-server-design.puml` | create | C6 Server Design Class Diagram (UC01 in Step 1, UC04 in Step 2) |
| `doc/design/C6-client-design.puml` | create | C6 Client Design Class Diagram (UC01 in Step 1, UC04 in Step 2) |
| `doc/design/C6-server-design.png` | create *(optional, IDE export by Marcos)* | Rendered preview from `C6-server-design.puml` (D-046) |
| `doc/design/C6-client-design.png` | create *(optional, IDE export by Marcos)* | Rendered preview from `C6-client-design.puml` (D-046) |

*(Note per Rule 23 / D-048: `AGENTS.md`, `DECISIONS.md`, `agents/*.md`, and `.github/` are NOT modified in this branch.)*

---

## Target design specification

### Package mapping & Maven modules (D-032)
- `common`: `es.uclm.esi.iso2.common.portal.domain` and `es.uclm.esi.iso2.common.portal.contracts`
- `server/c6-portal` (`artifactId`: `server-c6-portal`): `es.uclm.esi.iso2.server.portal.service` and `es.uclm.esi.iso2.server.portal.persistence`
- `client/c6-portal` (`artifactId`: `client-c6-portal`): `es.uclm.esi.iso2.client.portal.presentation`

### 1. `common` → `Domain (entities)` (`es.uclm.esi.iso2.common.portal.domain`)

| Class / Enum | UC / Version | Stereotype | Attributes / Literals | Notes |
|---|---|---|---|---|
| `PatientAppointmentView` | UC01 (`1.0.0`), reused by UC04 | `«entity»` | `-appointment : Appointment`, `-testTypeName : String`, `-preparation : String`, `-hasPendingRescheduleRequest : boolean` | **Derived query view, not persisted** (D50). Enriches C5 `Appointment` with C2 `TestType` (`name`, `preparation`) and pending reschedule flag. |
| `RescheduleRequest` | UC01 (`1.0.0`) | `«entity»` | `-requestId : String`, `-appointmentId : String`, `-patientId : String`, `-requestedAt : LocalDateTime`, `-preferredDates : String`, `-reason : String`, `-status : RescheduleRequestStatus` | Persisted (D52). ID format `REQ-0001`. Immutable with state-transition helper `handled()`. Never deleted (D-022). |
| `RescheduleRequestStatus` | UC01 (`1.0.0`) | `«enumeration»` | `PENDING`, `HANDLED` | D52 |
| `SurveyAspect` | UC04 (`1.1.0`) | `«enumeration»` | `CARE_QUALITY`, `WAITING_TIME`, `COMMUNICATION`, `FACILITIES` | D54. Fixed aspects rated on a `1..5` integer scale. |
| `IncidentReport` | UC04 (`1.1.0`) | `«entity»` | `-reportId : String`, `-appointmentId : String`, `-patientId : String`, `-submittedAt : LocalDateTime`, `-description : String` | Persisted (D54). ID format `INC-0001`. Immutable once created; never edited or deleted (D55, D-022). |
| `SatisfactionSurvey` | UC04 (`1.1.0`) | `«entity»` | `-surveyId : String`, `-appointmentId : String`, `-patientId : String`, `-submittedAt : LocalDateTime`, `-ratings : Map<SurveyAspect, Integer>`, `-comments : String` | Persisted (D54). ID format `SRV-0001`. Defensive unmodifiable copy of `ratings`; `comments` normalized to `""` when `null`. Immutable once created (D55). |

### 2. `common` → `Contracts (interfaces)` (`es.uclm.esi.iso2.common.portal.contracts`)

All service operations take `requester : String` first (D48):

- **`IPatientAppointmentService` (`«Interface»`, UC01 — C6 `1.0.0`):**
  - `+getMyUpcomingAppointments(requester : String) : List<PatientAppointmentView> throws AccessDeniedException, ValidationException`
  - `+confirmAppointment(requester : String, appointmentId : String) : Appointment throws AccessDeniedException, ValidationException`
  - `+requestReschedule(requester : String, appointmentId : String, preferredDates : String, reason : String) : RescheduleRequest throws AccessDeniedException, ValidationException`
  - `+cancelAppointment(requester : String, appointmentId : String) : Appointment throws AccessDeniedException, ValidationException`
  - `+getPendingRescheduleRequests(requester : String) : List<RescheduleRequest> throws AccessDeniedException` *(admissions staff contract for future C5 patch, `#186` §9; requires `MANAGE_APPOINTMENTS`)*
  - `+markRescheduleRequestHandled(requester : String, requestId : String) : RescheduleRequest throws AccessDeniedException, ValidationException` *(admissions staff contract for future C5 patch, `#186` §9; requires `MANAGE_APPOINTMENTS`)*
- **`IPatientFeedbackService` (`«Interface»`, UC04 — C6 `1.1.0`):**
  - `+getMyAppointments(requester : String) : List<PatientAppointmentView> throws AccessDeniedException, ValidationException`
  - `+submitIncidentReport(requester : String, appointmentId : String, description : String) : IncidentReport throws AccessDeniedException, ValidationException`
  - `+submitSatisfactionSurvey(requester : String, appointmentId : String, ratings : Map<SurveyAspect, Integer>, comments : String) : SatisfactionSurvey throws AccessDeniedException, ValidationException`

### 3. `server` (`es.uclm.esi.iso2.server.portal.{service,persistence}`)

- **`Service (control)`:**
  - `PatientAppointmentService` (`«control»`, realizes `IPatientAppointmentService` & `Control_PatientAppointments (UC01)`):
    - Uses `IRescheduleRequestRepository`, `IAppointmentService`, `ISystemParameterService`, `IRoleManagementService`, `IAuditRecorder`, and `Clock` (injected constructor for deterministic tests + default constructor with `InMemoryRescheduleRequestRepository` and `Clock.systemDefaultZone()`, matching `AppointmentService`).
    - Throws `AccessDeniedException`, `ValidationException`.
  - `PatientFeedbackService` (`«control»`, realizes `IPatientFeedbackService` & `Control_PatientFeedback (UC04)`):
    - Uses `IIncidentReportRepository`, `ISatisfactionSurveyRepository`, `IAppointmentService`, `ISystemParameterService`, `IRoleManagementService`, `IAuditRecorder`, and `Clock` (injected + default constructor).
    - Throws `AccessDeniedException`, `ValidationException`.
- **`Persistence`:**
  - `IRescheduleRequestRepository` & `InMemoryRescheduleRequestRepository` (`-requests : Map<String, RescheduleRequest>`, `LinkedHashMap`, nullable returns):
    - `+findById(requestId : String) : RescheduleRequest`
    - `+findPending() : List<RescheduleRequest>`
    - `+findPendingByAppointment(appointmentId : String) : RescheduleRequest`
    - `+findAll() : List<RescheduleRequest>`
    - `+save(request : RescheduleRequest) : void`
  - `IIncidentReportRepository` & `InMemoryIncidentReportRepository` (`-reports : Map<String, IncidentReport>`, `LinkedHashMap`):
    - `+findById(reportId : String) : IncidentReport`
    - `+findByPatient(patientId : String) : List<IncidentReport>`
    - `+findByAppointment(appointmentId : String) : List<IncidentReport>`
    - `+findAll() : List<IncidentReport>`
    - `+save(report : IncidentReport) : void`
  - `ISatisfactionSurveyRepository` & `InMemorySatisfactionSurveyRepository` (`-surveys : Map<String, SatisfactionSurvey>`, `LinkedHashMap`, nullable returns):
    - `+findById(surveyId : String) : SatisfactionSurvey`
    - `+findByAppointment(appointmentId : String) : SatisfactionSurvey`
    - `+findByPatient(patientId : String) : List<SatisfactionSurvey>`
    - `+findAll() : List<SatisfactionSurvey>`
    - `+save(survey : SatisfactionSurvey) : void`

### 4. `Presentation` (`es.uclm.esi.iso2.client.portal.presentation`)

Matches C3/C5 Swing conventions (`JPanel`, `confirm: Predicate<String>`, `onBack: Runnable`, `Main.java` / `IU_Welcome.java`):

- **`IU_PatientAppointments` (`«boundary»`, realizes `IU_PatientAppointments (UC01)`):**
  - `+IU_PatientAppointments(patientAppointmentService : IPatientAppointmentService, requester : String, confirm : Predicate<String>, onBack : Runnable)`
  - `+loadData() : void`
  - `+onConfirmAppointment(appointmentId : String) : void`
  - `+onRequestReschedule(appointmentId : String, preferredDates : String, reason : String) : void`
  - `+onCancelAppointment(appointmentId : String) : void`
  - `+showAppointments(appointments : List<PatientAppointmentView>) : void`
  - `+showNoAppointments() : void`
  - `+showErrors(errors : List<String>) : void`
  - `+showAccessDenied() : void`
  - `+showConfirmation(message : String) : void`
- **`IU_PatientFeedback` (`«boundary»`, realizes `IU_PatientFeedback (UC04)`):**
  - `+IU_PatientFeedback(patientFeedbackService : IPatientFeedbackService, requester : String, onBack : Runnable)`
  - `+loadData() : void`
  - `+onSubmitIncidentReport(appointmentId : String, description : String) : void`
  - `+onSubmitSatisfactionSurvey(appointmentId : String, ratings : Map<SurveyAspect, Integer>, comments : String) : void`
  - `+showAppointments(appointments : List<PatientAppointmentView>) : void`
  - `+showNoAppointments() : void`
  - `+showErrors(errors : List<String>) : void`
  - `+showAccessDenied() : void`
  - `+showConfirmation(message : String) : void`

---

## Domain rules, validations, limits, audit policy & user-facing texts (Design-owned)

Explicitly fixed here per *Decision ownership* (`AGENTS.md`) so Implementation and Testing share exact expectations:

### Domain rules, validations & limits
1. **Permissions & audit (D46, D47):**
   - Patient operations (`getMyUpcomingAppointments`, `confirmAppointment`, `requestReschedule`, `cancelAppointment`, `getMyAppointments`, `submitIncidentReport`, `submitSatisfactionSurvey`) require `USE_PATIENT_PORTAL`. On failure: register `(requester, "ACCESS_DENIED", "USE_PATIENT_PORTAL")` via `IAuditRecorder` and throw `AccessDeniedException(requester)`.
   - Admissions operations (`getPendingRescheduleRequests`, `markRescheduleRequestHandled`) require `MANAGE_APPOINTMENTS`. On failure: register `(requester, "ACCESS_DENIED", "MANAGE_APPOINTMENTS")` and throw `AccessDeniedException(requester)`.
   - **Audit actions emitted by C6 directly** (so C6 unit tests with mocked C5 verify every AC in `#186` §7, `#189`, `#192`):
     - `APPOINTMENTS_ACCESSED` (`PATIENT:<patientId>`) — `getMyUpcomingAppointments`, `getMyAppointments`
     - `APPOINTMENT_CONFIRMED` (`APPOINTMENT:<appointmentId>`) — `confirmAppointment`
     - `RESCHEDULE_REQUESTED` (`RESCHEDULE_REQUEST:<requestId>`) — `requestReschedule`
     - `RESCHEDULE_REQUEST_HANDLED` (`RESCHEDULE_REQUEST:<requestId>`) — `markRescheduleRequestHandled`
     - `APPOINTMENT_CANCELLED` (`APPOINTMENT:<appointmentId>`) — `cancelAppointment`
     - `INCIDENT_REPORTED` (`INCIDENT_REPORT:<reportId>`) — `submitIncidentReport`
     - `SURVEY_SUBMITTED` (`SATISFACTION_SURVEY:<surveyId>`) — `submitSatisfactionSurvey`
     *(C5 also audits its own operations when invoked, same pattern as C3 `PATIENT_ACCESSED` in AN-C5 D39.)*
2. **Linked patient resolution (D48):**
   - After permission check passes, resolve `patientId = roleManagementService.getLinkedPatientId(requester)`.
   - If `patientId == null || patientId.isBlank()`, throw `ValidationException` with `"No patient record is linked to this user account"`.
3. **Appointment lookup & ownership:**
   - Retrieve the patient's appointments via `appointmentService.getAppointmentsOfPatient(requester, patientId)` and find matching `appointmentId`.
   - If `appointmentId` is null/blank: `"The appointment is required"`.
   - If not found among the linked patient's appointments (either does not exist or belongs to another patient — do not leak other patients' IDs): `"The appointment " + appointmentId + " was not found for your account"`.
4. **UC01 upcoming & manageable rules (D50, D51, D52, D53):**
   - **Upcoming:** `appointment.getStart().isAfter(LocalDateTime.now(clock))` (no minimum notice, `#186` §9).
   - **Manageable:** owned by linked patient, status is `BOOKED` or `RESCHEDULED` (not `CANCELLED`), and upcoming (`start.isAfter(now)`).
   - Reject with `ValidationException`:
     - If `status == CANCELLED`: `"Cancelled appointments cannot be modified"`.
     - If `!start.isAfter(now)`: `"Past appointments cannot be modified"`.
   - **`getMyUpcomingAppointments`:** returns only manageable appointments (`status != CANCELLED && start.isAfter(now)`), sorted by `start` ascending, enriched with `TestType.name`, `TestType.preparation`, and `hasPendingRescheduleRequest = (rescheduleRepo.findPendingByAppointment(id) != null)`.
   - **`confirmAppointment`:** if manageable, delegates to `appointmentService.confirmAppointment(requester, appointmentId)` (idempotent if already `confirmedByPatient == true`) and logs `APPOINTMENT_CONFIRMED`.
   - **`requestReschedule`:** validates appointment is manageable AND:
     - `preferredDates` non-blank (`"Preferred dates are required"`) and `<= 500` chars (`"Preferred dates must be at most 500 characters"`).
     - `reason` non-blank (`"The reason is required"`) and `<= 500` chars (`"The reason must be at most 500 characters"`).
     - No existing `PENDING` request for `appointmentId` (`"There is already a pending reschedule request for this appointment"`).
     - Creates `RescheduleRequest` (`REQ-0001`, `requestedAt = LocalDateTime.now(clock)`, `status = PENDING`), saves it, logs `RESCHEDULE_REQUESTED`. Appointment is **not** moved.
   - **`markRescheduleRequestHandled`:** validates `requestId` non-blank (`"The reschedule request is required"`), exists (`"There is no reschedule request with the id " + requestId`), and is `PENDING` (`"The reschedule request " + requestId + " is already handled"`). Saves `request.handled()`, logs `RESCHEDULE_REQUEST_HANDLED`.
   - **`cancelAppointment`:** validates appointment is manageable, delegates to `appointmentService.cancelAppointment(requester, appointmentId)` (sets status `CANCELLED` and reverts `SchedulingNeed` to `PENDING` in C4, D-022 / D53), logs `APPOINTMENT_CANCELLED`.
5. **UC04 feedback rules (D54, D55):**
   - **`getMyAppointments`:** returns **all** appointments of the linked patient (past, upcoming, and cancelled), sorted by `start` descending (most recent first), enriched with `TestType.name`, `TestType.preparation`, and `hasPendingRescheduleRequest = false`.
   - **`submitIncidentReport`:** validates appointment belongs to linked patient (any status, past or upcoming) AND `description` non-blank (`"The incident description is required"`) and `<= 1000` chars (`"The incident description must be at most 1000 characters"`). Creates `IncidentReport` (`INC-0001`, `submittedAt = LocalDateTime.now(clock)`), saves, logs `INCIDENT_REPORTED`.
   - **`submitSatisfactionSurvey`:** validates appointment belongs to linked patient AND:
     - `status != CANCELLED` (`"Cancelled appointments cannot receive a satisfaction survey"`).
     - `start.isBefore(LocalDateTime.now(clock))` (`"Satisfaction surveys can only be submitted for past appointments"`).
     - `surveyRepo.findByAppointment(appointmentId) == null` (`"A satisfaction survey has already been submitted for this appointment"`).
     - `ratings != null` and contains every `SurveyAspect.values()` entry with non-null value in `1..5` (missing/null: `"A rating for every aspect is required"`; out of range: `"Every rating must be between 1 and 5"`).
     - `comments`: optional (`null` normalized to `""`); if length `> 1000`: `"Comments must be at most 1000 characters"`.
     - Creates `SatisfactionSurvey` (`SRV-0001`, `submittedAt = LocalDateTime.now(clock)`), saves, logs `SURVEY_SUBMITTED`.

### User-facing texts & UI flows (Design-owned)

| Screen | Element / Situation | Exact text / behaviour |
|---|---|---|
| `IU_PatientAppointments` | Screen title / empty state | Title: `"My Appointments"`. Empty list: `"You have no upcoming scheduled appointments."` |
| `IU_PatientAppointments` | Table columns | `ID`, `Date & Time`, `Test Type`, `Room`, `Preparation`, `Confirmed`, `Reschedule` (`"Pending"` / `"—"`) |
| `IU_PatientAppointments` | Buttons & state | `Confirm appointment` (disabled if selected appointment is already confirmed), `Request reschedule`, `Cancel appointment`, `Back` |
| `IU_PatientAppointments` | Cancel confirmation question | `"Cancel appointment " + appointmentId + "? This action cannot be undone."` — if declined, nothing is sent and nothing changes |
| `IU_PatientAppointments` | Success confirmations | Confirm: `"Appointment " + appointmentId + " confirmed."` · Reschedule: `"Reschedule request " + requestId + " submitted to admissions staff."` · Cancel: `"Appointment " + appointmentId + " cancelled."` |
| `IU_PatientAppointments` | Reload failure after action | `"Action completed, but the appointment list could not be refreshed."` |
| `IU_PatientAppointments` | Access denied | `"Access denied: you do not have permission to use the patient portal."` (disables action inputs, keeps *Back* enabled) |
| `IU_PatientFeedback` | Screen title / empty state | Title: `"Incident Reports & Satisfaction Surveys"`. Empty list: `"You have no appointments available for feedback."` |
| `IU_PatientFeedback` | Table columns | `ID`, `Date & Time`, `Test Type`, `Room`, `Status` |
| `IU_PatientFeedback` | Form labels | Incident tab/section: `Description` (required), button `Submit incident report`. Survey tab/section: `Care quality (1–5)`, `Waiting time (1–5)`, `Communication (1–5)`, `Facilities (1–5)` (all required), `Comments (optional)`, button `Submit satisfaction survey`. Plus `Back`. |
| `IU_PatientFeedback` | Success confirmations | Incident: `"Incident report " + reportId + " submitted."` · Survey: `"Satisfaction survey " + surveyId + " submitted. Thank you for your feedback."` |
| `IU_PatientFeedback` | Reload failure / Access denied | Reload failure: `"Feedback submitted, but the appointment list could not be refreshed."` · Access denied: `"Access denied: you do not have permission to use the patient portal."` |

---

## PlantUML design specification (final state after Step 2)

### `doc/design/common/C6-common.puml`
Defines `!startsub`/`!endsub` blocks for:
- Step 1 (UC01): `PatientAppointmentView`, `RescheduleRequest`, `RescheduleRequestStatus`, `IPatientAppointmentService`
- Step 2 (UC04): `SurveyAspect`, `IncidentReport`, `SatisfactionSurvey`, `IPatientFeedbackService`

### `doc/design/C6-server-design.puml`
- `common/Domain (entities)`: includes `Appointment`, `AppointmentStatus` (`C5-common`), `PatientAppointmentView`, `RescheduleRequest`, `RescheduleRequestStatus` (Step 1), plus `SurveyAspect`, `IncidentReport`, `SatisfactionSurvey` (Step 2).
- `common/Contracts (interfaces)`: includes `AccessDeniedException`, `ValidationException`, `IAuditRecorder` (`shared-contracts`), `IRoleManagementService` (`C1-common`), `ISystemParameterService` (`C2-common`), `IAppointmentService` (`C5-common`), `IPatientAppointmentService` (Step 1), plus `IPatientFeedbackService` (Step 2).
- `server/Service (control)`: `PatientAppointmentService <<control>>` (Step 1), `PatientFeedbackService <<control>>` (Step 2).
- `server/Persistence`: `IRescheduleRequestRepository` & `InMemoryRescheduleRequestRepository` (Step 1), plus `IIncidentReportRepository`, `InMemoryIncidentReportRepository`, `ISatisfactionSurveyRepository`, `InMemorySatisfactionSurveyRepository` (Step 2).

### `doc/design/C6-client-design.puml`
- `common/Domain (entities)`: includes `Appointment`, `AppointmentStatus` (`C5-common`), `PatientAppointmentView`, `RescheduleRequest`, `RescheduleRequestStatus` (Step 1), plus `SurveyAspect`, `IncidentReport`, `SatisfactionSurvey` (Step 2).
- `common/Contracts (interfaces)`: includes `AccessDeniedException`, `ValidationException` (`shared-contracts`), `IPatientAppointmentService` (Step 1), plus `IPatientFeedbackService` (Step 2).
- `Presentation`: `IU_PatientAppointments <<boundary>>` (Step 1), `IU_PatientFeedback <<boundary>>` (Step 2).

---

## Steps

Tick each step with the hash of its commit (green `mvn -B verify`).

- [x] 1. **UC01 Design (C6 `1.0.0`):** Update `C1-common.puml`, `C1-server-design.puml`, `C5-common.puml`, and `C5-server-design.puml` for D48/D51 contracts; create `doc/design/common/C6-common.puml`, `doc/design/C6-server-design.puml`, `doc/design/C6-client-design.puml` (UC01 scope), and `doc/plans/C6-context.md` (D-047); verify `mvn -B verify` is green; commit on `feature/c6-design`. (`df9d9d1`)
- [x] 2. **UC04 Design (C6 `1.1.0`):** Extend `doc/design/common/C6-common.puml`, `doc/design/C6-server-design.puml`, `doc/design/C6-client-design.puml`, and `doc/plans/C6-context.md` with UC04 (`SurveyAspect`, `IncidentReport`, `SatisfactionSurvey`, `IPatientFeedbackService`, `PatientFeedbackService`, repositories, `IU_PatientFeedback`); verify `mvn -B verify` is green; commit on `feature/c6-design`. (`d7c9537`)
- [x] 3. **Rendered previews:** Generate `doc/design/C6-server-design.png` and `doc/design/C6-client-design.png` (plus updated `C1-server-design.png`, `C1-client-design.png`, `C5-server-design.png`, `C5-client-design.png` if desired) via `plantuml -tpng` (D-046) and commit on `feature/c6-design`. (`72d27d1`)

## Verification
- [x] Every UC01 and UC04 requirement, alternative scenario, and acceptance criterion in `#186`–`#192` and `AN-C6-DECISIONS.md` (D45–D55) is covered by the design.
- [x] All `!includesub` references across `doc/design/*.puml` resolve to existing `!startsub`/`!endsub` blocks.
- [x] `doc/plans/C6-context.md` matches `CONTEXT-TEMPLATE.md` and cites every source (D-047, rule 12).
- [x] No shared process files (`AGENTS.md`, `DECISIONS.md`, `agents/*.md`, `.github/`) modified (rule 23, D-048).
- [x] `mvn -B verify` is green.

## Status
- **In progress:** nothing
- **Next step:** Review and push `feature/c6-design`, open PR to `develop` with `Refs #186` (D-048); once merged, tick the *Design* RADIT box on `#187`–`#192` (rule 22) and coordinate the D-036 PATCH PRs for C1, C2, C4, and C5 before UC01/UC04 implementation.
- **Open questions:** none (all Step 2 defaults approved by Marcos on 25/11/2026).
- **Last update:** 25/11/2026, Marcos Loro
