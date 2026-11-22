# Plan – Migrate UML diagrams from Visual Paradigm to PlantUML (#177)

| | |
|---|---|
| **Issue** | #177 *Migrate the UML diagrams from Visual Paradigm to PlantUML* · branch `feature/plantuml-migration` |
| **Related proposal** | #143 *Propose migrating UML models from Visual Paradigm (.vpp) to PlantUML (.puml)* (approved in Meeting 9, 22/11/2026) |
| **Sources** | `doc/xml_export/project.xml` (local VP 18.1 export), `doc/design/UC22-login-communication.png`, `doc/AN-IT1-DECISIONS.md`, `DECISIONS.md` (D-008, D-017, D-025, D-031, D-045), `AGENTS.md`, `README.md`, `doc/design/README.md`, Java sources in `common/`, `server/`, `client/` |
| **Plan approved by** | Marcos Loro, 22/11/2026 |

## Questions and answers
- **Q1 – Scope of the migration (#177 open point 1)?**
  **A:** Full migration of all 28 UML diagrams (6 Use Case diagrams, 1 Analysis Class Diagram, 11 Analysis Interaction diagrams, and 10 C1–C5 Design Class Diagrams) into modular `.puml` files with shared `!include` definitions (Marcos, 22/11/2026).
- **Q2 – Representation of Analysis interaction diagrams (#177 open point 2)?**
  **A:** Native PlantUML Sequence diagrams with `actor`/`boundary`/`control`/`entity` participants (Marcos, 22/11/2026).
- **Q3 – Rendered previews (#177 open point 3)?**
  **A:** Leave existing `.png` previews in `doc/design/` untouched so Wiki `It1`–`It3` embeds stay intact with zero churn; `.puml` is now the single source of truth, and future edits or new iterations (`C6`–`C9`) generate updated `.png` previews directly from `.puml` (Marcos, 22/11/2026).
- **Q4 – Accuracy reconciliation & `doc/ISO-Project.vpp` retirement?**
  **A:** Transcribe `project.xml` faithfully to PlantUML, repairing only broken XML (`UC22` Login corruption + normalize legacy `User_Account`/`Auditory_Register` to `UserAccount`/`AuditLog` per `AN-IT1-DECISIONS.md` D08, 3 minor C4/C5 typos, and `C4` package nesting — no content changes). Do **not** align C1–C3 design diagrams to Java code: list every difference in the PR description for owners to decide. Leave `#178` and `Component_2` (`Hospital System`) to their owners. Delete `doc/ISO-Project.vpp` (preserved in Git history) as agreed in Meeting 9 (Marcos, 22/11/2026).

## Files
| File | Action | What |
|---|---|---|
| `doc/plans/plantuml-migration.md` | create | This plan and step-by-step progress log |
| `doc/use-cases/_actors.puml` | create | Shared actor declarations (`!startsub`/`!endsub`) |
| `doc/use-cases/general-uc.puml`, `C1-use-cases.puml`..`C5-use-cases.puml` | create | 6 Use Case diagrams migrated from `project.xml` |
| `doc/analysis/analysis-class-diagram.puml` | create | 40-class BCE Analysis Class Diagram migrated from `project.xml` |
| `doc/analysis/UC22-login-sequence.puml`..`UC14-daily-schedule-sequence.puml` | create | 11 Analysis Sequence diagrams migrated from `project.xml` (UC22 reconstructed from PNG + D08) |
| `doc/design/common/_style.puml`, `shared-contracts.puml`, `C1-common.puml`..`C5-common.puml` | create | Shared `!include` definitions (single source of truth for `common` domain & contracts) |
| `doc/design/C1-client-design.puml`..`C5-server-design.puml` | create | 10 Design Class Diagrams (Client & Server for C1–C5; C4 package nesting repaired) |
| `doc/ISO-Project.vpp` | delete | Retired Visual Paradigm model (kept in Git history) |
| `doc/ISO-Project.vpp.bak_000f`, `doc/xml_export/` | remove (local untracked) | Clean working tree before final verification |
| `DECISIONS.md` | change | Add D-046 and `Status:` notes on D-008, D-017, D-031 (A4) |
| `AGENTS.md`, `README.md`, `doc/design/README.md` | change | Update diagram rules, directory structure, and `.puml` rendering/single-source-of-truth workflow |

## Steps
Tick each step with the hash of its commit (`mvn -B verify` green).
- [x] 1. Create and commit this plan (`docs(migration): plan for Visual Paradigm to PlantUML migration (#177)`). - `9a8ca99`.
- [x] 2. Migrate the 6 Use Case diagrams + `_actors.puml` to `doc/use-cases/` and verify against `project.xml`. - `bccf6aa`.
- [ ] 3. Migrate the Analysis Class Diagram + 11 Interaction diagrams (as PlantUML Sequence diagrams) to `doc/analysis/` and verify against `project.xml` + `UC22-login-communication.png`.
- [ ] 4. Migrate the 10 C1–C5 Design Class Diagrams + modular `doc/design/common/` `!include` files to `doc/design/` and verify against `project.xml`.
- [ ] 5. Run PlantUML syntax validation across all 28 `.puml` files, run automated C1–C3 Design-vs-Code diff audit (for PR description), update governance (`DECISIONS.md` D-046, `AGENTS.md`, `README.md`, `doc/design/README.md`), retire `doc/ISO-Project.vpp`, remove local `doc/xml_export/` & `doc/ISO-Project.vpp.bak_000f`, and prepare proposed Wiki updates.

## Verification
- [ ] All 28 UML diagrams from `project.xml` are present as `.puml` files and pass structural completeness verification.
- [ ] All 28 `.puml` files pass PlantUML syntax check with 0 errors and 0 broken `!include`s.
- [ ] Existing `doc/design/*.png` previews remain untouched.
- [ ] `doc/ISO-Project.vpp`, `doc/ISO-Project.vpp.bak_000f`, and `doc/xml_export/` are absent from working tree and Git status.
- [ ] Automated C1–C3 Design-vs-Code discrepancy table and XML repair log are generated for the PR description.
- [ ] `mvn -B verify` is green across all modules.

## Status
- **In progress:** Step 3 (migrating Analysis Class Diagram and 11 Sequence diagrams to `doc/analysis/`)
- **Next step:** Step 4 (`doc/design/common/*.puml` and `doc/design/C{1..5}-{client,server}-design.puml`)
- **Open questions:** none
- **Last update:** 22/11/2026, Marcos Loro
