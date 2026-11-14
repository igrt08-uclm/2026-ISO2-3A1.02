# Plan – UC17 – Server (#40)

| | |
|---|---|
| **Sub-issue** | #40 *UC17 – Server (C2 v1.0.0)* · branch `feature/UC17-server` |
| **Use case** | #38 *UC17 – Configure system parameters and exam catalogue (C2 v1.0.0)* · FR17 · component C2 1.0.0 |
| **Specification** | use case issue #38 and the wiki page *Requirements and Use Cases* |
| **Design** | `doc/design/C2-server-design.png`, `doc/AN-C2-C3-DECISIONS.md` · contracts: section 6 of #33 *C2. Configuration and integrations* |
| **Plan approved by** | Javier, 14/11/2026 |

## Decisions already taken (comments in #40, 13–14/11/2026)
- `requester` is the username as a `String`, as in C1 (Marcos).
- The permission `CONFIGURE_SYSTEM` is added in C1, and `IAuditRecorder` moves from `server-c1-access` to `common`, so that C2 does not depend on the module of C1 (Ignacio, owner of C1; Javier).
- Seed data (D-003): 6 test types (X-ray, CT, MRI, ultrasound, mammography, bone densitometry) with every field of FR17 filled, the 3 priorities (urgent, preferential, routine) and one example of protocol, schedule and appointment policy; all of them invented data (David).
- The contracts of #33 are the ones that count; Design will change the diagrams to follow them (Marcos). The reads follow what #33 says (see Q2).
- The `id` of an entry is given by the one who creates it and is rejected if it already exists in its group; the module is `server/c2-configuration` (Javier).
- The tests are Adam's (Testing): there is no step to write them here, only one to hand the branch over.

