# Components, iterations and dependencies

Data that the AI agents need and that is not written anywhere else in the repository. It comes from the team's
components spreadsheet and the plan approved in Meeting 4. If this file and the wiki disagree, **the wiki wins**
(*Traceability and releases* for dates and versions, *Requirements and Use Cases* for the requirements), and this
file must be corrected. The rules for agents are in [AGENTS.md](../AGENTS.md).

## Components and iterations (D-012, D-013)
| Component | Use cases | Iteration | Product version |
|---|---|---|---|
| C1 Access and security | UC22, UC18, UC19 | It1 | v0.1.0 |
| C2 Configuration and integrations | UC17, UC20 | It2 | v0.2.0 |
| C3 Patients | UC12 register and modify | It3 | v0.3.0 |
| C4 Clinical orders | UC06, UC07 | It4 | v0.4.0 |
| C5 Appointment planning | UC13, UC15, UC14 | It5 | v0.5.0 |
| C6 Patient portal (part 1) | UC01, UC04 | It6 | v0.6.0 |
| C7 Execution and reports | UC10, UC09 | It7 | v0.7.0 |
| C6 (part 2) and C3 discharge | UC03, UC05, UC12 discharge | It8 | v0.8.0 |
| C8 Notifications and C4 tracking | UC02, UC08 | It9 | v0.9.0 |
| C9 Statistics | UC11, UC16, UC21 | It10 | v0.10.0 |
| Integration, deployment and user manual | — | It11 (Transition) | v1.0.0 |

It0 (Inception) was the project plan.

## Module versions (D-014)
- `server` and `client` versions are **cumulative for the whole project**: they start at `1.0.0` with the first use
  case and MINOR goes up with each use case delivered (23 deliveries, because UC12 is delivered twice), so the
  planned sequence runs from `1.0.0` (It1) to `1.22.0` (It10).
- The version of each use case is written in the title of its issue, e.g. `UC17 – Server (module v1.3.0)`.
- Inside an iteration, a use case that depends on another one takes the higher version. C1 is planned as UC22
  `1.0.0`, UC19 `1.1.0`, UC18 `1.2.0`.

## Dependencies between use cases
Common to all use cases: UC22 (identified user) and UC18 (permissions). Use cases that access clinical data also
send their events to the audit log of UC19 (an acceptance criterion of each of them).

| Use case | Depends on |
|---|---|
| UC17, UC20 | UC18 |
| UC12 (register, modify) | only the common ones |
| UC12 (discharge) | UC13 (cancels the patient's appointments) |
| UC06 | UC12, UC17 |
| UC07 | UC06 |
| UC08 | UC06, UC13, UC14 |
| UC13 | UC06, UC07, UC12, UC17 |
| UC15 | UC13, UC12 |
| UC14 | UC13 |
| UC01 | UC13 |
| UC04 | UC12 |
| UC10 | UC13, UC14, UC17 |
| UC09 | UC10, UC12 |
| UC03 | UC09, UC10 |
| UC05 | UC07, UC17 |
| UC02 | UC13, UC09, UC17 (actor: Timer/System) |
| UC11 | UC06, UC13, UC10 |
| UC16 | UC13, UC15 |
| UC21 | UC11, UC16, UC19 and the data of all components |

**Apparent cycles, solved by delivering a part later** (this is why some use cases move to It8 and It9):
- C4 ↔ C5: UC13 books the orders of UC06/UC07, and UC08 shows the schedules of UC13/UC14 → UC08 goes in It9.
- C3 ↔ C5: UC13 needs registered patients, and the discharge cancels appointments → the discharge goes in It8.
- C6 ↔ C7: UC03 shows reports and images of UC09/UC10 → UC03 goes in It8.

UC22 does not depend on UC20: the login uses the simulated SSO, and UC20 later connects the real one.

## Rules already agreed on the data
- Nothing is deleted: test types, patients, orders and appointments are deactivated or cancelled (status change),
  so that the audit, the history and the statistics keep their references.
- The examination catalogue (FR17) is loaded as seed data (D-003). Its fields: modality, room, duration,
  preparation, contrast and consent requirements, safety information, priority and follow-up eligibility.
- A patient's reschedule is a request; the admissions staff do the rebooking (UC13).
- Assumption to be confirmed by the team (#33): the clinical guidelines that patients consult in UC05 are loaded
  by the administrator through UC17.

## Team
| Member | Role |
|---|---|
| Ignacio Romero Torres | Coordinator · Requirements |
| David Calzado Olmo | Requirements · Minutes |
| Rodrigo Muñoz Martín | Analysis |
| Marcos Loro Carrasco | Design |
| Denis-Alexandru Ionescu Telibasa | Design |
| Javier Villanueva Crespo | Implementation |
| Adam Gouram | Testing |
