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

1. **Read.** Run `git fetch origin` and `git pull` on the branch of your sub-issue. First read the context file of
   the component, `doc/plans/Cn-context.md` (D-047): the decisions, contracts, design classes and sub-issues, with
   their sources. Then read the sub-issue and its use case issue (`gh issue view <n>`) and, if it exists, the plan of
   the sub-issue in `doc/plans/`. Open the contracts (section 6 of the component issue), the approved design
   (`doc/design/`) and the decisions only for what the task needs, or if the component has no context file yet. If
   the context file disagrees with a source, the source wins (rule 11): tell the member. The pull request updates the
   lines of its own sub-issue in the context file, and the member who merges it checks that it did.
   Check that the sub-issue is assigned to the member who asked for the work. If it is assigned to someone else, or
   its plan shows a step *In progress* by another member, stop and tell the member (D-041). Remind the member to move
   it to *In progress* on the project board.
2. **Ask.** Write every doubt, ambiguity or missing piece in one message and wait for the answers. Never fill a gap
   with an assumption. If the design or a contract is missing or looks wrong, say so.
3. **Plan.** Write or update `doc/plans/UCxx-server.md` or `doc/plans/UCxx-client.md` from
   [doc/plans/TEMPLATE.md](doc/plans/TEMPLATE.md): the questions and answers, the files you will create or change,
   the classes and methods, the tests, and the steps in order.
4. **Wait for an explicit "yes"** from the member before changing anything. Anything outside the approved plan needs a
   new yes.
5. **Implement one step at a time.** Before each step, apply rule 14 and write the step as *In progress* in the
   plan. After the step, `mvn -B verify` must be green; then commit, tick the step in the plan with the commit hash,
   and set *In progress* back to nothing.
6. **If something does not go as planned, stop and re-plan** with the member. Do not improvise.
7. **Done needs evidence:** a green build, the tests passing and every acceptance criterion of the sub-issue checked.
   Never mark something as done because it "looks right".
8. **Leave it ready for the next session.** Before you stop, the plan says what is done, what was in progress and
   the exact next step (file, class or method), so that another session or another member can continue without
   asking.

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
   not in the approved plan, a worktree, a Java package or a Maven module, and before you change a `pom.xml`, the CI
   workflow, the issue templates, `AGENTS.md`, `DECISIONS.md` or the files in `agents/`.
10. **No destructive Git commands:** no `push --force`, `reset --hard`, `rebase` of a pushed branch, `clean` or
    `--no-verify`. If you think one is needed, stop and ask.
11. **Git is the truth.** If a plan or a document says one thing and `git log`, `git status` or the build say
    another, trust Git and the build, and tell the member.
12. **Cite your sources; do not invent.** Every fact about the project comes from a decision, an issue, a file or a
    diagram. If you cannot cite it, say that you do not know.
13. **Authorship.** Commits are made with the identity of the member who asked for the work, with no
    `Co-Authored-By` lines or tool signatures.
14. **Only start a step you can finish.** Before each step, estimate whether the context and tokens you have left are
    enough to implement it, build it, commit it **and** update the plan, and always keep a reserve for the plan
    update. If you are not sure, split the step into smaller ones, or do step 8 of the cycle now and stop.
15. **Never share a branch or a folder with another agent.** Two agents never work on the same branch or in the same
    folder at the same time: one can overwrite the other's changes without any warning. To work on two sub-issues at
    once on the same computer, each agent uses its own Git worktree (a second folder of the repository, on another
    branch), created only after the member approves it (rule 9):
    `git worktree add ../2026-ISO2-3A1.02-UC20-server feature/UC20-server`.
    Remove it with `git worktree remove <folder>` once its pull request is merged; the branch is kept (rule 7).
16. **The professor's repository is only an inspiration** (D-034). When you have a doubt, you may read
    https://github.com/IsmaelCaballero/PrISO2-25-26, explain to the member how it solves the problem and wait for the
    team to decide. Never copy its code or text, and when it differs from DECISIONS.md, DECISIONS.md wins: it uses a
    database, Spring Boot and JavaFX, and we do not (D-030, rule 6).
