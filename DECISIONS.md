# DECISIONS

Log of the project's decisions: requirements, analysis, design and architecture decisions, assumptions, changes to the plan, and AI tools and models. One entry per decision, written when the decision is taken and linked to the meeting minutes where it was agreed. Code details are not logged here; they are in the commits. Team organisation is on the wiki page *Team and roles*, and work assignments are GitHub issues.

Each entry starts with its date, iteration, minutes (with the time in the meeting recording) and, when there is one, the GitHub issue that tracks it. Then come its context, decision, alternatives, reason and consequences. A decision that is later changed is not rewritten: a **Status** line points to the decision that replaces or updates it.

This file is also context for the AI agent: it must follow these decisions and never change them on its own.

---

## D-001 · Branch protection and pull request rules
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 0:15:07–0:20:44, 0:45:15–0:46:02

- **Context:** The first ruleset blocked every merge, because it had extra rules enabled: require code scanning results, require signed commits and restrict updates.
- **Decision:** `main` and `develop` are protected. Changes only reach them through a pull request approved by someone other than its author. The three extra rules are disabled.
- **Alternatives:** Adding the coordinator to the bypass list. Rejected, because the kickoff asks for an empty bypass list.
- **Reason:** Keep the protection the course asks for without blocking every merge.
- **Consequences:** Nobody, including the AI agent, commits directly to `main` or `develop`. The first pull request (the *Use case* issue template) could be merged.

## D-002 · Functional requirements: 21 from the specification + login
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 0:06:11–0:08:28, 0:18:34–0:19:57

- **Context:** According to the kickoff, each bullet point under the system users is one functional requirement.
- **Decision:** The project has 22 functional requirements: 21 from the specification and FR22, *Log in through the corporate SSO*, added by the team. Denis's table is the base.
- **Alternatives:** Ignacio's first draft with 14 requirements (incomplete). A pre-appointment form where patients describe their problem, proposed by Denis, was not added.
- **Reason:** Users must be identified to apply the permissions of their role, and no bullet point covers the login.
- **Consequences:** 22 use cases (1 functional requirement = 1 use case). The table is on the wiki page *Requirements and Use Cases*.

## D-003 · The exam catalogue is not a separate requirement
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 1:19:16–1:21:12

- **Context:** The specification describes a catalogue of examination types in its own section, not as a bullet point of a user role.
- **Decision:** The catalogue is managed through "test types" in FR17 (Configure system parameters), and it is preloaded in the system as seed data.
- **Alternatives:** Adding the catalogue as a separate functional requirement.
- **Reason:** Functional requirements are the bullet points under each user role.
- **Consequences:** Seed data with the examination types is needed before procedures can be ordered (FR06) or booked (FR13).

## D-004 · Actor inheritance in the use case diagram
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 0:37:54–0:39:05

- **Context:** The specification says physicians have all the patient features that apply to them, and every user has to log in.
- **Decision:** The *Physician* actor inherits from *Patient*. A general *User* actor, inherited by the four roles, holds the login use case.
- **Alternatives:** None discussed.
- **Reason:** Avoid repeating the same use cases for several actors.
- **Consequences:** Patient use cases are developed before physician use cases.
- **Status:** The actor inheritance remains valid. The development order is now set by D-013, so some physician use cases (FR06 and FR07, in C4) come before the patient ones (C6).

## D-005 · Use case priorities
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 0:23:18–1:11:09

- **Context:** Use cases must be prioritised and assigned to phases.
- **Decision:**
  - **Priority 1:** FR17, FR18, FR19, FR20, FR22
  - **Priority 2:** FR01, FR04, FR12, FR13, FR15
  - **Priority 3:** FR02, FR03, FR05, FR06, FR07, FR08, FR09, FR10, FR14
  - **Priority 4:** FR11, FR16, FR21
