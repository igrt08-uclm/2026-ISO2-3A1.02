# Plan – Patch of C1 1.2.2 (#112)

| | |
|---|---|
| **Issue** | #112 *Patch of C1: permission CONFIGURE_INTEGRATIONS, IAuthenticatorSystem and hasPermission in common (C1 v1.2.2)* · branch `feature/C1-patch-1.2.2` (created by Javier from `develop`, 15/11/2026) |
| **Use case** | none: it is a PATCH of C1 that UC20 needs (#43 *UC20 – Server (C2 v1.1.0)*, plan `doc/plans/UC20-server.md`) |
| **Rule** | D-036: a finished component is changed in its own branch and pull request, approved by its owner (Ignacio). C1 `1.2.2`; `common` is already `1.4.0-SNAPSHOT` (#113) |
| **Specification** | #112, D-036, #43 (comment of 13/11/2026 and its answers) |
| **Design** | `doc/design/C2-server-design.png` (`IAuthenticatorSystem`, `IIntegrationManagementService`) · contracts: section 6 of #33 *C2. Configuration and integrations* and of #8 *C1. Access and security* |
| **Plan approved by** | Javier, 15/11/2026 |

## Decisions already taken
- Ignacio, owner of C1, approved the patch in #112 on 15/11/2026 (D-036). Marcos agreed in #43 with `CONFIGURE_INTEGRATIONS` and `ping()`.
- The patch does not add a use case and does not change the behaviour of C1: it is a PATCH.
- The tests are Adam's. This patch only adjusts, with Javier's approval, the tests that stop compiling or that check the exact list of permissions (the same as in #109).
- The branch is kept apart from UC20 so that Ignacio reviews C1 alone; UC20-server brings it from `develop` once it is merged.
- The team agreed in Meeting 8 (15/11/2026), as Javier reports, to put `MANAGE_PATIENTS` (UC12) in this patch too, so that C2 and C3 need only one patch of C1 and one pull request (Q8). D-036 and the text of #112 still list it as a later patch, so they are to be updated with the minutes of the meeting; Ignacio had already found the permission viable in #46 (point 3) and approves this pull request.

## Questions and answers
Each one has a proposal and all of them are answered.
- **Q1:** C2 cannot depend on the module `server-c1-access`, and `IAuthenticatorSystem` is in it. **Proposal:** move it with `git mv` to `common`, package `es.uclm.esi.iso2.common.access.contracts` (as `IAuditRecorder`), and add `ping()` to it as a `default` method that returns `true` (Q6); `AuthenticatorSystemSimulator` overrides it and returns `true`. The `import` changes in `AuthenticationService`, in the simulator and in the test double `SpyAuthenticatorSystem`. The real method of the interface is `validateCredentials`, not `authenticate` as the design says (the code wins, rule 11). **A:** approved (Javier, 14/11/2026)
- **Q2:** New permission `CONFIGURE_INTEGRATIONS`, given to the system administrator (`InMemoryPermissionRepository`, `InMemoryRoleRepository`). **A:** approved (Javier, 14/11/2026; Ignacio, 15/11/2026)
- **Q3:** The permission check of UC17 is private in `SystemParameterService` and Javier does not want to copy it nor to change UC17. **Proposal:** `IRoleManagementService` gets a `default` method `boolean hasPermission(String username, String permissionCode)`: true if the user is in `getUsers()`, is active and has the permission in `getPermissions(username)`, the same rule as UC17. As it is `default`, `RoleManagementService` and the two `FakeRoleManagementService` of the tests compile with no change. `SystemParameterService` is not touched. **A:** approved (Javier, 14/11/2026; Ignacio, 15/11/2026; David suggests in #112 that C2 uses it later in `SystemParameterService`)
- **Q4:** Versions. **Proposal:** `server-c1-access` goes from `1.2.1-SNAPSHOT` to `1.2.2-SNAPSHOT` in `server/c1-access/pom.xml` and in the managed version of `pom.xml`. `client-c1-access` (`1.2.1-SNAPSHOT`) and `common` (`1.4.0-SNAPSHOT`) do not change in this patch. **A:** approved (Javier, 14/11/2026)
- **Q5:** Tests of Adam that must be adjusted. **Proposal:** (a) `SpyAuthenticatorSystem`: the `import` and `ping()` returning what the wrapped system returns; (b) `InMemoryPermissionRepositoryTest`: the list now has 4 permissions (`MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS`, `MANAGE_PATIENTS`), so the expected size changes from 2 to 4, the assertions of the new codes are added and the name of the method and its `@DisplayName` say the four codes; (c) `InMemoryRoleRepositoryTest`: the row of `SYSTEM_ADMINISTRATOR` becomes `MANAGE_ROLES;CONFIGURE_SYSTEM;CONFIGURE_INTEGRATIONS`, the row of `ADMISSIONS_STAFF` becomes `MANAGE_PATIENTS`, and the name of the test `Only the System administrator has permissions` and its `@DisplayName` change, because now the administrator and the Admissions staff have permissions. Nothing else is changed and no test is written; Adam is told. If another test breaks, I stop and tell Javier. **A:** the values and the double are approved (Javier, 14/11/2026); the renames and the row of the Admissions staff are approved with the plan (Javier, 15/11/2026)
- **Q6:** with the abstract `ping()`, 3 tests of Adam in `AuthenticationServiceTest` (lines about 147, 167 and 187) stop compiling: they use lambdas `(u, p) -> true`, and with two abstract methods the interface is not functional. **Option 1 (proposal):** `ping()` is a `default` method that returns `true`: the 3 tests compile with no change and the simulator and the test double override it. Risk: a future real adapter could forget to override it and always answer OK; today everything is simulated (A5) and the Javadoc says it. **Option 2:** `ping()` stays abstract and the 3 tests build the double in another way (for example an anonymous class), with no expected value changed and with the approval of Javier and Adam. **A:** option 1 approved (Javier, 15/11/2026). Because `ping()` is `default`, `SpyAuthenticatorSystem` and the 3 tests of `AuthenticationService` compile; the Spy still overrides it to return what the wrapped system returns (Q5a).
- **Q7:** What `hasPermission` answers in the odd cases. **Proposal:** `false` (no exception) if the username is `null`, unknown or inactive, or if the user does not have the permission, as `canConfigure` does in UC17; the denied access is registered by the service that calls it, not by this method. **A:** approved with the plan (Javier, 15/11/2026)
- **Q8:** `MANAGE_PATIENTS` goes in this patch (decision of the team in Meeting 8, 15/11/2026, as Javier reports, instead of a second patch `1.2.3`). **Proposal:** the permission is `MANAGE_PATIENTS`, with the description "Search, register and modify patients"; it is the last of the list; and only the role `ADMISSIONS_STAFF` gets it, as #46 says (point 3), not the administrator. #112 and D-036 still say that it is a later patch: they are updated only if Javier asks for it, because changing the text of an issue is an action in GitHub. **A:** the decision to put it in this patch is the team's (Meeting 8); the details of the proposal (description "Search, register and modify patients", last of the list, only `ADMISSIONS_STAFF`) are confirmed (Javier, 15/11/2026)

## Files
| File | Action | What |
|---|---|---|
| `server/c1-access/.../server/access/integration/IAuthenticatorSystem.java` → `common/.../common/access/contracts/IAuthenticatorSystem.java` | move (`git mv`) | new package and `default boolean ping()` that returns `true` (Q1, Q6), with Javadoc |
| `server/c1-access/.../integration/AuthenticatorSystemSimulator.java` | change | the `import` and `ping()` (Q1) |
| `server/c1-access/.../service/AuthenticationService.java` | change | only the `import` |
| `server/c1-access/src/test/.../service/SpyAuthenticatorSystem.java` | change | the `import` and `ping()` (Q5a) |
| `server/c1-access/.../persistence/InMemoryPermissionRepository.java` | change | the permissions `CONFIGURE_INTEGRATIONS` (Q2) and `MANAGE_PATIENTS` (Q8) |
| `server/c1-access/.../persistence/InMemoryRoleRepository.java` | change | the administrator role gets `CONFIGURE_INTEGRATIONS`; `ADMISSIONS_STAFF` gets `MANAGE_PATIENTS` |
| `server/c1-access/src/test/.../InMemoryPermissionRepositoryTest.java`, `InMemoryRoleRepositoryTest.java` | change | the expected permissions (Q5b, Q5c) |
| `common/.../common/access/contracts/IRoleManagementService.java` | change | the `default` method `hasPermission`, with Javadoc (Q3, Q7) |
| `server/c1-access/pom.xml`, `pom.xml` | change | `server-c1-access` `1.2.2-SNAPSHOT` (Q4) |
| `agents/architecture.md` | change | the version of `server-c1-access` and `IAuthenticatorSystem` in `common` |
| `doc/plans/C1-patch-1.2.2.md` | create/update | this plan |

No file of C2 or of the client changes in this patch. The code of steps 2 to 4 was already started in `feature/UC20-server` and was saved with `git stash` ("UC20 step 2 parked", `stash@{0}`); it is recovered here with `git stash pop --index` only after this plan is approved.

## Steps
Tick each step with the hash of its commit (green `mvn -B verify -Djacoco.skip=true`, with the same tests; no test is written).
- [x] 1. This plan (`docs(C1): plan of the patch 1.2.2 (#112)`). - see `git log --oneline -- doc/plans/C1-patch-1.2.2.md`.
- [x] 2. `IAuthenticatorSystem` moves to `common` with `ping()`, and the version `1.2.2-SNAPSHOT` (Q1, Q4, Q5a, Q6). - 52f08a0 (the stash of 14/11 was recovered and `ping()` changed to a `default` method; 27 + 144 + 41 + 55 + 79 existing tests pass).
- [x] 3. Permissions `CONFIGURE_INTEGRATIONS` (administrator) and `MANAGE_PATIENTS` (Admissions staff) and the two expected lists (Q2, Q5b, Q5c, Q8). - d490dc8 (the two tests of Adam were adjusted as in Q5: size 4 and the three new names; no other test broke). The description of `CONFIGURE_INTEGRATIONS` is "Configure the integrations with the other hospital systems".
- [ ] 4. `hasPermission` as a `default` method of `IRoleManagementService` (Q3, Q7).
- [ ] 5. `agents/architecture.md`.
- [ ] 6. Hand the branch over to Adam (tests of `hasPermission`, of `ping()` and of the lists of permissions), who opens the pull request (`Closes #112`) for Ignacio to approve (D-036); close only with his tests green and the coverage check passing in `common` and in `server-c1-access`.

## Verification
- [ ] Every item of the checklist of #112 is done.
- [ ] No class of C2 and no class of the client changes; `SystemParameterService` is not touched.
- [ ] The login keeps working with the simulated SSO (the tests of `AuthenticationService` pass).
- [ ] `mvn -B verify` is green with Adam's tests (the coverage check included).
- [ ] The CI of the pull request is green and Ignacio approved it.

## Status
- **In progress:** nothing (this plan waits for the approval of Javier)
- **Next step:** step 4 (`hasPermission` as a `default` method of `IRoleManagementService`, in `common`)
- **Open questions:** none
- **Last update:** 15/11/2026, Javier
