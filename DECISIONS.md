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
- **Status:** Updated by D-038 on 15/11/2026: the coordinator may approve and merge his own pull requests.

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
- **Status:** Updated by D-034 on 11/11/2026: other agents may be used when the Copilot quota runs out.

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
- **Status:** Updated by D-046 on 22/11/2026: the official diagrams are PlantUML (`.puml`) files in `doc/`, reviewed and approved in their pull requests instead of Visual Paradigm; AI output is still only a proposal.

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
- **Status:** Updated by D-019 on 28/10/2026: the estimate is kept in the plan and only linked from the component issue.

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
- **Status:** Updated by D-021 on 28/10/2026: order of the module versions within an iteration. Updated by D-032 on 11/11/2026: each component submodule has its own version.

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
- **Status:** Updated by D-018 on 27/10/2026: the issues of later components can be created in advance.

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
- **Status:** Extended by D-023 on 28/10/2026: each component owns its data, and the interfaces of later components are simulated. Updated by D-032 on 11/11/2026: one Maven submodule per component instead of one package. Confirmed by Ismael on 12/11/2026: the `common` module for the shared interfaces is correct (#34).

## D-017 · Visual Paradigm version
**Date:** 25/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 4](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-4) (0:34:47–0:37:16)

- **Context:** A Visual Paradigm project saved with version 18.1 could not be opened by a member with an older version, and `.vpp` files are binary, so they cannot be merged.
- **Decision:** All members use Visual Paradigm 18.1, with automatic updates turned off. A change of version is agreed by the team. The commit that uploads a `.vpp` file states the Visual Paradigm version, for example `docs(UC22): server design model, VP 18.1 (#13)`.
- **Alternatives:** Each member keeps their own version.
- **Reason:** Everyone must be able to open and edit the same project files.
- **Consequences:** Every member updates to Visual Paradigm 18.1 before working on the diagrams.
- **Status:** Replaced by D-046 on 22/11/2026: Visual Paradigm is retired and all official UML diagrams live as PlantUML (`.puml`) files in `doc/`.

## D-018 · Use case issues can be created in advance
**Date:** 27/10/2026 · **Iteration:** 1 · **Minutes:** [Meeting 5](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-5) · **Time in the recording:** 0:25:45–0:26:55