- **Alternatives:** Several priorities were discussed one by one. FR05 and FR07 were aligned to the same priority.
- **Reason:** Build the base of the system first (configuration, roles, integrations and login), then the functions that depend on it. Statistics and reports only read existing data, so they go last.
- **Consequences:** The priorities define the iterations (see D-007).
- **Status:** Updated by D-013: FR06, FR07 and FR14 go up to priority 2.

## D-006 · AI tool: GitHub Copilot
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 1:12:11–1:14:41

- **Context:** The course allows AI agents to support software engineering tasks.
- **Decision:** The team uses GitHub Copilot in Visual Studio Code. For analysis and design, Copilot drafts the diagrams in PlantUML; they are brought into Visual Paradigm and the team reviews and approves them (see D-008). Copilot is also asked to estimate the cost of each use case in tokens/€, to compare it with our own estimates.
- **Alternatives:** Other agents allowed by the specification (OpenCode, Antigravity).
- **Reason:** It is the reference environment recommended by the specification, and it is free with the UCLM student licence.
- **Consequences:** The cost of AI models for the team is €0. *The agent suggests; we decide.*

## D-007 · Iteration N = use cases with priority N
**Date:** 13/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 2](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-2) · **Time in the recording:** 0:05:42, 0:28:21–0:34:24

- **Context:** The kickoff suggests 1 use case = 1 iteration, and the team asked whether use cases with the same priority can share an iteration.
- **Decision:** Each iteration develops all the use cases of one priority level. Iteration 1 covers FR17, FR18, FR19, FR20 and FR22. Within an iteration, the disciplines are done in order (requirements → analysis → design → implementation → testing), each one by its responsible member (see the wiki page *Team and roles*). The next iteration starts only when every discipline of the current one is finished, and its use cases are not prepared in advance.
- **Alternatives:** Strictly 1 use case = 1 iteration. Members helping in other disciplines when they finish their own work.
- **Reason:** Use cases with the same priority belong to the same level of the system and can be developed together. Working in order keeps each member's responsibility clear.
- **Consequences:** Four construction iterations, plus Iteration 0 and the transition iteration (ItN). Later disciplines wait at the start of each iteration, and this waiting time must be reflected in the plan. The length of each iteration is still to be decided. This change to the kickoff's simplification must be justified in the theory report.
- **Status:** Replaced by D-012 on 21/10/2026. The way of working within an iteration (disciplines in order) remains valid.

## D-008 · AI-generated diagrams are only proposals
**Date:** 13/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 2](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-2) · **Time in the recording:** 0:13:44–0:15:52

- **Context:** A use case diagram of all the requirements was generated with an AI agent before the session planned for it.
- **Decision:** Any diagram generated by an AI agent is only a proposal or a reference. The official diagrams (use case, analysis and design) are made and approved in Visual Paradigm by the members responsible for each discipline. The AI-generated diagram of all the requirements is kept only as a prototype, to get an overview of the system.
- **Alternatives:** Adopting AI-generated diagrams directly as the official ones.
- **Reason:** Requirements and use cases are a human task in this course ("do it yourselves, not the AI agent"), and each discipline has a responsible member.
- **Consequences:** AI output is always reviewed by the team before it becomes official. So far, PlantUML code has to be copied into Visual Paradigm by hand.

## D-009 · Login scope
**Date:** 13/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 2](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-2) · **Time in the recording:** 0:17:34, 0:23:20–0:24:19

- **Context:** Alternative scenarios were discussed for FR22 (Log in through the corporate SSO).
- **Decision:** The login checks the credentials against the external SSO system. *Recover password* is an extension point. There is no sign-up.
- **Alternatives:** A sign-up option for new users.
- **Reason:** Sign-up is not required by the specification.
- **Consequences:** These alternative scenarios go into the description of the login use case.

## D-010 · Every task assigned in a meeting is a GitHub issue
**Date:** 21/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 3](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-3) · **Time in the recording:** 0:33:51, 0:36:37

