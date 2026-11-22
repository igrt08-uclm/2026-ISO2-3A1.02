# Test design · UC12 – Client (C3 v1.0.0) · #45 – Manage patients

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#44, #45 and the answers Q1–Q10 of `doc/plans/UC12-client.md` (rules and messages of the data: Q5 of
`doc/plans/UC12-server.md`, D-037), and the fixes F-06 to F-10 and R-04 of `doc/plans/UC12-testing-fixes.md`
(findings of #122).

Code under test (`client-c3-patients`, branch `develop`): the screen `IU_ManagePatients` (its public methods
`onSearch`, `displayPatients`, `showValidationErrors`, `showExistingPatient`, `showOperationResultSuccess`,
`showOperationResultError`, and the actions *Search*, *New patient*, *Save*, *Back* and the selection of a row), and
the button *Manage patients* of `IU_Welcome` (`client-c1-access`). Tests: `IU_ManagePatientsTest` and
`IU_WelcomeManagePatientsTest`.

**Test data (precondition of every case).** The screen is built with a fake `IPatientService` (in memory, no window)
with the three invented patients of the seed data in the order of the server: P-0001 `HC-0000001` Lucía Ejemplo Prueba
(14/03/1980, female), P-0002 `HC-0000002` Mario Demo Simulado (02/11/1975, male, national ID `12345678Z`) and P-0003
`HC-0000003` Elena Muestra Ficticia (25/07/2001, unspecified). The fake does not validate (so it shows that nothing is
sent when the client rejects the form), records every search, registration and modification, gives the ids P-0004…,
finds a duplicate by the identifier ignoring case and spaces at the ends, can deny every call
(`AccessDeniedException`) and reject the next change with a list of errors (`ValidationException`). Every question is
answered by the test (accept or cancel), which also records it. The requester is `lmartin`. Dates that depend on the
day are written relative to `LocalDate.now()`.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| `IU_ManagePatients(service, requester, confirm, onBack)` | `service` (state) | Allows, with patients | the three patients; plus one with `null` optional data (P-0009) |
| | | Allows, no patients | empty fake |
| | | Denies | `denyAll()` before opening |
| | `confirm` (answer) | Accept / Cancel | `true` / `false` |
| `onSearch(filters)` and *Search* / Enter | `filters` | Finds one (Limit Val.: singular) | `mario`, `elena`, `HC-0000001` |
| | | Finds all | `""` |
| | | Finds none (Limit Val.: 0) | `zzz` |
| | | Conjeture | permission revoked while open |
| `displayPatients(found)` | `found` | One or more patients / empty | `[Elena]`, the three; `[]` |
| *Save* (`onSavePatient`) → `readForm` | identifier | Not blank | `HC-1234567`, `" hc-0000002 "` (exists) |
| | | Blank | `""`, `"   "` |
| | full name | Not blank / blank | `Ana Prueba Inventada` / `""`, `"   "` |
| | date of birth (text) | `dd/MM/yyyy`, real date, from 01/01/1900 to today | today (Limit Val.), `29/02/2000` (leap day), `"  01/01/1900  "`, `01/01/1900` (Limit Val.) |
| | | Before 01/01/1900 | `31/12/1899` (Limit Val.), `01/01/1899` |
| | | In the future | tomorrow (Limit Val.), next year |
| | | Not a real date or not `dd/MM/yyyy` | `31/02/2000`, `29/02/2001`, `31/04/2000`, `32/01/2000`, `00/01/2000`, `15/13/2000`, `1/2/2000`, `2000-02-01`, `01-02-2000`, `01/02/00`, `abc`, `01/02/2000x` |
| | | Blank | `""`, `"   "` |
| | sex | One of the list / none chosen | Female, Male, Unspecified / nothing |
| | e-mail | Empty (optional) | `""` |
| | | Form `name@domain.ext` | `a@b.c` (Limit Val.: shortest), `name@domain.ext`, `name.surname@sub.domain.ext` |
| | | Other form | `name.domain.ext`, `name@domain`, `name@@domain.ext`, `name@do@main.ext`, `@domain.ext`, `name@.ext`, `na me@domain.ext`, `name@domain.` |
| | | Conjeture (empty part, R-04) | `ana@example.test.`, `ana@example..test` |
| | phone | Empty (optional) | `""` |
| | | 9 to 15 digits, optional leading `+`, spaces allowed | `600000001` (9, Limit Val.), `123456789012345` (15, Limit Val.), `600 000 001`, `+34600000001`, `+34 600 000 001` |
| | | Fewer than 9 or more than 15 digits | `60000000` (8, Limit Val.), `1234567890123456` (16, Limit Val.), `123` |
| | | Conjeture | `60000000a`, `+`, `++34600000001`, `34+600000001`, `600-000-001`, `+ 6000000` |
| | national ID, address | Any text, optional | `""`, a text |
| | selected patient | None (register) / one (modify) | *New patient* / P-0002, P-0004 |
| | answer of the server | Saved / `ValidationException` / `DuplicatePatientException` / `AccessDeniedException` | the fake as configured |
| Unsaved changes (`confirmDiscard`) | state of the form | Clean (nothing changed, spaces at the ends, change undone) | — |
| | | Changed: any one of the eight fields of a shown patient | identifier, national ID, full name, date, sex, address, phone, e-mail |
| | | Written: any one field of a new-patient form | the same eight fields |
| | action | Another row, *New patient*, *Search*, Enter, *Back* | — |
| | answer | Accept / Cancel | — |
| | | Conjeture | selection dragged (value adjusting) over other rows and back |
| `showValidationErrors(errors)` | `errors` | One / several | `[One, Two]`, one message, six messages |
| `showOperationResultSuccess/Error(message)` | `message` | Short / long | `Done.`, `Short.`, six messages |
| `IU_Welcome(…, permissions, …, onManagePatients)` | `permissions` | With `MANAGE_PATIENTS` / without | Admissions staff, every permission / administrator, none |
| | `onManagePatients` | Given / `null` | an action / `null`; the 2-, 3-, 5-, 6- and 7-argument constructors |

Outputs: the table (rows, selection), the form (texts, sex, enabled fields), the line "n patients found.", the message
area (text, colour, ✔ or ⚠), the red borders, the calls sent to the fake, the questions asked and the action of *Back*.
No exception goes out of the screen: the exceptions of the server are shown as messages.

## 2. Combination of the values

- **Form of *Save*:** six validated fields (identifier, full name, date, sex, e-mail, phone). The suite uses **each
  use**: every invalid class alone with the rest valid (TC12C-13 to TC12C-17, TC12C-41, TC12C-42), every valid value at
  least once (TC12C-15 to TC12C-17, TC12C-20, TC12C-21), and the combination of every invalid class at once
  (TC12C-12, the four required fields empty; TC12C-18, the six rules broken). **Pairwise** is not applied as a full
  technique: each rule is an independent `if` that only adds its message and marks its own field, so a pair of
  invalid values gives the union of their two errors, which TC12C-12 and TC12C-18 already show.
- **Unsaved changes:** state (clean / changed / written) × action (row, *New patient*, *Search*, Enter, *Back*) ×
  answer (accept / cancel). The suite covers the eight fields one by one for a shown patient (TC12C-27) and for a new
  form (TC12C-31), and the pairs action × answer in TC12C-27 to TC12C-31 and TC12C-36 to TC12C-38: this is pairwise
  over action × answer, and each use over the eight fields.
- **`onSearch`, `displayPatients`, the message methods and `IU_Welcome`:** one parameter (or two in `IU_Welcome`, both
  combined: with and without the permission × with and without the action, TC12C-33 to TC12C-35).
- **Maximum number of test cases:** there is no maximum, because every field is a free text (any `String`) and the
  dates have no upper count. With the classes of section 1 (identifier 2, full name 2, date 5, sex 2, e-mail 3, phone
  3, selected patient 2, answer of the server 4) all the combinations of *Save* are 2 × 2 × 5 × 2 × 3 × 3 × 2 × 4 =
  2,880; each use needs at least 5 (the largest number of classes, the date).

## 3. Coverage (white box)

Derived from the code of `develop` and the assertions of the tests. T = true, F = false.

**Constructor and listeners of `IU_ManagePatients`**
- *Back*: `confirmDiscard()` T TC12C-05, TC12C-38 (accept, and without changes); F TC12C-38 (cancel).
- Renderer of the sex: `value == null` T TC12C-03 (`" "`); F TC12C-03 (Female, Male, Unspecified). `sexName`: the
  three branches of the `switch`, TC12C-03.
- Selection listener `!valueIsAdjusting && !loading && !restoring` (3 conditions):
  - All T: a row pressed, TC12C-06.
  - Only `!valueIsAdjusting` F: TC12C-32, TC12C-37 (dragging).
  - Only `!loading` F: TC12C-08 (a row selected, then a search clears the selection inside `displayPatients`),
    TC12C-01 (`displayPatients` with Lucía selected).
  - Only `!restoring` F: TC12C-07 (*New patient* with a row selected), TC12C-28 (cancel puts the row back).
  - **Decision, condition/decision and MC/DC: covered** (4 cases = n + 1).
- `addField`: `row == 0` T and F when the screen is built (TC12C-01).

**`onSearchRequested`** — `confirmDiscard()`: T TC12C-08, TC12C-11, TC12C-29; F TC12C-28 (cancel on *Search* and
Enter). Covered.

**`onSearch`** — `catch AccessDeniedException`: TC12C-25, TC12C-26 (*Search*). `found.size() == 1`: T TC12C-02,
TC12C-08; F TC12C-01, TC12C-02 (0), TC12C-10. Covered. **`displayPatients`** loop: 0 rows TC12C-02, TC12C-10; one or
more TC12C-01.

**`changeRow`**
- `table.getSelectedRow() == shownRow`: T TC12C-37 (dragged back to the same row; another row, cancel and the same row
  again); F TC12C-06.
- `!confirmDiscard()`: T TC12C-28, TC12C-31 (cancel); F TC12C-27, TC12C-31 (accept).
- `shownRow >= 0`: T TC12C-28 (the selection goes back to its row); F TC12C-31 (cancel on a new-patient form: nothing
  selected). Covered.

**`confirmDiscard`** — `!isDirty() || confirm.test(...)` (2 conditions): (T, –) TC12C-30, TC12C-38 (without changes);
(F, T) TC12C-27, TC12C-29, TC12C-31 (accept), TC12C-36 (accept); (F, F) TC12C-28, TC12C-31 (cancel), TC12C-36
(cancel), TC12C-38 (cancel). **MC/DC covered** (3 cases = n + 1).

**`isDirty`**
- `shownRow >= 0 && shownRow < patients.size()`: (F, –) TC12C-30, TC12C-31 (new form); (T, T) TC12C-27. (T, F) is
  **not covered** (see below).
- `shown == null ? "" : …` (eight times): T TC12C-31 (new form); F TC12C-27.
- Decision of eight conditions joined by `||` (`!same(identifier)`, `!same(nationalId)`, `!same(fullName)`,
  `!same(date)`, `sex != shown sex`, `!same(address)`, `!same(phone)`, `!same(email)`): each one T alone, TC12C-27
  (parameterized over the eight fields, shown patient) and TC12C-31 (the eight fields, new form); all F TC12C-30
  (also with spaces at the ends and a change undone). **MC/DC covered** (9 cases = n + 1).
- `same`: `saved == null` F TC12C-27; T **not covered** (see below).

**`onNewPatient`** — `!confirmDiscard()`: T TC12C-28, TC12C-36 (cancel); F TC12C-07, TC12C-29, TC12C-36 (accept).
Covered.

**`onSavePatient`**
- `!errors.isEmpty()`: T TC12C-12 to TC12C-19, TC12C-22, TC12C-41, TC12C-42; F TC12C-20, TC12C-21.
- `selected == null` (register / modify, and the text "registered." / "saved."): T TC12C-20; F TC12C-21.
- Exceptions: `AccessDeniedException` TC12C-26; `ValidationException` TC12C-24; `DuplicatePatientException`
  TC12C-23; none TC12C-20, TC12C-21. Covered.

**`readForm`**
- `identifier.isEmpty()`: T TC12C-12, TC12C-13; F TC12C-20. `fullName.isEmpty()`: T TC12C-12, TC12C-13; F TC12C-20.
  `sex == null`: T TC12C-12, TC12C-13; F TC12C-20.
- `!email.isEmpty() && !EMAIL.matches(email)`: (F, –) TC12C-16 (empty); (T, F) TC12C-16 (valid); (T, T) TC12C-16
  (invalid), TC12C-41. MC/DC covered.
- `!phone.isEmpty() && !PHONE_DIGITS.matches(digitsOf(phone))`: (F, –) TC12C-17 (empty); (T, F) TC12C-17 (valid);
  (T, T) TC12C-17 (invalid), TC12C-18. MC/DC covered.
- `digitsOf`: `startsWith("+")` T TC12C-17 (`+34600000001`, `+`, `++34600000001`); F TC12C-17 (`600000001`).

**`readBirth`** — `text.isEmpty()`: T TC12C-12, TC12C-13; F TC12C-15. `DateTimeParseException`: T TC12C-14; F
TC12C-15. `isAfter(today)`: T TC12C-15 (tomorrow), TC12C-18; F TC12C-15 (today). `isBefore(01/01/1900)`: T TC12C-42
(`31/12/1899`, `01/01/1899`); F TC12C-42 (`01/01/1900`), TC12C-15. Covered.

**`select(patientId)`** — loop with `getPatientId().equals(patientId)`: T TC12C-20, TC12C-21, TC12C-23; the loop
ending with no match is **not covered** (see below).

**`showSelected`** — `selected == null ? "" : …`: T TC12C-01, TC12C-07; F TC12C-06.

**`updateControls`** — `enabled && selectedPatient() == null` (2 conditions): (F, –) TC12C-25, TC12C-26; (T, T)
TC12C-01, TC12C-07; (T, F) TC12C-06. **MC/DC covered.**

**`selectedPatient`** — `row < 0 || row >= patients.size()`: (T, –) TC12C-01, TC12C-07; (F, F) TC12C-06; (F, T)
**not covered** (see below).

**`WrappedLabel`** (message area)
- `setText`: `area != null` F when the label is built (the constructor of `JLabel` calls `setText` before the field is
  set), TC12C-01; T TC12C-24, TC12C-39.
- `getPreferredSize`: `getWidth() > 0` F TC12C-39; T **not covered**.
- `paintComponent` and its decision `getPreferredSize().height != getHeight()`: **not covered**.

**`IU_Welcome`** — `onManagePatients != null && hasPermission(permissions, MANAGE_PATIENTS)` (2 conditions): (T, T)
TC12C-33, TC12C-34; (F, –) TC12C-35 (no action, and the older constructors); (T, F) TC12C-35 (administrator, no
permissions). **MC/DC covered.**

**Not covered (to add)**
- `isDirty`: the condition `shownRow < patients.size()` is never false while `shownRow >= 0`. The shown row is reset by
  `showSelected` every time the list changes, so it may not be reachable from the screen; it would need a test that
  calls `confirmDiscard` with a shown row after the list became shorter, or the team may consider it defensive code.
- `same`: `saved == null` is never true. TC12C-06 shows a patient with `null` national ID, address, phone and e-mail,
  but no test then changes the row, presses *New patient*, *Search* or *Back*, so `isDirty` is never evaluated with
  those `null` values. To add: show P-0009 and press another row (expected: no question).
- `select`: the loop never ends without a match (the saved or existing patient is always in the reloaded list). It
  would need a server that returns a saved patient that the search `""` does not list.
- `selectedPatient`: `row >= patients.size()` is never true (the rows of the table and the list always have the same
  size when it is called); possibly defensive code.
- `WrappedLabel.getPreferredSize` with `getWidth() > 0` and `WrappedLabel.paintComponent` are never executed with a
  real size or painted: the tests build the screen without a window. They would need a test that sets the size of the
  label (or lays out the screen) and paints it on an image.

## 4. Test cases

### 4.1 Display

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-01 | constructor, `onSearch`, `displayPatients` | EC valid: open the screen; `displayPatients` with one patient while a row is selected; `onSearch("HC-0000001")` | Columns Identifier, Full name, Date of birth; three rows in the order of the server; dates as `dd/MM/yyyy` (`14/03/1980`); cells not editable; "3 patients found."; nothing selected; empty form with the identifier enabled; empty message area; one search with an empty text and the requester `lmartin`. `displayPatients` shows the given list with nothing selected | `opensWithEveryPatient`, `DesignMethods.displayAndSearch` |
| TC12C-02 | `onSearch` | BV: search that finds one; a server with no patients | "1 patient found." (singular) with one row; "0 patients found." with an empty table | `lineWithOneAndNone` |
| TC12C-03 | constructor (sex list) | EC valid: the sex list | Three items Female, Male, Unspecified (plain names), not editable, nothing chosen at the start | `sexIsChosenFromAList` |
| TC12C-04 | constructor (keyboard) | EC valid: keyboard | *Search* Alt+H, *New patient* Alt+N, *Save* Alt+S, *Back* Alt+B; labels Alt+I, A, F, D, X, R, O, E | `keyboardShortcuts` |
| TC12C-05 | *Back* | EC valid: press *Back* | The action of *Back* runs; nothing is sent | `backRunsItsAction` |

### 4.2 Form of the selected patient

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-06 | selection of a row | EC valid: press a row, then another; a patient with `null` optional data | Every field of that patient, the sex chosen; identifier disabled, the other fields and *Save* enabled; no question; `null` optional data gives empty fields | `rowLoadsThePatient`, `anotherRowAndEmptyOptionalData` |
| TC12C-07 | *New patient* | EC valid: a row selected, press *New patient* | No row selected, empty form, identifier enabled; no question | `newPatientEmptiesTheForm` |

### 4.3 Search

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-08 | *Search* | EC valid: a row selected, search `mario` | The server receives `mario`; one row; "1 patient found."; nothing selected and empty form | `searchWithAText` |
| TC12C-09 | *Search* | EC valid: search `elena`, then an empty text | Every patient again; "3 patients found." | `emptyTextListsEveryone` |
| TC12C-10 | *Search* | EC invalid: search `zzz` | Empty table; "0 patients found."; empty form | `noMatch` |
| TC12C-11 | Enter in the search box | EC valid: Enter in the search box | Same as *Search* | `enterSearches` |

### 4.4 Validation in the client

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-12 | *Save* | EC invalid: *Save* an empty form | "⚠ The identifier is required. The full name is required. The date of birth is required. The sex is required." in red; those four fields with a red border; nothing sent | `emptyFormGivesFourErrors` |
| TC12C-13 | *Save* | BV invalid: identifier, full name or date with only spaces; no sex chosen | Only that error; only that field marked; nothing sent | `requiredTextWithSpaces`, `noSex` |
| TC12C-14 | *Save* | EC invalid / BV: date `31/02/2000`, `29/02/2001`, `31/04/2000`, `32/01/2000`, `00/01/2000`, `15/13/2000`, `1/2/2000`, `2000-02-01`, `01-02-2000`, `01/02/00`, `abc`, `01/02/2000x` | "⚠ The date of birth must be written as dd/MM/yyyy."; the date marked; nothing sent | `badDate` |
| TC12C-15 | *Save* | BV: date tomorrow; today; `29/02/2000`; `␣␣01/01/1900␣␣` | Tomorrow: "⚠ The date of birth cannot be in the future."; today, the leap day and the date with spaces are sent | `tomorrowIsRejected`, `validDateBoundaries` |
| TC12C-16 | *Save* | EC / BV: e-mail `a@b.c`, `name@domain.ext`, `name.surname@sub.domain.ext`, empty; `name.domain.ext`, `name@domain`, `name@@domain.ext`, `name@do@main.ext`, `@domain.ext`, `name@.ext`, `na me@domain.ext`, `name@domain.` | The first four are sent; the others: "⚠ The e-mail must have the form name@domain.ext.", the e-mail marked, nothing sent | `validEmail`, `invalidEmail` |
| TC12C-17 | *Save* | BV / EC: phone 9 and 15 digits, `600 000 001`, `+34600000001`, `+34 600 000 001`, empty; 8 and 16 digits, `60000000a`, `+`, `++34600000001`, `34+600000001`, `600-000-001`, `+ 6000000` | The first six are sent as typed; the others: "⚠ The phone must have 9 to 15 digits.", the phone marked, nothing sent | `validPhone`, `invalidPhone` |
| TC12C-18 | *Save* | EG: every rule broken at once (date next year, bad e-mail, phone `123`), address written | The six errors in one message in the order of the server; six fields marked; the form keeps what was typed; nothing sent | `everyErrorTogether` |
| TC12C-19 | *Save*, *New patient* | EG: errors, then *New patient*; errors, then a valid save | The red borders go away | `marksAreCleared` |

### 4.5 Registration and modification

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-20 | *Save* (register) | EC valid: after searching `mario`, *New patient*, a valid patient with spaces at the ends | Sent with no id and the texts stripped; "✔ Patient P-0004 registered." in green; search emptied and the table reloaded (4 rows, "4 patients found."); P-0004 selected and loaded with the identifier locked; no question; next *New patient* asks nothing | `validRegistration`, `afterRegistrationTheFormIsClean` |
| TC12C-21 | *Save* (modify) | EC valid: change phone, national ID (to empty) and sex of P-0002; register and then change P-0004 | `modifyPatient` with P-0002 and the new data; "✔ Patient P-0002 saved." in green; the same patient selected with the new data; "✔ Patient P-0004 saved." | `validModification`, `registerThenModify` |
| TC12C-22 | *Save* (modify) | EC invalid: bad e-mail on a selected patient | Error; nothing sent; the patient stays selected with what was typed | `invalidModification` |

### 4.6 Duplicate

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-23 | *Save*, `showExistingPatient` | EC invalid / EG: after a search, register `␣hc-0000002␣`; then change the phone and *Save* | "⚠ A patient with this identifier already exists: the record is loaded so that you can modify it." in red; nothing new stored; search emptied; P-0002 selected and loaded, identifier locked; no question. The next *Save* modifies P-0001/P-0002 ("saved") | `duplicateLoadsTheExistingPatient`, `saveAfterTheDuplicateModifies` |

### 4.7 Errors of the server

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-24 | *Save*, `showValidationErrors`, `showOperationResultSuccess` | EC invalid: the server rejects a modification with two errors; a registration with one | "⚠ The identifier cannot be changed. The phone must have 9 to 15 digits." (each with a full stop) in red; no field marked; the form and the selection are kept; nothing stored. `showOperationResultSuccess` green with ✔, `showValidationErrors` red with ⚠ | `serverErrorsOnModification`, `serverErrorOnRegistration`, `DesignMethods.messages` |

### 4.8 Denied access

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-25 | constructor | EC invalid: the server denies when the screen opens | "⚠ You do not have permission to manage patients." in red; empty table; *Search*, *New patient*, *Save*, the table and every field disabled; *Back* enabled and works | `deniedWhenOpening` |
| TC12C-26 | *Save*, *Search* | EG: permission revoked while open, then *Save*; or *Search* | Same message; only *Back* enabled; nothing stored | `deniedWhenSaving`, `deniedWhenSearching` |

### 4.9 Unsaved changes

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-27 | selection of a row | EC valid: change any one of the eight fields of a shown patient, press another row, accept | "Discard the unsaved changes?" asked; the other row is shown; nothing sent | `anotherRowAccept` |
| TC12C-28 | row, *New patient*, *Search*, Enter | EC invalid: same and cancel, on another row, *New patient*, *Search* and Enter | Selection back to its row; the changes kept; no search sent | `anotherRowCancel`, `newPatientCancel`, `searchCancel` |
| TC12C-29 | *New patient*, *Search* | EC valid: changes, accept on *New patient*; data in a new form, accept on *Search* | Empty form for a new patient; *Search* asks once and searches | `newPatientAccept`, `searchAccept` |
| TC12C-30 | row, *New patient*, *Search* | EC valid / EG: no changes: rows, *New patient*, *Search*; spaces at the ends; a change undone by hand; an empty new form with only spaces | No question | `noQuestionWhenClean`, `spacesAndUndoneChangesAreNotChanges` |
| TC12C-31 | selection of a row | EC valid: a new-patient form with any one field written, press a row; cancel / accept | The question; cancel: nothing selected and the data kept; accept: the row shown | `newPatientFormCancel`, `newPatientFormAccept` |
| TC12C-32 | selection of a row | EG: selection dragged over rows (value adjusting) with changes, then released | Nothing asked or loaded while dragging; one question when released, then the last row | `draggingTheSelection` |

### 4.10 Welcome button

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-33 | `IU_Welcome` | EC valid: Welcome of an Admissions user with `MANAGE_PATIENTS` and the action | Button *Manage patients* (Alt+P), runs its action | `userWithManagePatientsSeesTheButton` |
| TC12C-34 | `IU_Welcome` | EC valid: every permission and every action | Buttons in order: View audit log, Manage roles, Configure system, Configure integrations, Manage patients, Log out; each runs only its own action | `orderWithTheOtherButtons`, `managePatientsRunsOnlyItsOwnAction` |
| TC12C-35 | `IU_Welcome` | EC invalid: administrator without `MANAGE_PATIENTS`; no permissions; no action; the 2-, 3-, 5-, 6- and 7-argument constructors | No button *Manage patients*; the other buttons as before | `userWithoutManagePatientsDoesNotSeeTheButton`, `withoutTheActionNobodySeesTheButton`, `olderConstructorsDoNotShowTheButton` |

### 4.11 Fixes after the findings of #122 (15/11/2026)
Tests in the nested class *10. Fixes after the findings of #122* of `IU_ManagePatientsTest` (commits 35acffc, 8fc19fd
and c7780fc of `feature/UC12-client`).

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC12C-36 | *New patient* | EG: Mario selected, the address changed, *New patient*; cancel / accept | One question "Discard the unsaved changes?" (counted). Cancel: Mario stays selected with the changed address and the identifier locked. Accept: nothing selected, empty form, identifier enabled, nothing sent | `newPatientAsksOnceCancel`, `newPatientAsksOnceAccept` |
| TC12C-37 | selection of a row | EG: Lucía selected, the full name changed; the selection dragged to Mario, Elena and back to Lucía, then released. Also: another row, cancel, and Lucía pressed again | Drag: no question, Lucía selected, the changed name kept, nothing sent. Another row + cancel + same row: one question, Lucía selected, the changed phone kept | `dragAwayAndBackKeepsChanges`, `anotherRowCancelThenSameRowKeepsChanges` |
| TC12C-38 | *Back* | EC valid / invalid: *Back* with unsaved changes (cancel / accept) and without changes | With changes: one question; cancel does not run *Back* and keeps the changes and the selection; accept runs *Back* once and sends nothing. Without changes: *Back* runs with no question | `backWithChangesCancel`, `backWithChangesAccept`, `backWithoutChanges` |
| TC12C-39 | `showOperationResultError` | EG: a short error and then a long error of six messages | The message label is in `BorderLayout.CENTER` of the footer and *Back* in `EAST`; `getText()` is the whole plain message (`⚠ ` + text, no HTML); the preferred width does not grow and the preferred height grows (several lines) | `longErrorInTheCentreOfTheFooter` |
| TC12C-40 | constructor (keyboard) | EC valid: keyboard | Only the button *Search* has Alt+H; the label *Search* has no mnemonic and no label of the screen shows Alt+H | `onlyOneAltH` |
| TC12C-41 | *Save* | EC invalid: e-mail `ana@example.test.`, `ana@example..test` | "The e-mail must have the form name@domain.ext.", only the e-mail marked, nothing sent | `emailWithAnEmptyPart` |
| TC12C-42 | *Save* | BV: date of birth `01/01/1900`; `31/12/1899`, `01/01/1899` | `01/01/1900` registered ("Patient P-0004 registered."). The others: "The date of birth cannot be before 01/01/1900.", only the date marked, nothing sent | `earliestBirthIsAccepted`, `birthBefore1900IsRejected` |

