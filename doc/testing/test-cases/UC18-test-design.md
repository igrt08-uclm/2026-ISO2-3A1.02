# Test design · UC18 – Manage roles and permissions

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Component C1, iteration 1,
issues #10 (use case), #16 (client), #17 (server). Expected results come from #17, the plan `doc/plans/UC18-server.md`
and the code of `develop`.

Code under test (`server-c1-access`, `develop`): `RoleManagementService.assignRole`, `revokeRole`, `assignPermission`,
`revokePermission` (with the private checks `checkCanManageRoles`, `canManageRoles` and `requireAnAdministratorAfter`,
the rule of the last administrator), `getRoles`, `getPermissions`, and the default method
`IRoleManagementService.hasPermission(username, permissionCode)` (`common`), which uses `getUsers` and `getPermissions`.
Screen: `IU_ManageRoles` (`client-c1-access`).

Seed data (`InMemoryUserAccountRepository`, `InMemoryRoleRepository`, `InMemoryPermissionRepository` of `develop`):
`admin` (active, System administrator: `MANAGE_ROLES; CONFIGURE_SYSTEM; CONFIGURE_INTEGRATIONS; CONSULT_AUDIT`),
`mgarcia` (active, Physician + Patient), `lmartin` (active, Admissions staff: `MANAGE_PATIENTS`), `jfernandez`
(active, Patient), `pnavarro` (inactive, Physician). Physician and Patient have no permissions.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| `assignRole`, `revokeRole`, `assignPermission`, `revokePermission` | `requester` | Valid: active user with `MANAGE_ROLES` in a current role | `admin` (seed), `ana` (unit) |
| | | Valid: active user with `MANAGE_ROLES` through another role | `bob` after `MANAGE_ROLES` is given to Physician |
| | | Invalid: unknown user | `ghost` |
| | | Invalid: inactive user, even with `MANAGE_ROLES` | `old` (inactive administrator), `pnavarro` |
| | | Invalid: active user without `MANAGE_ROLES` | `bob`, `mgarcia` |
| | | Conjeture | `null`, `""`, `ADMIN` (case) |
| `assignRole`, `revokeRole` | `user` | Valid: existing account | `cid`, `bob`, `jfernandez` |
| | | Invalid: `null` | `null` |
| | | Invalid: account not in the repository | `ghost` |
| `assignRole`, `revokeRole`, `assignPermission`, `revokePermission` | `role` | Valid: existing role the target has / does not have | `PHYSICIAN` for `bob` (has), `PATIENT` for `bob` (has not) |
| | | Invalid: `null` | `null` |
| | | Invalid: role not in the repository | `GHOST`, `NURSE` |
| `assignPermission`, `revokePermission` | `permission` | Valid: existing permission the role has / does not have | `MANAGE_ROLES`, `TEST_OTHER` |
| | | Invalid: `null` | `null` |
| | | Invalid: permission not in the repository | `GHOST` |
| `revokeRole`, `revokePermission` | State: active users with `MANAGE_ROLES` after the change (rule of the last administrator) | At least one remains | 2 (Limit Val. above: `ana` and `eva`), or another role keeps `MANAGE_ROLES` |
| | | None remains | 0 (Limit Val.: `ana` is the only one) |
| | | Conjeture: an inactive administrator does not count | `old` is the only other administrator |
| `getRoles`, `getPermissions` | `username` | Valid: existing user | `bob`, `dan` (three roles that share a permission), `mgarcia` |
| | | Invalid: unknown user / `null` | `ghost`, `null` |
| `hasPermission` | `username` | Valid: active user with the permission | `admin` + `CONFIGURE_INTEGRATIONS`, `lmartin` + `MANAGE_PATIENTS` |
| | | Invalid: active user without it | `mgarcia`, `jfernandez`, `admin` + `MANAGE_PATIENTS` |
| | | Invalid: inactive user / unknown user / `null` | `pnavarro`, `nobody`, `null` |
| | `permissionCode` | Valid: an existing code | `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS`, `MANAGE_PATIENTS` |
| | | Conjeture | `null`, `configure_integrations` (case) |

Outputs and exceptions:
- A change that is done saves the account or the role once and registers `ROLE_ASSIGNED`, `ROLE_REVOKED`,
  `PERMISSION_ASSIGNED` or `PERMISSION_REVOKED` with the resource `user:<u>/role:<id>` or `role:<id>/permission:<code>`.