- **Context:** In Meetings 1 and 2 the tasks were only written in the minutes. Ismael explained by email that every assignment made in a meeting must become an issue, and the professor insisted on it in class (20/10/2026). The assignment asks that every decision and agreed deliverable maps to a GitHub issue tracked on the project board.
- **Decision:** Every task assigned in a meeting becomes a GitHub issue, assigned to its owner, with a due date, added to the project board and linked to the minutes. The work is done on its own branch and the issue is closed by a pull request (`Closes #n`). The tasks from Meetings 1 and 2 are created now, and the finished ones are created and closed.
- **Alternatives:** None. It is required by the professor.
- **Reason:** Traceability between the minutes, the issues, the branches and the pull requests (Kickoff, page 19).
- **Consequences:** The action items table of every minutes has an *Issue* column. If the AI agent creates issues, they must come from a task agreed by the team and follow the issue templates.

## D-011 · Planning rules: whole team, plan before modelling
**Date:** 21/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 3](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-3) · **Time in the recording:** 0:41:54–0:43:22, 1:25:26–1:28:16

- **Context:** In Meeting 2 we understood that the planning was the coordinator's job, and the Iteration 1 diagrams and analysis were started before the plan was finished. Ismael answered by email that the planning must be done by the whole team, and the professor said in class (20/10/2026) that no diagrams must be made until the planning is finished. The assignment also says that use cases must not be specified or modelled all at once, but progressively, iteration by iteration.
- **Decision:**
  - The plan is made by the whole team, and the effort estimate is agreed in a meeting. Each member can change the hours of their discipline.
  - The estimate is kept both in the planning table and in each component issue, as Ismael asked by email (21/10/2026).
  - No analysis or design diagrams are made until the plan is approved. After that, each iteration only models its own use cases.
- **Alternatives:** None. It is required by the professor.
- **Reason:** The plan must answer how many hours the project takes, how much it costs and when it ends, and it must be agreed by everyone who has to follow it.
- **Consequences:** The Iteration 1 work already done (use case diagram and analysis) is on hold until the plan is approved, and will be reused. The AI agent must not generate diagrams or specifications for use cases outside the current iteration.

## D-012 · One iteration per component
**Date:** 21/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 3](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-3) (0:00:39–0:04:30, 0:29:20, 0:55:10–0:57:37), [Meeting 4](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-4) (0:07:26–0:10:05, 0:27:01–0:27:47)

- **Context:** D-007 grouped the use cases by priority, but use cases with the same priority do not always work on the same part of the system, and some of them depend on use cases with a lower priority (see D-013). Exercise E.01.02 (page 4) allows several use cases in one iteration and iterations of different lengths.
- **Decision:** The 22 use cases are grouped by topic into 9 components (C1 Access and security, C2 Configuration and integrations, C3 Patients, C4 Clinical orders, C5 Appointment planning, C6 Patient portal, C7 Execution and reports, C8 Notifications, C9 Statistics). As a rule, each iteration develops one component. Two exceptions were agreed in Meeting 4: C6 is split into two iterations of two use cases each, and some iterations combine a component with parts of other components that had to wait for their dependencies (D-013), for example the second part of C6 with the FR12 discharge, and FR02 with FR08. Phases of the Unified Process:
  - **Inception:** It0 (project plan)
  - **Elaboration:** C1 and C2, priority 1: the base of the system and the adapters to the external systems
  - **Construction:** C3 to C9
  - **Transition:** ItN (integration, deployment and user manual)
- **Alternatives:** Keeping one iteration per priority (D-007). One use case per iteration, as the assignment suggests (22 iterations). Strictly one component per iteration, without exceptions.
- **Reason:** Each component is a coherent piece of the system that can be developed and delivered on its own, and the dependencies can be solved component by component. C6, with four use cases, is too large for one iteration.
- **Consequences:** Replaces D-007. Each iteration ends with a product release (see D-014). The iterations, their use cases and their dates are listed on the wiki page *Traceability and releases*.

