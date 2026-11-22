# Test design · Patch of C1 1.2.3 (#172) – permission CONSULT_AUDIT

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#172 and the answers Q1–Q5 of `doc/plans/C1-patch-1.2.3.md` ("Cases to cover").

Code under test: `AuditService.getActivityLog` (check `canConsultAudit`), `InMemoryPermissionRepository.findAll()` and
`findByCode(String code)`, `InMemoryRoleRepository.findAll()` (`server-c1-access` 1.2.3) and the constructors of
`IU_Welcome` (`client-c1-access` 1.2.4), branch `feature/C1-patch-1.2.3`, now in `develop`.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| `getActivityLog` | `requester` | Active user whose role (in the role repository) has `CONSULT_AUDIT` | `ana` (unit), `admin` (integration) |
| | | Active user whose roles do not have `CONSULT_AUDIT` | `bob`, `mgarcia` (physicians) |
| | | Inactive user with `CONSULT_AUDIT` | `old` |
| | | Unknown user | `ghost` |
| | | Conjeture | `null`; copy of the role in the account without permissions; a role no longer in the repository, alone (`lost`) or with a role that has `CONSULT_AUDIT` (`mix`) |
| | State: roles with `CONSULT_AUDIT` (changed with UC18) | Seed: only `SYSTEM_ADMINISTRATOR` | — |
| | | Granted to another role | `PHYSICIAN` |
| | | Revoked from the administrator | `SYSTEM_ADMINISTRATOR` |
| | | Revoked from every role (Limit Val.: 0 roles) | `SYSTEM_ADMINISTRATOR` and `PHYSICIAN` |
| `InMemoryPermissionRepository.findAll` / `findByCode` | `code` | Existing code | `CONSULT_AUDIT` (last of 5, Limit Val.), `MANAGE_ROLES` (first, Limit Val.) |
| | | Conjeture | `null`, `""`, `VIEW_AUDIT_LOG` (old name), `manage_roles` |
| `InMemoryRoleRepository.findAll` | (none; state) | Seed data | 4 roles; only the administrator has `CONSULT_AUDIT`, last |
| `IU_Welcome` | `permissions` | Contains `CONSULT_AUDIT` | `[CONSULT_AUDIT]`; the five permissions in another order |
| | | Does not contain it | `[]`; the other four; `[MANAGE_PATIENTS]` |
| | `user` (roles) | Role `SYSTEM_ADMINISTRATOR` / another role | `admin` / `mgarcia` (the role name must not count) |
| | `onViewAudit` | Action given / `null` | `() -> views++` / `null` |
| | Constructor | 2 or 3 arguments (no list of permissions) | `IU_Welcome(admin, onLogout)`, `IU_Welcome(admin, onLogout, onViewAudit)` |

Outputs and exceptions: the log and an `AUDIT_CONSULTED` entry on `audit`, or `AccessDeniedException(requester)` and
an `AUDIT_ACCESS_DENIED` entry on `audit`; the permission or `null`; the list of button texts of the welcome screen.

## 2. Combination of the values
- `getActivityLog`: one parameter and the state of the roles; **each use**: every class once (10 cases with the
  conjeture values). The state is combined only with the valid and the denied requester that it changes
  (`mgarcia`, `admin`).
- `IU_Welcome`: three factors (`CONSULT_AUDIT` in the list: yes/no; administrator role: yes/no; `onViewAudit`:
  given/`null`). **All combinations**: 2 × 2 × 2 = 8, plus the 2 old constructors. **Pairwise**: 4 combinations.
  The tests use (yes, admin, given), (yes, physician, given), (no, admin, given) twice, (no, physician, given),
  (yes, admin, `null`) and, through the 2-argument constructor of TC19C-04, (no, admin, `null`), plus the order of all
  five buttons. They cover every pair except (physician, `null`), so pairwise is **not** complete.
- Repositories: no parameters but `code`; each use, as in section 1.

## 3. Coverage (white box)

