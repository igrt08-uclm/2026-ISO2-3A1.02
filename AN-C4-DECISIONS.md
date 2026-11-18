# DECISIONS

Radiodiagnostics and Medical Imaging Management System (SESCAM)
Iteration 4 (Milestone 3) – Analysis discipline
- C4 Clinical orders ([#65](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/65)): UC06 ([#66](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/66)) and UC07 ([#69](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/69))

Each entry records the context, the decision and the rationale. The numbering continues from [AN-C2-C3-DECISIONS.md](AN-C2-C3-DECISIONS.md); the rules of [AN-IT1-DECISIONS.md](AN-IT1-DECISIONS.md) and [AN-C2-C3-DECISIONS.md](AN-C2-C3-DECISIONS.md) still apply (BCE pattern D07, naming D08, `AuditLog` written by every use case D10, permission check shown explicitly D16, one communication diagram per use case for its client and server parts D22). The decisions of the team are written `D-0xx`; the decisions of the analysis, `Dxx`.
Status: **Agreed** = decided by the team · **To confirm** = proposed, pending team approval.

Artefacts of this iteration (PlantUML proposals, D-008; the official versions are in `doc/ISO-Project.vpp`):
- `doc/analysis/UC06-communication.puml`
- `doc/analysis/UC07-communication.puml`

---

## A. Scope of Iteration 4

### D27 – Iteration 4 covers UC06 and UC07, without UC08
- **Status:** Agreed ([Meeting 8](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-8))
- **Context:** C4 was moved from Milestone 2 to Milestone 3 in Meeting 6 (AN-C2-C3 D24), together with C5, due on 22/11/2026. C5 has its own document, [AN-C5-DECISIONS.md](AN-C5-DECISIONS.md).
- **Decision:** This document covers the analysis of UC06 and UC07. UC08 belongs to Iteration 9 (D-013) and is not analysed.
- **Rationale:** D-011: each iteration only models its own use cases.

---

## B. Use case flows

The messages of each communication diagram are traced to these steps (AN-IT1 D06).

### UC06 – Order a diagnostic procedure
**Main flow**
1. The physician selects a patient. → msgs 1–2
2. The system checks the permission of the physician (UC18) and that the patient is active (UC12). → msgs 3–4
3. The system shows the order form with the active test types, the configured priorities and the orders already placed for the patient. → msgs 5–8
4. The physician chooses the test type and the priority, and writes the clinical indication and the justification. → msgs 9–10
5. The system checks the permission again and validates the order. → msgs 11–12
6. The system creates the order and a pending scheduling need for C5. → msgs 13–14
7. The system registers the order in the audit log and shows a confirmation. → msgs 15–16

**Alternative flows**
- 2a / 5a. No permission: the attempt is registered in the audit log and access is denied.
- 2b. The patient is not active: the screen explains why and does not allow ordering.
- 3a. Deactivated test types are not offered (D-022).
- 5b. A mandatory field is missing, or the patient, test type or priority is not valid: the reasons are shown and the order is not created.

### UC07 – Order periodic follow-up procedures
**Main flow**
1. The physician selects a patient. → msgs 1–2
2. The system checks the permission of the physician (UC18) and that the patient is active (UC12). → msgs 3–4
3. The system shows the form with the test types eligible for follow-up and the follow-up orders of the patient. → msgs 5–7
4. The physician chooses the test type, writes the clinical indication and the justification, and defines the recurrence interval. → msgs 8–9
5. The system checks the permission again and validates the order. → msgs 10–11
6. The system creates the follow-up order and the scheduling needs of the next 12 months (D-040). → msgs 12–13
7. The system registers the order in the audit log and shows a confirmation. → msgs 14–15

**Alternative flows**
- 2a / 5a. No permission: the attempt is registered in the audit log and access is denied.
- 2b. The patient is not active: the screen explains why and does not allow ordering.
- 5b. The recurrence interval is missing or not positive, or the test type is not eligible for follow-up: the reasons are shown and the order is not created.

---

## C. Analysis model

### D28 – `TestType` and `Priority` become subclasses of `SystemParameter`
- **Status:** Agreed (extends AN-C2-C3 D14)
- **Context:** D14 kept `SystemParameter` without subclasses until a use case needed a specific group. UC06 and UC07 use two of them.
- **Decision:** `TestType` (modality, room, duration, preparation, follow-up eligibility, active) and `Priority` (level) become subclasses of `SystemParameter`. The other groups are not added, because no use case of C4 uses them.
- **Rationale:** The analysis model only shows the classes that a use case needs.

### D29 – Permission of C4
- **Status:** To confirm (patch of C1, D-036; owner of C1: Ignacio)
- **Context:** The permissions that exist are `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` and `MANAGE_PATIENTS` (D-036). UC06 and UC07 need the permission of the physician role (#65).
- **Decision:** `ORDER_PROCEDURES`, given to the Physician, for UC06 and UC07.
- **Rationale:** One permission per kind of work, as in C2 and C3. Giving it is a patch of C1, asked in an issue and approved by its owner (D-036).

### D30 – Failed permission checks are registered
- **Status:** Agreed (#68, #71)
- **Decision:** In UC06 and UC07, when the permission check fails, the control registers the attempt in the audit log (`ACCESS_DENIED`) before denying access.
- **Rationale:** It is an alternative scenario of the server sub-issues of C4.

### D31 – `ClinicalOrder` is the abstract parent of the two kinds of order, and records who asked for it
- **Status:** To confirm
- **Decision:** `ClinicalOrder` (`orderId`, `indication`, `justification`, `creationDate`) is abstract, with `DiagnosticOrder` (associated with one `Priority`) and `FollowUpOrder` (`recurrenceInterval`). Every order is for one `Patient` and one `TestType`, generates one or more `SchedulingNeed`, and records the physician who asked for it (association `ClinicalOrder` `*` — `1` `UserAccount`), which UC06 (msg 13) and UC07 (msg 12) save when the order is created. Each use case keeps its own boundary and control, following D07.
- **Alternatives:** Two independent entities that repeat the common attributes; a single `Order` entity with an optional recurrence. For the physician: leaving it only in the audit log (UC19).
- **Rationale:** Both orders share most of their data and feed C5 in the same way. The physician who asked for the order is only known when it is created; UC08 (Iteration 9) shows each physician their **own** orders, and the worklists "per physician" of UC14 may need it (AN-C5 D44). It is not new functionality (D-039): it is the minimum data that FR08 needs, and the server already receives the `requester` of every operation (#138).

### D32 – Orders are not edited or cancelled
- **Status:** Agreed (D-039)
- **Decision:** UC06 and UC07 have no messages to edit or cancel an order.

### D33 – Scheduling needs
- **Status:** Agreed (D-040, contracts of #65)
- **Decision:** New entity `SchedulingNeed` (`needId`, `dueDate`, `status`: PENDING or BOOKED). A diagnostic order creates one; a follow-up order creates, when it is created and without a timer, the needs of the next 12 months (msg 13 of UC07). C5 (UC13) reads the pending ones (`getPendingSchedulingNeeds`) and marks them as booked (`markSchedulingNeedBooked`).
- **Rationale:** It is the link between the orders of C4 and the appointments of C5.

### D34 – C4 uses the entities of C2 and C3 directly in the analysis
- **Status:** Agreed (D-023, AN-C2-C3 D26)
- **Decision:** In the analysis, the controls of C4 talk directly to `Patient` (C3), `TestType` and `Priority` (C2). In design these accesses become the interfaces of each component (`getPatient`, `getTestType`). The access to patient data through `getPatient` is registered by C3 (D26), so it is not repeated in these diagrams.
- **Rationale:** The analysis model shows responsibilities, not modules.