## Questions and answers
Open points that #33 and the design do not settle; each one has a proposal.
- **Q1:** What data do protocols, schedules, appointment policies and priorities have? **Proposal:** `Protocol`, `AppointmentPolicy` and `Priority` have `id`, `name`, `description` and `active`; `Schedule` has `resource` (the room, its id), `daysOfWeek`, `startTime`, `endTime` and `active`, and the end must be after the start. A `TestType` has the fields of FR17: `id`, `name`, `modality`, `room`, `durationMinutes` (> 0), `preparation`, `requiresContrast`, `requiresConsent`, `safetyInfo`, `priority` (the id of a priority), `followUpEligible` and `active`. **A:** approved as proposed (Javier, 14/11/2026)
- **Q2:** Do the reads need the permission? **Proposal:** yes, as #33 says ("every operation in this table requires the permission"): all the operations take `requester` and need `CONFIGURE_SYSTEM`. When C4 and C5 need to read the catalogue, their iterations decide how (a new contract or a read permission). **A:** approved as proposed (Javier, 14/11/2026)
- **Q3:** #33 has no way to deactivate protocols, schedules, policies and priorities, but #38 and #40 say that every group can be deactivated. **Proposal:** add `deactivateProtocol(id)`, `deactivateSchedule(resource)`, `deactivateAppointmentPolicy(id)` and `deactivatePriority(id)`. **A:** approved as proposed (Javier, 14/11/2026)
- **Q4:** Versions. **Proposal:** `server-c1-access` goes to `1.2.1-SNAPSHOT` (PATCH, as told to Ignacio) and the managed version in the parent pom follows it; `server-c2-configuration` starts at `1.0.0-SNAPSHOT`; the version of `common` is not changed (open since the plan of #100). **A:** approved as proposed (Javier, 14/11/2026)
- **Q5:** How does C2 know that the requester is an active user with the permission, without a new method in `IRoleManagementService` (it would break the fake of the client tests)? **Proposal:** it looks for the requester in `getUsers()` (must exist and be active) and checks `getPermissions(requester)`; if not, `AccessDeniedException` and an audit entry. **A:** approved as proposed (Javier, 14/11/2026)
- **Q6:** Where does `ValidationException` (with the list of errors) live, since C3 also needs it? **Proposal:** a new package `es.uclm.esi.iso2.common.shared` in `common` (needs your approval, rule 9). **A:** approved as proposed (Javier, 14/11/2026)
- **Q7:** Names of the audit entries. **Proposal:** as in C1, one action per change: `PARAMETER_CREATED`, `PARAMETER_UPDATED`, `PARAMETER_DEACTIVATED` and `PARAMETER_ACCESS_DENIED`, with the resource `<group>:<id>` (for example `TEST_TYPE:CT-HEAD`). Reads are not audited (they are not changes). **A:** approved as proposed (Javier, 14/11/2026)
- **Q8:** The coverage check of D-033 (80 % lines, 70 % branches) fails in `common` and in the new module until Adam's tests exist. **Proposal:** in each step I run `mvn -B verify -Djacoco.skip=true` (everything compiles and the 233 tests of C1 pass); the full `mvn -B verify` is the criterion to close the pull request, with Adam's tests. **A:** approved, with a change: in each step I only check that it compiles and works roughly, without writing tests (Javier, 14/11/2026)
- **Note:** adding a permission to the seed of C1 can break a C1 test that checks the exact list of permissions. If it happens I stop and tell you, because changing an expected value is a decision (rule 17).

## Files
| File | Action | What |
|---|---|---|
| `common/.../common/access/contracts/IAuditRecorder.java` | move (`git mv`) from `server-c1-access` | same signature `register(String author, String action, String targetResource)`; the `import` changes in `AuditService`, `AuthenticationService`, `RoleManagementService` and the test `RecordingAuditRecorder` |
| `server/c1-access/.../persistence/InMemoryPermissionRepository.java` | change | the permission `CONFIGURE_SYSTEM` |
| `server/c1-access/.../persistence/InMemoryRoleRepository.java` | change | the administrator role gets `CONFIGURE_SYSTEM` |
| `server/c1-access/pom.xml`, `pom.xml` | change | version `1.2.1-SNAPSHOT` (Q4) |
| `common/.../common/shared/ValidationException.java` | create | `errors: List<String>` (Q6) |
| `common/.../common/configuration/package-info.java` | create | |
| `common/.../common/configuration/domain/` `TestType`, `Protocol`, `Schedule`, `AppointmentPolicy`, `Priority` | create | data classes (Q1), each with a method that returns a deactivated copy |
| `common/.../common/configuration/contracts/ISystemParameterService.java` | create | the operations of #33 with `requester` first (Q2, Q3) |
| `server/pom.xml` | change | module `c2-configuration` |
| `server/c2-configuration/pom.xml` | create | `server-c2-configuration` 1.0.0-SNAPSHOT, depends only on `common` and JUnit |
| `server/c2-configuration/.../server/configuration/persistence/IConfigurationRepository.java` | create | generic: `findById`, `findAll`, `save` |
| `server/c2-configuration/.../server/configuration/persistence/InMemoryConfigurationRepository.java` | create | one generic in-memory implementation, used for the five types |
| `server/c2-configuration/.../server/configuration/service/SystemParameterService.java` | create | implements the contract: permission check (Q5), validation, deactivate instead of delete, audit (Q7) and `loadSeedData()` |
| `agents/architecture.md` | change | the code tree with `c2-configuration` |
| `doc/plans/UC17-server.md` | create/update | this plan |

## Steps
Tick each step with the hash of its commit. Steps 2 and 3 are the change of C1; the rest do not touch it.
- [ ] 1. Plan of this sub-issue (`docs(UC17): plan for the server part (#40)`).
- [ ] 2. Move `IAuditRecorder` to `common` and update the imports in C1 (build green, 233 tests).
- [ ] 3. Permission `CONFIGURE_SYSTEM` in C1, and the version `1.2.1-SNAPSHOT` (Q4).
- [ ] 4. `ValidationException` and the domain classes of C2 in `common`.
- [ ] 5. The contract `ISystemParameterService` in `common`.
- [ ] 6. The module `server/c2-configuration` (poms) and the generic repository.
- [ ] 7. `SystemParameterService`: permission check, validation, create, update, deactivate, audit.
- [ ] 8. The seed data (`loadSeedData()`).
- [ ] 9. `agents/architecture.md`.
- [ ] 10. Hand the branch over to Adam (tests of UC17-server) and close only with his tests green and the coverage check passing.

## Verification
- [ ] Every acceptance criterion of #40 is checked: seed data loaded when starting with no data; every create, update and deactivate is validated, checked against the permission and audited; the interfaces match #33.
- [ ] No class of C1 changes except the `import`, the permission and the version.
- [ ] `server-c2-configuration` depends only on `common`.
- [ ] `mvn -B verify` is green with Adam's tests (the coverage check included).
- [ ] The CI of the pull request is green.

## Status
- **In progress:** step 2 (move `IAuditRecorder` to `common`)
- **Next step:** step 2 (move `IAuditRecorder` to `common`)
- **Open questions:** none
- **Last update:** 14/11/2026, Javier
