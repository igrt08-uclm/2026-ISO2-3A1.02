# The project and how we work

## The project
This is the lab project of Software Engineering II (UCLM, 2026/27), team 3A1.02. We build the Radiodiagnostics and Medical
Imaging Management System of SESCAM: a Java 21 client–server application, developed with the Unified Process
(iterations with RADIT disciplines: Requirements, Analysis, Design, Implementation, Testing).
The professor assesses how we follow **our own plan**, not how much code exists (Kickoff, p. 20).

## How the work is organised
- 22 functional requirements, 1 FR = 1 use case (D-002). They are grouped into 9 components, and as a rule each
  iteration develops one component (D-012). The list is in [components.md](components.md).
- In an iteration the disciplines go in order, each one done by its responsible member (D-007, still valid for this).
  Agents work mostly on **implementation** (Javier) and **testing** (Adam). The team designs the tests; agents can
  write and run them (assignment, p. 3).
- Work items: component issue → use case issue → one client sub-issue and one server sub-issue (D-015, D-018).
  The server part is built first, because the client calls it (D-015).

## Conventions
| Topic | Convention | Source |
|---|---|---|
| Names | Use cases `UCxx`, functional requirements `FRxx` (1 FR = 1 UC) | D-002, D-015 |
| Branches | `feature/UCxx-server`, `feature/UCxx-client`, `feature/short-description`, `bugfix/…`, `release/x.y.z`, `hotfix/x.y.z` | D-015, Kickoff p. 9, 19 |
| Commits | Conventional Commits: `type(scope): summary (#issue)`, e.g. `feat(UC22): validate credentials against the SSO (#13)`, `fix(UC22): … (#13)` | Git practice p. 28, Kickoff p. 9 |
| Pull requests | To `develop`, with `Closes #n` in the description; one sub-issue = one branch = one pull request | Kickoff p. 19 |
| Issues | Component issue → use case issue → client and server sub-issues, using the templates in `.github/ISSUE_TEMPLATE/` | D-015, D-018 |
| Versions | Product in the parent pom (`0.x.0-SNAPSHOT`, tag `v0.x.0` on `main`); `server` and `client` have independent versions: MINOR per use case, PATCH per fix | D-014, Maven practice p. 19 |
| Releases | `release/0.x.0` from `develop`, merged into `main` and `develop`, annotated tag `v0.x.0` on `main` | Kickoff p. 9, Git practice p. 19 |
| Diagrams | Propose them as PlantUML text, never as images; the team imports and approves them in Visual Paradigm 18.1 | D-008, D-017, assignment p. 5, note 9 |
| Language | Code, comments, commits, issues and documents in English | D-016 |

## Where to find things
| What | Where |
|---|---|
| Why the project is the way it is | [DECISIONS.md](../DECISIONS.md) |
| Components, dependencies, version sequence | [components.md](components.md) |
| State of the component you work on | `state/Cn.md` |
| Requirements (FR table, actors) | Wiki: *Requirements and Use Cases* |
| Plan, effort and cost | Wiki: *Planification* |
| Iterations, dates and versions | Wiki: *Traceability and releases* |
| Meeting minutes and assigned tasks | Wiki: *Minutes of meetings* and the issues |
| Errors already made | Wiki: *Errors and lessons learned* |
| Diagrams | `doc/ISO-Project.vpp` (Visual Paradigm 18.1) |

The wiki is at https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki. With the GitHub CLI, issues can be read with
`gh issue view <n>`.

The course guidelines (assignment, Kickoff, Git and Maven practices) are not in this repository. If a local
`guidelines/` folder exists, read it for the full rules; otherwise, the references above point to them.
