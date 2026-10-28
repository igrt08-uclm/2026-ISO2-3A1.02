# AGENTS.md

Instructions for the AI agents (GitHub Copilot) that work on this repository. Read this file before doing anything.
The reasons behind every rule are in [DECISIONS.md](DECISIONS.md); the project data (components, dependencies,
versions) is in [doc/CONTEXT.md](doc/CONTEXT.md).

## The project
Lab project of Software Engineering II (UCLM, 2026/27), team 3A1.02. We build the Radiodiagnostics and Medical
Imaging Management System of SESCAM: a Java 21 client–server application, developed with the Unified Process
(iterations with RADIT disciplines: Requirements, Analysis, Design, Implementation, Testing).
The professor assesses how we follow **our own plan**, not how much code exists (Kickoff, p. 20).

## Rules you must always follow
1. **You suggest, the team decides** (D-008). Do not take requirements, analysis, design or architecture decisions
   on your own, and do not change a decision of DECISIONS.md. If something is not decided, ask.
2. **Ask before acting on GitHub.** Do not create, edit or close issues, milestones, branches, pull requests or wiki
   pages unless the user asked for that exact action.
3. **Never commit to `main` or `develop`.** Work on a branch and open a pull request to `develop`; another member
   approves it (D-001). Never merge your own pull request. Never delete branches (Kickoff, p. 9).
4. **Only the current iteration.** Do not specify, model or implement use cases of other iterations (D-011;
   assignment, p. 6, note 14).
5. **Implement what was designed.** Code follows the approved Visual Paradigm diagrams and the contracts of the
   component issue. If the design is missing or wrong, say so instead of inventing it (Kickoff, p. 28).
6. **No new dependencies, frameworks or tools** unless the team decided them (assignment, p. 4).
7. **Authorship:** commits are made with the identity of the member who asked for the work, with no
   `Co-Authored-By` lines or tool signatures.
8. **Do not invent facts.** If a piece of data is not in this repository or its wiki, say that you do not know.

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

## Code structure (D-016)
```
radiodiagnostics/        parent pom, es.uclm.esi.iso2, version 0.1.0-SNAPSHOT, Java 21, JUnit 6.1.3
├── server/              server application
└── client/              client application
```
- One Java package per component in each module, e.g. `es.uclm.esi.iso2.server.access` for C1.
- Client and server talk through **interfaces (contracts)**; the communication is simulated with direct Java calls,
  not a network. A shared `common` module for those contracts is planned (#35) but **does not exist yet**.
- External hospital systems (SSO, EHR, admissions, RIS/PACS) are simulated behind interfaces.
- "The engineer designs the interfaces; the AI implements the contracts" (assignment, p. 8).

## Build and test
- `mvn -B verify` builds and runs all the tests. The CI (`.github/workflows/maven.yml`) runs it on every push and
  pull request to `main` and `develop`; a pull request with a red build is not approved.
- Every use case has unit tests. The team designs the tests; you can write and run them (assignment, p. 3).

## Where to find things
| What | Where |
|---|---|
| Why the project is the way it is | [DECISIONS.md](DECISIONS.md) |
| Components, dependencies, version sequence | [doc/CONTEXT.md](doc/CONTEXT.md) |
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
