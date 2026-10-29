# AGENTS.md

Instructions for every AI agent that works on this repository (GitHub Copilot or any other tool). This is the only
file of rules: read it completely before doing anything. The reasons behind every rule
are in [DECISIONS.md](DECISIONS.md).

## The project
Lab project of Software Engineering II (UCLM, 2026/27), team 3A1.02: the Radiodiagnostics and Medical Imaging
Management System of SESCAM, a Java 21 client–server application developed with the Unified Process (iterations with
the RADIT disciplines: Requirements, Analysis, Design, Implementation, Testing). The professor assesses how we follow
**our own plan**, not how much code exists (Kickoff, p. 20).

People do the requirements, the analysis, the design and the design of the tests; agents implement what was designed
and run the tests (assignment, p. 3). "The engineer designs the interfaces; the AI implements the contracts"
(assignment, p. 8).

## How you work: Spec-Driven Development (D-025)
Every task that changes files follows this cycle. Do not skip steps, and do not write code before step 4.

1. **Read.** Run `git fetch origin` and `git pull` on the branch of your sub-issue. Then read the sub-issue and its
   use case issue (`gh issue view <n>`), the contracts (section 6 of the component issue), the approved design
   (`doc/design/`), the decisions they cite and, if it exists, the plan of the sub-issue in `doc/plans/`.
2. **Ask.** Write every doubt, ambiguity or missing piece in one message and wait for the answers. Never fill a gap
   with an assumption. If the design or a contract is missing or looks wrong, say so.
3. **Plan.** Write or update `doc/plans/UCxx-server.md` or `doc/plans/UCxx-client.md` from
   [doc/plans/TEMPLATE.md](doc/plans/TEMPLATE.md): the questions and answers, the files you will create or change,
   the classes and methods, the tests, and the steps in order.
4. **Wait for an explicit "yes"** from the member before changing anything. Anything outside the approved plan needs a
   new yes.
5. **Implement one step at a time.** After each step `mvn -B verify` must be green; then commit and tick the step in
   the plan with the commit hash.
6. **If something does not go as planned, stop and re-plan** with the member. Do not improvise.
7. **Done needs evidence:** a green build, the tests passing and every acceptance criterion of the sub-issue checked.
   Never mark something as done because it "looks right".
8. **Leave it ready for the next session.** Before you stop, the plan says what is done and the exact next step
   (file, class or method), so that another session or another member can continue without asking.

A question that does not change any file does not need a plan.

## Rules you must always follow
1. **You suggest, the team decides** (D-008). Do not take requirements, analysis, design or architecture decisions,
   and do not change a decision of DECISIONS.md.
2. **Only your sub-issue.** One sub-issue and one branch per session. Change only what your sub-issue needs: do not
   refactor, rename or "improve" anything else; write it in the plan as an open question instead.
3. **Only the current iteration.** Do not specify, model or implement use cases of other iterations (D-011;
   assignment, p. 6, note 14).
4. **Implement what was designed.** The code follows the approved design and the contracts of the component issue
   (Kickoff, p. 28).
5. **Keep it simple.** The smallest change that meets the specification: no extra features, no temporary fixes, no
   overengineering (assignment, p. 3).
6. **No new dependencies, frameworks or tools** unless the team decided them (assignment, p. 4).
7. **Branches.** Work only on the branch of your sub-issue, `feature/UCxx-server` or `feature/UCxx-client` (D-015).
   A member creates it on GitHub from the issue: never create, rename or delete a branch yourself. Never commit to
   `main` or `develop`, and never merge a pull request: another member approves it (D-001).
8. **Ask before acting on GitHub.** Push, open pull requests, and create, edit or close issues, milestones or wiki
   pages only when the member asks for that exact action.
9. **Human approval for structural changes.** Ask before you create, move, rename or delete a file or folder that is
   not in the approved plan, a Java package or a Maven module, and before you change a `pom.xml`, the CI workflow,
   the issue templates, `AGENTS.md`, `DECISIONS.md` or the files in `agents/`.
10. **No destructive Git commands:** no `push --force`, `reset --hard`, `rebase` of a pushed branch, `clean` or
    `--no-verify`. If you think one is needed, stop and ask.
11. **Git is the truth.** If a plan or a document says one thing and `git log`, `git status` or the build say
    another, trust Git and the build, and tell the member.
12. **Cite your sources; do not invent.** Every fact about the project comes from a decision, an issue, a file or a
    diagram. If you cannot cite it, say that you do not know.
13. **Authorship.** Commits are made with the identity of the member who asked for the work, with no
    `Co-Authored-By` lines or tool signatures.

If two sources disagree, DECISIONS.md wins, then the wiki, then the issues, then the files in `agents/`. Do not
choose on your own: tell the member.

## Conventions
| Topic | Convention | Source |
|---|---|---|
| Names | Use cases `UCxx`, functional requirements `FRxx` (1 FR = 1 UC) | D-002, D-015 |
| Branches | `feature/UCxx-server`, `feature/UCxx-client`, `feature/short-description`, `bugfix/…`, `release/x.y.z`, `hotfix/x.y.z` | D-015, Kickoff p. 9, 19 |
| Commits | Conventional Commits: `type(scope): summary (#issue)`, e.g. `feat(UC22): validate credentials against the SSO (#13)`, `fix(UC22): … (#13)`, `docs(UC22): plan for the server part (#13)` | Git practice p. 28, Kickoff p. 9 |
| Pull requests | To `develop`, with `Closes #n` in the description; one sub-issue = one branch = one pull request | Kickoff p. 19 |
| Issues | Component issue → use case issue → client and server sub-issues, using the templates in `.github/ISSUE_TEMPLATE/` | D-015, D-018 |
| Versions | Product in the parent pom (`0.x.0-SNAPSHOT`, tag `v0.x.0` on `main`); `server` and `client` have independent versions: MINOR per use case, PATCH per fix | D-014, D-021, Maven practice p. 19 |
| Releases | `release/0.x.0` from `develop`, merged into `main` and `develop`, annotated tag `v0.x.0` on `main` | Kickoff p. 9, Git practice p. 19 |
| Diagrams | Propose them as PlantUML text, never as images; the team imports and approves them in Visual Paradigm 18.1 | D-008, D-017, assignment p. 5, note 9 |
| Language | Code, comments, commits, issues and documents in English | D-016 |

## Where to find things
| What | Where |
|---|---|
| Why the project is the way it is | [DECISIONS.md](DECISIONS.md) |
| Code structure, build and tests | [agents/architecture.md](agents/architecture.md) |
| Components, iterations, versions and dependencies | [agents/components.md](agents/components.md) |
| Plan and progress of each sub-issue | [doc/plans/](doc/plans/) |
| Approved design that agents can read | [doc/design/](doc/design/) (agents cannot open `doc/ISO-Project.vpp`) |
| Requirements (FR table, actors) | Wiki: *Requirements and Use Cases* |
| Plan, effort and cost | Wiki: *Planification* |
| Iterations, dates and versions | Wiki: *Traceability and releases* |
| Meeting minutes and assigned tasks | Wiki: *Minutes of meetings* and the issues |
| Errors already made | Wiki: *Errors and lessons learned* |

The wiki is at https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki. The course guidelines (assignment, Kickoff, Git
and Maven practices) are not in this repository; if a local `guidelines/` folder exists, read it for the full rules.

## Lessons
When the member corrects you, or you find a mistake that could happen again, propose a short lesson for the wiki
page *Errors and lessons learned*. Do not edit the wiki yourself (rule 8).
