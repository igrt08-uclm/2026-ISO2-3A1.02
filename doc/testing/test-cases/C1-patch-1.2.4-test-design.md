# Test design · Patch of C1 1.2.4 (#173) – permissions ORDER_PROCEDURES and MANAGE_APPOINTMENTS

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#173 and the plan `doc/plans/C1-patch-1.2.4.md` (Q2, Q3, Q5).

Code under test (`server-c1-access` 1.2.4): `InMemoryPermissionRepository.findAll()` and `findByCode(String code)`,
`InMemoryRoleRepository.findAll()` and `findById(String id)`.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| `findByCode` | `code` | Existing code (7 values) | `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS`, `MANAGE_PATIENTS`, `CONSULT_AUDIT`, `ORDER_PROCEDURES`, `MANAGE_APPOINTMENTS` |
| | | Boundary values of the list | `MANAGE_ROLES` (first, Limit Val.), `MANAGE_APPOINTMENTS` (last, Limit Val.) |
| | | Non-existing code | `VIEW_AUDIT_LOG` (removed permission) |
| | | Conjeture | `null`, `""`, `manage_roles`, `order_procedures`, `" ORDER_PROCEDURES"`, `"MANAGE_APPOINTMENTS "`, `"ORDER PROCEDURES"` |
| `findById` | `id` | Existing role (4 values) | `SYSTEM_ADMINISTRATOR`, `PHYSICIAN`, `ADMISSIONS_STAFF`, `PATIENT` |
| | | Non-existing role | `GHOST` |
| | | Conjeture | `null`, `""`, `physician` |
| `findAll` | (none; state of the repository) | The seed data of the patch | 7 permissions; 4 roles |

Outputs: the permission or role found (the same object as the constant), or `null`; no exceptions are thrown.

## 2. Combination of the values
Each method has one parameter, so **each use** and **pairwise** give the same suite: every testing value is used once.
Maximum number of test cases: there is no maximum for `code` and `id` (any `String`), so the domain is divided into
the classes of section 1; with them, 14 values for `findByCode` and 7 for `findById`.

## 3. Coverage (white box)
`findByCode` and `findById` are a loop with one decision `getCode().equals(code)` / `getId().equals(id)`:
- **Decision coverage:** the decision is true (an existing value) and false (the elements before it, and every
  element for a non-existing value, which returns `null`).
- **Loop boundaries:** the value found in the first iteration (`MANAGE_ROLES`, `SYSTEM_ADMINISTRATOR`), in the last
  one (`MANAGE_APPOINTMENTS`, `PATIENT`) and never (`null` returned).
- Each decision has a single condition, so condition/decision and MC/DC coverage are the same as decision coverage.

## 4. Test cases

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC18S-01 | `findAll` | New repository | `ORDER_PROCEDURES` ("Order diagnostic and periodic follow-up procedures") and `MANAGE_APPOINTMENTS` ("Manage appointments, waitlists and daily schedules") are the last two of 7 | `tc18s01NewPermissionsOfC4AndC5AreLastWithTheirDescriptions` |
| TC18S-02 | `findByCode` | `ORDER_PROCEDURES`, `MANAGE_APPOINTMENTS` | The constants of the repository | `tc18s02FindByCodeFindsTheNewPermissions` |
| TC18S-03 | `findById` | Each of the 4 roles (each use) | Administrator: `MANAGE_ROLES; CONFIGURE_SYSTEM; CONFIGURE_INTEGRATIONS; CONSULT_AUDIT`; Physician: `ORDER_PROCEDURES`; Admissions staff: `MANAGE_PATIENTS; MANAGE_APPOINTMENTS`; Patient: none | `eachRoleHasItsPermissions` |
| TC18S-04 | `findAll` | New repository | Only `PHYSICIAN` has `ORDER_PROCEDURES`; only `ADMISSIONS_STAFF` has `MANAGE_APPOINTMENTS` | `tc18s04EachNewPermissionBelongsToOneRole` |
| TC18S-05 | `findByCode` | Each of the 7 codes (each use, first and last included) | The permission at its position of `findAll` | `tc18s05FindByCodeFindsEachPermission` |
| TC18S-06 | `findByCode` | Conjeture: spaces, lower case, a space instead of `_` | `null` (exact comparison) | `tc18s06FindByCodeIsExact` |
| — | `findAll` | New repository | 7 permissions in the order of Q2 (`CONSULT_AUDIT` fifth) | `findAllReturnsTheSevenPermissionsInOrder`, `tc19s09aConsultAuditIsTheFifthPermissionWithItsDescription` |
| — | `findByCode` / `findById` | `null`, `""`, `VIEW_AUDIT_LOG`, `manage_roles` / `null`, `""`, `GHOST`, `physician` | `null` | existing tests of 1.2.3 |

Postcondition of every case: the repositories are not changed (they are read only).
