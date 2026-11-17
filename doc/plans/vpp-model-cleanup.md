# Plan – Visual Paradigm model cleanup and C1–C3 alignment (#144)

| | |
|---|---|
| **Issue** | #144 *Clean up Visual Paradigm model and align C1–C3 design diagrams* · branch `feature/vpp-model-cleanup` |
| **Related proposal** | #143 *Propose migrating UML models from Visual Paradigm (.vpp) to PlantUML (.puml)* (for Meeting 9, 22/11/2026) |
| **Sources** | `doc/ISO-Project.vpp`, `doc/design/README.md`, `DECISIONS.md` (D-008, D-017, D-022, D-025, D-032, D-036, D-037), and Java sources in `common/`, `server/`, and `client/` |
| **Plan approved by** | Marcos Loro, 17/11/2026 |

## Questions and answers
- **Q1 – Separate Meeting 9 `.puml` migration proposal from immediate `.vpp` cleanup?**
  **A:** Yes: #143 puts the possible `.vpp` → `.puml` migration on the Meeting 9 agenda, while #144 fixes the current `.vpp` and C1–C3 design diagrams immediately before C4/C5 (Marcos, 17/11/2026).
- **Q2 – Scope of C1, C2, and C3 diagram alignment?**
  **A:** Full structural cleanup plus near-1:1 alignment with the implemented contracts, domain classes, services, gateways, and repositories, skipping internal Swing/UI layout helper details (Marcos, 17/11/2026).
