# DECISIONS

Radiodiagnostics and Medical Imaging Management System (SESCAM)
Iteration 6 (Milestone 4) – Analysis discipline
- C6 Patient portal ([#186](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/186)): UC01 ([#187](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/187)) and UC04 ([#190](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/190))

Each entry records the context, the decision and the rationale. The numbering continues from [AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md); the rules of [AN-IT1-DECISIONS.md](../AN-IT1-DECISIONS.md), [AN-C2-C3-DECISIONS.md](../AN-C2-C3-DECISIONS.md), [AN-C4-DECISIONS.md](../AN-C4-DECISIONS.md) and [AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) still apply (BCE pattern D07, naming D08, `AuditLog` written by every use case D10, permission check shown explicitly D16). The decisions of the team are written `D-0xx`; the decisions of the analysis, `Dxx`.
Status: **Agreed** = decided by the team · **To confirm** = proposed, pending team approval.

Artefacts of this iteration (PlantUML, the official version since D-046):
- [`UC01-patient-appointments-sequence.puml`](UC01-patient-appointments-sequence.puml)
- [`UC04-patient-feedback-sequence.puml`](UC04-patient-feedback-sequence.puml)

The alternative scenarios are drawn in the diagrams as `alt`/`else` fragments. The messages of the main flow keep consecutive numbers; the messages of an alternative scenario take the number of the step where it starts plus a letter (`3a`, `3b`…).

---

## A. Scope of Iteration 6

### D45 – Iteration 6 covers UC01 and UC04
- **Status:** Agreed ([#186](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/186), [Meeting 9](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-9))
- **Context:** C6 is done in Milestone 4 together with C7 ([AN-C7-DECISIONS.md](AN-C7-DECISIONS.md)), due on 29/11/2026. UC03 and UC05 are deferred to Iteration 8 (#186).
- **Decision:** This document covers the analysis of UC01 and UC04. The interfaces declared for UC03 and UC05 (`getCompletedProcedureHistory`, `getClinicalGuidelines`) are not analysed.
- **Rationale:** D-011: each iteration only models its own use cases.

---

## B. Use case flows

The messages of each sequence diagram are traced to these steps (AN-IT1 D06).

### UC01 – View and manage own scheduled appointments
**Main flow (view)**
1. The patient opens their appointments. → msgs 1–2
2. The system checks the permission (UC18) and finds the patient linked to the user account (D48). → msgs 3–4
3. The system reads the upcoming appointments of the patient, with the test type of each one, registers the access and shows them (D50). → msgs 5–8

**Main flow (confirm)**
4. The patient confirms an appointment. → msgs 9–10
5. The system checks the permission and that the appointment is the patient's own, upcoming and not cancelled, records the confirmation (D51), registers it and shows a confirmation. → msgs 11–16

**Main flow (request reschedule)**
6. The patient asks to reschedule an appointment, with their preferred dates and a reason. → msgs 17–18
7. The system checks the permission, the appointment and the mandatory fields, stores the request for the admissions staff (D52), registers it and shows a confirmation. The appointment is not moved. → msgs 19–25

**Main flow (cancel)**
8. The patient cancels an appointment and confirms it. → msgs 26–29
9. The system checks the permission and the appointment, cancels it as UC13 does (D53), registers it and shows a confirmation. → msgs 30–36

**Alternative flows**
- 3a / 11a / 19a / 30a. No permission: the attempt is registered in the audit log and access is denied (D47).
- 8a. The patient has no upcoming appointments: the screen says so.
- 16a / 25a / 36a. The appointment does not exist, is not the patient's own, is past or is cancelled: the reason is shown and nothing changes.
- 25b. A mandatory field of the reschedule request is missing: the error is shown and nothing is sent.
- 28a. The patient does not confirm the cancellation: nothing is sent and nothing changes.

### UC04 – Submit incident reports and satisfaction surveys
**Main flow (choose an appointment)**
1. The patient opens the feedback screen. → msgs 1–2
2. The system checks the permission, finds the linked patient, reads all their appointments, registers the access and shows them. → msgs 3–7

**Main flow (incident report)**
3. The patient chooses one of their appointments, past or upcoming, and describes the incident. → msgs 8–9
4. The system checks the permission, that the appointment is the patient's own and the mandatory fields, stores the report (D54), registers it and shows a confirmation. → msgs 10–16

**Main flow (satisfaction survey)**
5. The patient chooses one of their past appointments, rates the care received and may add comments. → msgs 17–18
6. The system checks the permission, that the appointment is the patient's own, that it is past and has no survey yet (D55) and the mandatory fields, stores the survey, registers it and shows a confirmation. → msgs 19–27

**Alternative flows**
- 3a / 10a / 19a. No permission: the attempt is registered in the audit log and access is denied (D47).
- 16a / 27a. The appointment does not exist or is not the patient's own: the reason is shown and nothing is stored.
- 27b. The appointment is not past yet or already has a survey: the reason is shown.
- 16b / 27c. A mandatory field is missing: the error is shown and nothing is stored.

---

## C. Analysis model

### D46 – Permission of C6
- **Status:** To confirm (patch of C1, D-036; owner of C1: Ignacio)
- **Context:** #186 says that every operation of C6 requires a user with the Patient role (UC18), and that a new permission is a patch of C1.
- **Decision:** `USE_PATIENT_PORTAL`, given to the Patient, for UC01 and UC04.
- **Rationale:** One permission per kind of work, as in C2–C5 (AN-C4 D29, AN-C5 D37).

### D47 – Failed permission checks and every access are registered
- **Status:** Agreed (#186 §7, #189, #192)
- **Decision:** When the permission check fails, the control registers the attempt (`ACCESS_DENIED`) before denying access. Every access of the patient to their appointments is registered too (`APPOINTMENTS_ACCESSED`, msg 7 of UC01 and msg 6 of UC04), as well as every action (confirm, reschedule request, cancel, incident report, survey).
- **Rationale:** #186 says that every patient access and action emits an audit event (UC19); the failed checks are an alternative scenario of every server sub-issue of C6.

### D48 – The user account knows its patient
- **Status:** To confirm (affects C1 and C3: requirements and patches, D-036)
- **Context:** #189 says that the server returns the appointments of the **authenticated** patient (UC22), and #186 §9 that a patient only acts on their own records. Today nothing links a `UserAccount` (C1) with a `Patient` (C3), so the system cannot know which patient is logged in.
- **Decision:** New association `UserAccount` `0..1` — `0..1` `Patient`. The control finds the patient of the user with `getLinkedPatient(user)` (msgs 4, 12, 20, 31 of UC01; 4, 11, 20 of UC04), and every check of an appointment uses that patient, never a `patientId` sent by the client.
- **Alternatives:** The client sends the `patientId` (it could act on another patient's data); matching by e-mail or identifier (not reliable).
- **Rationale:** It is the minimum data needed for "a patient can only see and act on their own appointments" (#189). Who creates the link (C1 when the account is created, or C3 when the patient is registered) is a question for Requirements.

### D49 – C6 uses the entities of C2 and C5 directly in the analysis
- **Status:** Agreed (D-023, AN-C5 D39)
- **Decision:** In the analysis, the controls of C6 talk directly to `Appointment` and `SchedulingNeed` (C5, C4) and `TestType` (C2). In design these accesses become interfaces of each component. Confirming and cancelling change an `Appointment`, which belongs to C5: the operations that C6 needs from C5 (`getAppointmentsByPatient`, `confirmAppointment`, `cancelAppointment`) are a patch of C5 (D-036).
- **Rationale:** The analysis model shows responsibilities, not modules; each component owns its data (D-023).

### D50 – What the patient sees in UC01
- **Status:** To confirm (open question of #186 §10)
- **Decision:** UC01 shows only the **upcoming** appointments of the patient that are not cancelled (BOOKED or RESCHEDULED), with date, time, room, test type and its preparation (from `TestType`, AN-C2-C3 D14), and whether the patient has already confirmed it. Past and cancelled appointments are not shown in UC01; UC04 shows all of them (msg 5), and the history of completed procedures is UC03 (Iteration 8).
- **Rationale:** Only upcoming appointments can be confirmed, rescheduled or cancelled (#188), so the list only shows what the patient can act on.

### D51 – Confirming an appointment is an attribute, not a status
- **Status:** To confirm (open question of #186 §10)
- **Decision:** `Appointment` gets the attribute `confirmedByPatient` (boolean, false when it is booked). Confirming sets it to true (msg 14) and does not change the status. Confirm, request reschedule and cancel are allowed only for upcoming appointments in status BOOKED or RESCHEDULED (`isManageable`, msgs 13, 21, 32).
- **Alternatives:** A new status CONFIRMED.
- **Rationale:** A new status would change the diagrams and the rules of C5 (UC13, UC14, UC15 read the BOOKED appointments). Whether a rescheduled appointment keeps the confirmation is left to the team; it is proposed that UC13 sets it back to false.

### D52 – `RescheduleRequest`, and how UC13 sees it
- **Status:** To confirm (open question of #186 §10; the gap in UC13 is a patch of C5, D-036)
- **Context:** #186 §9 says that C6 stores the reschedule requests and offers them through its interface, that showing them in UC13 is a patch of C5, and that until then they are simulated (D-023).
- **Decision:** New entity `RescheduleRequest` (`requestId`, `date`, `preferredDates`, `reason`, `status`: PENDING or HANDLED), linked to the `Appointment` it is about (`RescheduleRequest` `*` — `1` `Appointment`). It is created PENDING (msg 23) and the appointment is not moved. UC13 (C5) has no message yet to read the pending requests or to mark them HANDLED when it rebooks: this is a point for the team.
- **Alternatives:** An attribute of `Appointment` (only one request, and the reason and dates would be lost).
- **Rationale:** The request has its own data and its own status, and it is handled later by another actor.

### D53 – Cancelling from the portal is the same as in UC13
- **Status:** To confirm (depends on AN-C5 D41)
- **Decision:** The patient's cancellation does the same as UC13 (msgs 31–32 of UC13): status CANCELLED and the slot is freed (msg 33); the appointment is not deleted (D-022). The scheduling need goes back to PENDING (msg 34) only if AN-C5 D41 is approved; otherwise msg 34 is removed.
- **Rationale:** The same rule in both places, whoever cancels.

### D54 – `IncidentReport` and `SatisfactionSurvey`
- **Status:** To confirm (open question of #186 §10)
- **Decision:** Two separate entities, without a common parent:
  - `IncidentReport` (`reportId`, `date`, `description`), `*` — `1` `Appointment` and `*` — `1` `Patient`.
  - `SatisfactionSurvey` (`surveyId`, `date`, `ratings`, `comments` optional), `0..1` — `1` `Appointment` and `*` — `1` `Patient`.
  The scale of `ratings` (proposal: one score from 1 to 5 for each aspect of the care) and the aspects that are rated are a question for Requirements.
- **Alternatives:** An abstract `PatientFeedback` with two subclasses; one entity with a type.
- **Rationale:** They only share the patient, the appointment and the date, and they follow different rules (D55). The analysis model only shows what a use case uses. The link to the patient is kept because #192 asks to link them "to the patient who sends them" and C9 will count them per patient.

### D55 – Rules of the incident reports and the surveys
- **Status:** Agreed for the first two points (#186 §9, #191, #192) · To confirm for the third
- **Decision:**
  - An incident report can be sent for any appointment of the patient, past, upcoming or cancelled (msg 12 only checks that it is the patient's own).
  - A survey is only for a **past** appointment, one per appointment (msgs 22–23). "Past" means that its date and time are before now, not that it was executed, so that C6 does not depend on C7 (#186 §9).
  - Reports and surveys are not changed or deleted once sent: UC04 has no messages for that.
- **Rationale:** #186 §9 says that no use case of this version manages them; they are counted later in C9.
