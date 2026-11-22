# Test design · Patch of C3 1.0.1 (#175) – reads of the patients for ORDER_PROCEDURES and MANAGE_APPOINTMENTS

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#175 and the plan `doc/plans/C3-patch-1.0.1.md` (Q2-Q5, *State for Adam* and *Cases to cover*).

Code under test (`server-c3-patients` 1.0.1): `PatientService.searchPatients(requester, filters)` and
`getPatient(requester, patientId)`, which now call the new private method `checkReadPermission(requester, resource)`;
`registerPatient` and `modifyPatient`, which keep `checkPermission` (only `MANAGE_PATIENTS`). The permission comes from
`IRoleManagementService.hasPermission`, which is `false` for a `null`, unknown or inactive user.

Users of the test double `FakeRoleManagementService` (the four last ones are new in this patch):

| User | Active | Permissions |
|---|---|---|
| `lmartin` | yes | `MANAGE_PATIENTS` |
| `admin` | yes | `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` |
| `mgarcia`, `jfernandez` | yes | none |
| `pnavarro` | no | `MANAGE_PATIENTS` |
| `rlopez` | yes | `ORDER_PROCEDURES` |
| `cruiz` | yes | `MANAGE_APPOINTMENTS` |
| `tgil` | no | `ORDER_PROCEDURES` |
| `evega` | no | `MANAGE_APPOINTMENTS` |
| `nobody` | (does not exist) | none |

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| all four | `requester` | Active with `MANAGE_PATIENTS` | `lmartin` |
| | | Active with only `ORDER_PROCEDURES` | `rlopez` |
| | | Active with only `MANAGE_APPOINTMENTS` | `cruiz` |
| | | Active with none of the three | `admin`, `mgarcia`, `jfernandez` |
| | | Conjeture: inactive with a read permission | `tgil` (`ORDER_PROCEDURES`), `evega` (`MANAGE_APPOINTMENTS`) |
| | | Inactive with `MANAGE_PATIENTS` | `pnavarro` |
| | | Unknown | `nobody` |
| | | Conjeture: `null` | `null` |
| (operation) | operation | Read | `searchPatients`, `getPatient` |
| | | Write | `registerPatient`, `modifyPatient` |
| `getPatient` | `patientId` (with a read user) | Existing | `P-0002` |
| | | Non-existing / Conjeture | `P-9999` / `null` |

Outputs:
- Allowed read: the patients found or the patient, and one audit entry `PATIENT_ACCESSED` with the resource
  (`patients` for `searchPatients`, `PATIENT:<id>` for `getPatient`).
- Denied operation: `AccessDeniedException` whose requester is the one given, one audit entry `PATIENT_ACCESS_DENIED`
  with the resource (`patients`, `PATIENT:<id>`, or `patients` if the id is `null`), and nothing saved or changed.
- `getPatient` with a non-existing or `null` id by an allowed user: `ValidationException`
  "There is no patient with the id …", and nothing audited.

## 2. Combination of the values
The permission depends on two parameters: the class of `requester` (7 classes) and the operation (4 values).
**Pairwise** of two parameters is the full product, so the maximum number of cases is 7 x 4 = 28. Each case below
runs several operations on one requester, so the 28 pairs are covered by these cases:

| Requester class | `searchPatients` | `getPatient` | `registerPatient` | `modifyPatient` |
|---|---|---|---|---|
| `MANAGE_PATIENTS` | TC12S-01 | TC12S-01 | TC12S-01 | TC12S-01 |
| only `ORDER_PROCEDURES` | TC12S-77 | TC12S-77 | TC12S-79 | TC12S-79 |
| only `MANAGE_APPOINTMENTS` | TC12S-78 | TC12S-78 | TC12S-79 | TC12S-79 |
| none | TC12S-02, -03, -04 | TC12S-02, -03, -04 | TC12S-02, -03, -04 | TC12S-02, -03, -04 |
| inactive with a read permission | TC12S-80 | TC12S-80 | TC12S-80 | TC12S-80 |
| unknown | TC12S-06 | TC12S-06 | TC12S-06 | TC12S-06 |
| `null` | TC12S-07 | TC12S-07 | TC12S-07 | TC12S-07 |