17. **Tests as in the lab session S.04.01** (D-034). The member gives you the table of cases (equivalence classes,
    boundary values, error guessing); you do not invent the expected results. Write one JUnit 6 method per case
    (`org.junit.jupiter`, never JUnit 4), with `@DisplayName` in English and arrange, act, assert, without changing
    the production code. When a test fails, explain whether the error is in the test or in the code, and never change
    an expected value to make a test pass. The build fails below 80 % of lines or 70 % of branches (D-033).
18. **Javadoc as you write** (D-034). Every class and public method you create or change has its Javadoc in English.
19. **Tokens of each iteration** (D-043). When an iteration ends, or when the member asks, count the tokens that the
    member used with you in that iteration from the logs of your tool (models with their % of output, calls, new
    input, cached input and output; premium requests if you are GitHub Copilot and have no local logs). Show the row
    to the member and, after a "yes", add it to their section of the wiki page *Token usage*, in the same format as
    the other rows. Never show paths, prompts or contents of the logs: only the counts.
20. **Traceability matrix** (D-044). When the pull request of a sub-issue is merged, or when the member asks, fill in
    its row of the wiki page *Requirements traceability matrix*: the pull request, the control class or the screen,
    and its test classes, with links to `develop`. Show the change to the member and update the page only after a
    "yes". Do not fill in cells you cannot cite (rule 12).
21. **Spend tokens where they add quality** (D-045). Every call re-reads the whole conversation:
    our calls re-read about 370,000 tokens each (wiki *Token usage*). Saving tokens never skips a rule above.
    - One task per conversation: when a step is committed and the plan updated (step 8), start a new conversation
      from AGENTS.md and the plan. Write first in the plan or the issue anything agreed only in the chat.
    - Read only what the task needs: the decisions cited (`grep -n "^## D-0" DECISIONS.md`, then only those
      entries), the *Status* of the plan first, and no file twice unless it changed.
    - Filter long output without hiding failures: the build as in
      [agents/token-efficiency.md](agents/token-efficiency.md), never `mvn -q`.
    - Read [agents/token-efficiency.md](agents/token-efficiency.md) before your first build, before you delegate to a
      subagent and when you count tokens (rule 19).
22. **The RADIT checklist of the issue** (D-048). When a pull request completes a discipline of its sub-issue and is
    merged, propose to the member to tick that box and, after a "yes" (rule 8), tick it. `Closes #n` closes the issue
    on merge, so use it only in the pull request that completes the last open box; in the others use `Refs #n`.
23. **Shared documents only in a `docs/` branch** (D-048). `AGENTS.md`, `DECISIONS.md`, `agents/*.md` and the templates
    of `.github/` are never changed in a `feature/` or `bugfix/` branch: the parallel branches conflict in them. A use
    case branch changes only its own plan and test design and, if its component has a context file (D-047), the lines
    of its own sub-issue in `doc/plans/Cn-context.md`. The versions in `agents/` are updated afterwards, in one `docs/`
    pull request.

If two sources disagree, DECISIONS.md wins, then the wiki, then the issues, then the files in `agents/`. Do not
choose on your own: tell the member.

## Conventions
| Topic | Convention | Source |
|---|---|---|
| Names | Use cases `UCxx`, functional requirements `FRxx` (1 FR = 1 UC) | D-002, D-015 |
| Branches | `feature/UCxx-server`, `feature/UCxx-client`, `feature/short-description`, `bugfix/…`, `docs/…`, `ci/…`, `release/x.y.z`, `hotfix/x.y.z` | D-015, D-048, Kickoff p. 9, 19 |
| Commits | Conventional Commits: `type(scope): summary (#issue)`, e.g. `feat(UC22): validate credentials against the SSO (#13)`, `fix(UC22): … (#13)`, `docs(UC22): plan for the server part (#13)` | Git practice p. 28, Kickoff p. 9 |
| Pull requests | To `develop`. When the pull request belongs to an issue, the description has `Refs #n`, or `Closes #n` only in the one that finishes the issue (rule 22); a standalone `docs/` or `ci/` pull request may have none | D-048, Kickoff p. 19 |
| Issues | Component issue → use case issue → client and server sub-issues, using the templates in `.github/ISSUE_TEMPLATE/`; the title of a use case issue carries its component and version, e.g. `UC20 – Server (C2 v1.1.0)` | D-015, D-018, D-032 |
| Versions | Product in the parent pom (`0.x.0-SNAPSHOT`, tag `v0.x.0` on `main`); each component submodule (`server/cN`, `client/cN`) has its own version: MINOR per use case of that component, PATCH per fix. Always write the component with its version (`C2 1.1.0`); names and versions are in [agents/components.md](agents/components.md) | D-014, D-021, D-032, Maven practice p. 19 |
| Releases | `release/0.x.0` from `develop`, merged into `main` and `develop`, annotated tag `v0.x.0` on `main` | Kickoff p. 9, Git practice p. 19 |
| Diagrams | Modular PlantUML text files (`.puml`) under `doc/` (`doc/use-cases/`, `doc/analysis/`, `doc/design/`), with shared definitions via `!includesub`; AI-generated diagrams are only proposals and become official once reviewed and approved in their pull request | D-008, D-046, assignment p. 5, note 9 |
| Language | Code, comments, commits, issues and documents in English | D-016 |
| Tests | Test class `<Class>Test` in `src/test/java`, same package; one method per case of the member's table; `@DisplayName` in English; arrange, act, assert | D-034, S.04.01 |
| Javadoc | Every class and public method, in English, written together with the code | D-034 |

