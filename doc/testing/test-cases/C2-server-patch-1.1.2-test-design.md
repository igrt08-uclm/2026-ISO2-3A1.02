# Test design · Patch of the server of C2 1.1.2 (#174) – reads for ORDER_PROCEDURES and MANAGE_APPOINTMENTS

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#174 and the plan `doc/plans/C2-server-patch-1.1.2.md` (*Decisions already taken*, Q2, Q3, Q4 and *Cases to cover*).

Code under test (`server-c2-configuration` 1.1.2): `SystemParameterService` — the five reads that changed
(`getTestTypes`, `getTestType`, `getSchedules`, `getAppointmentPolicies`, `getPriorities`), which now call the new
private method `checkReadPermission(requester, group)`; `getProtocols` and the eleven writes, which keep
`checkPermission`.

Test doubles: `FakeRoleManagementService` gets four users for the patch: `druiz` (active, `ORDER_PROCEDURES` only),
`aortega` (active, `MANAGE_APPOINTMENTS` only), `rmoreno` (inactive, `ORDER_PROCEDURES`) and `cvega` (inactive,
`MANAGE_APPOINTMENTS`). The users that were already there do not change: `admin` and `lmartin` (`CONFIGURE_SYSTEM`),
`mgarcia` (active, no permission), `pnavarro` (inactive, `CONFIGURE_SYSTEM`), and no `nobody`.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| the 17 methods of `ISystemParameterService` | `requester` | Active with `CONFIGURE_SYSTEM` | `lmartin` (only that permission), `admin` |
| | | Active with only `ORDER_PROCEDURES` (physician, C4) | `druiz` |
| | | Active with only `MANAGE_APPOINTMENTS` (admissions staff, C5) | `aortega` |
| | | Active with none of the three | `mgarcia` |
| | | Conjeture/EG: inactive with a read permission | `rmoreno` (`ORDER_PROCEDURES`), `cvega` (`MANAGE_APPOINTMENTS`) |
| | | Unknown user | `nobody` |
| | | Conjeture: `null` | `null` |
| | operation | The 5 reads that changed | `getTestTypes`, `getTestType`, `getSchedules`, `getAppointmentPolicies`, `getPriorities` |
| | | The read that did not change | `getProtocols` |
| | | The 11 writes | `createTestType`, `updateTestType`, `deactivateTestType`, `saveProtocol`, `deactivateProtocol`, `saveSchedule`, `deactivateSchedule`, `saveAppointmentPolicy`, `deactivateAppointmentPolicy`, `savePriority`, `deactivatePriority` |
| `getTestType` | `testTypeId` | Existing / non-existing id | `CT-HEAD` / `NOPE` |

Outputs:
- Allowed read: the data of the repository (the same that the administrator reads); nothing is audited (Q3).
- Allowed write: as in 1.1.1 (saved and audited with `PARAMETER_CREATED`, `PARAMETER_UPDATED` or
  `PARAMETER_DEACTIVATED`).
- Denied operation: `AccessDeniedException(requester)` and one audit entry
  `(requester, PARAMETER_ACCESS_DENIED, group)`, with the group `TEST_TYPE`, `PROTOCOL`, `SCHEDULE`,
  `APPOINTMENT_POLICY` or `PRIORITY` (Q3); nothing is changed.
- `getTestType` with a non-existing id by an allowed user: `ValidationException` "There is no test type with the id
  NOPE" (`SystemParameterService.existing`), not audited.

## 2. Combination of the values
Two parameters: `requester` (7 classes) and the operation group (3 groups). Maximum number of test cases:
7 × 3 = **21**. With two parameters, **pairwise** is the same as all the combinations, so the 21 cells are covered:

| requester \ operation | 5 reads that changed | `getProtocols` | 11 writes |
|---|---|---|---|
| `CONFIGURE_SYSTEM` | TC17S-42, TC17S-01 | TC17S-42, TC17S-01 | TC17S-42, TC17S-08 and the rest of 1.1.1 |
| only `ORDER_PROCEDURES` | TC17S-36, TC17S-43 | TC17S-38 | TC17S-39 |
| only `MANAGE_APPOINTMENTS` | TC17S-37, TC17S-43 | TC17S-38 | TC17S-39 |
| active, none | TC17S-06 | TC17S-06 | TC17S-06, TC17S-02 |
| inactive with a read permission | TC17S-40 | TC17S-41 | TC17S-41 |
| unknown | TC17S-40 | TC17S-41 | TC17S-41, TC17S-04 |
| `null` | TC17S-40 | TC17S-41 | TC17S-41 |

Inside each cell every method of the group is called (each use of the operation values), except in TC17S-42, where
the writes are represented by `saveProtocol` and `deactivatePriority` (the other writes by `CONFIGURE_SYSTEM` are
the cases of 1.1.1, TC17S-08 to TC17S-35).

## 3. Coverage (white box)
The new decision is in `checkReadPermission` (`SystemParameterService.java`, lines 283-285 after the patch):

```java
!canConfigure(requester)
        && !roleManagementService.hasPermission(requester, ORDER_PROCEDURES)
        && !roleManagementService.hasPermission(requester, MANAGE_APPOINTMENTS)
```

with conditions A = `!canConfigure`, B = `!hasPermission(ORDER_PROCEDURES)`, C = `!hasPermission(MANAGE_APPOINTMENTS)`.
If the decision is true, the access is denied and audited; if it is false, the read goes on. Java evaluates `&&`
from left to right and stops at the first false condition (– = not evaluated).