- **Context:** D-015 said that the use case issues are created one component at a time, once the previous component is approved. During Iteration 1, the coordinator created the issues of C2 and C3 (#33, #38 to #47), so that the team can plan the next iterations, especially the week of 4 November, which has no classes and in which three iterations end.
- **Decision:** The issues of a component (the parent issue, its use cases and their client and server sub-issues) can be created before the previous component is finished. Each issue is in the milestone of its iteration, and the work on it follows the dependencies of D-013: an issue that is blocked is not worked on until the issues that block it are closed.
- **Alternatives:** Keeping D-015: creating the issues one component at a time.
- **Reason:** The team can see, plan and assign the work in advance, and the issues already show the dependencies between components.
- **Consequences:** Updates D-015. Approved in Meeting 5 (28/10/2026).

## D-019 · The effort estimate is kept in the plan and linked from the component issue
**Date:** 28/10/2026 · **Iteration:** 1 · **Minutes:** [Meeting 5](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-5) · **Time in the recording:** 0:26:55–0:27:53, 0:45:28–0:46:05 · **Issues:** #52, PR #51

- **Context:** D-011 says that the estimate is kept in the planning table and in each component issue. The use case template also had an effort table, which was never filled in for C1 and repeated the numbers of the plan.
- **Decision:** The effort estimate (hours with and without AI, per RADIT discipline) is recorded once, on the *Planification* page and in its spreadsheet. Each component issue links to the tab of its component. Use case issues do not have an effort table.
- **Alternatives:** Keeping the table in the component issue and in every use case issue.
- **Reason:** A single source of the numbers: two copies stop matching as soon as a member updates the hours of their discipline.
- **Consequences:** Updates D-011. The effort table was removed from the use case template (PR #51).
- **Status:** Updated by D-035 on 12/11/2026: the hours are also split between the server and the client.

## D-020 · One milestone per delivery date
**Date:** 28/10/2026 · **Iteration:** 1 · **Minutes:** [Meeting 5](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-5) · **Time in the recording:** 0:26:55–0:27:53, 0:45:28–0:46:05 · **Issues:** #52

- **Context:** D-018 places each issue in the milestone of its iteration, but several iterations end on the same date: It2, It3 and It4 on 4 November, and It6 and It7 on 18 November.
- **Decision:** There is one milestone per delivery date, not per iteration. A milestone can cover several iterations: Milestone 2 = It2, It3 and It4; Milestone 4 = It6 and It7. The iterations are still the unit of work and of release (one product version each).
- **Alternatives:** One milestone per iteration, with several milestones on the same date.
- **Reason:** The team works by weeks, and the board is easier to follow with one milestone per deadline.
- **Consequences:** Milestones 1 to 7 cover It1 to It10, due on 28/10, 04/11, 11/11, 18/11, 25/11, 02/12 and 09/12. An issue goes in the milestone of the date of its iteration, so UC08 (It9) is in Milestone 6.

## D-021 · Order of the module versions within an iteration
**Date:** 28/10/2026 · **Iteration:** 1 · **Minutes:** [Meeting 5](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-5) · **Time in the recording:** 0:26:55–0:27:53, 0:45:28–0:46:05 · **Issues:** #52

- **Context:** D-014 raises the MINOR version with each use case, but it does not say which use case of an iteration takes the lower version. In C1, the use cases are listed as UC22, UC18 and UC19, but UC19 is developed before UC18.
- **Decision:** When a use case depends on another one of the same iteration, it takes the higher version. When they do not depend on each other, the order is free. The real versions are written on the *Traceability and releases* page when the iteration ends.
- **Alternatives:** Following the order in which the use cases are listed on the traceability page.
- **Reason:** The versions must show the order in which the use cases are really built, and a dependency is the only order that cannot be broken.
- **Consequences:** Updates D-014. C1 is planned as UC22 `1.0.0`, UC19 `1.1.0` and UC18 `1.2.0`.

## D-022 · Records are deactivated, never deleted
**Date:** 28/10/2026 · **Iteration:** 1 · **Minutes:** [Meeting 5](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-5) · **Time in the recording:** 0:26:55–0:27:53, 0:45:28–0:46:05 · **Issues:** #52, #33, #47

- **Context:** The C2 and C3 issues already say that a test type "is deactivated, never deleted" (#33) and that discharging a patient "deactivates the record" (#47). The components spreadsheet says the same for appointments: cancelling changes their status.
- **Decision:** No use case deletes a record. Test types, protocols, schedules, policies, priorities, patients, orders and appointments change their status (inactive, discharged, cancelled) and keep their identifier.
- **Alternatives:** Deleting the records, or deleting only the ones that nothing references.
- **Reason:** A clinical record is evidence for the audit (UC19), the history of the patient and the statistics (C9). Deleting it would break the references that other components keep.
- **Consequences:** The contracts offer operations to deactivate or cancel instead of deleting, and the queries filter by status.

## D-023 · Each component owns its data; interfaces of later components are simulated
**Date:** 28/10/2026 · **Iteration:** 1 · **Minutes:** [Meeting 5](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-5) · **Time in the recording:** 0:26:55–0:27:53, 0:45:28–0:46:05 · **Issues:** #52, #35

- **Context:** D-016 places the contracts between client and server in interfaces, but it does not say who owns each piece of data. Some use cases also need components that are delivered later: for example, the discharge of a patient (C3) cancels appointments of C5 (#47).
- **Decision:**
  - Each component is the only one that writes its data. The other components use it only through the interfaces of that component.
  - When a use case needs an operation of a component that is delivered in a later iteration, the operation is declared now as an interface and has a simulated implementation. The real implementation replaces it when that component is delivered, without changing the interface.
- **Alternatives:** Letting each component read the data of the others directly. Waiting for the other component before developing the use case.
- **Reason:** The components stay independent and can be developed and tested one at a time (D-012), and D-013 only postpones the part of a use case that depends on another component.
- **Consequences:** Extends D-016. The interfaces are part of section 6 of each component issue. Where the shared interfaces live depends on the `common` module, still pending Ismael's answer (#34, #35).

## D-024 · Discharging a patient deactivates the record
**Date:** 28/10/2026 · **Iteration:** 1 · **Minutes:** [Meeting 5](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-5) · **Time in the recording:** 0:26:55–0:27:53, 0:45:28–0:46:05 · **Issues:** #52, #47

- **Context:** The C3 issue (#47) has an assumption to be confirmed by the team: "discharge" means deactivating the patient's record, not a hospital discharge.
- **Decision:** Discharging a patient deactivates their record. The history stays readable (reports, audit, statistics), the patient's scheduled appointments are cancelled through C5, and no new order or appointment can be created for an inactive patient.
- **Alternatives:** A hospital discharge. Deleting the patient.
- **Reason:** The clinical history must be kept, and cancelling the appointments frees slots for the waitlist (UC15).
- **Consequences:** The discharge is delivered in It8, after C5 (D-013). C4 and C5 reject inactive patients. The patient identifier (national ID or health card number) is not decided yet: it is decided with the requirements of C3.

## D-025 · Working method with AI agents: Spec-Driven Development
**Date:** 29/10/2026 · **Iteration:** 1 · **Minutes:** merged in PR #61, approved by two members; extended in [Meeting 7](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-7) by D-034 · **Issues:** #60, PR #61

- **Context:** The team implements the use cases with GitHub Copilot (D-006). The assignment asks to bind the artefacts of the Unified Process to development descriptors such as PLAN.md or CONTEXT.md under Spec-Driven Development, always with a person in the loop (assignment, pp. 4 and 10), and the Kickoff describes the cycle: the team designs, the agent proposes PlantUML, the team approves it in Visual Paradigm, and the agent implements what was designed (Kickoff, p. 28). Without a fixed method, an agent can start coding on its own, fill gaps with assumptions or change things outside its task.
- **Decision:**
  - Every task done with an agent follows the same cycle, written in AGENTS.md: read → ask every doubt → plan → explicit approval of a member → implement step by step with a green build → verify with evidence.
  - The specification is the use case issue and the use case specification on the wiki, and the design is the approved diagrams and the contracts of the component issue: both are made by people. The plan of each sub-issue is a file in `doc/plans/`, written by the agent and approved by the member; it also records the progress, so that another session or member can continue.
  - AGENTS.md is the single file of rules for every agent, whatever the tool. A tool that does not load AGENTS.md by default gets a one-line file that only imports it (`CLAUDE.md` contains `@AGENTS.md`); no rules are written anywhere else.
- **Alternatives:** An SDD tool such as GitHub Spec Kit or Kiro, which would make the agent write the requirements and choose the architecture, and both are human tasks here (Kickoff, p. 23; D-016). A state file per component, which would conflict between branches and double the commits. No fixed method.
- **Reason:** The agent only works on what was specified and designed, every doubt reaches a person before the code is written, and the plans are evidence of how the team uses AI.
- **Consequences:** Extends D-006 and D-008. The designers keep the approved diagrams in `doc/design/` in a form that agents can read, because they cannot open `.vpp` files. Each sub-issue has its plan in `doc/plans/`, committed on its branch with the code.
- **Status:** In force since 29/10/2026. Extended by D-034 on 11/11/2026: tests, Javadoc and the professor's repository.

## D-026 · Conclusions on the effect of AI on the plan
**Date:** 06/11/2026 · **Iteration:** 1 · **Minutes:** [Meeting 6](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-6) · **Time in the recording:** 0:29:24–0:36:12 · **Issues:** #80

- **Context:** The Kickoff (p. 29) asks to record five conclusions in this file, and the assignment asks to discount the optimism bias of AI and to compare the estimate with one made by an agent (pp. 5 and 6). None of them was recorded.
- **Decision:**
  - **Disciplines that AI reduces:** Testing (36.0 %) and Implementation (30.8 %) the most, Requirements (20.7 %) and Analysis (20.6 %) less, and Design (12.2 %) the least.
  - **Disciplines that must not be touched:** the decisions of requirements, analysis, design, architecture and test design stay with people (assignment, p. 3; D-008). AI only helps to draft them.
  - **Estimate in tokens:** an AI agent forecast about 34.4 million input tokens and 1.6 million output tokens, from the real size of UC22. At an illustrative price of 3 € and 15 € per million tokens that is about 128 €, 6.4 % of the human cost of Implementation and Testing. With the UCLM licence it costs 0 € (D-006).
  - **Cost and date:** 9,080 € with AI and 10,982.50 € without AI; 29 working days of development with AI and 36 without. The final date does not change, because it is fixed by the course.
  - **Optimism bias:** the members estimate the hours and include the review of what the agent produces; the estimate is corrected with the real hours of each component; and the plan is checked against a scenario in which AI only gives half of the saving: 281.3 hours, 10,031.25 € and 33 working days.
- **Alternatives:** Taking the forecast of the agent as the estimate. Not applying any discount.
- **Reason:** The forecast does not count failed sessions, and it shows that what changes the budget is the time people need to review the output, not the tokens.
- **Consequences:** These conclusions are in sections 9.2, 9.4 and 9.6 of the theory report.
- **Status:** Approved in Meeting 6 (08/11/2026).

## D-027 · One desktop client
**Date:** 06/11/2026 · **Iteration:** 1 · **Minutes:** [Meeting 6](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-6) · **Time in the recording:** 0:17:23–0:21:20, 0:29:24–0:31:44 · **Issues:** #80

- **Context:** The assignment describes clients for desktop and mobile, and web access with DICOM image viewers (p. 1). The Kickoff (p. 11) speaks of one Maven module per type of client.
- **Decision:** We build one client, a Java desktop application. Mobile and web clients and DICOM viewers are out of scope.
- **Alternatives:** A web client or a mobile client as well.
- **Reason:** The project assesses the process and the design, not the functionality (Kickoff, p. 3), and the estimate does not include more clients.
- **Consequences:** It is assumption A9 of the theory report. Another client would be a new Maven module with the same contracts.
- **Status:** Approved in Meeting 6 (08/11/2026).

## D-028 · Correction of the plan totals and recovery of the delay
**Date:** 06/11/2026 · **Iteration:** 1 · **Minutes:** [Meeting 6](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-6) · **Time in the recording:** 0:09:57–0:11:15, 0:29:24–0:31:44 · **Issues:** #80, #57

- **Context:** The totals of the plan counted the discharge of FR12 twice (E-15). C1 ended on 7 November instead of 28 October (E-10), with the release v0.1.0, and C2, C3 and C4 have not started. The assignment asks to explain how the deviations are recovered (p. 6, note 13).
- **Decision:**
  - The totals are 243.2 hours and 9,080 € with AI, and 319.3 hours and 10,982.50 € without AI.
  - The 221.2 hours left are delivered in the same order: 09/11–15/11, C2 and C3 (32.5 h); 16/11–22/11, C4 and C5 (52.8 h); 23/11–29/11, the first part of C6, and C7 (45.1 h); 30/11–06/12, It8 and It9 (54.1 h); 07/12–09/12, C9 (36.7 h).
  - The end of development stays on 9 December. It11 does not move, and it includes the testing plan, the test reports and the maintenance plan, within its fixed cost.
- **Alternatives:** Keeping the dates and accepting more delays. Reducing the scope, which the assignment does not allow.
- **Reason:** All the functional requirements have to be fulfilled, and the final date is fixed.
- **Consequences:** It asks for 5 to 8 hours per member per week. When it is approved, the dates of the next milestones and of *Traceability and releases* are updated. The due date of Milestone 1 stays as it was, so that the delay can be seen (#57).
- **Status:** Approved in Meeting 6 (08/11/2026).

## D-029 · Critical quality characteristics
**Date:** 07/11/2026 · **Iteration:** 1 · **Minutes:** [Meeting 6](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-6) · **Time in the recording:** 0:14:40–0:17:23 · **Issues:** #80, #54

- **Context:** The professor asked the team to choose three quality attributes (Meeting 5). The practice P.01.03 asks for at least one subcharacteristic of each one and for their impact on the requirements (slide 13), and the assignment asks to identify and justify the critical characteristics for this healthcare domain (p. 9). Whatever we choose has to be fulfilled, so the proposal takes what C1 already does and what does not change the architecture.
- **Decision:** The three critical characteristics of ISO/IEC 25010 are **Security** (non-repudiation), **Functional suitability** (functional appropriateness) and **Usability** (user error protection). The other five are kept with a lower weight.
- **Alternatives:** Reliability instead of Usability: availability, fault tolerance and recoverability clash with having the data in memory (D-030), and C1 has no logging yet. All eight with the same weight.
- **Reason:** A failure in any of the three has a clinical or legal consequence: nobody can prove who did what, a profile reaches functions that are not for it, or wrong data reaches the patient.
- **Consequences:** Sections 22 and 23 of the theory report develop them, and the non-functional requirements of the wiki come from them. Each new component records its critical operations, checks the role and validates its forms, and the pull request template has the review checklist.
- **Status:** Approved in Meeting 6 (08/11/2026).

## D-030 · Swing for the client, and data in memory
**Date:** 07/11/2026 · **Iteration:** 1 · **Minutes:** reported in [Meeting 6](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-6) · **Issues:** #34, #80

- **Context:** The client of C1 is already written in Swing, and the users and the audit log are kept in memory. Marcos recalls Ismael saying in class that persistence is not needed.
- **Decision:** The desktop client uses Swing. The data is kept in memory while the program runs, and no database is implemented.
- **Alternatives:** Another graphical library, such as JavaFX. A database, or saving the data in files.
- **Reason:** Swing comes with Java, so it adds no dependency, and C1 already uses it. The project assesses the process and the design, not the functionality (Kickoff, p. 3), and the estimate does not include a database.
- **Consequences:** The data is lost when the program closes. It is assumption A10 of the theory report. A database would be a new implementation behind the same repository interfaces.
- **Status:** Approved.

## D-031 · Operational assumptions of the plan
**Date:** 07/11/2026 · **Iteration:** 1 · **Minutes:** [Meeting 6](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-6) · **Time in the recording:** 0:29:24–0:31:44 · **Issues:** #80

- **Context:** The assignment requires all operational assumptions to be documented in the theory report, in the wiki and in this file (p. 3). The assumptions about requirements are in D-002 to D-005 and D-009, and those of the plan were only in the report.
- **Decision:** The plan relies on these assumptions:
  - **A1.** Each consultant is available 40 hours per week, and the team has seven members, but not all of them work on every discipline: R and D have two owners; A, I and T have one each. Ismael confirmed on 12/11/2026 that 8 hours per day can be assumed for simplicity, although an organisation works 43 hours per week (9 hours from Monday to Thursday and 7 on Friday) (#34). *If it is false:* The critical path of It5 and It10 (the two heaviest iterations) lengthens; the calendar of section 6 of the report is no longer feasible and the end date moves.
  - **A2.** AI assistance is available 24 hours a day, but every AI output needs human review before it is merged; the schedule therefore books the review, not the generation. *If it is false:* A higher AI ratio does not shorten anything: the bottleneck is reviewer availability, not token throughput (E-12, E-13).
  - **A3.** The cost of AI tooling is zero for the team: GitHub Copilot is covered by the university licence. As most members ran out of their monthly Copilot quota, the professor allowed us to use Claude Code, which most of us already had, so the members without quota left use Claude Code under the same rules (AGENTS.md). It is a personal licence of each member and is not charged to the project (D-034). *If it is false:* The budget of section 8 of the report must add a licensing line. The 9,080 € figure assumes no tooling cost.
  - **A4.** Visual Paradigm 18.1 is available to all seven members with an academic licence (D-017). *If it is false:* Model exchange between members breaks, and the design discipline becomes a serial bottleneck.
  - **A5.** The external systems are simulated behind interfaces, and no integration test uses a real endpoint. *If it is false:* FR20 grows from a configuration use case to an integration project; C2 and C1 both change.
  - **A6.** The examination catalogue is seed data, not a separate requirement (D-003). *If it is false:* FR17 would not cover the catalogue: a new requirement would be needed, and FR06 and FR13 would depend on it.
  - **A7.** Iteration 0 and Iteration 11 have fixed costs of 1,000 € and 2,000 € given by the assignment, not estimated by us. *If it is false:* The total of 9,080 € and the comparison with 10,982.50 € both become invalid.
  - **A8.** Human work costs 25 € per hour in every discipline. The specification itself gives no rate. *If it is false:* Every cost of section 8 of the report changes. Exercise E.01.02 also considers a different rate for each profile.
  - **A9.** We build one client, a Java desktop application. Mobile and web clients and DICOM image viewers are out of scope (D-027). The Kickoff (p. 11) speaks of one Maven module per type of client. *If it is false:* Each new type of client is a new Maven module that uses the same contracts, and FR09 needs a viewer for the images.
  - **A10.** The client uses Swing, and the data is kept in memory while the program runs: no database is implemented (D-030). *If it is false:* The repositories get a new implementation behind the same interfaces, for example IUserAccountRepository.
- **Alternatives:** Keep them only in the report.
- **Reason:** The agents and the team read this file, so an assumption that is not here can be broken without anyone noticing.
- **Consequences:** They are section 2 of the theory report and the page Planification of the wiki, and the three have to say the same.
- **Status:** Approved in Meeting 6 (08/11/2026). Updated by D-046 on 22/11/2026: assumption A4 no longer applies because Visual Paradigm has been replaced by PlantUML.

## D-032 · One Maven submodule per component
**Date:** 09/11/2026 · **Iteration:** 2 · **Minutes:** [Meeting 7](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-7) · **Time in the recording:** 0:02:26–0:04:07, 0:43:07 · **Issues:** #34

- **Context:** The assignment says that each component must be an individual Maven submodule of the root multi-module architecture (p. 7), while the Kickoff speaks of one module per application (p. 11). Until now each component was a package inside `server` and `client` (D-016), and section 5.3 of the theory report argued for it. The reference solution that the professor shared on 9 November uses one Maven submodule per component. Only C1 is written, so this is the moment when changing the structure costs least.
- **Decision:** Each component is a Maven submodule. `server` and `client` become aggregator modules with one submodule per component on each side (for example `server/c1-access` and `client/c1-access`), and `common` keeps the contracts of all the components, one package per component. C1 is moved to its two submodules before C2 is implemented, in a feature branch with a pull request, and every later component is created directly as a submodule. Each component submodule has its own version: MINOR per use case of that component, starting at 1.0.0.
- **Alternatives:** Keep one package per component inside `server` and `client` (D-016). Ask Ismael and wait for his answer before changing anything.
- **Reason:** It meets both documents: the applications stay as modules, as the Kickoff asks, and the components become submodules, as the assignment asks. Each component gets its own pom, version and declared dependencies, so a component that uses the internals of another one does not compile unless its pom declares it.
- **Consequences:** More poms to maintain and a longer build. The hours of moving C1 are not in the estimate and are recorded as real hours of It2. The version per module of D-014 becomes a version per component, so the traceability page shows one version per component instead of one for the whole server and client. The README and sections 5.3, 13, 15, 18 and 19 of the theory report change with it. If it is approved, D-014 and D-016 get a Status line that points here.
- **Status:** Approved in Meeting 7 (11/11/2026).

## D-033 · Test coverage threshold
**Date:** 09/11/2026 · **Iteration:** 2 · **Minutes:** [Meeting 7](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-7) · **Time in the recording:** 0:29:09–0:30:28 · **Issues:** #89, #90

- **Context:** The quality plan of the theory report (sections 19.8, 23 and 27.1) needs a measurable threshold for the tests. Until now coverage was not measured. C1 was measured on 9 November: 93 % of its lines and 91 % of its branches are covered.
- **Decision:** Every module keeps at least 80 % of its lines and 70 % of its branches covered. JaCoCo checks it in `mvn verify` with a rule at BUNDLE level (#90), so the build fails below the threshold and the pull request cannot be merged (D-001). The same threshold applies to every release.
- **Alternatives:** A threshold that rises with each release (from 60 % to 80 %). No threshold, only the coverage report.
- **Reason:** It is below what C1 already reaches, so it works as a floor that new components must not break, and CI checks it on every pull request without anyone having to remember it.
- **Consequences:** Each new module inherits the rule from the parent pom, including the component submodules of D-032. Modules without branches pass the check. Changing the numbers needs a new decision.
- **Status:** Approved in Meeting 7 (11/11/2026). The check was merged in #90 on 10/11/2026.

## D-034 · Rules for the agents after Meeting 7
**Date:** 11/11/2026 · **Iteration:** 2 · **Minutes:** [Meeting 7](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-7) · **Time in the recording:** 0:04:07–0:07:48, 0:24:36–0:25:46, 0:31:04–0:33:45

- **Context:** The professor shared his own solution of the project, and the lab session on JUnit (S.04.01) explained how tests are designed and written with an agent. AGENTS.md only said that the team designs the tests and the agent writes and runs them. The report also said that only GitHub Copilot is used, and the professor said in class that other agents can be used when the Copilot quota runs out.
- **Decision:**
  - **The professor's repository as a reference.** When the agent has a doubt, it may read the reference repository shared by the professor, explain how he solved it and wait for the team to decide. It is only an inspiration: no code or text is copied, and our decisions win when they differ (for example, he uses a database, Spring Boot and JavaFX; we do not, D-030).
  - **Tests as in S.04.01.** The team designs the cases in a table (equivalence classes, boundary values and error guessing, combined with each use or pairwise), and each expected result comes from a requirement. The agent writes them with JUnit 6 (`org.junit.jupiter`), one method per case, with `@DisplayName` in English and the structure arrange, act, assert, without changing the production code. When a test fails, the agent explains whether the error is in the test or in the code; a bug in the code gets an issue and a `bugfix/…` branch, and an expected value is never changed to make a test pass. The tests of a use case stay in its branch, `feature/UCxx-server` or `feature/UCxx-client` (D-015).
  - **Javadoc.** The agent writes the Javadoc of every class and public method when it writes them, not at the end of the project.
  - **Other agents.** When the monthly GitHub Copilot quota runs out, other AI agents may be used, with the same rules (AGENTS.md).
- **Alternatives:** Not using the professor's repository; letting the agent design the tests; writing the Javadoc at the end of the project.
- **Reason:** The professor's repository shows what he expects, but our decisions are different. In the assessment, the design of the cases counts for more than the coverage or the JUnit code (S.04.01, slide 19). Documenting while the code is written is easier than documenting it later.
- **Consequences:** AGENTS.md and agents/architecture.md are updated in the same pull request. D-006 is updated by this decision.
- **Status:** Approved.

## D-035 · The estimate is split between the server and the client
**Date:** 12/11/2026 · **Iteration:** 2 · **Minutes:** Ismael's answer to #34 (12/11/2026) · **Issues:** #34

- **Context:** Ismael answered that the hours of each RADIT discipline must be separated between the client and the server, because they are two different systems. The estimate of the plan (D-019) was made per use case and per discipline, without that split, and Theory Assignment 1 is delivered on 13/11/2026.
- **Decision:** The hours of each use case and discipline are split between the server and the client with one rule per discipline: Requirements 50 % / 50 %, Analysis 60 % server / 40 % client, Design 50 % / 50 %, Implementation 40 % server / 60 % client, Testing 50 % / 50 %. The totals do not change: of the 243.2 hours with AI, 122.51 are for the server and 120.69 for the client (160.03 and 159.27 of the 319.3 hours without AI). From C2 on, each client and server sub-issue records its real hours, and the rule is corrected with them.
- **Alternatives:** Estimating again each use case for each side, which could not be done before the delivery of 13/11/2026.
- **Reason:** One specification per use case serves both sides. The control and entity classes of the analysis belong to the server and the boundary classes to the client. There is one design class diagram per side. In C1 the client has about 500 lines of code, and the server and `common` about 260 (JaCoCo, 09/11/2026).
- **Consequences:** Section 8.1 of the theory report and the page Planification show the split per iteration. Updates D-019.
- **Status:** Decided by David on 12/11/2026, after Ismael's answer; approved in the review of this pull request.

## D-036 · Patches of C1 for C2 and C3, and how a finished component is changed
**Date:** 15/11/2026 · **Iteration:** 2 · **Minutes:** [Meeting 8](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-8) · **Time in the recording:** 0:01:41–0:04:53 · **Issues:** #40, #109, #112, #116

- **Context:** C2 and C3 need changes in C1, which was finished in Iteration 1 (v0.1.0): new permissions, and interfaces of C1 that other components must use. By D-032, C2 cannot depend on the module of C1, so those interfaces have to be in `common`. Section 13.6 of the theory report does not say which version goes up when a component needs a change in a finished one.
- **Decision:**
  - **C1 1.2.1 (#109, merged with UC17):** `IAuditRecorder` moves to `common`, and the permission `CONFIGURE_SYSTEM` is given to the system administrator.
  - **C1 1.2.2 (#112, pull request #116):** one patch for what UC20 (C2) and UC12 (C3) need from C1. `IAuthenticatorSystem` moves to `common` with a new method `ping()`; the permission `CONFIGURE_INTEGRATIONS` is given to the system administrator and `MANAGE_PATIENTS` to the Admissions staff; and `IRoleManagementService` gets the default method `hasPermission(username, permissionCode)`, so that each component checks the permissions with it instead of writing its own check.
  - **Next patches** of the same kind follow the same rule.
  - **The rule:** the change is asked for in an issue and approved by the owner of the finished component. That component raises PATCH, in its own branch and pull request, and the owner approves the pull request. If contracts are added to `common` without breaking any, `common` raises MINOR; a breaking change is MAJOR and needs its own decision (section 13.6 of the report). `common` goes from 1.3.0-SNAPSHOT to 1.4.0-SNAPSHOT for the contracts added in Iteration 2.
- **Alternatives:** C2 depending on the module of C1, which D-032 rules out; or copying the interfaces in each component, which duplicates code (Annex A, item Q).
- **Reason:** The components only share contracts through `common`, and the owner of a finished component checks every change to it (section 14.4 of the report). A patch in its own pull request is easier to review than inside the pull request of another component, and it is not delayed if that one is.
- **Consequences:** `common` 1.4.0-SNAPSHOT; agents/components.md and the page Traceability and releases are updated.
- **Status:** Patches approved by Ignacio, owner of C1, in #40 (13/11/2026) and #112 (15/11/2026), and by the team in Meeting 8 (15/11/2026). After the meeting, 1.2.2 also included `MANAGE_PATIENTS` for UC12, and Ignacio approved it in pull request #116 (15/11/2026).

## D-037 · Patient identifier
**Date:** 15/11/2026 · **Iteration:** 3 · **Minutes:** [Meeting 5](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-5) (left pending) · **Time in the recording:** 0:38:07–0:40:30 · **Issues:** #46, #47

- **Context:** In Meeting 5 Marcos pointed out that the national ID and the health card number are not the same, and that the team had to choose with the requirements of C3. The contract of C3 (#47) said that the identifier is "the national ID or the health card number", which does not say which one detects a duplicate, and the design of UC12 only has `patientId` and `identifier`.
- **Decision:** The server generates the internal `patientId` (`P-0001`, `P-0002`…). The `identifier` is the health card number, it is required and unique, and it is what detects a duplicate: if it already exists, the registration is rejected and the screen shows the existing record. The national ID is an optional field, `nationalId`.
- **Alternatives:** The national ID as the identifier, which not every patient has (for example, a minor); or accepting either of them, which gives two values to check for a duplicate.
- **Reason:** Not every patient has a national ID, and one unique value is enough to detect a duplicate.
- **Consequences:** Design adds `nationalId` to `Patient` (#114), and the contract of #47 is updated with it. UC12 is implemented this way (#46).
- **Status:** Proposed by David (Requirements) in #47 on 07/11/2026 and agreed in #46 on 14/11/2026; approved in the review of this pull request.

## D-038 · The coordinator may approve his own pull requests
**Date:** 15/11/2026 · **Iteration:** 2 · **Minutes:** — · **Issues:** E-13

- **Context:** D-001 says that a pull request is approved by someone other than its author. Ignacio, the coordinator, merged three of his own pull requests without another approval (#7, #51 and the release #86, E-13). Ignacio asked Ismael whether this is admissible, and Ismael answered that it is, because he is the coordinator, and that it depends on the rules and assumptions that the team sets.
- **Decision:** The coordinator may approve and merge his own pull requests, with the build green. He writes in the pull request that it is merged under D-038. Every other member still needs the approval of a member who is not the author (D-001). The bypass list of the ruleset stays empty (Kickoff, pp. 12–13): the coordinator merges as the owner of the repository.
- **Alternatives:** Keeping D-001 strict for everyone, so that the coordinator also waits for another approval; or adding the coordinator to the bypass list, which D-001 already rejected.
- **Reason:** Ismael accepts it for the coordinator, and the coordinator is who merges most pull requests and the releases, so waiting for another member blocks them. The other members keep the review of D-001.
- **Consequences:** Updates D-001. E-13 is closed. The README says it.
- **Status:** Decided by David on 15/11/2026, after Ismael's answer; approved in the review of this pull request.

## D-039 · Orders cannot be edited or cancelled in C4
**Date:** 15/11/2026 · **Iteration:** 4 · **Minutes:** — · **Issues:** #65

- **Context:** In Meeting 5 (0:41:38–0:43:28) Ignacio proposed adding the editing and the cancelling of orders to FR06, and Marcos warned that adding functionality that is not in the requirements goes against planning everything from the beginning. It was left to be decided with the requirements of C4, and it was the first pending decision of section 9 of #65.
- **Decision:** A physician cannot edit or cancel an order in C4, because FR06 and FR07 do not ask for it. If it is needed later, it is approved as a change, and it is a change of status, never a deletion (D-022).
- **Alternatives:** Adding editing and cancelling to UC06 and UC07 now.
- **Reason:** The requirements do not ask for it, and C4 is done in the same week as C5 (Milestone 3), so it only covers what FR06 and FR07 ask for.
- **Consequences:** C4 has no operations to edit or cancel an order. Section 9 of #65 is closed for this point.
- **Status:** Proposed by David in #65 on 07/11/2026 and agreed by Ignacio on 15/11/2026.

## D-040 · Scheduling needs generated by a follow-up order
**Date:** 15/11/2026 · **Iteration:** 4 · **Minutes:** — · **Issues:** #65

- **Context:** A periodic follow-up order (UC07) has a recurrence interval, and the system generates the future scheduling needs that the admissions staff book in C5 (UC13). FR07 does not say how many needs are generated. It was the second pending decision of section 9 of #65.
- **Decision:** A follow-up order generates the scheduling needs of the next 12 months, for example 2 if the interval is six months. They are generated when the order is created, without a timer, and they stay as pending needs in `getPendingSchedulingNeeds()`, so that the admissions staff book them in UC13. If the follow-up continues after that, the physician creates another order.
- **Alternatives:** Generating only the next need, and the following one when it is booked; or generating the needs with a timer.
- **Reason:** It is simple and predictable: all the needs exist from the moment the order is created, and no process has to run in the background.
- **Consequences:** UC07 creates the needs of the next 12 months, and UC13 and UC15 (C5) read them as pending needs. Section 9 of #65 is closed.
- **Status:** Proposed by David in #65 on 07/11/2026 and agreed by Ignacio on 15/11/2026.

## D-041 · Issues in progress, and one agent per use case at a time
**Date:** 17/11/2026 · **Iteration:** 4 · **Minutes:** — (lab session of 17/11/2026)

- **Context:** In the lab session of 17/11/2026 the professor asked us to move the issues to *In progress* when we start them, and to make sure that two agents never work on the same use case at the same time, so that one does not overwrite the work of the other. Rule 15 of AGENTS.md already forbids two agents on the same branch or folder, but nothing showed who was working on each use case.
- **Decision:**
  - Before starting a sub-issue, the member assigns it to themself and moves it to *In progress* on the project board. It stays there until the pull request that closes it is merged.
  - A use case issue or sub-issue that is *In progress* belongs to its assignee: nobody else, and no other agent, works on it. Only one agent works on each sub-issue at a time, and the server and the client of the same use case can be done at the same time only on their own branches (rule 15).
  - In step 1 of the cycle, the agent checks that the sub-issue is assigned to the member who asked for the work and is not being worked on by someone else. If it is not, it stops and tells the member.
- **Alternatives:** Telling each other in WhatsApp who is working on what, which is not recorded and is easy to miss.
- **Reason:** The board shows at any moment who is working on each use case, and an agent cannot start on work that is someone else's.
- **Consequences:** Extends rule 15 of AGENTS.md and D-010. Step 1 of the cycle in AGENTS.md is updated in this pull request.
- **Status:** Proposed by David on 17/11/2026, after the professor's comments in class; approved in the review of this pull request.

## D-042 · Every member keeps write access, and the feature branches cannot be deleted or rewritten
**Date:** 17/11/2026 · **Iteration:** 4 · **Minutes:** — (lab session of 17/11/2026)

- **Context:** In the lab session of 17/11/2026 the professor asked why every member has access to every branch. All members have the *write* role in the repository. The only ruleset (*Restrictions branch*) protects `main` and `develop` (no deletion, no force push, no creation and a pull request to change them, D-001), but the `feature/*` branches have no protection, so anyone could delete or rewrite the branch of another member.
- **Decision:**
  - Every member keeps the *write* role, because each one works on one branch per sub-issue (D-015), and GitHub needs that role to push to a branch of the repository. A member cannot be limited to their own branches without adding bypass lists per branch, and the bypass list must stay empty (D-001, D-038).
  - What protects the work of each member is the process, not the permissions: `main` and `develop` only change through an approved pull request (D-001), and each sub-issue has its assignee and only one agent (D-041).
  - The coordinator adds a second ruleset for `feature/**`, `docs/**`, `ci/**` and `release/**` that blocks deleting them and force-pushing to them, with an empty bypass list. It does not stop anyone from pushing, so it does not block the work.
- **Alternatives:** Each member working on their own fork and opening pull requests from it, so nobody needs write access. Rejected, because the issue branches are created from the issue on GitHub (D-015), the CI and the plans of `doc/plans/` would be split between forks, and two members often work on the same component. Giving the *read* role to the members who only review. Rejected, because every member also implements.
- **Reason:** Write access is needed with one branch per sub-issue, and the new ruleset stops the only real risk of that access: losing someone else's branch.
- **Consequences:** Rule 10 of AGENTS.md (no force push) is now also enforced by GitHub on the work branches. Branches are not deleted after merging, as rule 7 already says.
- **Status:** Proposed by David on 17/11/2026, after the professor's question in class; approved in the review of this pull request. The coordinator creates the ruleset, because only the owner of the repository can.

## D-043 · Recording the tokens used by the agents, per iteration
**Date:** 17/11/2026 · **Iteration:** 4 · **Minutes:** — (lab session of 17/11/2026)

- **Context:** In the lab session of 17/11/2026 the professor asked us to keep track of the tokens we use. D-026 estimated about 34.4 million input tokens and 1.6 million output tokens, but we never recorded the real use, so the estimate cannot be checked.
- **Decision:**
  - The wiki page *Token usage* has one section per member, with one row per iteration: the models used, the calls, the new input, the cached input and the output tokens (or the premium requests in GitHub Copilot, which keeps no local logs).
  - It is a rule of the agent (rule 19 of AGENTS.md): when an iteration ends, or when the member asks, the agent counts the tokens of that iteration from the logs of its tool, shows the row to the member and, after a "yes", adds it to the member's section. The iterations that share dates (It2 and It3, It4 and It5) share one row.
  - The totals are compared with the estimate of D-026 on the page *Planification*.
- **Alternatives:** Not recording it, as until now; one row per session in each plan of `doc/plans/`, which is more work and is spread over many files; recording it in the hours spreadsheet, which has no place for each tool and model.
- **Reason:** It shows the real cost of using AI in each iteration, and it lets us check the estimate of D-026, as the assignment asks. The agent can count it from its own logs, so it costs the member almost nothing.
- **Consequences:** Rule 19 is added to AGENTS.md in this pull request. The page *Token usage* was created with David's section on 17/11/2026, and each member adds their own section for the iterations already done.
- **Status:** Proposed by David on 17/11/2026, after the professor's comments in class; approved in the review of this pull request.

## D-044 · Requirements traceability matrix
**Date:** 17/11/2026 · **Iteration:** 4 · **Minutes:** — (lab session of 17/11/2026)

- **Context:** In the lab session of 17/11/2026 the professor asked for a requirements traceability matrix. The wiki page *Requirements and Use Cases* only says in which iteration and component each requirement is built, not which analysis, design, code, tests and release come from it.
- **Decision:**
  - The wiki page *Requirements traceability matrix* has one row per functional requirement already worked on: its use case and issue, the component, the analysis, the design, the server and client sub-issues with the pull requests that closed them, the control class and the screen with their tests, and the release.
  - It is a rule of the agent (rule 20 of AGENTS.md): when the pull request of a sub-issue is merged, or when the member asks, the agent fills in its cells (pull request, classes and tests), shows the change to the member and, after a "yes", updates the page. The coordinator fills in the release when the iteration is released.
- **Alternatives:** Keeping only the table of *Requirements and Use Cases*, which stops at the component; a matrix in a spreadsheet, which is not linked to the issues, the pull requests and the code.
- **Reason:** A requirement can be followed down to the code and the tests that prove it, and every class or test can be traced back to its requirement, with links that can be checked.
- **Consequences:** The page was created on 17/11/2026 with the six use cases of C1, C2 and C3, and the five of C4 and C5 in progress. Rule 20 is added to AGENTS.md in this pull request.
- **Status:** Proposed by David on 17/11/2026, after the professor's comments in class; approved in the review of this pull request.

## D-045 · Spending tokens without losing quality
**Date:** 18/11/2026 · **Iteration:** 4 · **Minutes:** — · **Issues:** —

- **Context:** D-043 records the tokens of the agents. The page *Token usage* (17/11/2026) shows that 96.9 % of the
  1,949 million tokens counted for five members are cached input: the conversation that the model reads again in
  every call, about 370,000 tokens per call on average (David 411,000, Adam 361,000, Javier 339,000, Denis 332,000,
  Rodrigo 188,000). Most of it comes from conversations that last days or weeks. AGENTS.md has no rule on the size of
  the context: a green `mvn -B verify` prints about 16,500 tokens, and DECISIONS.md is about 18,600, and both are read
  again in every later call of the same conversation.
- **Decision:**
  - Rule 21 of AGENTS.md: one task per conversation (a new one after each committed step, with the plan as the
    hand-over), read only what the task needs, and filter long output without hiding failures.
  - The file `agents/token-efficiency.md` has the practices for every agent, those only for Claude Code, and how to
    count the tokens of rule 19. AGENTS.md sends the agent there before its first build, before it delegates to a
    subagent and when it counts tokens, so it is not read in every session.
  - Quality wins: no practice skips a rule of AGENTS.md. `mvn -q` is not used, because it hides which coverage rule
    failed and the number of tests run (E-14).
- **Alternatives:** Everything in AGENTS.md, which adds about 1,900 tokens to every session. A Claude Code skill, which
  only Claude reads and which would put rules outside AGENTS.md (D-025). No rule, as until now.
- **Reason:** What costs most is the length of the conversations, not the files. With Spec-Driven Development the plan
  already carries the state between sessions (step 8), so a new conversation per step loses nothing that was written
  down.
- **Consequences:** AGENTS.md grows by about 250 tokens. D-025 says that rules are only written in AGENTS.md: the file
  in `agents/` holds practices that rule 21 points to, and if it disagrees with AGENTS.md, AGENTS.md wins. Its effect
  is checked by repeating a similar sub-issue before and after and comparing the rows of *Token usage* and the quality
  of the result. Extends D-025 and D-043.
- **Status:** Proposed by Adam on 18/11/2026, after the research of the page *Token usage*; approved in the review of this pull request.

## D-046 · Migration of UML diagrams from Visual Paradigm to PlantUML
**Date:** 22/11/2026 · **Iteration:** 4–5 · **Minutes:** [Meeting 9](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-9) · **Time in the recording:** 0:21:02–0:23:15 · **Issues:** #143, #177

- **Context:** D-008 and D-017 made Visual Paradigm 18.1 (`doc/ISO-Project.vpp`) the official tool for UML models. Because `.vpp` is binary, it cannot be merged and forced serial hand-offs across Requirements, Analysis and Design (#107). Round-tripping PlantUML into Visual Paradigm caused `_renamed_on_import_` collisions, duplicate packages (`common2`, `server2`) and corrupted signatures (#144, #145), and editing the XML export directly risked drifting diagram shapes from the model. On 16/11/2026 the professor confirmed in class that the team may stop using Visual Paradigm if we have good engineering reasons and the diagrams remain clear.
- **Decision:**
  - **Full migration to modular `.puml` files under `doc/`:** All official UML diagrams (Use Cases in `doc/use-cases/`, Analysis Class Diagram and Analysis interaction diagrams in `doc/analysis/`, and Design Class Diagrams in `doc/design/`) are maintained as plain-text PlantUML (`.puml`) files reviewed and approved in pull requests like code.
  - **Shared definitions (`!includesub`):** Actors (`doc/use-cases/_actors.puml`), styling (`doc/design/common/_style.puml`) and shared `common` domain/contract types (`doc/design/common/*.puml`) are defined once using `!startsub`/`!endsub` and included via `!includesub` so contracts cannot drift across components.
  - **Analysis interaction diagrams:** Migrated as native PlantUML Sequence diagrams using `actor`, `boundary`, `control` and `entity` participants.
  - **Rendered previews:** Existing `.png` previews in `doc/design/` (`C1`–`C5` client/server and `UC22-login-communication.png`) are left as-is so Wiki links (`It1`–`It3`) stay intact. `.puml` is the single source of truth; future edits and new iterations (`C6`–`C9`) regenerate `.png` previews directly from `.puml` using PlantUML CLI (`plantuml -tpng <file>.puml`) or IDE PlantUML extensions.
  - **Retirement of `doc/ISO-Project.vpp`:** `doc/ISO-Project.vpp` is removed from `develop` and preserved in Git history.
- **Alternatives:** Keeping Visual Paradigm for all diagrams; migrating only Design diagrams first; keeping `.puml` with Communication diagram emulation instead of Sequence diagrams; deleting existing `.png` previews immediately.
- **Reason:** Plain-text `.puml` files support parallel work, line-by-line PR reviews, zero round-trip corruption, direct readability by Spec-Driven Development agents (D-025), and single-source-of-truth contract reuse via `!includesub`. Human review and approval in PRs preserve the discipline ownership of D-008.
- **Consequences:** Replaces D-017, updates D-008 (official models are PlantUML in `doc/`, AI proposals still require human approval) and retires assumption A4 of D-031. `AGENTS.md`, `README.md`, `doc/design/README.md` and the wiki (*Traceability and releases*, *Requirements and Use Cases*, *Iterations*, *Home*, *Working with AI*, *Decisions*, *Planification*) are updated.
- **Status:** Approved in Meeting 9 (22/11/2026) and implemented in #177; pending pull request review by another member (D-001).

## D-047 · One context file per component, read first in step 1
**Date:** 23/11/2026 · **Iteration:** 4–5 · **Minutes:** — · **Issues:** —

- **Context:** Step 1 of the cycle in AGENTS.md makes the agent read, in every session, the sub-issue, its use case
  issue, section 6 of the component issue, the approved design and the decisions they cite. Since D-045 there is one
  conversation per step, so this reading is repeated at every step of every sub-issue of the same component. Each
  decision entry costs about 400 tokens and each design image 700–4,800 (`agents/token-efficiency.md`).
- **Decision:**
  - Each component has a short summary, `doc/plans/Cn-context.md`, made from `doc/plans/CONTEXT-TEMPLATE.md`
    (about 40 lines): the decisions that apply (number and one line), the contracts of section 6 of the component
    issue, the classes of the approved design, the status of each sub-issue, and links. Every line cites its source
    (rule 12).
  - In step 1 of the cycle the agent reads it first, and opens the sources only for what the task needs. If the file
    disagrees with a source, the source wins (rule 11) and the agent tells the member.
  - Every pull request of the component updates the file, but only the lines of its own sub-issue (for example, its
    status), so that parallel branches do not conflict in it. The member who merges the pull request checks that it did. `develop` only changes through a pull request (D-001), so the file
    is not updated after the merge.
  - `doc/plans/C4-context.md` is the first one, as an example.
- **Alternatives:** Reading all the sources in every session, as until now. Writing the summary in the component issue,
  which is not versioned with the code and is owned by its author. One summary of all the components in
  `agents/components.md`, which would grow with every component and be read whole every time.
- **Reason:** The agent starts each conversation from a short file instead of several long sources, and the sources
  stay the reference, so nothing is lost if the summary is out of date.
- **Consequences:** Step 1 of the cycle and the table *Where to find things* of AGENTS.md are updated in this pull
  request. A file that is not updated misleads the agent: that is why the source always wins and the merger checks it.
  The saving is checked as in D-045, by comparing the rows of *Token usage* before and after. Extends D-025 and D-045.
- **Status:** Proposed by David on 23/11/2026, outside a meeting; pending pull request review by another member (D-001).

## D-048 · Links to issues in pull requests, shared documents and handoff between Implementation and Testing
**Date:** 24/11/2026 · **Iteration:** 4–5 · **Minutes:** — · **Issues:** #215 · **Pull requests:** #212

- **Context:** The implementation and testing of C4 and C5 (#202 to #210) showed four problems. The pull request
  template starts with `Closes #`, so #202 and #204 closed #68 and #67 when they were merged, with *Implementation* and
  *Testing* not ticked. The parallel branches changed the same shared documents, and #210 conflicted with another branch in
  `agents/components.md`. Testing and Implementation needed several rounds on the same pull requests (#204, #207, #210).
  Implementation changed a test of Testing (E-23), and a build looked red because of the compiled tests of another branch (E-24).
- **Decision:**
  - **Links:** a pull request that belongs to an issue has `Refs #n`, and `Closes #n` only if it completes the last box
    of the RADIT checklist. A standalone `docs/` or `ci/` pull request may have no issue. The CI checks only the name
    of the branch (#212). The agent proposes ticking the box of each discipline that is merged (rule 22).
  - **Shared documents:** `AGENTS.md`, `DECISIONS.md`, `agents/*.md` and the templates of `.github/` change only in
    `docs/` branches (rule 23). The context file of a component (`doc/plans/Cn-context.md`, proposed in D-047) is not one of
    them: each use case branch changes only the lines of its own sub-issue. `docs/` and `ci/` are added to the table of
    branches.
  - **Handoff:** the section *Cross-agent handoff* of AGENTS.md, proposed by Marcos in #212.
- **Alternatives:** The CI also checks `Refs #n` or `Closes #n` (first version of #212). An orchestrator agent.
- **Reason:** An issue is closed when its work is finished, each shared document changes in one place, and the two
  disciplines hand over the work once, in writing.
- **Consequences:** Updates D-010 and D-015 (`Closes #n` is no longer in every pull request). D-010 still applies: every
  task assigned in a meeting has its issue. Rule 23 is compatible with the proposal D-047 (#213).
- **Status:** Proposed by Marcos (comment in #212) and by David on 24/11/2026, outside a meeting; pending pull request
  review by another member (D-001).
