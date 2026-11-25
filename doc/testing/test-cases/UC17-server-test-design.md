# Test design · UC17 – Server (C2 v1.0.0) · #40

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#38, #40, the contracts of #33, the answers Q1–Q7 and Q9 of the plan `doc/plans/UC17-server.md` and, for TC17S-35,
F-20 of #122 (patch C2 1.1.1, #123, #125).

Code under test (`server-c2-configuration`, branch `develop`): `SystemParameterService` (the 17 operations of
`ISystemParameterService` and `loadSeedData()`), `InMemoryConfigurationRepository`, the seed data, and the data
classes of C2 in `common` (`TestType`, `Protocol`, `Schedule`, `AppointmentPolicy`, `Priority`, `ValidationException`).

**Test data.** `FakeRoleManagementService` (users of C1) and `RecordingAuditRecorder` (keeps every entry: author,
action, resource):

| User | Active | Permissions |
|---|---|---|
| `admin` | yes | `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` |
| `mgarcia` | yes | none (physician) |
| `pnavarro` | **no** | `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` |
| `nobody` | — (does not exist) | — |

(The fake also has `lmartin`, active with `CONFIGURE_SYSTEM`; it is used by the tests of UC20, not by these.)
The valid test type: `CT-HEAD`, name "Head CT", modality "CT", room "Room CT-1", 20 minutes, priority `URGENT`.
Since ce14819 the priority must exist (Q9), so every case that saves a test type first saves the priority `URGENT`.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| every operation (`checkPermission`) | `requester` | Active user with `CONFIGURE_SYSTEM` | `admin` |
| | | Active user without `CONFIGURE_SYSTEM` | `mgarcia` |
| | | Inactive user with `CONFIGURE_SYSTEM` | `pnavarro` |
| | | Unknown user | `nobody` |
| | | Conjeture | `null` |
| `createTestType` | `testType` | Valid (all fields filled, duration > 0, priority exists and is active, new id) | `CT-HEAD` with `URGENT` / `ROUTINE` |
| | | Empty required fields (id, name, modality, room, priority) | `""` in all of them |
| | | `durationMinutes` ≤ 0 | `0` (Limit Val.), `-1` |
| | | `durationMinutes` > 0 | `1` (Limit Val.), `20` |
| | | Duplicated id | `CT-HEAD` twice |
| | | Priority that does not exist | `XYZ`; `URGENT` with no priorities saved |
| | | Priority that exists but is deactivated (new test type) | `CT-NECK` with `URGENT` deactivated |
| | | Conjeture | `null`; fields of spaces `"   "`; empty priority `""` |
| `updateTestType` | `testTypeId` | Existing id | `CT-HEAD` |
| | | Non-existing id | `NOPE` |
| | `testType` | Same id, valid | `CT-HEAD` with 30 minutes, with `ROUTINE`/`URGENT` |
| | | Another id | `CT-NEW` |
| | | Priority that does not exist | `XYZ` |
| | | Deactivated priority (existing test type) | `URGENT` deactivated |
| `getTestType` | `testTypeId` | Existing / non-existing / Conjeture | `CT-HEAD` / `NOPE` / `null` |
| `saveProtocol`, `saveAppointmentPolicy`, `savePriority` | entry | Valid new id | `P1`, `A1`, `URGENT` |
| | | Valid existing id (replace) | `P1` with another name |
| | | Empty id and name | `""`, `" "`, `null` |
| | | Conjeture | `null` entry |
| `saveSchedule` | `schedule` | Valid | `Room CT-1`, Monday–Friday, 08:00–15:00 |
| | | End after start | 08:00–08:01 (Limit Val.) |
| | | End not after start | 08:00–08:00 (Limit Val.), 15:00–08:00 |
| | | No days | `Set.of()`, `null` |
| | | No resource / no times | `""`; start and end `null`, end `null` |
| | | Conjeture | `null` schedule |
| `deactivate*` (5 groups) | id | Existing active / existing inactive / non-existing | `CT-HEAD`, `P1`, `Room CT-1`, `A1`, `URGENT` / the same again / `NOPE` |
| `loadSeedData` | (none; state) | Empty system / system with data | new service / after a first load / after saving one priority |

