# Handoff: starting, continuing and leaving a task

An agent can stop at any moment (no tokens left, closed session, another tool). The next agent has only the
repository to go on: no chat history and no memory of the previous session. This file says how to leave the work so
that anyone can continue it.

Each component has a state file in `state/` (for example `state/C1.md`). It covers **implementation and testing**.
Requirements, analysis and design are tracked in the issues and the wiki. Inside the state file, each sub-issue
(for example *UC22 – Server (#13)*) has its own *Now* block.

## 1. When you start
1. Move to the branch of your sub-issue and get the latest work of the other members and agents:
   ```
   git fetch origin
   git switch feature/UCxx-server
   git pull
   ```
   The branch is created on GitHub by a member, from the sub-issue (rule 2). If it does not exist, stop and ask the
   user to create it. Then read the files listed in [AGENTS.md](../AGENTS.md) and the state file of the component.
2. Find the *Now* block of your sub-issue: the branch, the last thing done and the next step.
3. Check it against the repository:
   ```
   git status
   git branch --show-current
   git log --oneline -10
   mvn -B verify
   ```
   If they do not match the *Now* block, trust Git and the build (rule 10), correct the block and tell the user.
4. Read the sub-issue (`gh issue view <n>`), the contracts in section 6 of the component issue, and the design
   diagram it points to.
5. Tell the user your plan for the session: the steps, and every file you will create or change. Wait for their
   go-ahead. Only then commit on the branch. Anything outside the plan needs a new yes (rule 19), and push and the
   pull request are done only when the user asks for them (rule 2).

## 2. While you work
- One sub-issue and one branch per session (rule 13).
- Before each step, estimate whether the context and tokens you have left are enough to implement it, build it,
  commit it **and** update the state file, and always keep a reserve for the state update (rule 16). If you are not
  sure, split the step into smaller ones, or follow section 3 and hand off now. Then write the step as
  *In progress* in your *Now* block, so that the next agent knows what was being done if you are cut off.
- Work in small steps: one class or one method at a time. After each finished step (rule 14):
  1. Check that `mvn -B verify` is green.
  2. Commit the code, following the commit convention in [project.md](project.md).
  3. Tick the step in your *Now* block with the commit hash, and update *Last thing done* and *Next step*.
  4. Commit the state file, for example `docs(C1): update the agent state (#13)`.
  If the session stops after this, the next agent loses at most the step in progress.
- Write every doubt under *Open questions* instead of guessing (rules 8 and 11).

## 3. Before you stop
Do this before the session ends, and also as soon as you notice that your context or tokens are running low:
1. Update your *Now* block: what you did last, and a **next step** that another agent can do without asking
   (name the file, class or method).
2. Update your row in the *Overview* table and the *Open questions*, and add one line to the *Session log*. Keep only
   its 10 newest lines and delete the older ones: the full history is in `git log`.
3. Change *Updated* at the top of the file (date, member and agent).
4. Commit the state file on the same branch as the code. If some work could not be committed, describe it in your
   *Now* block.

## 4. Several agents at the same time (rule 15)
- Each agent works on its own sub-issue, on its own branch, and in its own folder. Two agents never work on the same
  branch or in the same folder at the same time, because one can overwrite the other's changes without any warning.
- To have two branches open at once on the same computer, use a Git worktree (a second working folder of the same
  repository, on another branch). Ask the user before you create or remove one (rule 19):
  ```
  git worktree add ../2026-ISO2-3A1.02-UC22-client feature/UC22-client
  ```
  The branch must already exist on GitHub (rule 2); run `git fetch origin` first.
  Remove it with `git worktree remove <folder>` once its pull request is merged; the branch is kept (rule 3).
- The work of each agent is joined by a person, through the pull request to `develop` (D-001).

## 5. Conflicts in the state file
The state file travels with the work branch and reaches `develop` with its pull request. Each branch only edits the
*Now* block and the *Overview* row of its own sub-issue, so two branches do not change the same lines. The *Session
log* and the *Open questions* are shared: if Git reports a conflict there, keep the lines of both branches.
