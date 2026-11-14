# Plan – UC20 – Server (#43)

| | |
|---|---|
| **Sub-issue** | #43 *UC20 – Server (C2 v1.1.0)* · branch `feature/UC20-server` (brought to `develop`, which already has UC17) |
| **Use case** | #41 *UC20 – Configure integrations with other hospital systems (C2 v1.1.0)* · FR20 · component C2 1.1.0 |
| **Patch of C1** | permission `CONFIGURE_INTEGRATIONS`, `IAuthenticatorSystem` in `common` and `hasPermission` in `IRoleManagementService` (steps 2, 3 and 7), in its own patch issue #112 *Patch of C1: permission CONFIGURE_INTEGRATIONS, IAuthenticatorSystem and hasPermission in common (C1 v1.2.2)*, C1 `1.2.2-SNAPSHOT` (Q14) |
| **Specification** | use case issue #41, sub-issue #43 and the wiki page *Requirements and Use Cases* |
| **Design** | `doc/design/C2-server-design.png` · analysis `doc/AN-C2-C3-DECISIONS.md` (D17 to D23) · contracts: section 6 of #33 *C2. Configuration and integrations* |
| **Plan approved by** | pending (Javier) |

## Decisions already taken
- The four integrations are SSO, electronic health record (EHR), admissions and RIS/PACS. Everything is simulated (A5); there is no real adapter.
- `requester` is the username as a `String`, as in C1 and UC17. Every operation (also the reads and the test) needs the permission and an active user.
- The connection test never changes the stored settings and is not audited (D18).
- Analysis D23: the gateways `getPatientRecord`, `getAdmission` and `getStudy` of #33 are not part of UC20; they only have `ping()` (the design).
- The tests are Adam's (Testing): there is no step to write them here, only one to hand the branch over.
- Waiting for UC17 to be merged into `develop` before starting was decided by Javier on 14/11/2026 (it is merged: PR #110 and #111).

## Questions and answers
Each one has a proposal and all of them are answered.
- **Q1:** Base of the branch. **Proposal:** `feature/UC20-server` = `develop` (fast-forward). **A:** done, after waiting for the merge of UC17 (Javier, 14/11/2026)
- **Q2:** Permission. **Proposal:** a new permission `CONFIGURE_INTEGRATIONS`, apart from `CONFIGURE_SYSTEM`, given to the system administrator, in C1 (`InMemoryPermissionRepository`, `InMemoryRoleRepository`). **A:** approved (Marcos in #43; Ignacio told Javier that it is fine; Javier, 14/11/2026)
- **Q3:** The SSO test needs `ping()`, but `IAuthenticatorSystem` is in `server-c1-access` and C2 cannot depend on that module. **Proposal:** move `IAuthenticatorSystem` to `common` (`es.uclm.esi.iso2.common.access.contracts`, as `IAuditRecorder`) and add `boolean ping()` to it and to `AuthenticatorSystemSimulator` (returns `true`). The imports change in `AuthenticationService`, the simulator and the test double `SpyAuthenticatorSystem`. The real method of the interface is `validateCredentials`, not `authenticate` as the design says (the code wins, rule 11). **A:** approved, option A, package `common.access.contracts` (Javier, 14/11/2026)
- **Q4:** Contract `IIntegrationManagementService` in `common.configuration.contracts`, as in the design: `getIntegrations(requester)`, `getIntegration(requester, system)`, `configureIntegration(requester, configuration)` and `testConnection(requester, system)`. **Proposal:** `requester` first in all, also in the reads and the test (the design has none in `testConnection`); `setIntegrationMode` of #33 is not added, because the mode and `enabled` are inside the configuration. **A:** approved (Javier, 14/11/2026)
- **Q5:** Data in `common.configuration.domain`: enums `HospitalSystemType` (`SSO`, `EHR`, `ADMISSIONS`, `RIS_PACS`), `IntegrationMode` (`SIMULATED`, `REAL`), `IntegrationStatus` (`ACTIVE`, `INACTIVE`, `ERROR`); classes `IntegrationConfiguration` (`system`, `endpoint`, `settings` as `Map<String, String>`, `enabled`, `mode`, `status`) and `ConnectionTestResult` (`success`, `responseTimeMs`, `message`). **Proposal:** the server sets `status` (`ACTIVE` if enabled, `INACTIVE` if not) and ignores the one that arrives; `ERROR` is never stored. **A:** approved (Javier, 14/11/2026)
- **Q6:** Gateways. **Proposal:** `IEhrGateway`, `IAdmissionsGateway` and `IRisPacsGateway`, each with `boolean ping()`, and their simulators `EhrGatewaySimulator`, `AdmissionsGatewaySimulator` and `RisPacsGatewaySimulator` (`ping()` returns `true`), in the new package `es.uclm.esi.iso2.server.configuration.integration` of `server-c2-configuration`. They move to `common` when another component needs them. **A:** approved, with the new package (Javier, 14/11/2026)
- **Q7:** What the test returns. **Proposal:** integration disabled → `success = false`, "integration disabled"; mode `REAL` → `success = false`, "real mode not available in this version"; mode `SIMULATED` and enabled → the gateway answers (`success = true` and the time measured with the clock, "Connection OK"; if the gateway answers `false`, "The system did not answer"). No time limit, everything is simulated. A call without the permission is registered as denied. **A:** approved (Javier, 14/11/2026). **Open detail Q7b:** when the integration is disabled **and** in real mode, which message comes first? **Proposal:** "integration disabled" (it is checked first). **A:** approved as proposed (Javier, 14/11/2026)
- **Q8:** Validation of `configureIntegration`. **Proposal:** the endpoint is required in mode `REAL`; if it is not empty, in any mode, it must start with `http://` or `https://`; in the settings no key and no value may be empty; the system and the mode are required. All the errors come together in one `ValidationException`, and if there is any, nothing is saved. **A:** approved (Javier, 14/11/2026; David in #42)
- **Q9:** Audit (C1 contract `register(author, action, targetResource)`). **Proposal:** `INTEGRATION_CHANGED` when a configuration is saved, `INTEGRATION_ACCESS_DENIED` when a user without the permission tries any operation, with the resource `INTEGRATION:<SYSTEM>` (for example `INTEGRATION:SSO`); the test is not audited (D18). **A:** approved (Javier, 14/11/2026). **Open detail Q9b:** for `getIntegrations`, which has no system, the resource of a denied access would be `INTEGRATION`. **Proposal:** that. **A:** approved as proposed (Javier, 14/11/2026)
- **Q10:** Start-up data. **Proposal:** `loadSeedData()` in the class only (not in the contract), called by `Main` as in UC17; it creates the four integrations simulated, enabled, with an empty endpoint and no settings, only if there are none. **A:** approved (Javier, 14/11/2026)
- **Q11:** Versions. **Proposal:** `server-c2-configuration` goes to `1.1.0-SNAPSHOT` (UC20 = C2 1.1.0) in the module and in the managed version of the parent pom; `server-c1-access` goes to `1.2.2-SNAPSHOT` (Q14); `common` is not changed. **A:** approved for C2; for C1 see Q14 (Javier, 14/11/2026)
- **Q12:** Time and order. **Proposal:** `responseTimeMs` is measured with the clock around `ping()`; `getIntegrations()` always returns SSO, EHR, admissions, RIS/PACS (the order of `HospitalSystemType`). **A:** approved (Javier, 14/11/2026)
- **Q13:** How to check the permission in the new service. The check of UC17 (`checkPermission` and `canConfigure` in `SystemParameterService`) is private; Javier does not want to copy it nor to change UC17. **Proposal:** add to `IRoleManagementService` (C1, `common`) a `default` method `boolean hasPermission(String username, String permissionCode)` that is true if the user is in `getUsers()`, is active and has the permission in `getPermissions(username)`, the same rule as UC17. As it is a `default` method, the three classes that implement the interface (`RoleManagementService` and the two `FakeRoleManagementService` of the tests) compile with no change. The new service calls it and registers the denied access itself. `SystemParameterService` is not touched: UC17 keeps its own check until someone moves it, in another sub-issue. **A:** option C approved (Javier, 14/11/2026)
- **Q14:** Where is the change of C1 registered, if #109 *Patch of C1: permission CONFIGURE_SYSTEM and IAuditRecorder in common (C1 v1.2.1)* is closed? **Proposal:** a new patch issue, as #109 was: *Patch of C1: permission CONFIGURE_INTEGRATIONS, IAuthenticatorSystem and hasPermission in common (C1 v1.2.2)*; `server-c1-access` goes to `1.2.2-SNAPSHOT` in its `pom.xml` and in the managed version of the parent pom; the commits of C1 cite that issue and #43. **A:** option 3 approved (Javier, 14/11/2026). Created by Claude on 14/11/2026 with the text approved by Javier: #112 (assigned to JavierVilla5). The commits of steps 2, 3 and 7 cite #112 and #43.
- **Q15:** Two tests of C1 of Adam check the exact list of permissions of the administrator and will fail when `CONFIGURE_INTEGRATIONS` is added: `InMemoryPermissionRepositoryTest` ("findAll returns MANAGE_ROLES and CONFIGURE_SYSTEM") and `InMemoryRoleRepositoryTest` (the row `SYSTEM_ADMINISTRATOR, MANAGE_ROLES;CONFIGURE_SYSTEM`). Changing an expected value is a decision (rule 17). **Proposal:** I update only those expected values to add the new permission (as in step 3 of UC17), and nothing else; I tell Adam. **A:** approved as proposed (Javier, 14/11/2026)
- **Q16:** The test double `SpyAuthenticatorSystem` (Adam) must implement `ping()` once the interface has it. Javier approved only the import and the new method. **Proposal:** `ping()` returns what the wrapped system returns (`delegate.ping()`, `true` with the simulator), without counting calls. **A:** approved as proposed (Javier, 14/11/2026)
- **Q17:** Repository. The design has its own `IIntegrationConfigurationRepository`. **Proposal:** reuse the generic `IConfigurationRepository<T>` and `InMemoryConfigurationRepository<T>` of UC17 (key `system`), as it was done for the five parameter groups, and sort by `HospitalSystemType` in the service. **A:** approved as proposed (Javier, 14/11/2026)

## Files
| File | Action | What |
|---|---|---|
| `server/c1-access/.../access/integration/IAuthenticatorSystem.java` → `common/.../common/access/contracts/IAuthenticatorSystem.java` | move (`git mv`) | adds `boolean ping()` (Q3) |
| `server/c1-access/.../integration/AuthenticatorSystemSimulator.java` | change | the import and `ping()` returning `true` |
| `server/c1-access/.../service/AuthenticationService.java` | change | the import |
| `server/c1-access/src/test/.../service/SpyAuthenticatorSystem.java` | change | the import and `ping()` (Q16) |
| `common/.../common/access/contracts/IRoleManagementService.java` | change | the `default` method `hasPermission` (Q13) |
| `server/c1-access/.../persistence/InMemoryPermissionRepository.java` | change | the permission `CONFIGURE_INTEGRATIONS` (Q2) |
| `server/c1-access/.../persistence/InMemoryRoleRepository.java` | change | the administrator role gets it |
| `server/c1-access/src/test/.../InMemoryPermissionRepositoryTest.java`, `InMemoryRoleRepositoryTest.java` | change | only the expected permissions (Q15) |
| `common/.../common/configuration/domain/` `HospitalSystemType`, `IntegrationMode`, `IntegrationStatus`, `IntegrationConfiguration`, `ConnectionTestResult` | create | Q5 |
| `common/.../common/configuration/contracts/IIntegrationManagementService.java` | create | Q4 |
| `pom.xml`, `server/c2-configuration/pom.xml`, `server/c1-access/pom.xml` | change | `server-c2-configuration` `1.1.0-SNAPSHOT` (Q11) and `server-c1-access` `1.2.2-SNAPSHOT` (Q14) |
| `server/c2-configuration/.../server/configuration/integration/` `IEhrGateway`, `IAdmissionsGateway`, `IRisPacsGateway` and the three simulators | create | Q6; **new Java package** |
| `server/c2-configuration/.../server/configuration/service/IntegrationManagementService.java` | create | permission (`hasPermission`), validation, configure, test, audit, `loadSeedData()` |
| `agents/architecture.md` | change | the new classes and the interface in `common` |
| `doc/plans/UC20-server.md` | create/update | this plan |

`server-c2-configuration` depends only on `common`: the SSO is reached through `IAuthenticatorSystem` in `common`, and `Main` (in the client, UC20-client) gives the service the same simulator that C1 uses. No file of the client changes in this sub-issue.

## Steps
Tick each step with the hash of its commit (green `mvn -B verify -Djacoco.skip=true` until Adam's tests exist; no test is written here).
- [x] 1. This plan (`docs(UC20): plan for the server part (#43)`) - 84209c0.
- [ ] 2. Version `1.2.2-SNAPSHOT` of `server-c1-access` (Q14); move `IAuthenticatorSystem` to `common` with `ping()` and update the imports, the simulator and the test double (Q3, Q16). The 233 tests of C1 and the ones of Adam pass.
- [ ] 3. Permission `CONFIGURE_INTEGRATIONS` for the administrator and the two expected lists of the tests (Q2, Q15).
- [ ] 4. The enums and the data classes of the integrations in `common` (Q5).
- [ ] 5. The contract `IIntegrationManagementService` in `common` (Q4).
- [ ] 6. Version `1.1.0-SNAPSHOT` of `server-c2-configuration` and the gateways with their simulators (Q6, Q11).
- [ ] 7. `hasPermission` as a `default` method of `IRoleManagementService` (Q13); the tests of C1, the ones of UC17 and the fakes compile with no change.
- [ ] 8. `IntegrationManagementService`: permission with `hasPermission`, `getIntegrations`, `getIntegration`, validation, `configureIntegration` and audit (Q8, Q9, Q12, Q17).
- [ ] 9. `testConnection` and `loadSeedData()` (Q7, Q10).
- [ ] 10. `agents/architecture.md`.
- [ ] 11. Hand the branch over to Adam (tests of UC20-server), who opens the pull request (`Closes #43`); close only with his tests green and the coverage check passing.

## Verification
- [ ] Every acceptance criterion of #43 is checked: each external system is accessed through its own interface with a simulated implementation; changing the settings or the mode is checked against the permissions, validated and registered in the audit log; the interfaces match #33 as changed in Q4.
- [ ] `server-c2-configuration` depends only on `common`.
- [ ] No class of C1 changes except the ones of steps 2, 3 and 7 (and the poms), `SystemParameterService` of UC17 is not changed, and the login keeps working with the simulated SSO.
- [ ] `mvn -B verify` is green with Adam's tests (the coverage check included).
- [ ] The CI of the pull request is green.

## Status
- **In progress:** nothing (this plan waits for the approval of Javier)
- **Next step:** Javier approves the plan; then step 2 (version `1.2.2-SNAPSHOT` of `server-c1-access` and `IAuthenticatorSystem` to `common`)
- **Open questions:** none
- **Last update:** 14/11/2026, Javier
