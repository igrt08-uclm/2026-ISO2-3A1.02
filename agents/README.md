# The `agents/` folder

> **For people.** This file explains the folder to the team. Agents do not need to read it: their reading list is in
> [AGENTS.md](../AGENTS.md).

Several AI agents (GitHub Copilot, Claude Code and others) work on this repository, and a session can stop at any
moment: the tokens run out, the session is closed, or another member continues with another tool. The next agent
has no chat history and no memory of what happened before. This folder gives every agent the same context and the
exact point where the work was left, so that it can continue without guessing.

## What is in the folder
```
AGENTS.md                 entry point: the files an agent reads, in order
CLAUDE.md                 one line (@AGENTS.md), so Claude Code reads the same files
agents/
├── README.md             this file (for people)
├── rules.md              the rules every agent follows
├── project.md            the project, how we work and our conventions
├── architecture.md       code structure, build and tests
├── components.md         components, iterations, versions and dependencies (was doc/CONTEXT.md)
├── handoff.md            how an agent starts, continues and leaves a task
└── state/
    ├── TEMPLATE.md       template for the state file of a new component
    └── C1.md             state of C1 (one file per component: C1.md, C2.md, ...)
```

| File | What it contains | When it changes |
|---|---|---|
| `AGENTS.md` | The reading order and which source wins when two of them disagree | Rarely |
| `rules.md` | Rules 1–8: the team rules (they were in `AGENTS.md`). Rules 9–19: working with several agents (see below) | When the team agrees a new rule |
| `project.md` | The project, the Unified Process and RADIT, issues and sub-issues, the table of conventions, and where to find things | When a convention changes |
| `architecture.md` | Maven modules, one package per component, contracts, simulated systems, D-022 and D-023, `mvn -B verify` | When the architecture changes |
| `components.md` | Components and iterations, module versions, dependencies between use cases, agreed rules on the data, the team | When the plan changes (the wiki wins if they disagree) |
| `handoff.md` | The protocol of a session: start, work, stop, several agents at the same time, conflicts | Rarely |
| `state/Cn.md` | Implementation and testing status of each sub-issue of the component | **All the time**: agents update it after every step |

Every file except the state files is **context**: it is the same for everyone and only changes when the team
decides. The state files are the **working memory** of the agents.

## The rules in short
| Group | Rules | Idea |
|---|---|---|
| The team decides | 1, 4, 5, 6, 8 | Agents suggest; they do not decide requirements, design or tools, and they do not invent data |
| Git and GitHub | 2, 3, 7, 18, 19 | Agents never create or delete branches, files, folders, packages or modules without a human yes; they never commit to `main` or `develop`; push and pull requests only when asked; no destructive commands; commits signed by the member |
| Reliable work | 9, 10, 11, 12 | Read everything at the start, Git is the truth, cite the source of every fact, *done* needs evidence (green build, commit, PR) |
| Small steps | 13, 14, 16, 17 | One sub-issue per session, one class or method per step, commit and update the state after each step, only start a step that can be finished, do not touch anything outside the sub-issue |
| Several agents | 15 | Never two agents on the same branch or folder; each one uses its own worktree |

## How we work: one session, step by step
Example: an agent implements *UC22 – Server (#13)*.

**1. Before the session.** A member creates the branch `feature/UC22-server` on GitHub from issue #13
(*Development → Create a branch*, source `develop`). Agents never create branches (rule 2).

**2. Start.** The agent runs `git fetch origin`, `git switch feature/UC22-server` and `git pull`, reads the files
of `AGENTS.md`, and opens `agents/state/C1.md`. There it finds the *Now* block of its sub-issue:

```markdown
### Server (#13) – Now
- **Branch:** `feature/UC22-server` · **Pull request:** none
- **In progress:** nothing
- **Last thing done:** nothing.
- **Next step:** read the server design class diagram of C1 (PR #49).
- **Blocked by:** nothing
```

It checks this against `git log` and `mvn -B verify`. Then it tells the member its plan, with every file it will
create or change, and waits for a yes (rule 19).

**3. Before each step,** the agent checks that it has enough tokens left to finish the step and update the state
(rule 16), and writes the step as *In progress*:

```markdown
- **In progress:** `SsoService` interface, simulated SSO (`SimulatedSso`)
```

**4. After each step,** with a green build, it commits the code, ticks the step with the commit hash, and commits
the state file:

```markdown
- **In progress:** nothing
- **Last thing done:** `SsoService` and `SimulatedSso` (a1b2c3d)
- **Next step:** `LoginService.login(username, password)` returns the role or an error
- **Steps:**
  - [x] `SsoService` and `SimulatedSso` (a1b2c3d)
  - [ ] `LoginService.login`
```

**5. If the session stops** (no tokens left, closed), the next agent, with any tool, repeats step 2. It reads
*In progress* and *Next step*, compares them with `git status` and `git log`, and continues. At most it loses the
step that was in progress.

**6. At the end,** the agent updates the *Overview* row, *Open questions* and the *Session log* (only its 10 newest
lines). The push and the pull request are done only when the member asks for them (rule 2), and another member
approves the pull request (D-001).

## Several agents at the same time
Each sub-issue has its own *Now* block, so two agents can work on the same component without touching the same
lines, as long as the use cases do not depend on each other ([components.md](components.md)). In C1 the use cases
form a chain (UC22 → UC19 → UC18), but in C2 UC17 and UC20 only depend on UC18, so they can be built at the same
time. Example: one agent on *UC17 – Server (#40)* and another on *UC20 – Server (#43)*:

| | Agent A | Agent B |
|---|---|---|
| Branch | `feature/UC17-server` | `feature/UC20-server` |
| Folder | `2026-ISO2-3A1.02` | `2026-ISO2-3A1.02-UC20-server` (a Git worktree) |
| Edits in `C2.md` | the *Now* block of #40 and its *Overview* row | the *Now* block of #43 and its *Overview* row |

A worktree is a second working folder of the same repository, on another branch. A member approves its creation
(rule 19), for example `git worktree add ../2026-ISO2-3A1.02-UC20-server feature/UC20-server`. Both pull requests are
joined in `develop` by people.

## A new component
When a component starts (for example C2), copy `state/TEMPLATE.md` to `state/C2.md` and fill in the header, the
*Overview* table and one section per use case, with its client and server *Now* blocks. The data comes from the
component issue and from [components.md](components.md).

## Where this comes from
The structure follows public practices for coding agents: the `AGENTS.md` open standard (one entry file in the root),
the progress files and one-task-per-session approach that Anthropic describes for long-running agents, Cline's
*memory bank* (stable context separated from the current state), and the use of one branch or worktree per agent
when several work in parallel. It was agreed by the team on 28/10/2026 (#60).
