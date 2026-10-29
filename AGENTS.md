# AGENTS.md

Entry point for the AI agents (GitHub Copilot, Claude Code and others) that work on this repository.
Before doing anything, read the files below **in this order**. They are short on purpose: read all of them.

1. [agents/rules.md](agents/rules.md): the rules you must always follow.
2. [agents/project.md](agents/project.md): what we build, how we work and our conventions.
3. [agents/architecture.md](agents/architecture.md): code structure, build and tests.
4. [agents/components.md](agents/components.md): components, iterations, versions and dependencies.
5. [agents/handoff.md](agents/handoff.md): how to start, continue and leave a task so that another agent can take it over.
6. The state file of the component you work on: `agents/state/Cn.md` (for example, [agents/state/C1.md](agents/state/C1.md)).

`agents/README.md` explains the folder to people; you do not need to read it.

The reasons behind every rule are in [DECISIONS.md](DECISIONS.md). If two sources disagree, DECISIONS.md wins, then
the wiki, then the issues, then the files in `agents/`. For the state of the work, Git and the build win over the
state files (rule 10). Do not choose between sources on your own: tell the user.
