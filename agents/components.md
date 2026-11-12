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

## Component names and versions (D-014, D-032)
Each component is a Maven submodule on each side, named `cN-<name>`: `server/cN-<name>` and `client/cN-<name>`
(D-032). Each component has **its own version**: it starts at `1.0.0` with its first use case, MINOR goes up with
each new use case of that component and PATCH with each fix. The server and the client side of a component may
differ if a use case changes only one of them. `common` keeps one package per component and its own version.

| Component | Submodule folder | Use case → component version |
|---|---|---|
| C1 Access and security | `c1-access` | UC22 → 1.0.0, UC19 → 1.1.0, UC18 → 1.2.0 |
| C2 Configuration and integrations | `c2-configuration` | UC17 → 1.0.0, UC20 → 1.1.0 |
| C3 Patients | `c3-patients` | UC12 register and modify → 1.0.0, UC12 discharge → 1.1.0 (It8) |
| C4 Clinical orders | `c4-orders` | UC06 → 1.0.0, UC07 → 1.1.0, UC08 → 1.2.0 (It9) |
| C5 Appointment planning | `c5-appointments` | UC13 → 1.0.0, UC15 → 1.1.0, UC14 → 1.2.0 |
| C6 Patient portal | `c6-portal` | UC01 → 1.0.0, UC04 → 1.1.0, UC03 → 1.2.0 and UC05 → 1.3.0 (It8) |
| C7 Execution and reports | `c7-reports` | UC10 → 1.0.0, UC09 → 1.1.0 |
| C8 Notifications | `c8-notifications` | UC02 → 1.0.0 |
| C9 Statistics | `c9-statistics` | UC11 → 1.0.0, UC16 → 1.1.0, UC21 → 1.2.0 |

- **Folder and artifactId.** The folder is the same on both sides (`server/c1-access`, `client/c1-access`), but
  Maven needs a different `artifactId` for each pom: `server-c1-access` and `client-c1-access`, and the same for
  the next components (`server-c2-configuration`, `client-c2-configuration`…) (#100, #101).
- **Always name the component with its version**, never a bare number: write `C2 1.1.0`, not `1.1.0` or
  `module v1.4.0`. Use it in issue titles (`UC20 – Server (C2 v1.1.0)`), plans, commit bodies, pull requests and
  release notes.
- Inside a component, a use case that depends on another one takes the higher version (D-021).
- Release v0.1.0 was built before D-032, with C1 as a package of `server` and `client` at `1.2.0`. Until C1 is
  moved to `server/c1-access` and `client/c1-access` (#93), those two poms carry the version of C1. The issue
  titles were changed to the component version on 11/11/2026 (#96); `(module v1.x.0)` in older commits or plans is
  the old cumulative numbering of D-014, and the table above replaces it.
- If a submodule name of the table is not the one used in the repository, the repository wins: tell the member and
  this file is corrected.

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