## D-013 · Development order by dependencies
**Date:** 21/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 3](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-3) · **Time in the recording:** 0:02:04–0:06:42, 1:11:26–1:18:47

- **Context:** The components spreadsheet shows the dependencies between use cases. For example, FR13 (priority 2) books the procedures ordered by FR06 and FR07 (priority 3), and FR14 (priority 3) builds the daily schedule from the appointments of FR13.
- **Decision:** The order between components follows their dependencies, not only the priority. Each component takes the highest priority of its use cases, so C4 (Clinical orders) goes up from priority 3 to 2. FR06, FR07 and FR14 go up from priority 3 to 2, so that the priorities match the development order (changed on 22/10/2026, after the review agreed in Meeting 3). When two components depend on each other, the dependent part is postponed until the component it needs exists: FR12 discharge (after C5), FR08 (after C5), FR03 and FR05 (after C7).
- **Alternatives:** Developing strictly by the priorities of D-005.
- **Reason:** A use case cannot be built before the use cases it needs.
- **Consequences:** Updates the priorities of D-005. The requirements table on the wiki and the functional requirements spreadsheet show the new priorities.

## D-014 · Versioning in two levels (SemVer)
**Date:** 21/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 3](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-3) (0:04:30–0:08:57, 1:18:51–1:23:47), [Meeting 4](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-4) (0:28:00–0:32:42)

- **Context:** The assignment asks for a release calendar with Semantic Versioning. The Maven practice (P.01.02, pages 10, 17 and 19) sets a parent pom with two modules, server and client, and versions in two levels. In Meeting 3 the team proposed one version per component, shared by the client and the server. It was aligned with the course guidelines on 23/10/2026, because the components are not Maven modules and their version would not be written in any pom, and approved in Meeting 4.
- **Decision:**
  - **Product** (parent pom): `0.1.0-SNAPSHOT` while it is developed. Each release goes through a `release/x.y.z` branch and is tagged `vx.y.z` on `main`. Only product releases reach `main` (Kickoff, page 9).
  - **Modules** (server and client): each one has its own version. It starts at **1.0.0** with its first use case, as SemVer defines (T.2.01). **MINOR** goes up with each new use case, **PATCH** with each fix, and **MAJOR** with a change that breaks compatibility. While a version is being developed it carries `-SNAPSHOT`. The two versions are independent: if a use case only changes one module, only that module goes up.
  - The release calendar of the plan sets which product version is released at the end of each iteration, and the module versions it includes.
- **Alternatives:** One version per component, shared by the client and the server (proposed in Meeting 3). Separate versions for the client and the server with a compatibility table between them.
- **Reason:** Follow the course guidelines (Maven practice, Git Flow and T.2.01): the version lives in the poms and in the tags of `main`, so it can be traced and built.
- **Consequences:** Every issue shows the version of the module it changes (for example, server 1.0.0). Tags per component, such as `C1-v1.0.0`, are not used. When the product version changes, `<parent><version>` must also change in the server and client poms.

## D-015 · Client and server: sub-issues and development order
**Date:** 21/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 3](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-3) (0:08:59–0:27:58, 1:23:47–1:24:55), [Meeting 4](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-4) (0:20:25)

- **Context:** Ismael explained in class that, for each functional requirement, the client and the server work together, with a use case diagram for each. The Kickoff (page 19) shows a use case issue with a server sub-issue and a client sub-issue, each with its own branch and pull request. Asked by email (21/10/2026), Ismael answered that using a parent issue per component "could be a good idea", and that the number of sub-issues should match the complexity of the use case, with at least one per system.
- **Decision:**
  - Each use case is an issue with at least one sub-issue for each system it involves: one for the client and one for the server. Complex use cases can have more sub-issues.
  - Each sub-issue has its own branch and pull request (`Closes #n`), and the use case issue is closed when both sub-issues are closed. Branch names follow the course guidelines (Kickoff, pages 9, 11 and 19), with UC instead of CU:
    - Use cases, one branch per module: `feature/UCxx-client` and `feature/UCxx-server` (for example, `feature/UC22-client`).
    - Other tasks: `feature/short-description` (for example, `feature/planification`).
    - Fixes on `develop`: `bugfix/short-description`. Releases: `release/x.y.z`. Production fixes: `hotfix/x.y.z`.
  - The use case issues of a component hang from a parent issue for that component, and they are created one component at a time, once the previous component is approved.
  - The client and the server of a use case are developed in the same iteration, step by step. The server part starts first, because the client calls it.