| Method | Decision | True with | False with |
|---|---|---|---|
| `AuditService.getActivityLog` | `!canConsultAudit(requester)` | TC19S-02, -03, -05a, -07 | TC19S-01, -04, -05b, -06 |
| `AuditService.canConsultAudit` | `user != null && user.isActive() && anyMatch(CONSULT_AUDIT)` | TC19S-01 (T, T, T) | TC19S-03 `ghost`, `null` (C1 F); TC19S-03 `old` (T, F); TC19S-02 (T, T, F) |
| `AuditService.canConsultAudit` | `filter(Objects::nonNull)` | TC19S-05b (the administrator role) | TC19S-05a, -05b (the removed role) |
| `InMemoryPermissionRepository.findByCode` | `getCode().equals(code)` in the loop | TC19S-09b (last iteration) | the elements before it; `null`, `""`, `VIEW_AUDIT_LOG` (`null` returned) |
| `IU_Welcome` | `onViewAudit != null && hasPermission(permissions, CONSULT_AUDIT)` | TC19C-01 | TC19C-03, TC19C-04 with 2 arguments (C1 F); TC19C-02, TC19C-04 with 3 arguments (C1 T, C2 F) |
| `IU_Welcome.hasPermission` | `code.equals(getCode())` in `anyMatch` | TC19C-01, TC19C-05 | TC19C-02 (other codes), empty list |

- **Decision coverage:** every decision is true and false.
- **Condition/decision:** every condition takes both values.
- **MC/DC:** `canConsultAudit`: TC19S-03 `ghost` vs TC19S-01 (C1), TC19S-03 `old` vs TC19S-01 (C2), TC19S-02 vs
  TC19S-01 (C3). `IU_Welcome`: TC19C-03 vs TC19C-01 (C1), TC19C-02 vs TC19C-01 (C2).

## 4. Test cases

### Server (TC19S)

| ID | Technique | Input | Expected result | Source | Test |
|---|---|---|---|---|---|
| TC19S-01 | EC valid | Active user whose role (in the role repository) has `CONSULT_AUDIT` consults the log | Gets the log; `AUDIT_CONSULTED` with target `audit` is registered | #172, Q2 | `AuditServiceTest.tc19s01ActiveUserWithConsultAuditGetsTheLog` |
| TC19S-02 | EC invalid | Active user whose roles do not have `CONSULT_AUDIT` (physician) | `AccessDeniedException(requester)`; one `AUDIT_ACCESS_DENIED` with target `audit` | Q4 | `AuditServiceTest.tc19s02ActiveUserWithoutConsultAuditIsDenied` |
| TC19S-03 | EC invalid / EG | `null` requester, unknown user, inactive user with `CONSULT_AUDIT` | Each one denied and `AUDIT_ACCESS_DENIED` registered | Q4 | `AuditServiceTest.tc19s03InactiveUnknownOrNullRequesterIsDenied` (`null`, `old`, `ghost`) |
| TC19S-04 | EG | The copy of the role inside the account has no permissions, the role in the repository has `CONSULT_AUDIT` | Allowed: the permissions are read from the repository of roles | Q2 | `AuditServiceTest.tc19s04PermissionsAreReadFromTheRoleRepository` |
| TC19S-05 | EG | A role of the user is no longer in the repository: (a) it is the only one, (b) another role has `CONSULT_AUDIT` | (a) denied, (b) allowed: the missing role is ignored | Q2 (`filter(Objects::nonNull)`) | `AuditServiceTest.tc19s05aRemovedRoleIsIgnoredWhenItIsTheOnlyOne`, `tc19s05bRemovedRoleIsIgnoredWhenAnotherRoleHasConsultAudit` |
| TC19S-06 | EC valid (integration) | `CONSULT_AUDIT` granted to `PHYSICIAN` with UC18 | `mgarcia`, denied before, gets the log at once | Q2 | `RoleManagementServiceIntegrationTest.tc19s06ConsultAuditGrantedToAnotherRoleAllowsAtOnce` |
| TC19S-07 | EC invalid (integration) | `CONSULT_AUDIT` revoked from `SYSTEM_ADMINISTRATOR` with UC18 | `admin` is denied at once; `AUDIT_ACCESS_DENIED` registered | Q2, Q4 | `RoleManagementServiceIntegrationTest.tc19s07ConsultAuditRevokedFromTheAdministratorDeniesAtOnce` |
| TC19S-08 | EG (integration) | `CONSULT_AUDIT` revoked from every role | Allowed (not protected like `MANAGE_ROLES`); then nobody can consult the log | Q4 | `RoleManagementServiceIntegrationTest.tc19s08ConsultAuditCanBeRevokedFromEveryRole` |
| TC19S-09 | EC valid | `InMemoryPermissionRepository.findAll()` and `findByCode("CONSULT_AUDIT")` | 5 permissions, `CONSULT_AUDIT` last with "Consult the audit log of the system activity"; found by its code | Q1 | `InMemoryPermissionRepositoryTest.tc19s09aConsultAuditIsTheLastPermissionWithItsDescription`, `tc19s09bFindByCodeFindsConsultAudit` |
| TC19S-10 | EC valid | `InMemoryRoleRepository.findAll()` | Only `SYSTEM_ADMINISTRATOR` has `CONSULT_AUDIT`, as its last permission | Q1 | `InMemoryRoleRepositoryTest.tc19s10OnlyTheAdministratorHasConsultAuditAsItsLastPermission` |