- A change that is already done (assign what is there, revoke what is not) saves and registers nothing.
- `AccessDeniedException(requester)` and `ROLE_CHANGE_DENIED` on `roles` for an invalid requester.
- `IllegalArgumentException` for a `null` or unknown user, role or permission; nothing changes.
- `IllegalStateException` when no active user with `MANAGE_ROLES` would remain; nothing changes.
- `hasPermission` never throws: `true` or `false`.

## 2. Combination of the values
The four changes check their parameters in a fixed order: requester, then user (or role), then role (or permission),
then the state. A rejected value stops the method, so the values after it are never read and do not need to be
combined with it.
- **All combinations** (maximum number of test cases) of one change: requester 5 classes (valid, unknown, inactive,
  without permission, `null`) × role 4 (has, has not, `null`, unknown) × user or permission 3 (valid, `null`, unknown)
  = 60 per method, 240 for the four, plus the 3 states of the rule for the two revokes.
- **Pairwise** would need about 20 per method.
- **Each use** is the one chosen: every class is used at least once, an invalid value always with valid values in the
  other parameters. The invalid requesters are used in the four methods (3 × 4 = 12 calls in one test); the invalid
  user, role and permission are spread over the methods (7 cases); "already done" is used once per method (4 cases);
  the rule of the last administrator in the two revokes.
- `hasPermission`: two parameters, each use and the seed data give 9 combinations with the real service, plus 7 with
  a fake in `common`.

## 3. Coverage (white box)

| Method | Decision | True with | False with |
|---|---|---|---|
| `checkCanManageRoles` | `user == null \|\| !canManageRoles(user, null)` | `ghost` (C1 true); `old`, `bob` (C1 false, C2 true) | `ana` (both false) |
| `canManageRoles` | `user.isActive() && anyMatch(hasPermission(role, MANAGE_ROLES))` | `ana` | `old` (C1 false); `bob` (C1 true, C2 false) |
| `canManageRoles` (lambda) | `changedRole != null && role.getId().equals(changedRole.getId())` | `revokePermission` of the administrator role, user `ana` | `changedRole` `null` (every check of the requester and every `revokeRole`); `changedRole` given but another role (`bob` with Physician in `aRoleCanLoseManageRolesIfAnotherRoleKeepsIt`) |
| `requireAnAdministratorAfter` (lambda) | `changedUser != null && user.getUsername().equals(changedUser.getUsername())` | `revokeRole`, the changed user | `changedUser` `null` (`revokePermission`); the other users in `revokeRole` |
| `requireAnAdministratorAfter` | `!remains` | `ana` only administrator; `old` inactive (both revokes) | `eva` remains; Physician keeps `MANAGE_ROLES`; `revokeRole` of a non-administrator role |
| `assignRole` | `!hasRole(account, assigned)` | `assignRoleSavesAndRegisters` | "already done" (`bob` already Physician) |
| `revokeRole` | `hasRole(account, revoked)` | `revokeRoleSavesAndRegisters` | "already done" (`bob` is not Patient) |
| `assignPermission` | `!hasPermission(current, code)` | `assignPermissionSavesAndRegisters` | "already done" |
| `revokePermission` | `hasPermission(current, code)` | `revokePermissionSavesAndRegisters` | "already done" |
| `existingUser(UserAccount)` | `user == null` | null user | every valid user |
| `existingUser(String)` | `user == null` | `ghost`, `null` (queries), unknown user (`assignRole`) | valid users |
| `existingRole` | `role == null ? …` then `existing == null` | null role (`assignRole`, `assignPermission`); `GHOST` (`revokeRole`) | valid roles |
| `existingPermission` | `permission == null ? …` then `existing == null` | null permission (`assignPermission`); `GHOST` (`revokePermission`) | valid permissions |
| `currentRoles` | `filter(Objects::nonNull)` | every role of the tests is in the repository | **never false** (see section 6) |
| `getPermissions` | `put` of an existing code | `dan`: `TEST_OTHER` twice is kept once | `bob` |
| `hasPermission` | `username == null` | `null` | the others |
| `hasPermission` | `activeUser && anyMatch(code equals)` | `admin` + `CONFIGURE_INTEGRATIONS` | `pnavarro`, `nobody` (C1 false); `mgarcia` (C1 true, C2 false) |
| `hasPermission` (lambda) | `username.equals(getUsername()) && isActive()` | `admin` | other usernames (C1 false); `pnavarro` (C1 true, C2 false) |

- **Decision coverage:** every decision of the table is true and false, except `filter(Objects::nonNull)` of
  `currentRoles`.