Outputs: the list or the entry saved; `AccessDeniedException` (requester not allowed, audited as
`PARAMETER_ACCESS_DENIED` with the group); `ValidationException` with **every** error at once (nothing saved or
audited); audit entries `PARAMETER_CREATED`, `PARAMETER_UPDATED`, `PARAMETER_DEACTIVATED` with `GROUP:id`.

## 2. Combination of the values
- `requester` is checked before anything else, so its 4 invalid classes are combined **each use** with a valid entry
  (TC17S-02 to TC17S-06), and the invalid values of the entries are combined only with `admin`. TC17S-06 combines the
  invalid requester with all 17 operations (each use of the operation).
- The fields of a test type are not combined pairwise: each invalid class is tested alone (TC17S-11, -12, -13, -15,
  -34, -35) and all together (TC17S-09, TC17S-10), because the service accumulates the errors (Q1).
- Maximum number of test cases: there is no maximum, because `requester`, the ids and the text fields are any
  `String` and the duration any `int`; with the classes of section 1, `createTestType` alone would have 5 requesters ×
  12 classes of test type = 60 combinations (all), reduced to 4 + 12 = 16 with each use, as above.

## 3. Coverage (white box)

| Method | Decision (conditions) | Decision / condition coverage | MC/DC |
|---|---|---|---|
| `canConfigure` | `requester == null` | T: TC17S-05; F: the rest | single condition |
| | `activeUser && hasPermission` (`activeUser` = `username.equals && isActive`) | `activeUser` F: TC17S-03 (inactive), TC17S-04 (unknown); T+perm T: TC17S-01; T+perm F: TC17S-02 | `activeUser`: TC17S-01 vs -03; permission: TC17S-01 vs -02 |
| `checkPermission` | `!canConfigure` | T: TC17S-02..06; F: TC17S-01, -08 | single condition |
| `createTestType` | `testType != null && !blank(id) && findById(id) != null` | (F): TC17S-13; (T,F): TC17S-09, -10; (T,T,F): TC17S-08; (T,T,T): TC17S-12 | each condition: TC17S-13 / -09 / -08 against TC17S-12 |
| `updateTestType` | `existing` (`entry == null`) | T: TC17S-16; F: TC17S-14 | single condition |
| | `testType != null && !id.equals(testType.getId())` | (T,T): TC17S-15; (T,F): TC17S-14 | 2nd condition: TC17S-15 vs -14; **1st not covered** |
| `validate` | `testType == null` | T: TC17S-13; F: the rest | single condition |
| | `requireText` → `blank` (`value == null \|\| isBlank`) | null: TC17S-19 (policy id); `""`: TC17S-09; spaces: TC17S-10; filled: TC17S-08 | covered |
| | `!blank(priority) && priority == null` | (T,T): TC17S-34 (`XYZ`, no priorities); (F,-): TC17S-34 (empty priority), -09; (T,F): TC17S-08 | both covered |
| | `priority != null && !isActive && !blank(id) && findById(id) == null` | (F): TC17S-34 empty; (T,F): TC17S-08; (T,T,T,T): TC17S-35 new `CT-NECK`; (T,T,T,F): TC17S-34 deactivated, update | C1, C2, C4 covered; **C3 (`!blank(id)` false) not covered** |
| | `durationMinutes <= 0` | T: TC17S-11 (0, -1); F: TC17S-11 (1), -08 | single condition, both limits |
| `saveSchedule` | `schedule == null` | T/F: TC17S-22 / -20 | single |
| | `days == null \|\| days.isEmpty()` | (T): TC17S-22 null; (F,T): TC17S-22 empty; (F,F): TC17S-20 | both covered |
| | `start == null \|\| end == null` | (T): TC17S-22 both null; (F,T): TC17S-22 end null; (F,F): TC17S-20 | both covered |
| | `!end.isAfter(start)` | T: TC17S-21 equal, before; F: TC17S-21 +1 min | single |
| `saveProtocol`, `…Policy`, `savePriority` | entry `== null`; `requireText` id, name | T/F: TC17S-19 / -17 | single conditions |
| `save` (private) | `!errors.isEmpty()`; `isNew` | errors T: TC17S-09; F: -08; new T: TC17S-08, -17; F: TC17S-14, -18 | single conditions |
| `deactivate` (private) | `existing`; `isActive` | not existing: TC17S-25; active: TC17S-23; inactive: TC17S-24 | single conditions |
| `loadSeedData` | 5 `findAll().isEmpty()` joined with `&&` | T: TC17S-26; F: TC17S-27 (test types), -28 (priorities) | C1 and C5 covered; **C2, C3, C4 not covered** |

