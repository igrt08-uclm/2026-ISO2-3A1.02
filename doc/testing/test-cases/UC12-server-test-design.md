# Test design · UC12 – Server (C3 v1.0.0) · #46 – Manage patients

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#44, #46, the contract `IPatientService`, D-037, the answers Q2 to Q10 of the plan `doc/plans/UC12-server.md` (and the
cases of its step 8), and the fixes R-02, R-03, R-04 and F-11 of `doc/plans/UC12-testing-fixes.md` (findings of #122).

Code under test (`server-c3-patients`, branch `develop`): `PatientService` (`searchPatients`, `getPatient`,
`registerPatient`, `modifyPatient`, `loadSeedData` and the constructors), `PatientSeedData`,
`InMemoryPatientRepository` (`findById`, `findByIdentifier`, `search`, `save`) and the data classes of UC12 in
`common` (`Patient`, `Sex`, `DuplicatePatientException`). Tests: `PatientServiceTest`, `PatientSeedDataTest`,
`InMemoryPatientRepositoryTest`, `PatientTest`, `SexTest`, `DuplicatePatientExceptionTest`.

**Test data (precondition of every case).** A fake `IRoleManagementService` with the users of C1 and their permissions
as in C1 1.2.2 (`hasPermission` is the default method of the interface, the same rule as C1). The administrator does
**not** have `MANAGE_PATIENTS` (#46):

| User | Active | Permissions |
|---|---|---|
| `lmartin` | yes | `MANAGE_PATIENTS` (Admissions staff) |
| `admin` | yes | `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` |
| `mgarcia` | yes | none (physician) |
| `jfernandez` | yes | none (patient) |
| `pnavarro` | **no** | `MANAGE_PATIENTS` (so that only the inactive state denies him) |
| `nobody` | — (does not exist) | — |

A recording `IAuditRecorder` keeps every entry (author, action, resource). Unless a case says otherwise, the service
starts with `loadSeedData()` (P-0001 Lucía Ejemplo Prueba `HC-0000001`, P-0002 Mario Demo Simulado `HC-0000002`,
P-0003 Elena Muestra Ficticia `HC-0000003`) and the audit is cleared after it. The valid patient of the tests is
`HC-1234567`, "Ana Prueba Inventada", born 10/05/1990, `FEMALE`, national ID `00000000T`, address "Calle Falsa 1",
phone `600111222`, e-mail `ana.prueba@example.test`. The dates "today" and "tomorrow" are computed with
`LocalDate.now()` so that the tests do not depend on the day they are run.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| every operation | `requester` | Active user with `MANAGE_PATIENTS` | `lmartin` |
| | | Active user without `MANAGE_PATIENTS` | `admin`, `mgarcia`, `jfernandez` |
| | | User with `MANAGE_PATIENTS` but inactive | `pnavarro` |
| | | Unknown user | `nobody` |
| | | Conjeture | `null`; a denied user followed by an allowed one |
| `registerPatient` | `patient` | Not `null` | the valid patient |
| | | Conjeture | `null` |
| | `identifier` | New (not blank, no other patient with the same key) | `HC-1234567`, `HC-00000011` (Limit Val.: one more character than `HC-0000001`), `" hc 12-34 567 "` |
| | | Existing (same key: case, spaces and `-` ignored, R-02) | `HC-0000001`, `"  hc-0000001 "`, `HC 0000001`, `HC0000001`, `hc0000001`, `hc - 0000 001`, `" HC--0000001 "` |
| | | Blank | `null`, `""` (Limit Val.), `"   "` |
| | | Conjeture | an invalid patient (no sex) with an existing identifier; the same identifier registered twice |
| | `fullName` | Not blank | `Ana Prueba Inventada`; with spaces at the ends |
| | | Blank | `null`, `""` (Limit Val.), `"   "` |
| | `dateOfBirth` | From 01/01/1900 to today | 10/05/1990; 01/01/1900 (Limit Val.); today (Limit Val.) |
| | | Before 01/01/1900 | 31/12/1899 (Limit Val.), 15/06/1800 |
| | | In the future | tomorrow (Limit Val.), in a year |
| | | Conjeture | `null` |
| | `sex` | One of the enumeration | `FEMALE`, `MALE`, `UNSPECIFIED` |
| | | Conjeture | `null` |
| | `email` | Empty (optional) | `null`, `""`, `"   "`, `"\t"` |
| | | Form `name@domain.ext` | `a@b.c` (Limit Val.: shortest form), `name.surname@sub.example.test`, `ana+urg@example.test` |
| | | Other form | `ana`, `ana@example`, `ana.example.test`, `ana@@example.test`, `ana prueba@example.test`, `ana@`, `@example.test`, `ana@.test`, `ana@example.` |
| | | Conjeture (empty part, R-04) | `ana@example.test.`, `ana@example..test`, `.@` |
| | `phone` | Empty (optional) | `null`, `""`, `"   "`, `"\t"` |
| | | 9 to 15 digits, optional leading `+`, spaces allowed | `600111222` (9, Limit Val.), `346001112223334` (15, Limit Val.), `600 111 222`, `+34600111222`, `+34 600 111 222`, `6 0 0 1 1 1 2 2 2` (9), `346 001 112 223 334` and `+34 600 111 222 3334` (15) |
| | | Fewer than 9 or more than 15 digits | `60011122` (8, Limit Val.), `3460011122233344` (16, Limit Val.), `600 111 22`, `6 0 0 1 1 1 2 2` (8), `346 001 112 223 3345`, `+34 600 111 222 33345` (16) |
| | | Conjeture | `60011122a`, `+`, `++34600111222`, `600-111-222`, `34+600111222` |
| | `nationalId`, `address` | Any text, optional | `00000000T`, `Calle Falsa 1`; `null`, `""`, `"   "`, `"\t"` |
| | `patientId`, `active` (in the data) | Ignored by the server | Conjeture: `P-9999` and `false` |
| `modifyPatient` | `patientId` | Existing id | `P-0001`, `P-0004` (just registered) |
| | | Unknown id | `P-9999`, `p-0001` (lower case), `""` |
| | | Conjeture | `null` |
| | `patient` | Same rules as `registerPatient` | the values above (e-mail `lucia@example..test`, date 31/12/1899, today, blanks) |
| | `identifier` (in the data) | Same key as the stored one | `HC-0000001`, `" hc-0000001 "`, `HC 0000001`, `HC0000001`, `hc0000001`, `hc - 0000 001` |
| | | Another key | `HC-0000002`, `HC 0000002`, `HC0000011`, `HC-000001` |
| | | Blank | `" "` (gives two messages) |
| | `patientId`, `active` (in the data) | Ignored | Conjeture: `P-0002` and `false` |
| `getPatient` | `patientId` | Existing id | `P-0001`, `P-0002`, a patient just registered |
| | | Unknown id | `P-9999` |
| | | Conjeture | `null` |
| `searchPatients` | `filters` | Part of an identifier | `0002`, `hc-` |
| | | Part of a full name (case and accents ignored, R-03) | `muestra`, `EJEMPLO`, `"  mario  "`, `lucia`, `LUCIA`, `Lucía`, `LUCÍA`, `lucía ejemplo` |
| | | Empty (all the patients) | `""` (Limit Val.), `null`, `"   "` |
| | | No match | `zzz` |
| `loadSeedData` | (state of the repository) | Empty | new service |
| | | Not empty | after `loadSeedData()`; a patient registered before; a repository with P-0005 |
| `PatientService(repository, …)` | `repository` (ids stored) | Empty | new `InMemoryPatientRepository` |
| | | Ids `P-nnnn` | `P-0007`, `P-0003`, `P-0002` |
| | | Other ids (ignored when counting) | `Q-0050`, `P-00a9`, `P-` (Limit Val.: no digits), `P-123456789` (Limit Val.: 9 digits), `null` |
| `InMemoryPatientRepository.findById` | `patientId` | Stored / unknown / `null` | `P-0001`; unknown id; `null` |
| `InMemoryPatientRepository.findByIdentifier` | `identifier` | Stored (case and end spaces ignored) | `HC-0000001`, `"  hc-0000002 "` |
| | | Not stored | `HC-0000003`, `HC 0000001` (inner space is not ignored here) |
| | | Conjeture | `null`; a stored patient with `null` identifier |
| `InMemoryPatientRepository.search` | `filters` | Part of identifier / of name / empty / no match | `0002`, `" hc-0 "`; `EJEMPLO`, `lucia`, `" LUCÍA "`, `ejémplo`; `""`, `null`; `zzz`, `lucio`, `x` |
| `InMemoryPatientRepository.save` | `patient` | New id / existing id | `P-0003`; `P-0001` (replaces) |

Outputs: `searchPatients` returns the list of patients in the order of saving (maybe empty); `getPatient`,
`registerPatient` and `modifyPatient` return the patient read or saved. Exceptions: `AccessDeniedException` (no
permission, checked first), `ValidationException` with the list of messages of Q5 in their order (and "The identifier
cannot be changed", "There is no patient with the id …"), `DuplicatePatientException` carrying the existing patient.
Side effects: one audit entry per call (`PATIENT_ACCESSED`, `PATIENT_REGISTERED`, `PATIENT_MODIFIED`,
`PATIENT_ACCESS_DENIED`), none for a validation error.

## 2. Combination of the values

- **`registerPatient` and `modifyPatient`** have six validated fields (identifier, full name, date of birth, sex,
  e-mail, phone), two optional free fields, the requester and the state of the repository (duplicate or not). The suite
  uses **each use**: every invalid class alone, with the rest of the valid patient (TC12S-24 to TC12S-31, TC12S-73 to
  TC12S-75), and every valid value at least once (TC12S-15 to TC12S-23). The interaction of the rules (all the
  messages together and in order) is checked with the combinations of every invalid class at once (TC12S-32,
  TC12S-33, TC12S-46, TC12S-47) and with the order of the checks: permission before data (TC12S-08), data before
  duplicate (TC12S-34). **Pairwise** is not applied as a full technique: the validations are independent (one `if`
  each, results only added to a list), so a pair of invalid values gives the union of their two messages, which
  TC12S-32/33/46 already show.
- **`searchPatients`, `getPatient`, `findById`, `findByIdentifier`, `search`** have one parameter besides the
  requester, so each use and pairwise are the same suite: every testing value is used once.
- **Requester × operation:** all the combinations (6 users × 4 operations) are run, in TC12S-01 to TC12S-07.
- **Maximum number of test cases:** there is no maximum, because every text parameter is any `String` and the date any
  `LocalDate`. With the classes of section 1 (requester 5, identifier 3, full name 2, date of birth 4, sex 2, e-mail 3,
  phone 3, optional fields 2 × 2) all the combinations of `registerPatient` are 5 × 3 × 2 × 4 × 2 × 3 × 3 × 4 = 8,640;
  each use needs at least 5 (the largest number of classes, the requester), and the suite has more because it also
  uses the boundary values and the conjetures.

## 3. Coverage (white box)

Derived from the code of `develop` and the assertions of the tests. T = true, F = false.

**`PatientService(repository, roleManagementService, auditRecorder)` → `highestNumber`**
- Loop over the repository: 0 iterations (TC12S-76 empty repository; TC12S-10 with the two-argument constructor), one
  or more (TC12S-76).
- Decision `id != null && id.startsWith("P-") && digits && id.length() > 2 && id.length() <= 10` (5 conditions):
  - All T: `P-0007`, `P-0003` (TC12S-76 `nextIdAfterTheHighest`), `P-0002` (TC12S-76 `otherIdsAreIgnored`).
  - Only `id != null` F: `null`; only `startsWith` F: `Q-0050`; only `digits` F: `P-00a9`; only `length > 2` F: `P-`;
    only `length <= 10` F: `P-123456789` (all in TC12S-76 `otherIdsAreIgnored`).
  - **Decision, condition/decision and MC/DC: covered**, with the minimum of n + 1 = 6 cases (each condition is F
    alone while the others are T, and the outcome changes). `Math.max` keeps the highest (P-0007 before P-0003 gives
    P-0008).

**`loadSeedData`** — `patients.search("").isEmpty()`: T TC12S-10; F TC12S-11, TC12S-13, TC12S-76. One condition:
decision = condition/decision = MC/DC, covered.

**`checkPermission`** — `!hasPermission(requester, MANAGE_PATIENTS)`: T TC12S-02 to TC12S-08; F TC12S-01, TC12S-09.
Covered (the conditions inside `hasPermission` belong to C1: active, permission, unknown and `null` user are given by
TC12S-02 to TC12S-07).

**`existing(patientId)`** — `patientId == null`: T TC12S-44 (`nullIdIsRejected`), TC12S-50; F TC12S-40, TC12S-49.
`patient == null`: T TC12S-44 (`P-9999`, `p-0001`, `""`), TC12S-50; F TC12S-40, TC12S-49. Covered.

**`resourceOf(patientId)`** — `patientId == null`: T TC12S-08 (`getPatient(admin, null)` audited on `patients`); F
TC12S-01. Covered.

**`registerPatient`**
- `!errors.isEmpty()`: T TC12S-24 to TC12S-34; F TC12S-15.
- `duplicate != null`: T TC12S-35/36, TC12S-39, TC12S-69; F TC12S-15, TC12S-38.
- Loop of `findDuplicate`: match found (TC12S-35), no match after every patient (TC12S-15), 0 iterations (TC12S-76
  `emptyRepositoryStartsAtOne`). Covered.

**`modifyPatient`**
- Decision `patient != null && !identifierKey(current).equals(identifierKey(patient))` (2 conditions):
  - (F, –) TC12S-45; (T, T) TC12S-42, TC12S-46, TC12S-47, TC12S-70 (`reallyDifferentIdentifierIsRejected`);
    (T, F) TC12S-40, TC12S-41, TC12S-70 (`identifierWithOtherSpacesIsNotAChange`).
  - **Decision, condition/decision and MC/DC: covered** (3 cases = n + 1).
- `!errors.isEmpty()`: T TC12S-42, TC12S-45, TC12S-46, TC12S-73, TC12S-74; F TC12S-40. Covered.

**`validate(patient)`** (each `if` has one condition unless said otherwise)
- `patient == null`: T TC12S-24, TC12S-45; F TC12S-15.
- `clean(identifier).isEmpty()`: T TC12S-25, TC12S-32, TC12S-47; F TC12S-15.
- `clean(fullName).isEmpty()`: T TC12S-26, TC12S-32; F TC12S-15.
- `dateOfBirth == null`: T TC12S-27, TC12S-32; F TC12S-15. `isAfter(today)`: T TC12S-28, TC12S-33; F TC12S-20
  (today), TC12S-15. `isBefore(01/01/1900)`: T TC12S-74 (31/12/1899, 15/06/1800); F TC12S-74 (01/01/1900),
  TC12S-15.
- `sex == null`: T TC12S-29, TC12S-34; F TC12S-21.
- `!email.isEmpty() && !EMAIL.matches(email)`: (F, –) TC12S-18; (T, F) TC12S-22; (T, T) TC12S-30, TC12S-73. MC/DC
  covered.
- `!phone.isEmpty() && !PHONE_DIGITS.matches(digitsOf(phone))`: (F, –) TC12S-18; (T, F) TC12S-23, TC12S-75; (T, T)
  TC12S-31, TC12S-75. MC/DC covered.
- `digitsOf`: `startsWith("+")` T TC12S-23 (`+34600111222`), TC12S-31 (`+`, `++34600111222`); F TC12S-23
  (`600111222`).
- `clean`: `text == null` T TC12S-18 (`null`), TC12S-25; F TC12S-17.

**`InMemoryPatientRepository`**
- `findById`: no decision (a map read); TC12S-59.
- `findByIdentifier`: loop with `normalized(...).equals(wanted)` T TC12S-60 (`HC-0000001`), F until the end and
  `null` returned TC12S-60 (`HC-0000003`, `HC 0000001`); `normalized`: `text == null` T TC12S-60 (`null`), TC12S-63;
  F TC12S-60.
- `search`: decision `text.isEmpty() || identifier contains text || full name contains text` (3 conditions):
  (T, –, –) `""`, `null` (TC12S-61, TC12S-55); (F, T, –) `0002` (TC12S-61, TC12S-52); (F, F, T) `EJEMPLO`
  (TC12S-61, TC12S-53); (F, F, F) `zzz` (TC12S-61, TC12S-56). **Decision, condition/decision and MC/DC: covered**
  (4 cases = n + 1). Accents ignored on both sides: TC12S-71.
- `save`: no decision; new id (TC12S-63) and existing id (TC12S-62).

**Data classes:** `Patient` and `Sex` have no decisions (TC12S-65 to TC12S-67); `DuplicatePatientException` neither
(TC12S-68).

**Not covered (to add)**
- No decision or branch of `PatientService` or `InMemoryPatientRepository` is left without a test that makes it true
  and false.
- Boundary values not tested in `highestNumber`: `P-1` (3 characters, the first length that makes `length > 2` true)
  and `P-12345678` (10 characters, the last length that makes `length <= 10` true). MC/DC is reached with `P-0002`,
  but the limits of both conditions are only tested from the false side (`P-`, `P-123456789`).
- Conjeture not tested: an id after `P-9999` (`String.format("P-%04d")` gives `P-10000`, 7 characters).
- Conjeture not tested: `new DuplicatePatientException(null)` (finding 8).

## 4. Test cases

Every case of a "Registering a patient: invalid data" row expects `ValidationException` with exactly the listed
messages, nothing saved (still three patients) and nothing audited.

### 4.1 Permission (Security, Functional suitability)
The four operations are run with: `searchPatients("")` (resource `patients`), `getPatient("P-0001")`
(`PATIENT:P-0001`), `registerPatient(valid)` (`patients`) and `modifyPatient("P-0001", …)` (`PATIENT:P-0001`).

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-01 | all four | EC valid: `lmartin` calls each of the four operations | It works; audit `PATIENT_ACCESSED`, `PATIENT_ACCESSED`, `PATIENT_REGISTERED`, `PATIENT_MODIFIED`, author `lmartin` | `admissionsStaffUsesEveryOperation` |
| TC12S-02 | all four | EC invalid: `admin` (active, no `MANAGE_PATIENTS`) calls each operation | `AccessDeniedException` with the requester; one entry `PATIENT_ACCESS_DENIED` per call with the resource of the operation; nothing saved or changed | `administratorIsDenied` |
| TC12S-03 | all four | EC invalid: `mgarcia` (physician) calls each operation | Same as TC12S-02 (C4 will have its own read permission, D-011) | `physicianIsDenied` |
| TC12S-04 | all four | EC invalid: `jfernandez` (patient) calls each operation | Same as TC12S-02 | `patientUserIsDenied` |
| TC12S-05 | all four | EC invalid: `pnavarro` (has `MANAGE_PATIENTS` but is inactive) calls each operation | Same as TC12S-02 | `inactiveUserIsDenied` |
| TC12S-06 | all four | EC invalid: `nobody` (unknown) calls each operation | Same as TC12S-02 | `unknownUserIsDenied` |
| TC12S-07 | all four | EG: `null` requester calls each operation | `AccessDeniedException`, no other exception; the audit entries have a `null` author | `nullRequesterIsDenied` |
| TC12S-08 | `registerPatient`, `modifyPatient`, `getPatient` | EG: `admin` sends a `null` patient, an invalid patient, an unknown id `P-9999` and a `null` id | `AccessDeniedException`, not `ValidationException` (the permission comes first); resources `patients`, `patients`, `PATIENT:P-9999` and `patients` | `permissionComesBeforeTheData` |
| TC12S-09 | `searchPatients` | EG: `admin` is denied a search, then `lmartin` makes the same call | The second call works; one denial and one access are audited | `denialDoesNotBlockTheNextUser` |

### 4.2 Seed data (Q10)

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-10 | `loadSeedData` | EC valid: `loadSeedData()` on an empty service | Three patients `P-0001`–`P-0003` with `HC-0000001`–`HC-0000003`, active; nothing audited | `seedDataIsLoadedOnAnEmptySystem` |
| TC12S-11 | `loadSeedData` | EG: `loadSeedData()` twice | Still three patients | `seedDataIsLoadedOnlyOnce` |
| TC12S-12 | `registerPatient` | BV: register a patient after the seed data | Its id is `P-0004` (the counter continues after the seed) | `counterContinuesAfterTheSeed` |
| TC12S-13 | `loadSeedData` | EC: a patient registered before `loadSeedData()` | The seed is not loaded: one patient only, `P-0001` is the registered one | `seedDataIsNotLoadedWhenThereIsData` |
| TC12S-14 | `PatientSeedData.patients` | EC valid: every seed patient | Name, date of birth in the past, sex, a phone and an e-mail that pass the validation of Q5; invented `example.test` e-mails | `seedPatientsAreComplete` |

### 4.3 Registering a patient: valid data (Functional suitability)

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-15 | `registerPatient` | EC valid: the valid patient | Returned with id `P-0004`, every field as given, active; `getPatient` gives the same; audit `PATIENT_REGISTERED`, `lmartin`, `PATIENT:P-0004` | `validPatientIsRegistered` |
| TC12S-16 | `registerPatient` | EG: the valid patient with `patientId` `P-9999` and `active = false` | The id sent is ignored (`P-0004`) and the record is active; there is no `P-9999` | `sentIdAndActiveAreIgnored` |
| TC12S-17 | `registerPatient` | EG: every text with spaces at the ends | Saved without the spaces at the ends | `textsAreStripped` |
| TC12S-18 | `registerPatient` | BV: national ID, address, phone and e-mail `null`; and `"   "` (also `""` and `"\t"`) | Accepted; saved as `""` (optional) | `optionalFieldsCanBeEmpty` |
| TC12S-19 | `registerPatient` | EC valid: two registrations | Ids `P-0004` and `P-0005` | `idsAreConsecutive` |
| TC12S-20 | `registerPatient` | BV: date of birth today | Accepted | `dateOfBirthTodayIsValid` |
| TC12S-21 | `registerPatient` | EC valid: each value of `Sex` | Accepted | `everySexIsAccepted` |
| TC12S-22 | `registerPatient` | EC valid / BV: e-mail `a@b.c` (shortest form) and `name.surname@sub.example.test` (also `ana+urg@example.test`) | Accepted | `validEmailsAreAccepted` |
| TC12S-23 | `registerPatient` | EC valid / BV: phones of 9 and 15 digits, `600 111 222`, `+34600111222`, `+34 600 111 222` | Accepted; saved as given (only the ends stripped) | `validPhonesAreAccepted` |

### 4.4 Registering a patient: invalid data (Functional suitability, Usability)

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-24 | `registerPatient` | EC invalid: `null` patient | "The patient is required" | `RegisterInvalidTests.nullPatientIsRejected` |
| TC12S-25 | `registerPatient` | EC invalid / BV: identifier `null`, `""`, `"   "` | "The identifier is required" | `identifierIsRequired` |
| TC12S-26 | `registerPatient` | EC invalid / BV: full name `null`, `""`, `"   "` | "The full name is required" | `fullNameIsRequired` |
| TC12S-27 | `registerPatient` | EC invalid: date of birth `null` | "The date of birth is required" | `dateOfBirthIsRequired` |
| TC12S-28 | `registerPatient` | BV: date of birth tomorrow, and in a year | "The date of birth cannot be in the future" | `futureDateOfBirthIsRejected` |
| TC12S-29 | `registerPatient` | EC invalid: sex `null` | "The sex is required" | `sexIsRequired` |
| TC12S-30 | `registerPatient` | EC invalid / EG: e-mail `ana`, `ana@example`, `ana.example.test`, `ana@@example.test`, `ana prueba@example.test`, `@example.test`, `ana@.test`, `ana@example.` | "The e-mail must have the form name@domain.ext" | `invalidEmailIsRejected` |
| TC12S-31 | `registerPatient` | BV / EG: phone of 8 digits, of 16 digits, with a letter, `+` alone, `++34600111222`, `600-111-222`, `34+600111222` | "The phone must have 9 to 15 digits" | `invalidPhoneIsRejected` |
| TC12S-32 | `registerPatient` | EG: every field wrong at once (blank identifier and name, `null` date and sex, bad e-mail and phone) | The six messages together, in the order of Q5 | `RegisterInvalidTests.everyErrorComesTogether` |
| TC12S-33 | `registerPatient` | EG: same as TC12S-32 but with a date in the future | The future-date message in its place | `everyErrorWithAFutureDate` |
| TC12S-34 | `registerPatient` | EG: an invalid patient (no sex) with an identifier that already exists | `ValidationException`, not `DuplicatePatientException`; nothing audited | `validationComesBeforeTheDuplicate` |

### 4.5 Duplicate patient (Q4, D-037)

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-35 | `registerPatient` | EC invalid: a valid patient with identifier `HC-0000001` | `DuplicatePatientException` carrying P-0001 (Lucía) with its data; message "A patient with the identifier HC-0000001 already exists" | `duplicateCarriesTheExistingPatient` |
| TC12S-36 | `registerPatient` | EG: identifier `"  hc-0000001 "` (other case and spaces) | Same as TC12S-35 | `duplicateCarriesTheExistingPatient` |
| TC12S-37 | `registerPatient` | EC: after a duplicate | Nothing saved (still three); the audit has only `PATIENT_ACCESSED` `PATIENT:P-0001`; the next registration still gets `P-0004` | `duplicateSavesNothing` |
| TC12S-38 | `registerPatient` | BV: identifier `HC-00000011` (one more character) | Not a duplicate: registered | `longerIdentifierIsNotADuplicate` |
| TC12S-39 | `registerPatient` | EG: register a patient, then the same identifier again | The second one is a duplicate of the first registered (`P-0004`) | `registeredPatientIsADuplicateLater` |

### 4.6 Modifying a patient (Q6)

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-40 | `modifyPatient` | EC valid: `P-0001` with a new name, national ID, date, sex, address, phone and e-mail | Returned and stored; id and identifier stay; still three patients; audit `PATIENT_MODIFIED`, `PATIENT:P-0001` | `validChangeIsSaved` |
| TC12S-41 | `modifyPatient` | EG: same identifier with other case and spaces (`" hc-0000001 "`) | Accepted; the stored identifier stays `HC-0000001` | `sameIdentifierInOtherCaseIsAccepted` |
| TC12S-42 | `modifyPatient` | EC invalid: another identifier (`HC-0000002`) | "The identifier cannot be changed"; nothing changed, nothing audited | `identifierCannotBeChanged` |
| TC12S-43 | `modifyPatient` | EG: the body carries `patientId` `P-0002` and `active = false` | Only `P-0001` changes, still active; `P-0002` unchanged | `idAndActiveInTheDataAreIgnored` |
| TC12S-44 | `modifyPatient` | EC invalid: unknown id `P-9999`; `null` id; `p-0001` (lowercase) (also `""`) | "There is no patient with the id P-9999" / "… id null" / "… id p-0001"; nothing audited | `unknownIdIsRejected`, `nullIdIsRejected` |
| TC12S-45 | `modifyPatient` | EC invalid: `null` patient | "The patient is required" | `ModifyTests.nullPatientIsRejected` |
| TC12S-46 | `modifyPatient` | EG: blank name, future date, `null` sex, bad e-mail and phone, and another identifier | The five messages of Q5 and then "The identifier cannot be changed"; nothing changed, nothing audited | `ModifyTests.everyErrorComesTogether` |
| TC12S-47 | `modifyPatient` | EG: blank identifier | "The identifier is required" and "The identifier cannot be changed" | `blankIdentifierGivesBothMessages` |
| TC12S-48 | `modifyPatient` | EG / BV: texts with spaces at the ends, optional fields `null`, date of birth today | Accepted; stripped; optionals saved as `""` | `modificationIsCleaned` |

### 4.7 Reading a patient (getPatient)

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-49 | `getPatient` | EC valid: `P-0002` | Mario Demo Simulado with every field; one audit entry `PATIENT_ACCESSED` `PATIENT:P-0002` | `existingPatientIsReturned` |
| TC12S-50 | `getPatient` | EC invalid: `P-9999`, `null` | `ValidationException` "There is no patient with the id P-9999" / "… id null"; nothing audited | `GetPatientTests.unknownIdIsRejected` |
| TC12S-51 | `getPatient` | EC valid: a patient just registered | Found with the data returned by the registration | `registeredPatientCanBeRead` |

### 4.8 Searching patients (searchPatients)

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-52 | `searchPatients` | EC valid: `0002` (part of the identifier) | Only Mario | `partOfTheIdentifier` |
| TC12S-53 | `searchPatients` | EC valid: `muestra`, `EJEMPLO` (part of the name, other case) | Only Elena; only Lucía | `partOfTheFullNameIgnoringCase` |
| TC12S-54 | `searchPatients` | EG: `"  mario  "` | Only Mario (spaces at the ends ignored) | `spacesAtTheEndsAreIgnored` |
| TC12S-55 | `searchPatients` | BV: `""`, `null`, `"   "` | The three patients, in the order of saving | `emptyTextReturnsEveryPatient` |
| TC12S-56 | `searchPatients` | EC invalid: `zzz` | Empty list | `noMatchReturnsEmpty` |
| TC12S-57 | `searchPatients` | EC valid: `hc-` after registering one more | The four, in the order of saving (P-0001…P-0004) | `orderOfSaving` |
| TC12S-58 | `searchPatients` | EC valid: a search that finds nothing and one that finds all | One `PATIENT_ACCESSED` `patients` entry per search, author `lmartin` | `everySearchIsAuditedOnce` |

### 4.9 Repository, contract and data classes (to reach the coverage of D-033)

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-59 | `InMemoryPatientRepository.findById` | EC valid: a saved id, an unknown id and `null` | The patient; `null`; `null` | `findById` |
| TC12S-60 | `InMemoryPatientRepository.findByIdentifier` | EG: the same identifier in other case and spaces, an unknown one and `null` | The patient; `null`; `null` | `findByIdentifier` |
| TC12S-61 | `InMemoryPatientRepository.search` | EC valid / BV: part of identifier, of name, ignoring case; empty and `null` | The matching patients in the order of saving; all for empty and `null` | `search` |
| TC12S-62 | `InMemoryPatientRepository.save` | EG: `save` with an id that exists | Replaces it, keeps its place in the order, no new entry | `saveReplaces` |
| TC12S-63 | `search`, `findByIdentifier` | EG: a stored patient with `null` identifier and name, then `search("x")` and `findByIdentifier("x")` | No exception; nothing found | `nullFieldsDoNotBreakTheReads` |
| TC12S-64 | `PatientService(repository, …)` | EC valid: `PatientService` built with a repository given by the test | The registered patient is in that repository; the service is an `IPatientService` | `serviceUsesTheGivenRepository` |
| TC12S-65 | `Patient` getters | EC valid: every field given | Every field as given | `PatientTest.keepsEveryField` |
| TC12S-66 | `Patient` constructor | EG: every nullable field `null` | Kept as `null` (the server validates) | `PatientTest.keepsNullFields` |
| TC12S-67 | `Sex.values`, `Sex.valueOf` | EC valid: `Sex.values()`, `Sex.valueOf("UNSPECIFIED")` | `FEMALE`, `MALE`, `UNSPECIFIED`; `UNSPECIFIED` | `SexTest.values`, `SexTest.valueOfFindsTheValue` |
| TC12S-68 | `DuplicatePatientException(patient)` | EC valid: `new DuplicatePatientException(patient)` | Carries the same patient; message with the identifier; checked exception | `carriesTheExistingPatient` |

### 4.10 Fixes after the findings of #122 (15/11/2026)
The behaviours fixed in batch 1 (steps 4 to 6) and batch 2 (step 10) of `doc/plans/UC12-testing-fixes.md`.
`InMemoryPatientRepository.findByIdentifier` is unchanged (TC12S-60 still holds): the R-02 check is in `PatientService`.

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12S-69 | `registerPatient` | EC invalid / EG: register identifiers `HC 0000001`, `HC0000001`, `hc0000001`, `hc - 0000 001`, `" HC--0000001 "`; and a new identifier `" hc 12-34 567 "` | The first five: `DuplicatePatientException` carrying P-0001 (`HC-0000001`), audit only `PATIENT_ACCESSED` `PATIENT:P-0001`, nothing saved; the new one is saved as typed, only stripped (`hc 12-34 567`, `P-0004`) | `identifierWithOtherSpacesIsADuplicate`, `newIdentifierIsSavedAsTyped` |
| TC12S-70 | `modifyPatient` | EC valid / EC invalid: modify P-0001 with identifier `HC 0000001`, `HC0000001`, `hc0000001`, `hc - 0000 001`; and with `HC 0000002`, `HC0000011`, `HC-000001` | The first four are not a change: saved, the identifier stays `HC-0000001`; the last three: only "The identifier cannot be changed", nothing changed or audited | `identifierWithOtherSpacesIsNotAChange`, `reallyDifferentIdentifierIsRejected` |
| TC12S-71 | `InMemoryPatientRepository.search` | EC valid / EG: `lucia`, `LUCIA`, `Lucía`, `" LUCÍA "`, `lucia ejemplo`, `ejémplo`, `lucio` (stored "Lucía Ejemplo Prueba" and "Lucia Sin Tilde") | Accents and case ignored in the text and in the name: both Lucías for the first four; only "Lucía Ejemplo Prueba" for the next two; nothing for `lucio` | `InMemoryPatientRepositoryTest.searchIgnoresAccents` |
| TC12S-72 | `searchPatients` | EC valid: `lucia`, `LUCIA`, `Lucía`, `LUCÍA`, `lucía ejemplo`; and `Lucía` after registering "Lucia Sin Tilde" | P-0001, audited once (`PATIENT_ACCESSED` `patients`); P-0001 and P-0004 | `PatientServiceTest.searchIgnoresAccents`, `accentInTheTextFindsANameWithoutIt` |
| TC12S-73 | `registerPatient`, `modifyPatient` | EC invalid / BV: e-mail `ana@example.test.`, `ana@example..test`, `@example.test`, `ana@.test`, `ana@`, `ana@example`, `ana@example.`, `.@` when registering; `lucia@example..test` when modifying P-0001 | Only "The e-mail must have the form name@domain.ext"; nothing saved or audited | `emailWithAnEmptyPartIsRejected`, `emailWithAnEmptyPartIsRejectedInAModification` |
| TC12S-74 | `registerPatient`, `modifyPatient` | BV: date of birth 01/01/1900; 31/12/1899 and 15/06/1800 when registering; 31/12/1899 when modifying P-0001 | 01/01/1900 accepted; the others: only "The date of birth cannot be before 01/01/1900", nothing saved or changed | `firstDayOf1900IsAccepted`, `dateBefore1900IsRejected`, `dateBefore1900IsRejectedInAModification` |
| TC12S-75 | `registerPatient` | BV: phones with inner spaces: `600 111 222`, `6 0 0 1 1 1 2 2 2` (9 digits), `346 001 112 223 334`, `+34 600 111 222 3334` (15); `600 111 22`, `6 0 0 1 1 1 2 2` (8), `346 001 112 223 3345`, `+34 600 111 222 33345` (16) | 9 and 15 digits accepted and saved as typed; 8 and 16: only "The phone must have 9 to 15 digits" | `phoneWithSpacesAtTheLimitsIsAccepted`, `phoneWithSpacesOutOfTheLimitsIsRejected` |
| TC12S-76 | `PatientService(repository, …)`, `registerPatient`, `loadSeedData` | EG / BV: `PatientService` built with a repository that has P-0007 and P-0003; with an empty repository; with P-0002 and ids `Q-0050`, `P-00a9`, `P-`, `P-123456789`, `null`; `loadSeedData()` over a repository with P-0005 | P-0008 then P-0009, P-0007 and P-0003 not overwritten; P-0001; P-0003 (the other ids are ignored when counting); the seed is not loaded | `nextIdAfterTheHighest`, `emptyRepositoryStartsAtOne`, `otherIdsAreIgnored`, `seedIsNotLoadedOverExistingPatients` |

Test classes: TC12S-01 to TC12S-09, TC12S-15 to TC12S-58, TC12S-64 and TC12S-69, TC12S-70, TC12S-72 to TC12S-76 are in
`PatientServiceTest` (nested classes by section); TC12S-10 to TC12S-14 in `PatientSeedDataTest`; TC12S-59 to
TC12S-63 and TC12S-71 in `InMemoryPatientRepositoryTest`; TC12S-65 and TC12S-66 in `PatientTest`; TC12S-67 in
`SexTest`; TC12S-68 in `DuplicatePatientExceptionTest`. When a method name repeats, the nested class is given.

Postcondition of every case: a rejected call (access denied, validation error, duplicate) leaves the repository as it
was and writes only the audit entry that the expected result says; an accepted call leaves exactly the patient
returned stored (active, with the id given by the server, the texts stripped) and one audit entry of its operation.
The seed data and the counter of ids are not shared between tests (a new service per test).

## Findings while designing (to ask Javier, not tested as errors)

1. **The id counter belongs to the service, not to the repository.** A `PatientService` built with a repository that
   already holds patients (the three-argument constructor) starts again at `P-0001`, and `save` replaces the existing
   `P-0001`: a registration would overwrite a patient. It does not happen with `Main` (empty repository and then the
   seed), so it is not a failure of Q8 as written, but it is fragile if a shared or persistent repository is used.
2. **"Ignoring case and spaces" (Q4) means only the spaces at the ends.** `HC 0000001` and `HC0000001` are different
   identifiers. The Javadoc of the repository says "the spaces at the ends", so it is tested that way (TC12S-36).
3. **The search does not ignore accents:** `lucia` does not find "Lucía Ejemplo Prueba". Q2 only says "ignoring case",
   so it is not tested as an error; it may matter for the Admissions screen.
4. **Blank identifier in a modification gives two messages** ("The identifier is required" and "The identifier cannot
   be changed", TC12S-47). Not wrong, but redundant for the user.
5. **The e-mail rule accepts `ana@example.test.` and `ana@example..test`** (a dot at the end or two dots), because it
   only asks for a dot after the `@` with something on both sides. It follows Q5 literally.
6. **(Fixed by R-04, tested by TC12S-73 to TC12S-75.) The date of birth has no lower bound** (`1800-01-01` is accepted) and the phone keeps its inner spaces when saved
   (`600 111 222`). Both follow Q5.
7. **`getPatient` and `modifyPatient` of an unknown id are not audited**, while a denied one is. Q7 says one entry per
   `getPatient`; the plan does not say whether a failed read counts. Tested as it is (TC12S-44, TC12S-50).
8. `new DuplicatePatientException(null)` throws `NullPointerException`; the service never does it.

- **Update (15/11/2026):** findings 1, 2, 3 and 5 are fixed in `develop` (F-11, R-02, R-03 and R-04) and tested by
  TC12S-76, TC12S-69/70, TC12S-71/72 and TC12S-73.