- **Condition/decision:** every condition of the compound decisions takes both values and so does the decision.
- **MC/DC:** for each `||` / `&&` above there is a pair of cases that changes only one condition and changes the
  result: `ghost` vs `ana` (C1 of `checkCanManageRoles`), `bob` vs `ana` (C2); `old` vs `ana` and `bob` vs `ana`
  (`canManageRoles`); `pnavarro` vs `admin` and `mgarcia` vs `admin` (`hasPermission`).

## 4. Test cases (manual)

**Test data:** the seed users of the simulated SSO. Only the role `SYSTEM_ADMINISTRATOR` has the permission
`MANAGE_ROLES`, and only `admin` / `Admin.2026` has that role. `MANAGE_ROLES` is the only permission in C1.
Data is in memory (D-030): restart the program before each case to start from the seed data.

Rule of minimums: 1 normal + at least 2 error + 1 permission case.

| ID | Title | Type | Quality characteristic | Steps | Expected result | Actual result | Pass | Date / tester |
|---|---|---|---|---|---|---|---|---|
| TC18-01 | Assign a role to a user | normal | Functional suitability | Log in as `admin`, *Manage roles*, tab *User roles*: User = `jfernandez`, Role to assign = Physician, *Assign role*, confirm. | Message "✔ Role Physician assigned to jfernandez."; the list *Roles of the user* shows Patient and Physician. Audit: `ROLE_ASSIGNED` by `admin`, resource `user:jfernandez/role:PHYSICIAN`. | | | |
| TC18-02 | Revoke a role from a user | normal | Functional suitability | As `admin`: User = `mgarcia`, select Patient in *Roles of the user*, *Revoke selected*, confirm. | Message "✔ Role Patient revoked from mgarcia."; `mgarcia` keeps only Physician. Audit: `ROLE_REVOKED`. | | | |
| TC18-03 | A permission given to a role reaches its users | normal | Functional suitability | As `admin`, tab *Role permissions*: Role = Admissions staff, Permission = `MANAGE_ROLES`, *Assign permission*, confirm. Log out and log in as `lmartin` / `Admissions.2026`. | `lmartin` now sees the *Manage roles* button and can change roles. Audit: `PERMISSION_ASSIGNED`, resource `role:ADMISSIONS_STAFF/permission:MANAGE_ROLES`. | | | |
| TC18-04 | Assign a role the user already has | error | Usability (user error protection) | As `admin`: User = `admin`, Role = System administrator, *Assign role*. | Message "Nothing changed: admin already has that role."; no audit entry is added. | | | |
| TC18-05 | Revoking would leave no administrator | error | Security + Usability | As `admin`: User = `admin`, select System administrator, *Revoke selected*, confirm. Repeat in tab *Role permissions*: Role = System administrator, revoke `MANAGE_ROLES`. | Both times: "⚠ This change would leave the system without an active administrator." Nothing changes and `admin` keeps the role and the permission. | | | |
| TC18-06 | Cancel the confirmation | error | Usability (user error protection) | As `admin`: assign Physician to `lmartin`, and click *Cancel* / *No* in the confirmation. | Nothing changes and no audit entry is added. | | | |
| TC18-07 | User without `MANAGE_ROLES` | permission | Security + Functional suitability | Log in as `mgarcia` / `Physician.2026`. Then, from a test, call `assignRole("mgarcia", PHYSICIAN, lmartin)`. | The welcome screen has no *Manage roles* button. The call throws `AccessDeniedException`, nothing changes, and `ROLE_CHANGE_DENIED` with author `mgarcia` is recorded. | | | |
| TC18-08 | Inactive user cannot manage roles | permission | Security | From a test: give `MANAGE_ROLES` to the Physician role, then call `assignRole("pnavarro", …)` (`pnavarro` is an inactive physician). | `AccessDeniedException`: the permission does not count for an inactive account. | | | |
| TC18-09 | Any user can consult roles and permissions | normal | Functional suitability | From a test: `getRoles("mgarcia")` and `getPermissions("admin")`. | `mgarcia`: Physician and Patient. `admin`: `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` and `CONSULT_AUDIT` (since the patches of C1 1.2.2 and 1.2.3; before them, only `MANAGE_ROLES`). No exception and nothing audited. | | | |
| TC18-10 | Unknown user or role | error | Usability | From a test: `getRoles("ghost")`; `assignRole("admin", <role "NURSE">, jfernandez)`. | `IllegalArgumentException` in both. On the screen this is shown as "⚠ That user, role or permission no longer exists. Reload the screen." | | | |

