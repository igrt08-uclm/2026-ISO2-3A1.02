# DECISIONS

Radiodiagnostics and Medical Imaging Management System (SESCAM)
Iteration 5 (Milestone 3) – Analysis discipline
- C5 Appointment planning ([#138](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/138)): UC13 ([#129](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/129)), UC15 ([#132](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/132)) and UC14 ([#135](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/135))

Each entry records the context, the decision and the rationale. The numbering continues from [AN-C4-DECISIONS.md](AN-C4-DECISIONS.md); the rules of [AN-IT1-DECISIONS.md](AN-IT1-DECISIONS.md) and [AN-C2-C3-DECISIONS.md](AN-C2-C3-DECISIONS.md) still apply (BCE pattern D07, naming D08, `AuditLog` written by every use case D10, permission check shown explicitly D16, one communication diagram per use case for its client and server parts D22). The decisions of the team are written `D-0xx`; the decisions of the analysis, `Dxx`.
Status: **Agreed** = decided by the team · **To confirm** = proposed, pending team approval.

Artefacts of this iteration: the communication diagrams of UC13, UC15 and UC14, in `doc/ISO-Project.vpp` (Visual Paradigm 18.1, the official version, D-008). The PlantUML proposals are not kept in the repository.

---

## A. Scope of Iteration 5

### D35 – Iteration 5 covers UC13, UC15 and UC14
- **Status:** Agreed ([Meeting 8](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-8))
- **Context:** C5 is done in Milestone 3 together with C4 ([AN-C4-DECISIONS.md](AN-C4-DECISIONS.md)), due on 22/11/2026. #138 says that UC14 is the first candidate to move if C5 does not fit.
- **Decision:** This document covers the analysis of UC13, UC15 and UC14, in the order in which they are developed (#138).
- **Rationale:** D-011: each iteration only models its own use cases. The analysis of UC14 is ready for Design if it stays in the milestone.

---

## B. Use case flows

The messages of each communication diagram are traced to these steps (AN-IT1 D06).

### UC13 – Manage the appointment schedule
**Main flow (book)**
1. The admissions staff member opens the appointments screen. → msgs 1–2
2. The system checks the permission (UC18) and shows the pending scheduling needs (patient, test type, priority, date of the order). → msgs 3–5
3. The staff member selects a need. → msgs 6–7
4. The system computes the free slots of the room where that test type is done, from the schedule of the room, the duration of the test type and the booked appointments, and shows them. → msgs 8–11
5. The staff member books a slot. → msgs 12–13
6. The system checks the permission again, that the patient is active and that the slot is free, creates the appointment and marks the need as booked in C4. → msgs 14–18
7. The system registers the booking in the audit log and shows a confirmation. → msgs 19–20

**Main flow (reschedule)**
8. The staff member moves an appointment to another free slot. → msgs 21–22
9. The system checks the permission and that the new slot is free, moves the appointment and frees the old slot. → msgs 23–25
10. The system registers the change and shows a confirmation. → msgs 26–27

**Main flow (cancel)**
11. The staff member cancels an appointment, after a confirmation. → msgs 28–29
12. The system checks the permission, cancels the appointment (status change, the slot is freed) and gives the need back as pending (D41). → msgs 30–32
13. The system registers the cancellation and shows a confirmation. → msgs 33–34

**Alternative flows**
- 2a / 6a / 9a / 12a. No permission: the attempt is registered in the audit log and access is denied.
- 6b / 9b. The slot is taken, outside the schedule of the room or in the past, or the patient is not active: the reason is shown and nothing is booked.
- 6c. A mandatory field is missing: the error is shown and nothing is booked.

### UC15 – Manage waitlists, triage priority tiers and calendar openings
**Main flow (waitlist)**
1. The admissions staff member selects a test type. → msgs 1–2
2. The system checks the permission (UC18) and shows its waitlist, ordered by priority tier and date, and its calendar openings. → msgs 3–6
3. The staff member adds a pending need to the waitlist or changes its tier. → msgs 7–8
4. The system checks the permission and that the tier is a configured priority, saves the entry and registers the change. → msgs 9–13

**Main flow (fill an opening)**
5. The staff member selects a calendar opening, and the system proposes the first suitable patient of the waitlist. → msgs 14–17
6. The staff member confirms, and the system books the appointment through UC13 (include), closes the entry and registers it. → msgs 18–24

**Alternative flows**
- 2a / 4a / 6a. No permission: the attempt is registered in the audit log and access is denied.
- 2b. The waitlist or the openings are empty: the screen says so.
- 4b. The tier is not a configured priority: the reason is shown.
- 6b. The opening is no longer free: UC13 rejects the booking and the reason is shown.
- 3a. Removing a patient from the waitlist asks for confirmation and changes the status of the entry; it is not deleted (D-022).

### UC14 – Prepare the daily schedule and manage the worklists
**Main flow**
1. The admissions staff member chooses a day. → msgs 1–2
2. The system checks the permission (UC18) and reads the booked appointments of that day, with the patient and the test type of each one. → msgs 3–6
3. The system registers the access in the audit log and shows the schedule per room, in time order, with the patient, the test type and its preparation. → msgs 7–8
4. The staff member chooses a room, and the system builds its worklist for that day the same way. → msgs 9–14

**Alternative flows**
- 2a / 4a. No permission: the attempt is registered in the audit log and access is denied.
- 2b. The date is not valid: the reason is shown.
- 2c. There are no appointments that day: the screen says so. Cancelled appointments are never shown.

---

## C. Analysis model

### D36 – `Schedule` becomes a subclass of `SystemParameter`
- **Status:** Agreed (extends AN-C2-C3 D14 and AN-C4 D28)
- **Decision:** `Schedule` (room, available slots) becomes a subclass of `SystemParameter`, because UC13 uses the schedule of each room to compute the free slots. C5 also uses `TestType` and `Priority`, added in AN-C4 D28. `Protocol` and `AppointmentPolicy` are still not added: no use case of C5 uses them.
- **Rationale:** The analysis model only shows the classes that a use case needs.

### D37 – Permission of C5
- **Status:** To confirm (patch of C1, D-036; owner of C1: Ignacio)
- **Context:** #138 says that every operation of C5 requires the permission of the admissions staff, and that a new permission is a patch of C1.
- **Decision:** `MANAGE_APPOINTMENTS`, given to the Admissions staff, for UC13, UC15 and UC14.
- **Rationale:** One permission per kind of work, as in C2, C3 and C4 (AN-C4 D29).

### D38 – Failed permission checks are registered
- **Status:** Agreed (#131, #134, #137)
- **Decision:** In UC13, UC15 and UC14, when the permission check fails, the control registers the attempt in the audit log (`ACCESS_DENIED`) before denying access.
- **Rationale:** It is an alternative scenario of every server sub-issue of C5.

### D39 – C5 uses the entities of C2, C3 and C4 directly in the analysis
- **Status:** Agreed (D-023, AN-C2-C3 D26, AN-C4 D34)
- **Decision:** In the analysis, the controls of C5 talk directly to `Patient` (C3), `TestType`, `Priority` and `Schedule` (C2) and `SchedulingNeed` (C4). In design these accesses become the interfaces of each component (`getPatient`, `getTestType`, `getPendingSchedulingNeeds`, `markSchedulingNeedBooked`…). The access to patient data through `getPatient` is registered by C3 (D26), so it is not repeated in these diagrams.
- **Rationale:** The analysis model shows responsibilities, not modules.

### D40 – `Appointment`, and free slots computed
- **Status:** To confirm
- **Decision:** New entity `Appointment` (`appointmentId`, `room`, `start`, `end`, `status`: BOOKED, RESCHEDULED or CANCELLED), linked to the `SchedulingNeed` it books. The free slots are not an entity: `Control_ManageAppointments` computes them from the `Schedule` of the room, the duration of the `TestType` and the booked appointments. The room is an attribute, without a `Room` entity.
- **Alternatives:** A `Slot` entity stored for every room and time; a `Room` entity.
- **Rationale:** #131 says the server "builds the free slots" from the schedules and the durations, so they are derived data. No use case manages rooms on their own.

### D41 – Cancelling an appointment gives the need back as pending
- **Status:** To confirm (question for Requirements)
- **Context:** #130 and #131 say that cancelling changes the status of the appointment and frees the slot, but not what happens to the scheduling need that it booked.
- **Decision:** It is proposed that the need goes back to PENDING (msg 32 of UC13), so that it can be booked again or put on the waitlist (UC15).
- **Rationale:** Otherwise the procedure ordered by the physician would be lost with the cancellation.

### D42 – `WaitlistEntry`, calendar openings computed and the include of UC13
- **Status:** To confirm
- **Decision:** New entity `WaitlistEntry` (`entryId`, `date`, `status`: WAITING, BOOKED or REMOVED), linked to one `SchedulingNeed` and to its tier (`Priority`). The calendar openings are not an entity: they are the free future slots, which `Control_ManageWaitlist` asks to `Control_ManageAppointments`. Filling an opening books the appointment through `Control_ManageAppointments` (control to control), which is the `<<include>>` of UC15 (Server) to UC13 (Server) in *Component_5*.
- **Rationale:** The booking rules (free slot, active patient, audit) are written once, in UC13.

### D43 – The daily schedule and the worklists are queries
- **Status:** To confirm
- **Decision:** UC14 has no `Worklist` or `DailySchedule` entity: `Control_DailySchedule` builds them from the booked appointments of a day, ordered by time, and registers every access in the audit log (#137).
- **Rationale:** #137 says they are built from the appointments; nothing new is stored.

### D44 – Worklists per room; per physician is a question for Requirements
- **Status:** To confirm (question for Requirements)
- **Context:** #135 and #136 speak of worklists "per room or per physician". An appointment does not record which physician performs the procedure; the order records the physician who asked for it (AN-C4 D31).
- **Decision:** UC14 is analysed with worklists per room. It is asked to Requirements which physician they mean: if it is the one who asked for the order, the worklist can be built from AN-C4 D31; if it is the one who performs the procedure, `Appointment` needs that data.
- **Rationale:** The analysis cannot use data that no use case records.
