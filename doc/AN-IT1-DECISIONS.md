# DECISIONS

Radiodiagnostics and Medical Imaging Management System (SESCAM)
Iteration 1 – Analysis discipline

Each entry records the context, the decision and the rationale.
Status: **Agreed** = decided by the team · **To confirm** = proposed, pending team approval.

---

## A. Scope of iteration 1

### D01 – "Recover password" is not analysed in this iteration
- **Status:** Agreed
- **Context:** "Recover password" extends "Log in", but it has no specification of its own. RF22 only mentions it in the alternative flow.
- **Decision:** No communication diagram is made for "Recover password" in iteration 1.
- **Rationale:** Authentication is delegated to the corporate SSO, so password recovery belongs to the external system. Our system would only forward the request. It can be analysed in a later iteration if needed.

### D02 – "Test types" is split from "Configure system parameters"
- **Status:** Agreed
- **Context:** RF17 includes test types, but our notes mark it as a split candidate. Test types form the examination catalogue, with many attributes (modality, room, duration, preparation, contrast and consent, safety, priority, follow-up eligibility).
- **Decision:** "Configure system parameters" covers only protocols, schedules, appointment policies and priorities. The examination catalogue will be its own use case in a later iteration.
- **Rationale:** Its complexity justifies a separate use case, and keeping it out of iteration 1 avoids over-engineering.

### D03 – No `Patient` entity in iteration 1
- **Status:** To confirm
- **Context:** RF19 audits access to patients' clinical data, but patients are managed in a later iteration (RF12).
- **Decision:** `AuditLog` stores the accessed resource or patient as an attribute. It becomes an association when `Patient` exists.
- **Rationale:** Iteration 1 only models the elements its use cases need.

---

## B. Use case model

### D04 – External hospital systems are modelled as actors
- **Status:** Agreed
- **Context:** RF20 notes: "the external systems are actors too. The kickoff slides say these can be simulated behind interfaces." The use case "Configure and manage integration with other hospital systems" had no external actors.
- **Decision:** Add a generic actor **Hospital System**, associated with that use case, with the specializations **EHR**, **Admissions System** and **RIS/PACS**. SSO stays as a specialization of **Authenticator System**.
- **Rationale:** This follows the same pattern already used for Authenticator System ← SSO, and it allows any external system to be simulated behind a common interface.

### D05 – "Sign up" is removed from the "Log in" alternative flow
- **Status:** To confirm
- **Context:** The "Log in" specification says the user "can either sign up or try to recover the password", but there is no "Sign up" use case and no requirement covers self-registration.
- **Decision:** Remove "sign up" from the specification.
- **Rationale:** Professionals access through the corporate SSO, and patients are registered by admissions staff (RF12). Self-registration contradicts both.

### D06 – Main flows written for the use cases of iteration 1
- **Status:** To confirm
- **Context:** The requirements list (functional-requirements.xlsx) describes what each requirement does, but not step by step.
- **Decision:** Each use case analysed in iteration 1 gets a short main flow (and alternative flows where relevant), with a reference to the requirement it realizes (RF17, RF18, RF19, RF20, RF22).
- **Rationale:** Each message in the communication diagrams must be traceable to a step of its use case flow.

---

## C. Analysis model

### D07 – Boundary–Control–Entity (BCE) pattern
- **Status:** Agreed
- **Decision:** Each use case is realized with one boundary per actor, one control class, and the entities it uses. Communication rules: actor ↔ boundary ↔ control ↔ entity. Boundaries never talk to entities directly.
- **Rationale:** This is the standard analysis approach of the Unified Process.

### D08 – Naming conventions
- **Status:** Agreed
- **Decision:**
  - The whole model is written in English.
  - `IU_<UseCase>` for user interfaces, `IF_<ExternalSystem>` for boundaries with external systems, and `Control_<UseCase>` for control classes.
  - The user entity is called `UserAccount`, so it doesn't share a name with the `User` actor.
  - The audit entity is called `AuditLog` ("auditory" means related to hearing).

### D09 – No `Session` entity
- **Status:** Agreed
- **Context:** A `Session` entity was considered for "Log in".
- **Decision:** It is not included in the analysis model.
- **Rationale:** The specification doesn't mention it, and a session is a technical detail (token or cookie) that belongs to design.

### D10 – `AuditLog` is written by every use case
- **Status:** Agreed
- **Context:** RF19 notes: "Implies the system records every access automatically."
- **Decision:** Every control class records its relevant actions (access, changes) in `AuditLog`. "Audit system activity…" only reads it.
- **Rationale:** Without recorded activity, there would be nothing to audit.

### D11 – `AuditLog` is append-only
- **Status:** To confirm
- **Decision:** Audit records are only inserted. They are never modified or deleted.
- **Rationale:** This ensures the audit trail is reliable, which is essential when dealing with clinical data.

### D12 – Consulting the audit log is itself audited
- **Status:** To confirm (choose one option)
- **Context:** Audit records contain information about access to clinical data.
- **Options:** (a) "Audit" records its own consultation in `AuditLog`, for consistency with the other use cases. (b) It doesn't, because it is read-only.

### D13 – Permissions are obtained from the role in "Log in"
- **Status:** Agreed
- **Decision:** In "Log in", the message `obtainRole()` returns the user's role together with its permissions. There is no separate message to `Permission`.
- **Rationale:** RF22 only needs to apply role permissions, not manage them.

### D14 – `Permission` is a separate entity
- **Status:** Agreed
- **Context:** RF18: "Assign roles to users and grant or revoke permissions."
- **Decision:** `Permission` is an entity of its own, associated with `Role` (many-to-many). `UserAccount` and `Role` are also associated many-to-many.
- **Rationale:** Permissions are granted and revoked one by one, so they have their own identity.

### D15 – A single communication diagram for "Manage roles and permissions"
- **Status:** Agreed
- **Context:** The use case includes two operations: assigning roles to users, and granting or revoking a role's permissions.
- **Decision:** One communication diagram with guard conditions. Messages "a" are `[operation = assignRole]` and messages "b" are `[operation = managePermissions]`.
- **Rationale:** It keeps one diagram per use case, consistent with a single specification and without duplicating elements.

### D16 – Generic `SystemParameter` entity
- **Status:** Agreed
- **Context:** "Configure system parameters" covers protocols, schedules, appointment policies and priorities (see D02).
- **Decision:** A single generic entity `SystemParameter` (category, name, value), instead of one entity per parameter type.
- **Rationale:** It avoids over-engineering in iteration 1. Specific entities (for example, `Priority`) can be extracted in later iterations if other use cases need them.

### D17 – Common boundary for external hospital systems
- **Status:** Agreed
- **Decision:** "Configure and manage integration…" uses a single boundary, `IF_HospitalSystem`, and the entity `IntegrationConfiguration` (system, protocol, endpoint, status).
- **Rationale:** This is consistent with D04. External systems can be simulated behind this boundary, as the kickoff slides allow.

### D18 – Simplified analysis class diagram
- **Status:** Agreed
- **Decision:** The analysis class diagram shows only the classes with their BCE stereotype icons and the associations between them, following the examples given in class. No attributes, operations or multiplicities are included.
- **Rationale:** Class behaviour is already described in the communication diagrams. Attributes, typed operations and multiplicities belong to design.

### D19 – One analysis class diagram per use case or one global diagram
- **Status:** To confirm (choose one option)
- **Options:** (a) One diagram per use case, as the class example seems to show. (b) One global diagram for iteration 1, where shared classes (`UserAccount`, `Role`, `AuditLog`) appear only once.