| Requester | A | B | C | Decision | Case |
|---|---|---|---|---|---|
| `lmartin` (`CONFIGURE_SYSTEM`) | F | – | – | F (read) | TC17S-42 |
| `druiz` (`ORDER_PROCEDURES`) | T | F | – | F (read) | TC17S-36 |
| `aortega` (`MANAGE_APPOINTMENTS`) | T | T | F | F (read) | TC17S-37 |
| `mgarcia`, `rmoreno`, `cvega`, `nobody`, `null` | T | T | T | T (denied) | TC17S-06, TC17S-40 |

- **Decision coverage:** true (TC17S-40) and false (TC17S-36, TC17S-37, TC17S-42).
- **Condition/decision coverage:** A, B and C are each true (TC17S-40) and false (TC17S-42, TC17S-36, TC17S-37), and
  the decision is true and false.
- **MC/DC:** each condition changes the decision on its own, with the others fixed: A with the pair TC17S-42 /
  TC17S-40, B with TC17S-36 / TC17S-40, C with TC17S-37 / TC17S-40. Four cases (n + 1 for n = 3).
- Inside `canConfigure` (unchanged), `requester == null` is true with `null` and false with the others; the active
  user is false with `rmoreno`, `cvega` and `nobody`. Inside `hasPermission` (default method of
  `IRoleManagementService`, in `common`) the inactive users and `null` give `false` (TC17S-40).
- `checkPermission` does not change; TC17S-38, TC17S-39 and TC17S-41 check that the users of the patch still take its
  true branch.

Measured after the full build: `SystemParameterService` 100 % of lines; the three lines of the new decision have all
their branches covered (JaCoCo, "All 2 branches covered"); module `server-c2-configuration` lines 100.0 %, branches
96.7 %.

## 4. Test cases

All the cases are in `SystemParameterServiceTest` (`server/c2-configuration/src/test/java`, package
`es.uclm.esi.iso2.server.configuration.service`), nested class `ReadPermissionTests`.
Precondition of TC17S-36 to TC17S-42: the administrator has saved the priority `URGENT`, the test type `CT-HEAD`
(20 minutes), the protocol `P1`, the schedule `Room CT-1` and the policy `A1`
(five audit entries).

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC17S-36 | the 5 reads | `druiz` (active, `ORDER_PROCEDURES` only) | Each read returns what the administrator reads (one entry per group; `getTestType("CT-HEAD")` the same object); the audit does not change | `orderingPhysicianReadsWithoutAudit` |
| TC17S-37 | the 5 reads | `aortega` (active, `MANAGE_APPOINTMENTS` only) | The same as TC17S-36 | `admissionsStaffReadsWithoutAudit` |
| TC17S-38 | `getProtocols` | `druiz`, `aortega` (parameterized) | `AccessDeniedException`; one more audit entry `(requester, PARAMETER_ACCESS_DENIED, PROTOCOL)` | `readPermissionCannotReadProtocols` |
| TC17S-39 | the 11 writes | `druiz`, `aortega` (parameterized); valid data | `AccessDeniedException` in each; 11 audit entries `PARAMETER_ACCESS_DENIED` with the requester and the groups `TEST_TYPE` ×3, `PROTOCOL` ×2, `SCHEDULE` ×2, `APPOINTMENT_POLICY` ×2, `PRIORITY` ×2; nothing is created, updated or deactivated | `readPermissionCannotWrite` |
| TC17S-40 | the 5 reads | `rmoreno`, `cvega` (inactive with a read permission), `nobody`, `null` (parameterized) | `AccessDeniedException` in each; audit entries `(requester, PARAMETER_ACCESS_DENIED, group)` for `TEST_TYPE`, `TEST_TYPE`, `SCHEDULE`, `APPOINTMENT_POLICY`, `PRIORITY` | `inactiveUnknownOrNullCannotRead` |
| TC17S-41 | `getProtocols` and the 11 writes | `rmoreno`, `cvega`, `nobody`, `null` (parameterized) | `AccessDeniedException` in each; 12 audit entries `PARAMETER_ACCESS_DENIED` with the requester, the last one with `PROTOCOL`; nothing is saved | `inactiveUnknownOrNullCannotReadProtocolsNorWrite` |
| TC17S-42 | the 5 reads, `getProtocols`, `saveProtocol`, `deactivatePriority` | `lmartin` (`CONFIGURE_SYSTEM` only) saves `P2` and deactivates `URGENT` | Reads as the administrator; 2 protocols; `URGENT` inactive; audit `PARAMETER_CREATED PROTOCOL:P2` and `PARAMETER_DEACTIVATED PRIORITY:URGENT`, no denial | `configuratorStillReadsAndWrites` |
| TC17S-43 | `getTestType` | `druiz`, `aortega` (parameterized), id `NOPE`; empty repositories | `ValidationException` "There is no test type with the id NOPE", the same errors as for `admin`; nothing audited | `unknownTestTypeGivesTheSameError` |
| — | every operation | `mgarcia` (active, none) | `AccessDeniedException`, audited with the group | existing TC17S-06 |
| — | the 5 reads, `getProtocols` | `admin` | Data returned, not audited | existing TC17S-01 |

Postcondition of every case: a denied operation changes no repository and adds exactly one audit entry; an allowed
read changes nothing and adds no audit entry.

## Not defined
- The plan says "the twelve write methods" (Q2) and "the twelve writes" (*State for Adam*); the code has **eleven**
  writes plus `getProtocols`, which are the twelve calls of `checkPermission`. The design uses the eleven writes of
  the code; the plan's wording should be checked by its owner.
- A user with both `ORDER_PROCEDURES` and `MANAGE_APPOINTMENTS`, or with a read permission and `CONFIGURE_SYSTEM`,
  is not in the plan nor in the roles of C1 1.2.4; no case is designed for it (it only takes the false branch of A
  or B, already covered).
