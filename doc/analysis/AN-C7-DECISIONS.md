# DECISIONS

Radiodiagnostics and Medical Imaging Management System (SESCAM)
Iteration 7 (Milestone 4) – Analysis discipline
- C7 Execution and reports ([#193](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/193)): UC10 ([#194](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/194)) and UC09 ([#197](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/197))

Each entry records the context, the decision and the rationale. The numbering continues from [AN-C6-DECISIONS.md](AN-C6-DECISIONS.md); the rules of [AN-IT1-DECISIONS.md](AN-IT1-DECISIONS.md), [AN-C2-C3-DECISIONS.md](AN-C2-C3-DECISIONS.md), [AN-C4-DECISIONS.md](AN-C4-DECISIONS.md), [AN-C5-DECISIONS.md](AN-C5-DECISIONS.md) and [AN-C6-DECISIONS.md](AN-C6-DECISIONS.md) still apply (BCE pattern D07, naming D08, `AuditLog` written by every use case D10, permission check shown explicitly D16). The decisions of the team are written `D-0xx`; the decisions of the analysis, `Dxx`.
Status: **Agreed** = decided by the team · **To confirm** = proposed, pending team approval.

Artefacts of this iteration (PlantUML, the official version since D-046):
- [`analysis/UC10-record-execution-sequence.puml`](analysis/UC10-record-execution-sequence.puml)
- [`analysis/UC09-diagnostic-reports-sequence.puml`](analysis/UC09-diagnostic-reports-sequence.puml)

As in C6, the alternative scenarios are `alt`/`else` fragments, and their messages take the number of the step where they start plus a letter.

---

## A. Scope of Iteration 7

### D56 – Iteration 7 covers UC10 and UC09, in this order
- **Status:** Agreed ([#193](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/193), [Meeting 9](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-9))
- **Context:** C7 is done in Milestone 4 together with C6 ([AN-C6-DECISIONS.md](AN-C6-DECISIONS.md)), due on 29/11/2026. #193 says that UC09 requires an executed procedure (UC10).
- **Decision:** This document covers the analysis of UC10 and then UC09.
- **Rationale:** D-011: each iteration only models its own use cases.

---

## B. Use case flows

The messages of each sequence diagram are traced to these steps (AN-IT1 D06).

### UC10 – Record procedure execution details and document incidents
**Main flow (own worklist)**
1. The physician opens their worklist of a day. → msgs 1–2
2. The system checks the permission (UC18) and builds the worklist from the booked appointments of that day of the physician's assigned patients (D59, D60), with the test type of each one. → msgs 3–6
3. The system registers the access and shows the worklist. → msgs 7–8

**Main flow (execution form)**
4. The physician chooses a procedure of the worklist. → msgs 9–10
5. The system checks the permission and shows the patient and the safety, consent and contrast requirements of the test type (UC17). → msgs 11–15

**Main flow (record)**
6. The physician checks the requirements, enters the contrast given and, if needed, marks and describes an incident, and saves. → msgs 16–17
7. The system checks the permission, that the appointment can be executed and the mandatory fields against the requirements of the test type, stores the execution (D61), marks the appointment as executed (D62), registers the execution and the incident, if any, and shows a confirmation. → msgs 18–26

**Alternative flows**
- 3a / 11a / 18a. No permission: the attempt is registered in the audit log and access is denied (D58).
- 6a. Incident: the physician marks the incident and describes it with the rest of the execution; it is registered too (msg 25).
- 6b. The procedure could not be done (for example, the patient did not come): the physician records it as not performed, with an incident and its reason (D62).
- 26a. The appointment does not exist, is cancelled or is already executed: the reason is shown and nothing is stored.
- 26b. A mandatory field is missing or a safety requirement is not checked: the error is shown and nothing is stored.

### UC09 – Write, edit and review diagnostic reports
**Main flow (choose a procedure)**
1. The physician opens the reports. → msgs 1–2
2. The system checks the permission and shows the performed executions of the physician's assigned patients, with their report, if any, and registers the access. → msgs 3–8

**Main flow (write or edit the draft)**
3. The physician chooses a procedure; the system shows the execution, the images of its study from RIS/PACS (D67) and the draft, if any. → msgs 9–16
4. The physician writes the findings and the impression and links images. → msgs 17–18
5. The system checks the permission, that the procedure was performed, that the patient is assigned to the physician, that the report is not validated and the mandatory fields, saves the report as a draft (D64), registers it and shows a confirmation. → msgs 19–26

**Main flow (validate)**
6. Another physician opens the reports waiting for validation (D65). → msgs 27–32
7. The reviewer validates a report; the system checks the permission and that the reviewer is not the author, marks it as validated, registers it and shows a confirmation. → msgs 33–39

**Main flow (addendum)**
8. The physician adds an addendum to a validated report. → msgs 40–41
9. The system checks the permission, that the report is validated, that the patient is assigned to the physician and the text, stores the addendum (D66), registers it and shows a confirmation. The validated text does not change. → msgs 42–48

**Alternative flows**
- 3a / 11a / 19a / 29a / 42a. No permission: the attempt is registered in the audit log and access is denied (D58). The check of msg 35 follows the same alternative.
- 26a. The procedure was not performed or the patient is not assigned to the physician: the reason is shown and nothing is saved.
- 26b / 39b. The report is already validated: it cannot be edited or validated again.
- 39a. The reviewer is the author of the report: the validation is rejected.
- 48a. The report is still a draft: no addendum is added (the draft is edited instead).
- 48b. The patient is not assigned to the physician: the addendum is rejected.
- 26c / 48c. A mandatory field is missing: the error is shown and nothing is saved.

---

## C. Analysis model

### D57 – Permissions of C7
- **Status:** To confirm (patch of C1, D-036; owner of C1: Ignacio)
- **Context:** #193 says that every operation of C7 requires a user with the Physician role (UC18).
- **Decision:** `RECORD_EXECUTIONS` for UC10 and `WRITE_REPORTS` for UC09 (also used by the reviewer), both given to the Physician.
- **Alternatives:** One permission for the whole of C7.
- **Rationale:** One permission per kind of work (AN-C4 D29, AN-C5 D37): recording an execution and writing a report are different tasks, and in a real service they may be done by different staff.

### D58 – Failed permission checks and every access are registered
- **Status:** Agreed (#193 §7, #196, #199)
- **Decision:** When the permission check fails, the control registers the attempt (`ACCESS_DENIED`) before denying access. The accesses to the worklist and to the reports (`WORKLIST_ACCESSED`, `REPORTS_ACCESSED`) and every action (`EXECUTION_RECORDED`, `INCIDENT_RECORDED`, `REPORT_SAVED`, `REPORT_VALIDATED`, `ADDENDUM_ADDED`) are registered too. The access to patient data through `getPatient` is registered by C3 (AN-C2-C3 D26).
- **Rationale:** #193 says that every execution and report action emits an audit event (UC19).

### D59 – Every patient has an assigned physician
- **Status:** To confirm (affects C3: requirements and patch, D-036)
- **Context:** #197 and #199 speak of the "assigned patients" of a physician, and #193 §10 asks how a patient is assigned to a physician. Today nothing records it: the order only records who asked for it (AN-C4 D31).
- **Decision:** New association `Patient` `*` — `1` `UserAccount` (the assigned physician). UC10 and UC09 read the patients of a physician (`getAssignedPatients`, msg 4 of both) and check it (`isAssignedTo`, msgs 21 and 44 of UC09). Who assigns the physician is a question for Requirements; the proposal is the admissions staff, in UC12 (C3).
- **Alternatives:** The physician who asked for the order (AN-C4 D31); the physician who recorded the execution (UC10); the worklist of UC14.
- **Rationale:** A patient may have several orders from different physicians, so the order does not say who is responsible for them. One assignment is the simplest rule that serves the worklist (D60) and the reports.

### D60 – The worklist of UC10 is the physician's own
- **Status:** To confirm (patch of C5, D-036; resolves AN-C5 D44)
- **Context:** #194 says that the physician records a procedure of **their** worklist (UC14). AN-C5 D44 analysed the worklists of UC14 per room and asked Requirements which physician "per physician" means.
- **Decision:** The worklist of a physician is the booked appointments of a day of their assigned patients (D59), in time order (msgs 4–6). In the analysis the control of UC10 reads `Appointment` directly (D63); in design it is the worklist of UC14 per physician (`getWorklist(date, physicianId)`), which is a patch of C5. UC14 per room stays as it is until the team decides.
- **Rationale:** With D59 the worklist "per physician" of UC14 can be built from data that already exists.

### D61 – `ProcedureExecution`
- **Status:** To confirm (open question of #193 §10) · the rule "only once" is Agreed (#196)
- **Decision:** New entity `ProcedureExecution` (`executionId`, `date`, `performed`, `safetyChecked`, `consentChecked`, `contrastSubstance`, `contrastDose`, `incident`, `incidentDescription`), `0..1` — `1` `Appointment` and `*` — `1` `UserAccount` (the physician who recorded it). The incident is two attributes, not an entity. Safety and consent must be checked when the test type requires them (from `TestType`, AN-C2-C3 D14, msg 20), and the contrast fields are mandatory when the test type uses contrast. An execution is recorded once and is not changed: UC10 has no messages to edit it.
- **Alternatives:** An `Incident` entity of its own; a list of checks per requirement.
- **Rationale:** #196 asks for one execution per booked appointment, with the checks of its test type, the contrast and the incident, if any.

### D62 – The appointment becomes EXECUTED; a procedure not done
- **Status:** To confirm (open question of #193 §10; patch of C5)
- **Decision:** `Appointment` gets the status EXECUTED (msg 23, `markExecuted`), so it is not executed twice and leaves the worklist. When the procedure could not be done (#195), the physician records the execution with `performed` = false and an incident with the reason; the safety and contrast fields are then not mandatory. The appointment is also EXECUTED (closed), and only a **performed** execution can have a report (UC09 msgs 5 and 20).
- **Alternatives:** A status NOT_PERFORMED for the appointment; not recording anything and cancelling the appointment in UC13.
- **Rationale:** The status is needed so that #196 can reject an appointment "already executed". The attribute `performed` keeps the rule "a report only for an executed procedure" (#199) when the patient did not come. Whether the scheduling need goes back to PENDING when the procedure was not done is a question for the team (as AN-C5 D41).

### D63 – C7 uses the entities of C2, C3 and C5 directly in the analysis
- **Status:** Agreed (D-023, AN-C5 D39)
- **Decision:** The controls of C7 talk directly to `Patient` (C3), `TestType` (C2) and `Appointment` (C5). In design these accesses become interfaces of each component (`getPatient`, `getTestType`, `getWorklist`, `markAppointmentExecuted`…); the ones that C5 does not have yet are a patch of C5 (D-036).
- **Rationale:** The analysis model shows responsibilities, not modules.

### D64 – `DiagnosticReport`
- **Status:** To confirm (open question of #193 §10)
- **Decision:** New entity `DiagnosticReport` (`reportId`, `date`, `findings`, `impression`, `imageReferences`, `status`: DRAFT or VALIDATED, `validationDate`), `0..1` — `1` `ProcedureExecution`, with two associations to `UserAccount`: the author (`*` — `1`) and the validator (`*` — `0..1`). `save` creates the report as DRAFT or updates its draft (msg 24); a VALIDATED report is never changed (msgs 22 and 26b). The `reportText` of the contracts of #193 is the findings and the impression; Design decides whether it is kept as a separate field.
- **Rationale:** #199 asks to keep the report linked to the executed procedure and to the images, with its status, author and validator.

### D65 – Second opinion: how the reviewer finds the reports
- **Status:** Agreed for the rule (#193 §9, #199) · To confirm for the list
- **Decision:** A report is validated by a physician who is not its author (msg 36, alternative 39a). The reviewer sees the DRAFT reports of other authors (msg 30), of any patient. Which drafts are ready to be reviewed is not recorded: the team may add a status READY (a question for Requirements).
- **Rationale:** #193 says that "review" is a second opinion by another physician, but not who chooses the reviewer.

### D66 – `Addendum`
- **Status:** To confirm (open question of #193 §10)
- **Decision:** New entity `Addendum` (`addendumId`, `date`, `text`), `*` — `1` `DiagnosticReport` and `*` — `1` `UserAccount` (its author). An addendum is only added to a VALIDATED report (alternative 48a), by a physician to whom the patient is assigned (msg 44). It is not edited once added.
- **Rationale:** #199 says that a validated report is completed with addenda and its text does not change.

### D67 – Medical images through the RIS/PACS gateway
- **Status:** To confirm (open question of #193 §10; uses #33 and AN-C2-C3 D23)
- **Decision:** UC09 reads the study of the procedure through the existing boundary `IU_HospitalSystem` (`getStudy(studyId)`, msgs 13–14, simulated, D-016). The report stores only the references of the images it links (`imageReferences`), never the images. The `studyId` of a procedure is its appointment (accession number) in the simulated RIS/PACS; the team may decide to record it in `ProcedureExecution` instead.
- **Rationale:** AN-C2-C3 D23 left `getStudy` to be analysed in C7; the images belong to RIS/PACS, not to our system.
