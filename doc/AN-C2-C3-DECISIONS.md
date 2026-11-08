# DECISIONS

Radiodiagnostics and Medical Imaging Management System (SESCAM)
Milestone 2 (Iterations 2 and 3) – Analysis discipline
- C2 Configuration and integrations ([#33](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/33)): UC17 ([#38](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/38)) and UC20 ([#41](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/41))
- C3 Patients ([#47](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/47)): UC12 register and modify ([#44](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/44))

Each entry records the context, the decision and the rationale. The numbering continues from [AN-IT1-DECISIONS.md](AN-IT1-DECISIONS.md), whose rules still apply (BCE pattern D07, naming D08, `AuditLog` written by every use case D10).
Status: **Agreed** = decided by the team · **To confirm** = proposed, pending team approval.

Artefacts of this iteration (PlantUML proposals, D-008; the official versions are in `doc/ISO-Project.vpp`):
- `doc/analysis/UC17-communication.puml`
- `doc/analysis/UC20-communication.puml`
- `doc/analysis/UC12-communication.puml`
- `doc/analysis/M2-analysis-class-diagram.puml`

---

## A. Scope of Milestone 2

### D11 – The Iteration 1 diagrams of UC17 and UC20 are updated, not redrawn
- **Status:** Agreed
- **Context:** When Iteration 1 still covered every priority 1 use case (D-007), communication diagrams were made for "Configure system parameters" and "Configure and manage integration with other hospital systems". D-012 later moved both use cases to Iteration 2 (C2), and D-011 says the work already done is reused.
- **Decision:** The two existing communication diagrams in `doc/ISO-Project.vpp` are updated to the current specification (#39, #40, #42, #43), and the C2 classes are added to the existing "Analysis Class Diagram".
- **Rationale:** It keeps the model in one place and keeps the work already done.

### D12 – D02 is replaced: the examination catalogue is part of UC17
- **Status:** Agreed
- **Context:** D02 (Iteration 1) split "Test types" out of "Configure system parameters". Since then, D-003 and the UC17 issues (#38–#40) include the examination catalogue in UC17 ("Configure system parameters and exam catalogue").
- **Decision:** D02 no longer applies. UC17 covers five groups of parameters: test types (examination catalogue), protocols, schedules, appointment policies and priorities.
- **Rationale:** The analysis follows the requirements agreed by the team.

### D13 – Loading the seed data is not part of the communication diagram
- **Status:** To confirm
- **Context:** #40 says that when the system starts with no data, the server loads the seed data, including the examination catalogue (D-003).
- **Decision:** It is not modelled in the UC17 communication diagram, which shows the interaction of the system administrator. It is left to design and implementation.
- **Rationale:** No actor starts it and it has no screen: it is a start-up behaviour of the server, not a step of the use case.

### D24 – Milestone 2 covers C2 and C3; C4 is postponed
- **Status:** Agreed ([Meeting 6](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-6), 08/11/2026)
- **Context:** Milestone 2 was planned with C2, C3 and C4 ("Components 2, 3 and 4"). In Meeting 6 the team decided to deliver only C2 and C3 in this milestone.
- **Decision:** This document covers the analysis of UC17, UC20 and UC12 (register and modify). C4 (UC06, UC07) is analysed when the team plans it again. The discharge of UC12 (Iteration 8, D-024) is not analysed either.
- **Rationale:** D-011: each iteration only models its own use cases.

---

## B. Use case flows

The messages of each communication diagram are traced to these steps (AN-IT1 D06).

### UC17 – Configure system parameters and exam catalogue
**Main flow**
1. The administrator selects a group of parameters (test types, protocols, schedules, appointment policies or priorities). → msgs 1–2
2. The system checks that the administrator has the permission (UC18). → msg 3
3. The system shows the entries of the group, with the deactivated ones marked as inactive. → msgs 4–5
4. The administrator creates, edits or deactivates an entry. → msgs 6–7
5. The system checks the permission again, validates the data and saves the change. → msgs 8–10
6. The system registers the change in the audit log (UC19) and shows a confirmation. → msgs 11–12

**Alternative flows**
- 2a / 5a. The administrator has no permission: access is denied and nothing is done.
- 5b. A field is missing or invalid (missing required field, duplicated identifier, non-positive duration): the errors are shown and nothing is saved.
- 4a. The entry is referenced by other data: it can only be deactivated, never deleted (D-022).

### UC20 – Configure integrations with other hospital systems
**Main flow (configure)**
1. The administrator opens the integrations screen. → msgs 1–2
2. The system checks that the administrator has the permission (UC18). → msg 3
3. The system lists the four integrations (SSO, electronic health record, admissions, RIS/PACS) with their status and mode, and the simulated ones are clearly marked. → msgs 4–5
4. The administrator edits the settings of an integration, enables or disables it, or switches its mode. → msgs 6–7
5. The system checks the permission again, validates the settings and saves them. → msgs 8–10
6. The system registers the change in the audit log (UC19) and shows a confirmation. → msgs 11–12

**Main flow (connection test)**
7. The administrator runs a connection test on an integration. → msgs 13–14
8. The system reads its configuration and tests the connection through the interface of that external system. → msgs 15–17
9. The system shows the result: OK, or error with its reason. The stored configuration does not change. → msg 18

**Alternative flows**
- 2a / 5a. The administrator has no permission: access is denied.
- 5b. The settings are invalid: the errors are shown and the integration keeps its previous settings.
- 8a. The external system does not answer within the time limit: the test returns an error with its reason, and the configuration does not change.
- 8b. The integration is in simulated mode: the interface answers without contacting the real system.

### UC12 – Manage patient demographic data (register and modify)
**Main flow**
1. The admissions staff member searches for a patient. → msgs 1–2
2. The system checks the permission (UC18), finds the patients, registers the access in the audit log (UC19) and shows them. → msgs 3–6
3. The staff member enters the data of a new patient, or modifies the data of an existing one. → msgs 7–8
4. The system checks the permission again and validates the data. → msgs 9–10
5. For a new patient, the system checks that no patient has the same identifier and registers the patient; for an existing one, it saves the changes. → msgs 11–12a / 12b
6. The system registers the change in the audit log and shows a confirmation. → msgs 13–15

**Alternative flows**
- 2a / 4a. No permission: access is denied.
- 4b. A required field is missing or invalid: the errors are shown and nothing is saved.
- 5a. A patient with the same identifier already exists: the existing record is shown and no duplicate is created.

---

## C. Analysis model

### D14 – `SystemParameter` covers the five parameter groups, without subclasses
- **Status:** Agreed
- **Context:** In Iteration 1, UC17 used a single entity `SystemParameter`. UC17 now covers five groups (test types, protocols, schedules, appointment policies and priorities), and test types have many attributes of their own (modality, room, duration, preparation, contrast and consent requirements, safety information, priority and follow-up eligibility). Subclasses for each group were considered.
- **Decision:** `SystemParameter` stays as a single entity, with the attribute `group` to say which group an entry belongs to, plus `id`, `name` and `active`. No subclasses (`TestType`, `Protocol`, `Schedule`, `AppointmentPolicy`, `Priority`) are added to the analysis class diagram in this milestone. The communication diagram of UC17 talks to `SystemParameter` for any group.
- **Alternatives:** An abstract `SystemParameter` with one subclass per group; one control and one diagram per group.
- **Rationale:** No use case of this milestone uses a specific group on its own: the five groups follow the same flow (list, create, edit, deactivate). The subclasses are added when a use case needs them, for example C4 (orders use test types and priorities) or C5 (appointments use schedules and policies).

### D15 – Deactivate instead of delete
- **Status:** Agreed (D-022)
- **Decision:** `SystemParameter` has the attribute `active`. The operations of UC17 are create, edit and deactivate; there is no delete. Deactivated entries are shown as inactive.
- **Rationale:** Orders and appointments of later components (C4, C5) keep a valid reference to the test types.

### D16 – The permission check is shown explicitly
- **Status:** To confirm
- **Context:** #39, #40, #42 and #43 say that a user without permission is denied access and that every change is checked against the permissions (UC18). The Iteration 1 diagrams did not show this check.
- **Decision:** The control asks `UserAccount` with `hasPermission(user, permission)` when the screen opens and before saving a change. The permissions are `CONFIGURE_SYSTEM` (UC17), `CONFIGURE_INTEGRATIONS` (UC20) and `MANAGE_PATIENTS` (UC12). The C1 entities `UserAccount`, `Role` and `Permission` are shown in grey in the class diagram, only for context.
- **Rationale:** The check is part of the acceptance criteria, so it must be traceable in the analysis. The names of the permissions are to be confirmed by the designers of C1.

### D17 – The message to the audit log follows the C1 contract
- **Status:** To confirm
- **Context:** In Iteration 1 the diagrams used `registerChanges(admin, ...)`. The C1 contract (#8) is `register(action)`.
- **Decision:** C2 uses `register(user, action, details)`, with the actions `PARAMETER_CHANGED` and `INTEGRATION_CHANGED`.
- **Rationale:** The same name as the contract makes the analysis easier to follow in design.

### D18 – The connection test is a separate action that never changes the configuration
- **Status:** Agreed (contract of #33)
- **Context:** In the Iteration 1 diagram the connection was tested while saving the settings. The contract of #33 says that `testConnection` never modifies the stored settings, and #42 says that if the test fails the integration keeps its previous settings.
- **Decision:** Saving (msgs 6–12) and testing (msgs 13–18) are two separate flows. The test reads the configuration, calls `IU_HospitalSystem` and shows the result; it does not write to `IntegrationConfiguration` or to `AuditLog`.
- **Rationale:** It follows the contract and avoids a failed test leaving half-saved settings.

### D19 – Mode and enabled status of each integration
- **Status:** Agreed (#42, #43)
- **Decision:** `IntegrationConfiguration` has the attributes `system`, `endpoint`, `settings`, `enabled`, `mode` (`SIMULATED` by default, or `REAL`) and `status`.
- **Rationale:** The screen must show the status and the mode, and the rest of the system must work without the real systems.

### D20 – SSO is one more hospital system in UC20
- **Status:** To confirm
- **Context:** D04 made SSO a specialization of **Authenticator System**, and EHR, Admissions System and RIS/PACS specializations of **Hospital System**. UC20 configures the four of them from the same screen (#42, #43). However, the use case diagram *Component_2* in `doc/ISO-Project.vpp` only links **Authenticator System** (with **SSO**) to "Configure and manage integration with other hospital systems (Server)": the actor **Hospital System** and its specializations are missing.
- **Decision:** The UC20 communication diagram uses the generic actor **Hospital System** and the boundary `IU_HospitalSystem` for the four systems, SSO included. It is proposed that the requirements team adds to *Component_2* (Server) the actor **Hospital System**, with the specializations **EHR**, **Admissions System**, **RIS/PACS** and **Authenticator System** (which keeps **SSO** below it), associated with the server use case of UC20.
- **Rationale:** The actors of the analysis must exist in the use case diagram, and the specification of UC20 covers four external systems, not only SSO. One flow and one boundary for the four integrations, with no duplicated diagram for SSO.

### D21 – Names of the boundaries with external systems
- **Status:** To confirm
- **Context:** D08 says `IF_<ExternalSystem>` for boundaries with external systems, but the model of Iteration 1 uses `IU_AuthenticatorSystem` and `IU_HospitalSystem`.
- **Decision:** C2 keeps `IU_HospitalSystem`, the name already in the model. If the team prefers D08, both boundaries are renamed to `IF_AuthenticatorSystem` and `IF_HospitalSystem` at the same time.
- **Rationale:** Consistency between iterations; one rename in one place if it is changed.

### D22 – One communication diagram per use case covers its client and server parts
- **Status:** Agreed (same approach as Iteration 1)
- **Context:** The use case diagram *Component_2* splits each use case into a client and a server use case (D-015), linked through the actors **Client** and **Server**.
- **Decision:** Each use case keeps a single communication diagram. The boundary `IU_<UseCase>` is the client part; the control, the entities and the boundaries with external systems are the server part. The messages from the boundary to the control (for example `saveEntry`, `saveIntegration`) are the interaction between the **Client** and **Server** actors of the use case diagram. Splitting the classes into the `client`, `server` and `common` modules is left to design.
- **Rationale:** The analysis model is independent of the technology and the deployment; the same diagram is traced to both sub-issues (#39/#40 and #42/#43).

### D23 – The gateway interfaces of #33 are not analysed in UC20
- **Status:** To confirm
- **Context:** The contracts of #33 include `getPatientRecord`, `getAdmission` and `getStudy`, which give access to the external systems.
- **Decision:** They are not part of the UC20 flow. They are analysed in the use cases that use them (C3, C7).
- **Rationale:** AGENTS.md and D-011: each iteration only models its own use cases.

### D25 – `Patient` entity (C3) and its link with the audit log
- **Status:** To confirm
- **Context:** AN-IT1 D03 stored the accessed patient as an attribute of `AuditLog` until `Patient` existed. UC12 now creates it.
- **Decision:** `Patient` has `patientId`, `identifier`, `fullName`, `dateOfBirth`, `sex`, `address`, `phone`, `email` and `active` (#47). The type of `identifier` (national ID or health card number) is still open (D-024). As D03 planned, `AuditLog` gets an association with `Patient` (`*` to `0..1`): a record may refer to a patient or not.
- **Rationale:** D03 said the attribute becomes an association when `Patient` exists; the audit of access to clinical data (UC19) can then be traced to the patient.

### D26 – Every access to patient data is registered
- **Status:** Agreed (#46, #47)
- **Context:** The C3 contract (#47) says that every access to patient data is registered in the audit log (UC19), not only the changes.
- **Decision:** UC12 registers the access when it shows the patients found (msg 5, `PATIENT_ACCESSED`), as well as every registration and modification (msg 13). When later components read patients through `getPatient(patientId)`, C3 registers that access too, so they do not need to repeat it (D-023).
- **Rationale:** Each component owns its data (D-023), so the component that gives access to it is the one that registers the access.