- **Alternatives:** One issue per use case with no sub-issues. Two separate issues per use case. Developing all the server first and then all the client.
- **Reason:** Each part can be assigned and tracked on its own, and a use case is only finished when both parts work together. Developing both parts in the same iteration allows testing each use case end to end, for example the login.
- **Consequences:** There are two use case diagrams, one for the client and one for the server, and the effort is estimated for both parts. The Maven modules are named `server` and `client`, in English like the rest of the project and the branches (the Maven practice uses `servidor` and `cliente`).

## D-016 · Maven structure and client–server communication
**Date:** 25/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 4](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-4) (0:13:35–0:24:43) · **Issues:** #18, #35

- **Context:** The client and the server are two applications in the same repository, and the assignment asks for a Maven multi-module project. The Maven practice (P.01.02) sets a parent pom with a server module and a client module. Both applications need the same contracts (interfaces and data classes), and the Kickoff (page 11) allows simulating the communication between them. We are a bilingual group: the course and the whole project are in English.
- **Decision:**
  - A parent pom `radiodiagnostics` (groupId `es.uclm.esi.iso2`, version `0.1.0-SNAPSHOT`) with the modules `server` and `client` (`1.0.0-SNAPSHOT`), Java 21 and JUnit 6.1.3 set in the parent. GitHub Actions builds and tests every push and pull request to `main` and `develop`.
  - One Java package per component inside each module, for example `es.uclm.esi.iso2.server.access` for C1.
  - A third module, `common`, holds the shared contracts (interfaces and data classes). The server implements them and the client uses them.
  - The client–server communication is simulated: the client and the server stay as separate modules, but they communicate with direct Java calls inside one program instead of a network connection.
  - The external hospital systems (SSO, electronic health record, admissions, RIS/PACS) are simulated behind interfaces.
- **Alternatives:** Two repositories; a single module; one Maven module per component; Spanish names (`radiodiagnostico`, `servidor`, `cliente`), as in the Spanish version of the Maven practice; the contracts copied in both modules; real communication over the network (REST or sockets).
- **Reason:** One `mvn verify` builds and tests everything, and the versions and libraries are set once in the parent. The names are in English, like the rest of the project and the branches. With a single copy of the contracts, the client and the server cannot get out of sync, and the build fails if they stop matching. Real network communication is outside the scope of the subject.
- **Consequences:** The `common` module has to be added to the Maven structure, with its own version (D-014) (#35). Nobody approves a pull request with a red build. The team will check with Ismael that the `common` module is accepted (#34).

## D-017 · Visual Paradigm version
**Date:** 25/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 4](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-4) (0:34:47–0:37:16)

- **Context:** A Visual Paradigm project saved with version 18.1 could not be opened by a member with an older version, and `.vpp` files are binary, so they cannot be merged.
- **Decision:** All members use Visual Paradigm 18.1, with automatic updates turned off. A change of version is agreed by the team. The commit that uploads a `.vpp` file states the Visual Paradigm version, for example `docs(UC22): server design model, VP 18.1 (#13)`.
- **Alternatives:** Each member keeps their own version.
- **Reason:** Everyone must be able to open and edit the same project files.
- **Consequences:** Every member updates to Visual Paradigm 18.1 before working on the diagrams.