## Cross-agent handoff (Implementation ↔ Testing)
To avoid several rounds between agents and the chat (D-048):

1. **The pull request is the handoff channel, not WhatsApp.** Testing posts one comment on the pull request with all
   the defects and the exact expected values or signatures, after the member's "yes" (rule 8). The agent of
   Implementation reads it with `gh pr view <n> --comments` and shows its plan before changing anything (step 4).
2. **One test update before one fix.** When a test expects an old or wrong behaviour, Testing pushes all the new
   expectations in one commit and lists the red tests in the comment. Implementation then pulls and fixes the code in
   one pass, and never edits a test (rule 17; error E-23 of the wiki).
3. **Tests come from the approved specification and plan, not from code in progress.** An open question is not fixed in
   an assertion. If Design has not decided the text of an error, the test checks the error state and the identifier or
   keyword, not the whole sentence.
4. **One use case at a time.** Finish and merge a use case (server, client and tests) before branching the next one of
   the same component.
5. **Build before every push** (step 5), with `mvn -B clean verify` after changing branch (error E-24 of the wiki). The
   tests of the Swing client need JDK 21 and, on Linux, `fontconfig` and fonts (`doc/plans/UC06-client.md`). Never push
   code for another member to build it for you.

## Where to find things
| What | Where |
|---|---|
| Why the project is the way it is | [DECISIONS.md](DECISIONS.md) |
| Code structure, build and tests | [agents/architecture.md](agents/architecture.md) |
| Components, iterations, versions and dependencies | [agents/components.md](agents/components.md) |
| Summary of each component (read first in step 1) | `doc/plans/Cn-context.md`, from [doc/plans/CONTEXT-TEMPLATE.md](doc/plans/CONTEXT-TEMPLATE.md) |
| Plan and progress of each sub-issue | [doc/plans/](doc/plans/) |
| Approved UML models (PlantUML) | [doc/use-cases/](doc/use-cases/), [doc/analysis/](doc/analysis/) and [doc/design/](doc/design/) (official `.puml` sources; `doc/ISO-Project.vpp` retired by D-046) |
| Requirements (FR table, actors) | Wiki: *Requirements and Use Cases* |
| Plan, effort and cost | Wiki: *Planification* |
| Iterations, dates and versions | Wiki: *Traceability and releases* |
| Meeting minutes and assigned tasks | Wiki: *Minutes of meetings* and the issues |
| Errors already made | Wiki: *Errors and lessons learned* |
| How to spend fewer tokens without losing quality | [agents/token-efficiency.md](agents/token-efficiency.md) |
| How the professor solved a similar problem (inspiration only, rule 16) | https://github.com/IsmaelCaballero/PrISO2-25-26 |

The wiki is at https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki. The course guidelines (assignment, Kickoff, Git
and Maven practices) are not in this repository; if a local `guidelines/` folder exists, read it for the full rules.

## Lessons
When the member corrects you, or you find a mistake that could happen again, propose a short lesson for the wiki
page *Errors and lessons learned*. Do not edit the wiki yourself (rule 8).