The `patientId` of `getPatient` with a read user is combined with each use: `P-0002` (TC12S-77, TC12S-78), `P-9999`
and `null` (TC12S-81).

## 3. Coverage (white box)
`checkReadPermission` has one decision with three conditions, evaluated with short circuit:
`!A && !B && !C`, where A = `hasPermission(requester, MANAGE_PATIENTS)`, B = `hasPermission(requester,
ORDER_PROCEDURES)` and C = `hasPermission(requester, MANAGE_APPOINTMENTS)`. The decision is true when the access is
denied.

| Case | A | B | C | Decision (denied) | Requester |
|---|---|---|---|---|---|
| 1 | true | (not evaluated) | (not evaluated) | false | `lmartin` (TC12S-01) |
| 2 | false | true | (not evaluated) | false | `rlopez` (TC12S-77) |
| 3 | false | false | true | false | `cruiz` (TC12S-78) |
| 4 | false | false | false | true | `mgarcia` (TC12S-03); also TC12S-02, -04 to -07, TC12S-80 |

- **Decision coverage:** true in case 4, false in cases 1 to 3.
- **Condition/decision coverage:** A is true in 1 and false in 2-4; B is true in 2 and false in 3-4; C is true in 3
  and false in 4; the decision is true and false.
- **MC/DC:** each condition changes the decision alone against case 4: A with the pair (1, 4), B with (2, 4) and C
  with (3, 4). Four cases (n + 1 for three conditions).
- `checkPermission` (writes) is not changed; its decision is still covered by TC12S-01 (true) and TC12S-79 (false
  for a user with only a read permission).

## 4. Test cases

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-77 | `searchPatients`, `getPatient` | `rlopez` (active, only `ORDER_PROCEDURES`); filters `""`, id `P-0002` | The same patients as for `lmartin` (`P-0001` to `P-0003`) and the same `P-0002`; audit `PATIENT_ACCESSED` `patients`, then `PATIENT_ACCESSED` `PATIENT:P-0002` | `orderProceduresUserReads` |
| TC12S-78 | `searchPatients`, `getPatient` | `cruiz` (active, only `MANAGE_APPOINTMENTS`); the same input | The same as TC12S-77 with author `cruiz` | `manageAppointmentsUserReads` |
| TC12S-79 | `registerPatient`, `modifyPatient` | `rlopez` and `cruiz` (each use); the valid patient Ana; new data of `P-0001` | `AccessDeniedException` with the requester; audit `PATIENT_ACCESS_DENIED` `patients` and `PATIENT_ACCESS_DENIED` `PATIENT:P-0001`; still 3 patients and `P-0001` unchanged | `readOnlyUserCannotWrite` |
| TC12S-80 | all four | Conjeture: `tgil` and `evega` (inactive, with a read permission; each use) | `AccessDeniedException` in the four operations, each audited with `PATIENT_ACCESS_DENIED` and its resource; nothing saved | `inactiveReadUserIsDenied` |
| TC12S-81 | `getPatient` | `rlopez` and `cruiz` (each use); ids `P-9999` and `null` | `ValidationException` "There is no patient with the id P-9999" / "… id null", as for `lmartin` (TC12S-50); nothing audited | `readUserGetsTheErrorOfAnUnknownId` |
| — | all four | `lmartin`, `admin`, `mgarcia`, `jfernandez`, `pnavarro`, `nobody`, `null` | As before the patch (the reads now go through `checkReadPermission`) | existing tests TC12S-01 to TC12S-07 |

Postcondition of every case: no patient is saved or changed (only reads, or denied writes), and the audit holds only
the entries listed.

## Findings
- TC12S-03 ("a physician is denied in every operation") still passes with `mgarcia`, who has no permission in the test
  double. Since C1 1.2.4 the real physician has `ORDER_PROCEDURES` (`InMemoryRoleRepository.java:15`), so a real
  physician is now allowed to read (TC12S-77) and the name of TC12S-03 no longer matches the real roles. Proposal for
  Testing to decide: rename it (for example "an active user without any permission is denied in every operation") or
  keep it. It has not been changed.

## Not defined
- A user with `MANAGE_PATIENTS` and also a read permission is not defined by #175 or the plan; by the short circuit
  it behaves as `lmartin`, so no case is written.
