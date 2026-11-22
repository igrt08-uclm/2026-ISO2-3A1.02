# Test design · UC19 – Audit system activity and access to clinical data

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Component C1, iteration 1,
issues #11 (use case), #14 (client), #15 (server). Expected results come from #15, the plans `doc/plans/UC19-server.md`
and `doc/plans/C1-patch-1.2.3.md` (Q2, Q4), and the code of `develop`.

Code under test (`server-c1-access`, `develop`): `AuditService.getActivityLog(requester, filters)` with the private
check `canConsultAudit`, `AuditService.register(author, action, targetResource)`, and
`InMemoryAuditLogRepository.find(filters)`, which parses the filters. Screen: `IU_Audit` (`client-c1-access`), which
builds the filters `key=value;key=value` from its fields.

Since the patch 1.2.3 (#172, already in `develop`), the check is by permission: the requester must exist, be active
and have `CONSULT_AUDIT` in one of its roles, read from the repository of roles. With the seed data only `admin`
(System administrator) has it.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| `getActivityLog` | `requester` | Valid: active user whose current role has `CONSULT_AUDIT` | `admin`, `ana`, `eva` (two roles, one with it) |
| | | Invalid: active user without `CONSULT_AUDIT` | `bob`, `mgarcia`, `lmartin`, `jfernandez` |
| | | Invalid: inactive user with `CONSULT_AUDIT` | `old` |
| | | Invalid: unknown user | `ghost` |
| | | Conjeture: `null`; a role of the account no longer in the repository (alone / with another role that has it); the copy of the role in the account without permissions | `null`, `lost`, `mix`, `ana` with an empty copy |
| `getActivityLog` → `find` | `filters` | No filter | `null`, `""`, `"   "`, `";"` |
| | | `author=<u>` (exact) | `author=admin`, `author=bob`; no match: `author=adm`, `author=ADMIN`, `author=ghost` |
| | | `action=<a>` (exact) | `action=LOGIN_FAILED`, `action=AUDIT_ACCESS_DENIED`; no match: `action=LOGIN` |
| | | `target=<text>` (contains) | `target=PAT-1`, `target=UNKNOWN`, `target=authentication` (2 entries) |
| | | `from=<yyyy-MM-dd>` (included) | `from=2026-11-02` (Limit Val.: the entry of that day is included, the one before is not) |
| | | `to=<yyyy-MM-dd>` (included, whole day) | `to=2026-11-02`; `to=2026-11-01` with entries at `23:59:59` (Limit Val., in) and `00:00:00` of the next day (Limit Val., out) |
| | | Several filters (AND) | `from=2026-11-02;to=2026-11-02`, `author=admin;action=LOGIN_FAILED;from=2026-11-01`; no match: `author=admin;action=VIEW_REPORT` |
| | | Invalid: unknown key, pair without `=`, wrong date | `colour=red`, `author`, `from=yesterday`, `to=2026-13-01`, `from=01/11/2026` |
| | | Conjeture: spaces and empty pairs | `" author = admin "`, `author=admin;`, `;author=admin`, `author=admin; ;` |

Outputs and exceptions:
- Allowed: the entries that meet every filter, newest first (same date: highest id first), and then one new entry
  `AUDIT_CONSULTED` on `audit` by the requester, which is not in its own result.
- Denied: `AccessDeniedException(requester)`, one `AUDIT_ACCESS_DENIED` on `audit`; the filters are never searched.
- Wrong filters: `IllegalArgumentException` and no `AUDIT_CONSULTED` (on the screen: "Check the dates: use the
  format yyyy-MM-dd.").

## 2. Combination of the values
`getActivityLog` has two parameters, `requester` (5 classes + conjeture) and `filters` (8 classes). An invalid
requester stops the method before the filters are read, so it is combined only with `null` filters (and once with
`author=ana`, to check that the search is not reached).
- **All combinations** (maximum number of test cases): 9 requester values × 8 filter classes = 72, and the filter
  keys alone have 2⁵ = 32 subsets.
- **Pairwise** and **each use** coincide for the requester, since only the valid one reaches the filters.
- **Each use** is the one chosen for the filters: every key alone, one or two combinations of several keys, the
  invalid classes and the boundary dates, all tested on `InMemoryAuditLogRepository.find` directly; `AuditService`
  is tested with one valid filter and with filters that fail.

## 3. Coverage (white box)

| Method | Decision | True with | False with |
|---|---|---|---|
| `getActivityLog` | `!canConsultAudit(requester)` | `bob`, `old`, `ghost`, `null`, `lost` | `ana`, `eva`, `mix` |
| `canConsultAudit` | `user != null && user.isActive() && anyMatch(CONSULT_AUDIT)` | `ana` (T, T, T) | `ghost`, `null` (C1 F); `old` (T, F); `bob` (T, T, F) |
| `canConsultAudit` | `filter(Objects::nonNull)` | the roles in the repository | `lost`, `mix` (TC19S-05) |
| `nextId` | `max().orElse(0)` | empty log (first id 1) | log with entries (next id) |
| `parseFilters` | `filters == null \|\| filters.isBlank()` | `null` (C1 T); `""`, `"   "` (C1 F, C2 T) | `author=admin`, `";"` (both F) |
| `parseFilters` | `!pair.isBlank()` | `author=admin` | `;author=admin`, `author=admin; ;` (`";"` gives no pair at all) |
| `parseFilters` | `separator < 0` | `author` | every pair with `=` |
| `filterFor` | `switch (key)` | each of `author`, `action`, `target`, `from`, `to` | `default`: `colour=red` |
| `filterFor` (target) | `getTargetResource() != null && contains(value)` | `target=PAT-1` | `target=UNKNOWN` on `PAT-1` (C1 T, C2 F); **C1 false never** |
| `filterFor` (from / to) | `!isBefore(from)` / `!isAfter(to)` | the entry of the boundary day | the day before / the day after |
| `parseDate` | `catch DateTimeParseException` | `yesterday`, `2026-13-01`, `01/11/2026` | `2026-11-02` |

- **Decision coverage:** reached in `AuditService` and in `InMemoryAuditLogRepository`.
- **Condition/decision and MC/DC:** reached for `canConsultAudit` (`ghost` vs `ana` for C1, `old` vs `ana` for C2,
  `bob` vs `ana` for C3) and for the first decision of `parseFilters` (`null` vs `author=admin`, `""` vs
  `author=admin`). **Not reached** for the target filter: its first condition (`getTargetResource() != null`) is never
  false.

## 4. Test cases (manual)

**Test data:** the seed users of the simulated SSO. Only `admin` / `Admin.2026` is an active system administrator.
The audit log starts empty every time the program starts (D-030), so each case first creates the activity it needs
(logins with the users below).

Rule of minimums: 1 normal + at least 2 error + 1 permission case.

| ID | Title | Type | Quality characteristic | Steps | Expected result | Actual result | Pass | Date / tester |
|---|---|---|---|---|---|---|---|---|
| TC19-01 | Administrator consults the whole log | normal | Security (non-repudiation) | 1. Log in as `mgarcia` / `Physician.2026`, log out. 2. Log in as `nobody` / `x`. 3. Log in as `admin` / `Admin.2026`, click *View audit log*, click *Search* with no filters. | The table shows the entries newest first, with date, user, action and resource: `LOGIN_OK` of `mgarcia`, `LOGIN_FAILED` of `nobody` (`authentication:UNKNOWN_USER`), `LOGIN_OK` of `admin`. A new `AUDIT_CONSULTED` entry by `admin` is recorded. | | | |
| TC19-02 | Filter by user and action | normal | Functional suitability | After TC19-01, as `admin`: User = `mgarcia`, Action = `LOGIN_OK`, *Search*. | Only the `LOGIN_OK` entries of `mgarcia` are shown, and the counter says how many ("1 entry"). | | | |
| TC19-03 | Filter by dates | normal | Functional suitability | As `admin`: From = today, To = today (`yyyy-MM-dd`), *Search*. Then From = tomorrow. | With today: all of today's entries. With tomorrow: no rows and the message "No activity matches these filters." | | | |
| TC19-04 | Wrong date format | error | Usability (user error protection) | As `admin`: From = `10/11/2026`, *Search*. | Nothing is searched and the message "⚠ Check the dates: use the format yyyy-MM-dd." is shown. The application does not crash. | | | |
| TC19-05 | Filter that matches nothing | error | Usability | As `admin`: User = `ghost`, *Search*. | Empty table and the message "No activity matches these filters." | | | |
| TC19-06 | User without the administrator role | permission | Security + Functional suitability | Log in as `lmartin` / `Admissions.2026`. | The welcome screen has no *View audit log* button. Calling `getActivityLog("lmartin", null)` from a test throws `AccessDeniedException`. | | | |
| TC19-07 | Denied access is also recorded | permission / audit | Security (non-repudiation) | 1. Call `getActivityLog("jfernandez", null)` (or log in as a non-administrator and try to open the audit). 2. Log in as `admin` and consult the log with Action = `AUDIT_ACCESS_DENIED`. | An `AUDIT_ACCESS_DENIED` entry with author `jfernandez` and resource `audit` is in the log. | | | |
| TC19-08 | Inactive administrator | permission | Security | Make `admin` inactive (a test with its own repository), then call `getActivityLog("admin", null)`. | `AccessDeniedException`: an inactive account cannot consult the log. | | | |
| TC19-09 | The log is append-only | audit | Security (integrity) | As `admin`, look for any option to edit or delete an entry. Consult the log twice. | There is no option to change or delete entries. The second consultation shows the first `AUDIT_CONSULTED` entry unchanged, plus a new one. | | | |

Postcondition of every case: no entry of the log is changed or removed; each consultation adds exactly one entry,
`AUDIT_CONSULTED` or `AUDIT_ACCESS_DENIED`, except a consultation with wrong filters, which adds none.

### Automated tests that cover it

| Manual case | JUnit test class | Key methods |
|---|---|---|
| TC19-01 | `AuditServiceIntegrationTest`, `AuditServiceTest`, `InMemoryAuditLogRepositoryTest`, `IU_AuditTest` | `adminCanConsultTheLogins`, `allowedConsultationIsRegisteredButNotInItsOwnResults`, `newestEntriesComeFirst`, `withTheSameDateTheHighestIdComesFirst`, `openingTheScreenShowsEveryEntry`, `eachRowShowsItsData` |
| TC19-02 | `InMemoryAuditLogRepositoryTest`, `IU_AuditTest` | `eachFilterFindsItsEntry`, `authorAndActionFiltersAreExact`, `severalFiltersOnlyFindEntriesThatMeetAllOfThem`, `selectedActionIsSent`, `everyFilterIsSent`, `statusShowsHowManyEntriesWereFound` |
| TC19-03 | `InMemoryAuditLogRepositoryTest`, `IU_AuditTest` | `datesAreIncludedInTheRange`, `toFilterIncludesTheWholeDay`, `eachTextFilterSendsItsPair` |
| TC19-04 | `InMemoryAuditLogRepositoryTest`, `AuditServiceTest`, `IU_AuditTest` | `wrongFiltersGiveAnError`, `wrongFiltersGiveAnErrorAndNoConsultationIsRegistered`, `wrongDateShowsAMessageAndKeepsTheTable`, `correctSearchAfterAnErrorRemovesTheMessage` |
| TC19-05 | `InMemoryAuditLogRepositoryTest`, `IU_AuditTest` | `filtersWithNoResultGiveAnEmptyList`, `statusShowsHowManyEntriesWereFound` |
| TC19-06, TC19-07 | `AuditServiceTest`, `AuditServiceIntegrationTest`, `IU_WelcomeViewAuditTest`, `IU_AuditTest` | `otherRequestersAreRejected`, `rejectedRequesterNeverReachesTheSearch`, `rejectedConsultationRegistersWhoTried`, `tc19s02ActiveUserWithoutConsultAuditIsDenied`, `otherUsersAreRejectedAndAdminSeesTheirAttempts`, `userWithAnotherPermissionOnlyDoesNotSeeTheButton`, `accessDeniedShowsAMessageAndEmptiesTheTable` |
| TC19-08 | `AuditServiceTest`, `AuditServiceIntegrationTest` | `tc19s03InactiveUnknownOrNullRequesterIsDenied` (`old`), `otherUsersAreRejectedAndAdminSeesTheirAttempts` (`pnavarro`) |
| TC19-09 | `InMemoryAuditLogRepositoryTest`, `IU_AuditTest` | `returnedListCannotChangeTheRepository`, `cellsCannotBeEdited` |
| Filters (error guessing) | `InMemoryAuditLogRepositoryTest`, `IU_AuditTest` | `noFiltersGiveEveryEntry`, `spacesAndEmptyPairsAreIgnored`, `valuesAreTrimmedAndEmptyOnesAreNotSent`, `separatorDoesNotAppearInTheTextFields` |

## 5. Findings while designing these cases (to report to Javier / Design)

- The *Action* filter of `IU_Audit` only offers `LOGIN_OK`, `LOGIN_FAILED`, `AUDIT_CONSULTED` and
  `AUDIT_ACCESS_DENIED`. The actions of UC18 (`ROLE_ASSIGNED`, `ROLE_REVOKED`, `PERMISSION_ASSIGNED`,
  `PERMISSION_REVOKED`, `ROLE_CHANGE_DENIED`) are recorded but cannot be selected in the filter. Still true on
  `develop`.
- `AuditService` still checks the role `SYSTEM_ADMINISTRATOR`, not a permission (comment "Temporary: with UC18 this
  check uses the permissions of the role"). Meeting 6, point 8, has to decide it. If it changes, TC19-06 and TC19-08
  change their expected result. **Update:** decided and done in the patch 1.2.3 (#172): the check is now
  `CONSULT_AUDIT` from the repository of roles. With the seed data TC19-06 and TC19-08 keep their expected result
  (`lmartin` has no `CONSULT_AUDIT`; an inactive account is denied); the title of TC19-06 should say "without
  `CONSULT_AUDIT`".
- `AuditServiceTest.otherRequestersAreRejected` is named "Unknown, null, inactive or non-administrator requesters are
  rejected", but its values are `ghost`, `old` and `bob`: `null` is not among them (it is tested in TC19S-03).
- `IU_Audit` shows "Check the dates: use the format yyyy-MM-dd." for every `IllegalArgumentException`, also for an
  unknown key or a pair without `=`; the screen cannot build those, so it only matters for other clients.

## 6. Not covered (to add)
- Target filter on an entry whose resource is `null` (`getTargetResource() != null` false): MC/DC of that decision is
  not reached.
- A filter with an empty value (`author=`, `target=`): `target=` matches every entry, `author=` none; not tested.
- `from` later than `to` (an empty range) and a range of several days with entries outside on both sides.
- `getActivityLog` with filters through the real `AuditService` and `InMemoryAuditLogRepository` other than
  `target=authentication` and `action=AUDIT_ACCESS_DENIED` (dates are only tested on the repository).
- A requester whose account has a role with `CONSULT_AUDIT` given with UC18 to a role that is not the
  administrator's, consulted with filters (TC19S-06 only uses `null` filters).

## 7. Not defined (to ask)
- What `from` later than `to` should give (today: an empty list, no error).
- Whether `author=` with an empty value is a wrong filter or "no filter" (the screen never sends it).