- **Q3 – `IU_Welcome` and `Main` in `C1-client-design`?**
  **A:** Omit both `IU_Welcome` and `Main` from `C1-client-design` (removing the orphan `Main` left by PR #98) so C1 stays focused on UC22/UC18/UC19 like C2 and C3, and document them as skipped client-shell/navigation wiring in `doc/design/README.md` alongside C2's internal helper panels (Marcos, 17/11/2026).
- **Q4 – `AuditRecorderSimulator` in `C1-server-design`?**
  **A:** Remove `AuditRecorderSimulator` and its temporary UC22 note, since it was replaced by `AuditService` and removed from the codebase (Marcos, 17/11/2026).
- **Q5 – `.gitignore`, backup files, and XML export?**
  **A:** Add `*.bak_*` and `*.lck` to `.gitignore`, delete `doc/ISO-Project.vpp.bak_000f`, and keep XML export folders untracked/outside the repository after re-importing into Visual Paradigm 18.1 (Marcos, 17/11/2026).

## Files
| File | Action | What |
|---|---|---|
| `doc/plans/vpp-model-cleanup.md` | create | This plan and step-by-step progress log |
| `.gitignore` | change | Ignore Visual Paradigm backup (`*.bak_*`) and lock (`*.lck`) files |
| `doc/ISO-Project.vpp.bak_000f` | delete | Remove untracked VP backup file |
| `doc/xml_export/project.xml` (local only) | change, then remove outside repo | Clean `<Models>` and `<Diagrams>` for VP 18.1 blank-project import; never committed |
| `doc/ISO-Project.vpp` | change (Marcos via VP 18.1) | Re-imported clean project saved in VP 18.1 |
| `doc/design/C1-client-design.png`, `C1-server-design.png`, `C2-client-design.png`, `C2-server-design.png`, `C3-client-design.png`, `C3-server-design.png` | change (Marcos via VP 18.1) | Re-exported PNG diagrams from the cleaned `.vpp` |
| `doc/design/README.md` | change | Document skipped shell/internal UI details (`Main`, `IU_Welcome`, C2 helper panels) and remove resolved C2/C3 discrepancies |

## Target model and diagram alignment
- **Model Explorer cleanup (`project.xml`):**
  - Canonical `common` (`Domain (entities)` + `Contracts (interfaces)`), `server`, and `Presentation` packages shared across all diagrams.
  - Remove `common2`, `server2`, `Domain` (duplicate), `Domain_renamed_on_import_*`, `Presentation_renamed_on_import_*`, `UserAccount_renamed_on_import_*`, `Class` (`vo3F7oWFS_lqMgr0`), `From C1` (`RzYQq4WGAqBwARCq`), `AuditRecorderSimulator`, and orphan `Main`.
  - Strip `_renamed_on_import_20261112_134055` from the 4 Analysis classes (`SystemParameter`, `IU_ConfigureParameters`, `IntegrationConfiguration`, `IU_ManageIntegrations`) so `Analysis Class Diagram` and C2 Communication `TransitTo` links stay intact; trim leading spaces from `AccessDeniedException`.
  - Convert C2's mangled PlantUML `"op: Type"` / `"Type : param:"` strings into properly typed VP attributes, operations, and parameters; keep Use Case diagrams (`General_UC`, `Component_1`–`Component_4`) unchanged.
- **C1 Client (`C1-client-design.png`):** `Presentation` (`IU_Login`, `IU_ManageRoles`, `IU_Audit`) + `common` (`UserAccount`, `Role`, `Permission`, `AuditLog`; `IAuthenticationService`, `IRoleManagementService` with `String requester` first and `hasPermission`, `IAuditService.getActivityLog(String, String)`, `AuthenticationException`, `AuthenticationFailureReason`, `AccessDeniedException`); resize boxes so no signatures are clipped.
- **C1 Server (`C1-server-design.png`):** `common` (same Domain + Contracts + `IAuditRecorder` and `IAuthenticatorSystem` with `validateCredentials` and `ping()` per D-036) + `server` (`AuthenticationService`, `RoleManagementService`, `AuditService`, `AuthenticatorSystemSimulator`, `IUserAccountRepository`, `IRoleRepository`, `IPermissionRepository`, `IAuditLogRepository`, and all 4 `InMemory*Repository` classes).
- **C2 Client (`C2-client-design.png`):** `Presentation` (`IU_ConfigureParameters`, `IU_ManageIntegrations`) + `common` (`TestType`, `Protocol`, `Schedule`, `AppointmentPolicy`, `Priority`, `HospitalSystemType`, `IntegrationMode`, `IntegrationStatus`, `IntegrationConfiguration`, `ConnectionTestResult`; `ISystemParameterService`, `IIntegrationManagementService`, `ValidationException`, `AccessDeniedException`).
- **C2 Server (`C2-server-design.png`):** `common` (same C2 Domain + Contracts + `IAuditRecorder`, `IAuthenticatorSystem`, `IRoleManagementService`, `ValidationException`, `AccessDeniedException`) + `server` (`SystemParameterService`, `IntegrationManagementService`, `ConfigurationSeedData`, `IConfigurationRepository<T>`, `InMemoryConfigurationRepository<T>`, `IEhrGateway`/`EhrGatewaySimulator`, `IAdmissionsGateway`/`AdmissionsGatewaySimulator`, `IRisPacsGateway`/`RisPacsGatewaySimulator`); fix backwards `Realization` arrows (`From` = interface, `To` = implementation), resize clipped `80x40` notes, and compact the `3350px` layout.
- **C3 Client (`C3-client-design.png`):** `Presentation` (`IU_ManagePatients`) + `common` (`Patient` with `nationalId`, `Sex`; `IPatientService`, `DuplicatePatientException`, `ValidationException`, `AccessDeniedException`).
- **C3 Server (`C3-server-design.png`):** single `common` box (same C3 Domain + Contracts + `IAuditRecorder`, `IRoleManagementService`, `ValidationException`, `AccessDeniedException`) + `server` (`PatientService` with `loadSeedData()`, `PatientSeedData`, `IPatientRepository`, `InMemoryPatientRepository`).

## Steps
Tick each step with the hash of its commit (`mvn -B verify` green).
- [x] 1. Create and commit this plan (`docs(design): plan for Visual Paradigm model cleanup (#144)`). - 1a71ca7.
- [x] 2. Update `.gitignore` to ignore `*.bak_*` and `*.lck`, and delete `doc/ISO-Project.vpp.bak_000f`. - be2ede2.
- [x] 3. Transform `doc/xml_export/project.xml` (untracked local export) to clean `<Models>` and `<Diagrams>` across Analysis and C1–C3, and run Python structural/reference integrity checks. - verified locally (untracked XML export).
- [ ] 4. **Manual VP 18.1 round-trip (Marcos):** import `doc/xml_export/project.xml` into a new blank Visual Paradigm 18.1 project, visually check all diagrams, save `doc/ISO-Project.vpp`, re-export the 6 design PNGs into `doc/design/`, and remove/move `doc/xml_export/` outside the repo.
- [ ] 5. Update `doc/design/README.md` with the aligned skipped-details notes, commit `doc/ISO-Project.vpp`, `doc/design/*.png`, and `doc/design/README.md`, and run final `mvn -B verify`.

## Verification
- [x] Python integrity check passes on `doc/xml_export/project.xml` (0 `_renamed_on_import_`, 62 unique Design classes with 0 duplicate names in design packages, 0 non-viewpoint dangling IDs, 17/17 realization arrows `From = Interface, To = Impl`, all 37 non-design diagrams preserved, 0 box overlaps across all 6 design diagrams).
- [ ] `doc/ISO-Project.vpp` opens cleanly in Visual Paradigm 18.1 with all 43 diagrams intact and legible PNGs exported to `doc/design/`.
- [ ] `doc/xml_export/` and `doc/ISO-Project.vpp.bak_000f` are absent from Git and working tree.
- [ ] `mvn -B verify` is green across all modules.

## Status
- **In progress:** Step 4 (manual Visual Paradigm 18.1 import and PNG export by Marcos)
- **Next step:** Import `doc/xml_export/project.xml` into a new blank Visual Paradigm 18.1 project, inspect all diagrams, save `doc/ISO-Project.vpp`, export the 6 design PNGs into `doc/design/`, and move/delete `doc/xml_export/` outside the repository before running Step 5
- **Open questions:** none
- **Last update:** 17/11/2026, Marcos Loro