### Client (TC19C) – `IU_Welcome`

| ID | Technique | Input | Expected result | Source | Test |
|---|---|---|---|---|---|
| TC19C-01 | EC valid | `CONSULT_AUDIT` in the list and the action set (also for a physician) | *View audit log* shown (Alt+V) and pressing it runs the action | Q5 | `IU_WelcomeViewAuditTest.userWithConsultAuditSeesTheButton`, `userWithoutAdministratorRoleButWithConsultAuditSeesTheButton` |
| TC19C-02 | EC invalid | Role `SYSTEM_ADMINISTRATOR` but no `CONSULT_AUDIT` (empty list, or the other four permissions); another permission only | Not shown: the role name no longer counts | Q5, #172 | `IU_WelcomeViewAuditTest.administratorWithoutConsultAuditDoesNotSeeTheButton`, `userWithAnotherPermissionOnlyDoesNotSeeTheButton` |
| TC19C-03 | EG | `CONSULT_AUDIT` but `onViewAudit` is `null` | Not shown | Q5 | `IU_WelcomeViewAuditTest.withoutTheActionNobodySeesTheButton` |
| TC19C-04 | EG | The 2- and 3-argument constructors | Never show it (they pass an empty list of permissions) | Q5 (Javadoc) | `IU_WelcomeViewAuditTest.theTwoAndThreeArgumentConstructorsNeverShowTheButton` |
| TC19C-05 | EC valid | All five permissions, in any order | Order unchanged: View audit log, Manage roles, Configure system, Configure integrations, Manage patients, Log out | Q5 ("no button order changes") | `IU_WelcomeViewAuditTest.orderOfTheButtonsDoesNotChange` |

Postcondition of every case: the repositories of permissions and roles are only changed by the UC18 calls of
TC19S-06 to TC19S-08; each consultation adds exactly one audit entry; the welcome screen does not change any data.

### Existing tests adapted (no expected value changed except one, by decision of the plan)
- Server: `AuditServiceTest`, `AuditServiceIntegrationTest`, `RoleManagementServiceIntegrationTest` (3-argument
  constructor; one shared `InMemoryRoleRepository`, as `Main`), `InMemoryPermissionRepositoryTest` (5 permissions),
  `InMemoryRoleRepositoryTest` (administrator with `CONSULT_AUDIT`).
- Client: `IU_WelcomeTest`, `IU_WelcomeConfigureSystemTest`, `IU_WelcomeConfigureIntegrationsTest`,
  `IU_WelcomeManagePatientsTest`: the administrator gets `CONSULT_AUDIT` in the list of permissions.
- **Changed expected value:** in `IU_WelcomeTest.theOlderConstructorsDoNotShowManageRoles` the 3-argument constructor
  now shows only `[Log out]` instead of `[View audit log, Log out]`, because Q5 of the plan decided that the 2- and
  3-argument constructors never show the button.

## 5. Findings
- TC19C-05 says "in any order", but the test uses one order only (`CONSULT_AUDIT, MANAGE_PATIENTS,
  CONFIGURE_INTEGRATIONS, CONFIGURE_SYSTEM, MANAGE_ROLES`, different from the order of the buttons). It shows that the
  order of the list does not matter, not every permutation.
- TC19S-06 and TC19S-07 check the effect with `null` filters only; this is enough for the check, which does not read
  the filters.

## 6. Not covered (to add)
- `IU_Welcome` for a physician with `onViewAudit` `null` (for example with `CONSULT_AUDIT`): the only pair missing
  for pairwise (section 2).
- `getActivityLog` for a user with `CONSULT_AUDIT` only through a role that is not the administrator's in the unit
  test (only in the integration test TC19S-06).

## 7. Not defined (to ask)
- A `null` list of permissions in the constructors that take one (today a `NullPointerException`); not tested.
