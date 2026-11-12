# Radiodiagnostics and Medical Imaging Management System

[![Build with Maven](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/actions/workflows/maven.yml/badge.svg?branch=develop)](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/actions/workflows/maven.yml)

Lab project of **Software Engineering II** (Computer Science, UCLM, 2026/27), team **3A1.02**.

A Java client–server application for the radiodiagnostics service of SESCAM. Patients, doctors, administrative staff and administrators use it for their appointments, clinical orders, reports and statistics. We develop it with the **Unified Process**: one iteration per component, and the five RADIT disciplines (Requirements, Analysis, Design, Implementation and Testing) in every iteration.

## Current state

| | |
|---|---|
| **Last release** | [v0.1.0](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/releases/tag/v0.1.0) (7 November 2026): C1 Access and security (UC22 log in, UC18 roles and permissions, UC19 audit log) |
| **In progress** | Iterations 2 and 3: C2 Configuration and integrations, and C3 Patients ([Milestone 2](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/milestones), 15 November) |
| **Delivery** | Version 1.0.0 on 15 December 2026 |

The full calendar, with the component, use cases, versions and date of every iteration, is on the wiki page [Traceability and releases](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Traceability-and-releases).

## Team

| Member | Role |
|---|---|
| Ignacio Romero Torres | Coordinator · Requirements |
| David Calzado Olmo | Requirements · Meeting minutes |
| Rodrigo Muñoz Martín | Analysis |
| Marcos Loro Carrasco | Design |
| Denis-Alexandru Ionescu Telibasa | Design |
| Javier Villanueva Crespo | Implementation |
| Adam Gouram El Bouhali | Testing |

Every discipline has an owner, but everyone helps where it is needed. How we decided the roles is in [Team and roles](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Team-and-roles).

## Build and run

You need **Java 21 or later** and **Maven 3.9 or later**.

Build the project and run all the tests (the same command that CI runs):

```bash
mvn -B verify
```

Run the application (a Swing desktop window):

```bash
mvn -B install -DskipTests
mvn -B -pl client exec:java -Dexec.mainClass=es.uclm.esi.iso2.client.Main
```

The client and the server run in the same program: the client calls the server through Java interfaces, and the data is kept in memory, so it starts again from zero every time the application is opened (D-016, D-030). The test accounts are defined in [`AuthenticatorSystemSimulator`](server/c1-access/src/main/java/es/uclm/esi/iso2/server/access/integration/AuthenticatorSystemSimulator.java), which simulates the hospital's login system.

## Project structure

```text
pom.xml          Parent pom: product version, Java 21 and the versions of the dependencies
common/          Contracts (interfaces), domain classes and exceptions shared by client and server
server/          Aggregator of the server submodules, one per component
  c1-access/       C1: services, in-memory repositories and simulations of the external systems
client/          Swing windows (IU_ classes) and Main, which starts the application
doc/             Visual Paradigm project, approved designs and implementation plans
  ISO-Project.vpp  Official model with all the diagrams
  design/          Exported diagrams of each iteration
  plans/           Plan of each use case, written before implementing it
DECISIONS.md     Every decision of the team, with its reason (D-001, D-002…)
AGENTS.md        Rules for the AI agents that help us implement and test
```

Inside each module, every component has its own package, for example `es.uclm.esi.iso2.server.access` for C1. Each component is a Maven submodule of `server` and `client` (`server/c1-access`, `client/c1-access`…), with its own version (D-032). The server of C1 is already in `server/c1-access`; its client follows (#101). The interfaces are designed by the team and live in `common`, so the client and the server only depend on contracts.

## How we work

- **Git Flow.** Nobody commits to `main` or `develop`: every change goes through a branch and a pull request.
  - Use cases: `feature/UCxx-server` and `feature/UCxx-client`
  - Other tasks: `feature/short-description`
  - Fixes: `bugfix/…`; releases: `release/x.y.z`; urgent fixes: `hotfix/x.y.z`
- **Pull requests** need a green build and the approval of a member who is not the author (D-001). The build fails below 80 % of line coverage or 70 % of branch coverage (JaCoCo, D-033). The review checklist is in the [pull request template](.github/pull_request_template.md).
- **Versions** follow [Semantic Versioning 2.0.0](https://semver.org/). The product goes from 0.1.0 to 1.0.0, one MINOR per iteration, and every release is an annotated tag on `main` (D-014). Each component has its own version, starting at 1.0.0 (D-032).
- **Tasks** are GitHub issues: one per component, one per use case and one sub-issue per RADIT discipline, grouped in one milestone per iteration.
- **Meetings** are recorded, and their minutes and decisions are published on the wiki.
- **AI agents** (GitHub Copilot and others) implement and test what the team has designed, following [AGENTS.md](AGENTS.md). The team does the requirements, the analysis, the design and the design of the tests, and reviews everything the agents produce before it is merged.

## Documentation

| Where | What |
|---|---|
| [Wiki](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki) | Planning, requirements and use cases, minutes of meetings, self-assessment |
| [Planification](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Planification) | Effort estimate per component and discipline, and the real hours |
| [Requirements and Use Cases](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Requirements-and-Use-Cases) | The 22 functional requirements, actors and use case diagrams |
| [Minutes of meetings](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Minutes-of-meetings) | One page per meeting, with its decisions and action items |
| [DECISIONS.md](DECISIONS.md) | Decision log |
| [doc/](doc/) | Models, designs and implementation plans |