Test classes: TC12C-01 to TC12C-32 and TC12C-36 to TC12C-42 are in `IU_ManagePatientsTest` (nested classes by
section; `DesignMethods` holds the extra methods of TC12C-01 and TC12C-24); TC12C-33 to TC12C-35 in
`IU_WelcomeManagePatientsTest`.

Postcondition of every case: when the client rejects the form, nothing reaches the fake (no registration or
modification recorded) and the form keeps what was typed; when the server accepts, the fake stores exactly the patient
sent (texts stripped, no id) and the screen shows it selected; a question cancelled leaves the selection and the form
as they were; *Back* runs at most once. Each test builds a new fake and a new screen.

## Defects found (not covered by a passing test)
- **D1 (TC12C-29, EG):** with unsaved changes on a selected patient, *New patient* asks "Discard the unsaved changes?" **twice** (once in `onNewPatient` and again from the selection listener when the selection is cleared). If the user accepts the first and cancels the second, the selection stays on the patient and the form is reloaded with the saved data: the changes are lost and no new form is shown. Expected (Q8): one question.
- **D2 (TC12C-32, EG):** with unsaved changes, dragging the selection to another row and back to the same one before releasing reloads the saved data without asking: the changes are lost.
- **Update (15/11/2026):** D1 and D2 are fixed (F-06, F-07) and now tested by TC12C-36 and TC12C-37.

## Manual check (step 8 of the plan), with `lmartin` / `Admissions.2026`
- *Manage patients* shows for `lmartin` and not for `admin`, `mgarcia` or `jfernandez`.
- The screen opens with the three example patients and "3 patients found.".
- Pressing a row loads the form with the identifier locked; *New patient* empties it.
- Saving an empty form shows the four errors together and marks the fields in red.
- A date such as 31/02/2000, a phone of 8 digits and an e-mail without `@` are rejected.
- A valid registration says "Patient P-0004 registered." and selects it; modifying says "Patient P-0004 saved.".
- Registering the same identifier says that the patient exists and loads the existing record.
- With unsaved changes, pressing another row, *New patient* or *Search* asks "Discard the unsaved changes?".
- `admin` (who has no `MANAGE_PATIENTS`) does not see the button.
- Each row of *Usability decisions* of the plan is checked in the running application.
