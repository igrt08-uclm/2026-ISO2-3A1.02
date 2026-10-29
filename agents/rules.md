# Rules

Follow these rules exactly. The reasons are in [DECISIONS.md](../DECISIONS.md).

## Team rules
1. **You suggest, the team decides** (D-008). Do not take requirements, analysis, design or architecture decisions
   on your own, and do not change a decision of DECISIONS.md. If something is not decided, ask.
2. **Ask before acting on GitHub.** Work only on the branch of your task: for a use case, `feature/UCxx-server` or
   `feature/UCxx-client` (D-015). A member creates it on GitHub from the issue: never create, rename or delete a
   branch yourself. After the user's go-ahead at the start of the session, you can commit on that branch. Push,
   open pull requests, and create, edit or close issues, milestones or wiki pages only when the user asks for that
   exact action.
3. **Never commit to `main` or `develop`.** Work only reaches `develop` through a pull request approved by another
   member (D-001). Never merge your own pull request. Never delete branches (Kickoff, p. 9).
4. **Only the current iteration.** Do not specify, model or implement use cases of other iterations (D-011;
   assignment, p. 6, note 14).
5. **Implement what was designed.** Code follows the approved Visual Paradigm diagrams and the contracts of the
   component issue. If the design is missing or wrong, say so instead of inventing it (Kickoff, p. 28).
6. **No new dependencies, frameworks or tools** unless the team decided them (assignment, p. 4).
7. **Authorship:** commits are made with the identity of the member who asked for the work, with no
   `Co-Authored-By` lines or tool signatures.
8. **Do not invent facts.** If a piece of data is not in this repository or its wiki, say that you do not know.

## Rules for working with several agents
9. **Read before you act.** At the start of each session, run `git fetch origin` and `git pull`, then read every file
   listed in [AGENTS.md](../AGENTS.md). Do not rely on what you remember from an earlier session.
10. **Git is the truth.** If the state file says one thing and `git log`, `git status` or the build say another,
    trust Git and the build, correct the state file and tell the user.
11. **Cite your source.** When you state a fact about the project, say where it comes from: a decision (`D-016`),
    an issue (`#13`), a file or a diagram. If you cannot cite a source, it is not a fact: write it under
    *Open questions* in the state file.
12. **Done needs evidence.** A step is done only with evidence: a green `mvn -B verify`, a commit hash or a pull
    request number. Never mark a step as done because the code "looks right".
13. **One task at a time.** Work on one sub-issue (one branch) per session. Do not start another one until the
    current one is committed and the state file is updated.
14. **Leave the state ready for the next agent.** Work one class or method at a time, and commit it and update your
    *Now* block after each one and before you stop ([handoff.md](handoff.md), sections 2 and 3).
15. **Never share a branch or a folder with another agent.** When several agents work at the same time, each one
    works on its own sub-issue, branch and folder (a Git worktree), and only edits the *Now* block of its own
    sub-issue. A person joins the work through the pull requests.
16. **Only start a step you can finish.** If your context and tokens may not be enough to implement, build, commit
    **and** update the state, split the step or hand off now ([handoff.md](handoff.md), section 2).
17. **Stay inside your sub-issue.** Only change the files and code that your sub-issue needs. Do not refactor, rename
    or "improve" anything else while you are at it. If you see something that should change, write it under
    *Open questions* in the state file.
18. **No destructive Git commands.** Never use `git push --force`, `git reset --hard`, `git rebase` on a branch that
    is on GitHub, `git clean`, or `--no-verify`. They can erase the work of other members or agents. If you think one
    is needed, stop and ask the user.
19. **Creating or removing parts of the project needs human approval.** Ask the user and wait for an explicit yes
    before you:
    - create, rename or delete a branch or a worktree (rule 2);
    - delete, move or rename any file or folder;
    - create a folder, a Java package or a Maven module;
    - change a `pom.xml`, the CI workflow, the issue templates, `AGENTS.md`, `DECISIONS.md` or the files in `agents/`
      (except the state files).

    The files listed in the plan that the user approved at the start of the session ([handoff.md](handoff.md),
    section 1) can be created and changed without asking again. Anything outside that plan needs a new yes.