`InMemoryConfigurationRepository` and the data classes have no decisions; their statements are covered by
TC17S-30 to TC17S-33.

**Not covered (to add).** The expected result of each one is to be given by Testing; the code today does what is said:
1. `validate`, C3 of the deactivated-priority decision: a **new** test type with an empty id and a deactivated
   priority (the code gives "The id is required" and not "is deactivated").
2. `updateTestType` with a `null` test type (`testType != null` false): the code gives "The test type is required".
3. `loadSeedData` when only protocols, only schedules or only policies have data (conditions 2, 3 and 4 false alone).

## 4. Test cases

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC17S-01 | the 5 reads | EC valid: `admin` calls each read (`getTestTypes`, `getProtocols`, `getSchedules`, `getAppointmentPolicies`, `getPriorities`) | The list is returned; nothing is audited (reads are not audited) | `administratorReadsEveryGroupWithoutAudit` |
| TC17S-02 | `createTestType` | EC invalid: `mgarcia` (active, no permission) calls `createTestType` | `AccessDeniedException`; nothing saved; audit `PARAMETER_ACCESS_DENIED`, author `mgarcia`, resource `TEST_TYPE` | `userWithoutPermissionIsDeniedAndAudited` |
| TC17S-03 | `getTestTypes` | EC invalid: `pnavarro` (has the permission but is inactive) calls `getTestTypes` | `AccessDeniedException` | `inactiveUserIsDenied` |
| TC17S-04 | `saveProtocol` | EC invalid: `nobody` (unknown) calls `saveProtocol` | `AccessDeniedException`; audit `PARAMETER_ACCESS_DENIED` with resource `PROTOCOL` | `unknownUserIsDenied` |
| TC17S-05 | `getPriorities` | Conjeture: `null` requester calls `getPriorities` | `AccessDeniedException`, no other exception | `nullRequesterIsDenied` |
| TC17S-06 | all 17 operations | EC invalid, one per group: `mgarcia` calls one operation of each group (test type, protocol, schedule, policy, priority) | Always `AccessDeniedException`, and the resource of the audit entry is the group | `everyOperationChecksThePermission` |
| TC17S-07 | `createTestType` | Conjeture: `mgarcia` is denied, then `admin` makes the same call | The second call works: the denial leaves nothing behind | `denialLeavesNothingBehind` |
| TC17S-08 | `createTestType` | EC valid: `admin` creates the valid `CT-HEAD` | It is returned and saved; `getTestType("CT-HEAD")` gives it; audit `PARAMETER_CREATED`, resource `TEST_TYPE:CT-HEAD` | `createValidTestType` |
| TC17S-09 | `createTestType` | EC invalid: id, name, modality, room and priority empty and duration 0 | `ValidationException` with **6 errors together**; nothing saved; nothing audited | `allValidationErrorsTogether` |
| TC17S-10 | `createTestType` | Conjeture: fields made only of spaces (`"   "`) | Treated as empty: one error per field | `blankFieldsCountAsEmpty` |
| TC17S-11 | `createTestType` | Limit Val. of the duration: `0`, `-1`, `1` | `0` and `-1` rejected ("greater than zero"); `1` accepted | `nonPositiveDurationIsRejected` (0, -1), `durationOfOneMinuteIsAccepted` |
| TC17S-12 | `createTestType` | EC invalid: create `CT-HEAD` twice | Second call: `ValidationException` "already exists"; the first entry does not change | `duplicatedIdIsRejected` |
| TC17S-13 | `createTestType` | Conjeture: `createTestType(admin, null)` | `ValidationException` "The test type is required" | `nullTestTypeIsRejected` |
| TC17S-14 | `updateTestType` | EC valid: `CT-HEAD` saved; update it with duration 30 | Saved; audit `PARAMETER_UPDATED` | `updateExistingTestType` |
| TC17S-15 | `updateTestType` | EC invalid: update `CT-HEAD` with a test type whose id is `CT-NEW` | `ValidationException` "The id of a test type cannot be changed" | `idCannotBeChanged` |
| TC17S-16 | `getTestType`, `updateTestType` | EC invalid: an id that does not exist (`NOPE`), and `null` | `ValidationException` "There is no test type with the id …" | `unknownOrNullIdIsRejected` |
| TC17S-17 | `saveProtocol`, `saveAppointmentPolicy`, `savePriority` | EC valid: id and name | Saved; audit `PARAMETER_CREATED` with `PROTOCOL:<id>`, `APPOINTMENT_POLICY:<id>`, `PRIORITY:<id>` | `saveSimpleEntries` |
| TC17S-18 | `saveProtocol` | EC valid: save again an entry with the same id and another name | It replaces the old one; audit `PARAMETER_UPDATED` | `saveAgainReplaces` |
| TC17S-19 | `saveProtocol`, `saveAppointmentPolicy`, `savePriority` | EC invalid: empty id and name, and `null` | `ValidationException` (2 errors, or "is required" for `null`) | `simpleEntriesWithoutIdOrNameAreRejected` |
| TC17S-20 | `saveSchedule` | EC valid: `Room CT-1`, Monday–Friday, 08:00–15:00 | Saved; audit `SCHEDULE:Room CT-1` | `saveValidSchedule` |
| TC17S-21 | `saveSchedule` | Limit Val. of the times: end = start (08:00–08:00), one minute after (08:00–08:01), before (15:00–08:00) | Equal and before rejected ("end time must be after the start time"); one minute after accepted | `endTimeBoundaries` |
| TC17S-22 | `saveSchedule` | EC invalid: no days (empty set and `null`), no resource, no start or end time | One error for each problem, all together | `incompleteScheduleGetsEveryError` |
| TC17S-23 | the 5 `deactivate*` | EC valid, one per group: deactivate `CT-HEAD`, a protocol, a schedule, a policy and a priority | The entry still exists with `active = false`; the other fields do not change; audit `PARAMETER_DEACTIVATED` | `deactivateEveryGroup` |
| TC17S-24 | `deactivateTestType` | Conjeture: an entry that is already inactive | Nothing changes and **no second audit entry** | `deactivateTwiceIsAuditedOnce` |
| TC17S-25 | the 5 `deactivate*` | EC invalid: an id that does not exist | `ValidationException` "There is no …" | `deactivateUnknownIdIsRejected` |
| TC17S-26 | `loadSeedData` | EC valid: empty service | 6 test types (`XRAY-CHEST`, `CT-HEAD`, `MRI-KNEE`, `US-ABDOMEN`, `MAMMO-BILATERAL`, `DEXA-SPINE`), 3 priorities (`URGENT`, `PREFERENTIAL`, `ROUTINE`), 1 protocol, 1 schedule, 1 policy; all active; nothing audited | `seedDataIsLoadedOnAnEmptySystem` (`SeedDataTest`) |
| TC17S-27 | `loadSeedData` | Conjeture: call it twice | The second call loads nothing more (still 6 test types) | `seedDataIsLoadedOnlyOnce` |
| TC17S-28 | `loadSeedData` | EC invalid: one priority saved first | Nothing is loaded: there was already data | `seedDataIsNotLoadedWhenThereIsData` |
| TC17S-29 | `loadSeedData` | Conjeture: after loading, check every seed test type | All the required fields are filled, duration > 0, and its priority is one of the 3 seed priorities | `seedTestTypesAreComplete` |
| TC17S-30 | `InMemoryConfigurationRepository` | EC valid: save, `findById`, `findAll` | Found by its id; unknown id gives `null`; saving the same id replaces it; the order of insertion is kept; the list cannot be modified from outside | `findById`, `findAllKeepsTheOrder`, `saveSameIdReplaces`, `findAllCannotBeModified` |
| TC17S-31 | data classes | EC valid: getters and `deactivated()` of `TestType`, `Protocol`, `Schedule`, `AppointmentPolicy`, `Priority` | `deactivated()` returns a copy with `active = false` and the rest equal; the original does not change | `TestTypeTest` (`keepsEveryField`, `deactivatedReturnsAnInactiveCopy`), `SimpleConfigurationEntriesTest` (`protocol`, `appointmentPolicy`, `priority`), `ScheduleTest` (`keepsItsData`, `nullDaysStayNull`, `deactivatedReturnsAnInactiveCopy`) |
| TC17S-32 | `Schedule` | Conjeture: a modifiable set of days, changed outside | The schedule keeps its own copy of the days | `keepsItsOwnCopyOfTheDays` |
| TC17S-33 | `ValidationException` | EC valid: two errors | `getErrors()` gives both; the message joins them with `; ` | `keepsEveryError`, `keepsItsOwnCopy` |
| TC17S-34 | `createTestType`, `updateTestType` | EC invalid / EC valid / Conjeture: create with priority `XYZ` (not saved), create `CT-HEAD` with no priorities saved, update `CT-HEAD` with `XYZ`; create and update with another saved priority (`ROUTINE`); create and update with a priority that exists but is deactivated; create with an empty priority | `XYZ` and the missing `URGENT`: `ValidationException` with only "The priority <id> does not exist", nothing saved or changed and no test type audit entry; an existing priority, active or deactivated, is accepted; an empty priority only gets "The priority is required" (so TC17S-09 still has 6 errors) | `createWithUnknownPriorityIsRejected`, `createWithoutAnyPriorityIsRejected`, `updateWithUnknownPriorityIsRejected`, `existingPriorityIsAccepted`, `deactivatedPriorityIsAccepted`, `emptyPriorityOnlyGetsRequired` |
| TC17S-35 | `createTestType`, `updateTestType` | EC invalid / EC valid (since patch C2 1.1.1): create a **new** `CT-NECK` with `URGENT` deactivated; a new one with another active priority; update the existing `CT-HEAD` with `URGENT` deactivated | New with the deactivated priority: `ValidationException` "The priority URGENT is deactivated", nothing saved or audited; with an active priority: accepted; updating the existing one: still accepted (Q9) | `newTestTypeWithDeactivatedPriorityIsRejected`, `newTestTypeWithActivePriorityIsAccepted`; the update is checked by `deactivatedPriorityIsAccepted` (named TC17S-34) |

Test classes: TC17S-01..25, -34, -35 in `SystemParameterServiceTest`; TC17S-26..29 in `SeedDataTest`; TC17S-30 in
`InMemoryConfigurationRepositoryTest` (its display names carry no TC id); TC17S-31..33 in the tests of `common`.

Postcondition of every case: each test starts from a new service with empty repositories; after a rejected call
(`AccessDeniedException` or `ValidationException`) the repositories are as they were and only a denial is audited;
reads never change the state or the audit.

## Findings while designing (to ask Javier, not tested as errors)

1. **Fixed in ce14819.** The priority of a test type was not checked against the priorities that exist (a test type
   with priority `XYZ` was accepted). Javier added Q9 to the plan (option B): the server rejects it with
   "The priority <id> does not exist" in create and in update, a deactivated priority is still valid, and an empty
   priority only gets "The priority is required". It is now tested by TC17S-34.
2. **The C1 patch (#109)** adds `CONFIGURE_SYSTEM` to the administrator. It is already covered by the tests of C1 that
   Javier updated (`InMemoryPermissionRepositoryTest`, `InMemoryRoleRepositoryTest`); there is nothing more to test here.
