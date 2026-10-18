# DECISIONS

Log of the project's decisions: requirements, analysis, design and architecture decisions, assumptions, changes to the plan, and AI tools and models. One entry per decision, written when the decision is taken and linked to the meeting minutes where it was agreed. Code details are not logged here; they are in the commits. Team organisation is on the wiki page *Team and roles*, and work assignments are GitHub issues.

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
- **Decision:** The project has 22 functional requirements: 21 from the specification and RF22, *Log in through the corporate SSO*, added by the team. Denis's table is the base.
- **Alternatives:** Ignacio's first draft with 14 requirements (incomplete). A pre-appointment form where patients describe their problem, proposed by Denis, was not added.
- **Reason:** Users must be identified to apply the permissions of their role, and no bullet point covers the login.
- **Consequences:** 22 use cases (1 functional requirement = 1 use case). The table is on the wiki page *Requirements and Use Cases*.

## D-003 · The exam catalogue is not a separate requirement
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 1:19:16–1:21:12

- **Context:** The specification describes a catalogue of examination types in its own section, not as a bullet point of a user role.
- **Decision:** The catalogue is managed through "test types" in RF17 (Configure system parameters), and it is preloaded in the system as seed data.
- **Alternatives:** Adding the catalogue as a separate functional requirement.
- **Reason:** Functional requirements are the bullet points under each user role.
- **Consequences:** Seed data with the examination types is needed before procedures can be ordered (RF06) or booked (RF13).

## D-004 · Actor inheritance in the use case diagram
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 0:37:54–0:39:05

- **Context:** The specification says physicians have all the patient features that apply to them, and every user has to log in.
- **Decision:** The *Physician* actor inherits from *Patient*. A general *User* actor, inherited by the four roles, holds the login use case.
- **Alternatives:** None discussed.
- **Reason:** Avoid repeating the same use cases for several actors.
- **Consequences:** Patient use cases are developed before physician use cases.

## D-005 · Use case priorities
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 0:23:18–1:11:09

- **Context:** Use cases must be prioritised and assigned to phases.
- **Decision:**
  - **Priority 1:** RF17, RF18, RF19, RF20, RF22
  - **Priority 2:** RF01, RF04, RF12, RF13, RF15
  - **Priority 3:** RF02, RF03, RF05, RF06, RF07, RF08, RF09, RF10, RF14
  - **Priority 4:** RF11, RF16, RF21
- **Alternatives:** Several priorities were discussed one by one. RF05 and RF07 were aligned to the same priority.
- **Reason:** Build the base of the system first (configuration, roles, integrations and login), then the functions that depend on it. Statistics and reports only read existing data, so they go last.
- **Consequences:** The priorities define the iterations (see D-007).

## D-006 · AI tool: GitHub Copilot
**Date:** 11/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 1](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-1) · **Time in the recording:** 1:12:11–1:14:41

- **Context:** The course allows AI agents to support software engineering tasks.
- **Decision:** The team uses GitHub Copilot in Visual Studio Code. For analysis and design, Copilot drafts the diagrams in PlantUML; they are brought into Visual Paradigm and the team reviews and approves them (see D-008). Copilot is also asked to estimate the cost of each use case in tokens/€, to compare it with our own estimates.
- **Alternatives:** Other agents allowed by the specification (OpenCode, Antigravity, Claude Code).
- **Reason:** It is the reference environment recommended by the specification, and it is free with the UCLM student licence.
- **Consequences:** The cost of AI models for the team is €0. *The agent suggests; we decide.*

## D-007 · Iteration N = use cases with priority N
**Date:** 13/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 2](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-2) · **Time in the recording:** 0:05:42, 0:28:21–0:34:24

- **Context:** The kickoff suggests 1 use case = 1 iteration, and the team asked whether use cases with the same priority can share an iteration.
- **Decision:** Each iteration develops all the use cases of one priority level. Iteration 1 covers RF17, RF18, RF19, RF20 and RF22. Within an iteration, the disciplines are done in order (requirements → analysis → design → implementation → testing), each one by its responsible member (see the wiki page *Team and roles*). The next iteration starts only when every discipline of the current one is finished, and its use cases are not prepared in advance.
- **Alternatives:** Strictly 1 use case = 1 iteration. Members helping in other disciplines when they finish their own work.
- **Reason:** Use cases with the same priority belong to the same level of the system and can be developed together. Working in order keeps each member's responsibility clear.
- **Consequences:** Four construction iterations, plus Iteration 0 and the transition iteration (ItN). Later disciplines wait at the start of each iteration, and this waiting time must be reflected in the plan. The length of each iteration is still to be decided. This change to the kickoff's simplification must be justified in the theory report.

## D-008 · AI-generated diagrams are only proposals
**Date:** 13/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 2](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-2) · **Time in the recording:** 0:13:44–0:15:52

- **Context:** A use case diagram of all the requirements was generated with an AI agent before the session planned for it.
- **Decision:** Any diagram generated by an AI agent is only a proposal or a reference. The official diagrams (use case, analysis and design) are made and approved in Visual Paradigm by the members responsible for each discipline. The AI-generated diagram of all the requirements is kept only as a prototype, to get an overview of the system.
- **Alternatives:** Adopting AI-generated diagrams directly as the official ones.
- **Reason:** Requirements and use cases are a human task in this course ("do it yourselves, not the AI agent"), and each discipline has a responsible member.
- **Consequences:** AI output is always reviewed by the team before it becomes official. So far, PlantUML code has to be copied into Visual Paradigm by hand.

## D-009 · Login scope
**Date:** 13/10/2026 · **Iteration:** 0 · **Minutes:** [Meeting 2](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/wiki/Meeting-2) · **Time in the recording:** 0:17:34, 0:23:20–0:24:19

- **Context:** Alternative scenarios were discussed for RF22 (Log in through the corporate SSO).
- **Decision:** The login checks the credentials against the external SSO system. *Recover password* is an extension point. There is no sign-up.
- **Alternatives:** A sign-up option for new users.
- **Reason:** Sign-up is not required by the specification.
- **Consequences:** These alternative scenarios go into the description of the login use case.