Postcondition of every case: a rejected or cancelled change leaves the accounts, the roles and the audit log as they
were; a change that is done is in the repositories and has exactly one audit entry.

### Automated tests that cover it

| Manual case | JUnit test class | Key methods |
|---|---|---|
| TC18-01, TC18-02 | `RoleManagementServiceTest`, `IU_ManageRolesTest`, `RoleManagementServiceIntegrationTest` | `assignRoleSavesAndRegisters`, `revokeRoleSavesAndRegisters`, `assigningARoleWithOk`, `revokingARoleWithOk`, `anAssignedRoleIsInTheNextLoginAndInTheAuditLog` |
| TC18-03 | `RoleManagementServiceTest`, `IU_ManageRolesTest` | `assignPermissionSavesAndRegisters`, `aPermissionGivenToARoleTakesEffectAtOnce`, `assigningAPermissionWithOk` |
| TC18-04 | `RoleManagementServiceTest`, `IU_ManageRolesTest` | `aChangeAlreadyDoneDoesNothing`, `aChangeAlreadyDoneAsksNothing` |
| TC18-05 | `RoleManagementServiceTest`, `IU_ManageRolesTest` | `theOnlyAdministratorCannotLoseManageRoles`, `anInactiveAdministratorDoesNotCount`, `anAdministratorCanLoseTheRoleIfAnotherRemains`, `aRoleCanLoseManageRolesIfAnotherRoleKeepsIt`, `aRejectedChangeShowsItsMessage` |
| TC18-06 | `IU_ManageRolesTest` | `eachChangeAsksItsQuestion`, `cancelDoesNotSendTheChange` |
| TC18-07, TC18-08 | `RoleManagementServiceTest`, `RoleManagementServiceIntegrationTest`, `IU_WelcomeTest` | `aRequesterThatCannotManageRolesIsRejected` (`ghost`, `old`, `bob`), `mgarciaIsRejectedAndAdminSeesTheAttempt`, `userWithoutManageRolesDoesNotSeeIt` |
| TC18-09, TC18-10 | `RoleManagementServiceTest` | `getRolesReturnsTheSavedPermissions`, `getPermissionsReturnsEachPermissionOnce`, `queriesRejectAnUnknownUser`, `anUnknownValueIsRejected` |
| `hasPermission` | `RoleManagementServiceHasPermissionIntegrationTest`, `IRoleManagementServiceHasPermissionTest` (`common`) | `seedPermissionsReachTheirUsers` (9 rows), plus the 7 methods of the `common` test (`null`, unknown, inactive user, `null` code, lower-case code) |

## 5. Findings
- The test data paragraph and the notes of the manual cases say that `MANAGE_ROLES` is the only permission of C1.
  On `develop` there are five (`MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS`, `MANAGE_PATIENTS`,
  `CONSULT_AUDIT`); the administrator has four of them and Admissions staff has `MANAGE_PATIENTS`. The expected result
  of **TC18-09** (`admin`: `MANAGE_ROLES`) no longer matches the seed data: it should be
  `MANAGE_ROLES; CONFIGURE_SYSTEM; CONFIGURE_INTEGRATIONS; CONSULT_AUDIT`. Testing updated it on 22/11/2026 to the current seed data
  (the patch 1.2.4 adds no permission to the administrator).
- The rule of the last administrator is also checked when a role that is not the administrator's is revoked (it is
  true, so nothing is rejected); this is the code, not a defect.
- A change of role of a logged-in user is applied when they log in again (the welcome screen is reloaded on *Back*);
  TC18-03 checks it with a new login.

## 6. Not covered (to add)
- `currentRoles` with an account whose role is no longer in the repository of roles (`filter(Objects::nonNull)`
  false): no test of `RoleManagementService` (the same case is tested in `AuditService`, TC19S-05).
- `requester` `null` in the four changes (error guessing): only `ghost`, `old` and `bob` are used.
- `hasPermission` with the real `RoleManagementService` for a `null` or unknown user, a `null` code and
  `CONSULT_AUDIT`: only tested with the fake of `common`; the integration test uses 9 seed rows without them.
- Boundary of the rule with exactly one other active administrator through a role that loses `MANAGE_ROLES` in the
  same change (two users with the administrator role, `revokePermission` on it): not tested.

## 7. Not defined (to ask)
- Whether a `null` or empty `permissionCode` should be rejected instead of answering `false` in `hasPermission`.
- What a `null` requester registers as author of `ROLE_CHANGE_DENIED` (today `null`).
