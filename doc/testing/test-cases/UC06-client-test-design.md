# Test design · UC06 – Client (C4 v1.0.0) · #67 – Order a diagnostic procedure

Designed by Testing (Adam Gouram) following lesson T.04 (Testing Management): parameters of each input, equivalence
classes, boundary values and error guessing, combination of the values, and coverage of the code. Expected results
come from #66, #67, D-022, D-039 and the answers Q1–Q11 of `doc/plans/UC06-client.md` (the messages of the data:
Q4–Q5 of `doc/plans/UC06-server.md`); where the plan gives no text, the text is the one of the code that was
approved with it (`IU_OrderProcedure`, commit `cef2828`).

Code under test (`client-c4-orders` 1.0.0-SNAPSHOT, branch `feature/UC06-client`): the screen `IU_OrderProcedure`
(its public methods `onSearch`, `displayPatients`, `onSelectPatient`, `displayOrderForm`, `displayOrders`,
`onSubmitOrder`, `showPatientInactive`, `showValidationErrors`, `showOperationResultSuccess`,
`showOperationResultError`, the actions *Search*, *Place order*, *Back* and the selection of a row), and the button
*Order procedure* of `IU_Welcome` (`client-c1-access` 1.2.5-SNAPSHOT). Tests: `IU_OrderProcedureTest` and
`IU_WelcomeOrderProcedureTest`.

**Test data (precondition of every case).** The screen is built for the requester `mgarcia` (physician) and is never
shown. Three hand-written fakes, in memory:
- `FakePatientService` (C3): P-0001 `HC-0000001` Lucía Ejemplo Prueba (14/03/1980, active), P-0002 `HC-0000002`
  Mario Demo Simulado (02/11/1975, active) and the invented P-0009 `HC-0000009` Inés Inactiva Inventada (no date of
  birth, **inactive**). It searches by a part of the identifier or of the name ignoring case, records every search,
  can deny every call and can fail every search.
- `FakeSystemParameterService` (C2): the six active test types of the seed data (`XRAY-CHEST` Chest X-ray, `CT-HEAD`
  Head CT, `MRI-KNEE` Knee MRI, `US-ABDOMEN`, `MAMMO-BILATERAL`, `DEXA-SPINE`) plus the invented **deactivated**
  `XRAY-OLD`; the priorities `URGENT` Urgent, `PREFERENTIAL` Preferential, `ROUTINE` Routine plus the invented
  **deactivated** `OLD-TIER`.
- `FakeClinicalOrderService` (C4): P-0001 has ORD-0001 (`XRAY-CHEST`, `ROUTINE`, "Persistent cough", 20/11/2026) and
  ORD-0002 (`MRI-KNEE`, `URGENT`, "Knee pain after a fall", 21/11/2026); the others have none. It does not validate
  (so it shows that nothing is sent when the client rejects the form), records every order sent, every requester and
  every reading, gives the ids ORD-0003…, and can deny every call (`AccessDeniedException`), reject the next order
  (`ValidationException`), fail the next order or every reading.

Every question "Discard the unsaved changes?" is answered by the test (Yes or No), which also records it.

## 1. Parameters and testing values

| Input | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| Opening (constructor, `onSearch`) | state of C3 | Allows, with patients | the three patients |
| | | Denies | `denyAll()` before opening |
| | | Fails (Conjeture) | `IllegalStateException("The server cannot be reached")` |
| *Search* / Enter | search text | Finds all | `""` (on opening) |
| | | Finds one (Limit Val.: singular) | `"  mario "` (spaces at the ends, Conjeture) |
| | | Finds none (Limit Val.: 0) | `zzz` |
| Selected patient (row, `onSelectPatient`) | patient | Active, with orders | Lucía (2 orders) |
| | | Active, without orders (Limit Val.: 0 orders) | Mario |
| | | Inactive | Inés (P-0009) |
| | | None selected | the screen just opened |
| | | Not found (Conjeture) | `HC-9999999`; a part of an identifier `HC-000000` |
| Reading the orders of the patient | answer of C4 | Ok / `AccessDeniedException` / `ValidationException` / failure | 2 orders; `denyAll()`; "There is no patient with the id P-0001"; `IllegalStateException` |
| Order shown in the list (Conjeture) | data of the order | Usual / no date / test type not in the catalogue / not a `DiagnosticOrder` | ORD-0001; `null` date; `PET-BODY`; an order of another subclass |
| Test type (list) | catalogue of C2 | Active (offered) | the six of the seed data |
| | | Deactivated (not offered) | `XRAY-OLD` |
| | | No active one (Limit Val.: 0) | all deactivated; empty catalogue |
| Priority (list) | priorities of C2 | Active (offered) | `URGENT · Urgent`, `PREFERENTIAL · Preferential`, `ROUTINE · Routine` (F-05) |
| | | Deactivated (not offered) | `OLD-TIER` |
| | | No active one (Limit Val.: 0) | all deactivated; none |
| Clinical indication | text | Not blank | `Headache after a fall`, `"  Headache after a fall "` (trimmed) |
| | | One character (Limit Val.) | `a` |
| | | Empty (Limit Val.) | `""` |
| | | Blank (Conjeture) | `"   "`, `"  "` |
| Justification | text | Not blank / one character / empty / blank | `Rule out bleeding`, `" Rule out bleeding  "` / `b` / `""` / `" "` |
| *Place order* | answer of C4 | Ok | ORD-0003 created |
| | | `ValidationException` (1 error, Limit Val.) | "The priority URGENT is not active" |
| | | `ValidationException` (2 errors) | "The patient P-0001 is not active", "The test type CT-HEAD is not active" |
| | | `AccessDeniedException` | `denyAll()` |
| | | Failure (Conjeture) | `IllegalStateException("The server cannot be reached")` |
| *Back* / another patient with data | unsaved data | None / indication only / justification only / text but no patient | — |
| | answer to the question | Yes / No | `true` / `false` |
| `IU_Welcome` | permissions | With `ORDER_PROCEDURES` / without (admissions staff, none) | — |
| | action | Given / `null` / older constructor (UC12) | — |

Outputs: the rows of the two tables, the items of the two lists, the enabled state of the form, the text and colour of
the message area (green for success, red with `⚠` for errors), the order sent to `orderDiagnosticProcedure`, the
questions asked and whether *Back* runs its action.

## 2. Combination of the values
The inputs of the form only matter after an active patient is selected, so the suite is built with **each use**: every
testing value of section 1 appears in at least one case, and the rest of the form takes a valid value (`CT-HEAD`,
`URGENT · Urgent`, "Headache after a fall", "Rule out bleeding", Lucía). The pairs that the code combines in one
decision are also covered **pairwise** (all the combinations of two values): blank indication × blank justification
(TC06C-12, 13, 14, 15, 17), no active test type × no active priority (TC06C-07, 08, 16, 03), patient selected × unsaved
text (TC06C-30 to 36). Maximum number of test cases: the free texts have no maximum (any `String`, no maximum length, Q5
of the server plan), so the domain is divided into the classes of section 1; with them the full product of the form
would be 4 patients × 3 test-type classes × 3 priority classes × 4 × 4 texts = 576 cases, which each use plus the pairs
above reduce to the 43 of section 4; the fixes F-01 to F-04 add TC06C-44 to 49.

## 3. Coverage (white box)
Decisions of `IU_OrderProcedure` and the cases that make each condition true (T) and false (F):

| Decision | Condition | T | F |
|---|---|---|---|
| `onSearch`, `onSelectPatient`, `onSubmitOrder`: `denied` | — | TC06C-22 | every other case |
| `onSelectPatient` → `findPatient`: C3 denies after opening (F-01) | — | TC06C-44 | TC06C-03 |
| `displayPatients`: `getDateOfBirth() == null` | — | TC06C-01 (Inés) | TC06C-01 (Lucía) |
| `displayPatients`: `size() == 1` | — | TC06C-02 (mario) | TC06C-01 (3), TC06C-02 (0) |
| `selectedRowChanged`: `row < 0 \|\| row == lastPatientRow` | `row < 0` | — (see below) | TC06C-03 |
| | `row == lastPatientRow` | TC06C-34 (row given back) | TC06C-36 |
| `selectedRowChanged`: `hasUnsavedChanges() && !confirm` | unsaved | TC06C-34, 35 | TC06C-36 |
| | `!confirm` | TC06C-34 (No) | TC06C-35 (Yes) |
| `onSelectPatient`: `patient == null` | — | TC06C-10, 43 | TC06C-03 |
| `onSelectPatient`: `!patient.isActive()` | — | TC06C-09 | TC06C-03 |
| `findPatient`: `identifier.equals(...)` | — | TC06C-03 | TC06C-43 |
| `displayOrderForm`: `testType.isActive()` / `priority.isActive()` | — | TC06C-05, 06 | TC06C-05 (`XRAY-OLD`), 06 (`OLD-TIER`) |
| `displayOrderForm`: `activeTestTypes > 0 && activePriorities > 0` (MC/DC) | test types | TC06C-03 (T,T) | TC06C-07 (F,T) |
| | priorities | TC06C-03 (T,T) | TC06C-08 (T,F) |
| `displayOrders`: `getCreationDate() == null` | — | TC06C-29 | TC06C-03 |
| `displayOrders`: `instanceof DiagnosticOrder` | — | TC06C-03 | TC06C-29 |
| `validateForm`: `selectedPatient == null` | — | TC06C-11 | TC06C-12 |
| `validateForm`: no test type / no priority | — | TC06C-11, 16 | TC06C-12 |
| `validateForm`: indication blank | — | TC06C-12, 13, 14 | TC06C-15, 17 |
| `validateForm`: justification blank | — | TC06C-12, 13, 15 | TC06C-14, 17 |
| `onSubmitOrder`: `errors.isEmpty()` | — | TC06C-17, 18 | TC06C-11 to 16 |
| `showValidationErrors`: `size() == 1` | — | TC06C-14, 20 | TC06C-12, 19 |
| `markErrorFields`: `error.field() != null` (F-03) | — | TC06C-46 (the four fields) | TC06C-11 (the patient) |
| `clearErrorBorders` (F-03): before validating, after a success, on reload | — | TC06C-47 | — |
| `onBackRequested`: `hasUnsavedChanges() && !confirm` | unsaved | TC06C-31, 32 | TC06C-30 |
| | `!confirm` | TC06C-31 (No) | TC06C-32 (Yes) |
| `hasUnsavedChanges` (MC/DC): `patient != null && (!indication.isBlank() \|\| !justification.isBlank())` | patient | TC06C-31 | TC06C-33 |
| | indication | TC06C-31 | TC06C-30 |
| | justification | TC06C-32 | TC06C-30 |
| `catch` of every operation | `AccessDeniedException` | TC06C-22, 23, 24 | — |
| | `ValidationException` | TC06C-19, 20, 25 | — |
| | `RuntimeException` | TC06C-21, 26, 27 | — |

`IU_Welcome`: `onOrderProcedure != null && hasPermission(ORDER_PROCEDURES)`: (T,T) TC06C-38, (F,T) TC06C-41,
(T,F) TC06C-40; the older constructor passes `null` (TC06C-42).

Result of JaCoCo for `client-c4-orders` (`mvn -B verify`, with TC06C-44 to 49 and the fixes `c227441`, `4fd6204`): 100 %
of the lines (295 of 295) and 97.6 % of the branches (82 of 84), above the 80 % / 70 % of D-033. Not covered: `row < 0`
true in `selectedRowChanged` (a selection cleared by Swing while a row is selected) and the `valueIsAdjusting` branch of
the listener of the table (Swing only sends it while the mouse is dragged). The `catch` of `findPatient` that hid F-01
was removed by `c227441`.

## 4. Test cases

| ID | Method/action | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC06C-01 | constructor | Open the screen | Patients table *Identifier*, *Full name*, *Date of birth* with the 3 patients (`14/03/1980`, `02/11/1975`, empty for Inés); "3 patients found."; one search with `""`; no patient selected; form disabled; orders table *Date*, *Test type*, *Priority*, *Indication* empty; *Back* enabled; no call to C4 (Q3) | `tc06c01OpeningListsThePatients` |
| TC06C-02 | *Search*, Enter | `"  mario "` with the button; `zzz` with Enter | Mario only and "1 patient found."; then no row and "0 patients found."; searches `""`, `mario`, `zzz` (trimmed) | `tc06c02SearchFindsOneOrNone` |
| TC06C-03 | select row | Lucía (active, 2 orders) | Form enabled and empty; orders `20/11/2026 · Chest X-ray · ROUTINE · Routine · Persistent cough` and `21/11/2026 · Knee MRI · URGENT · Urgent · Knee pain after a fall`; one reading of P-0001 by `mgarcia` (Q4) | `tc06c03ActivePatientShowsTheFormAndTheOrders` |
| TC06C-04 | select row | Mario (active, no orders) | Form enabled; empty list of orders | `tc06c04PatientWithoutOrders` |
| TC06C-05 | `displayOrderForm` | Lucía; catalogue with `XRAY-OLD` deactivated | The six active test types as `id · name`, in the order of C2; `XRAY-OLD` not offered (Q5, D-022) | `tc06c05OnlyActiveTestTypes` |
| TC06C-06 | `displayOrderForm` | Lucía; `OLD-TIER` deactivated | `URGENT · Urgent`, `PREFERENTIAL · Preferential`, `ROUTINE · Routine` (id · name, F-05: Design, PR #204, 23/11/2026); `OLD-TIER` not offered (Q5) | `tc06c06OnlyActivePriorities` |
| TC06C-07 | `displayOrderForm` | Every test type deactivated | No test type; form disabled; "⚠ There are no active test types or priorities to order with." in red; the orders are still listed (Q5) | `tc06c07NoActiveTestTypes` |
| TC06C-08 | `displayOrderForm` | Every priority deactivated | No priority; form disabled; the same message (Q5) | `tc06c08NoActivePriorities` |
| TC06C-09 | select row, *Place order* | Lucía, then Inés (inactive) | "⚠ The patient P-0009 is not active, so no order can be placed." in red; form disabled; orders table empty; the orders of Inés are not read; nothing sent (#67, Q4) | `tc06c09InactivePatient` |
| TC06C-10 | `onSelectPatient` | `HC-9999999` (Conjeture) | "⚠ There is no patient with the identifier HC-9999999"; no patient; form disabled; no call to C4 | `tc06c10UnknownPatient` |
| TC06C-11 | `onSubmitOrder` | Just opened: no patient, empty lists and fields | "⚠ Select a patient before placing an order. The test type is required The priority is required The clinical indication is required The justification is required" in red; nothing sent (Q6) | `tc06c11EmptyFormWithoutPatient` |
| TC06C-12 | *Place order* | Lucía; indication and justification empty | "⚠ The clinical indication is required The justification is required"; nothing sent (Q6) | `tc06c12EmptyIndicationAndJustification` |
| TC06C-13 | *Place order* | Lucía; `"   "` and `" "` | The same two errors; nothing sent (Q5 of the server: `isBlank()`) | `tc06c13BlankIndicationAndJustification` |
| TC06C-14 | *Place order* | Indication `"  "`, justification valid | Only "⚠ The clinical indication is required"; the justification is kept | `tc06c14OnlyIndicationBlank` |
| TC06C-15 | *Place order* | Indication valid, justification `""` | Only "⚠ The justification is required"; the indication is kept | `tc06c15OnlyJustificationEmpty` |
| TC06C-16 | `onSubmitOrder` | Lucía; C2 without test types and priorities; texts valid | "⚠ The test type is required The priority is required"; nothing sent | `tc06c16NoTestTypeAndNoPriority` |
| TC06C-17 | *Place order* | Lucía, `CT-HEAD · Head CT`, `URGENT · Urgent`, `"  Headache after a fall "`, `" Rule out bleeding  "` | One call by `mgarcia` with order id `null`, P-0001, `CT-HEAD`, `URGENT`, the texts trimmed, no date; "Order ORD-0003 created." in green; fields emptied; Lucía still selected; the orders read again, the new one last with today's date (Q7) | `tc06c17ValidOrder` |
| TC06C-18 | *Place order* | Mario, `XRAY-CHEST`, `ROUTINE · Routine`, `a`, `b` (Limit Val.) | Sent with `a` and `b`; "Order ORD-0003 created."; one order in the list | `tc06c18OneCharacterTexts` |
| TC06C-19 | *Place order* | Valid form; C4 rejects with 2 errors | "⚠ The patient P-0001 is not active The test type CT-HEAD is not active" in red; the list is not read again; the form keeps the data (Q8) | `tc06c19ServerRejectsWithTwoErrors` |
| TC06C-20 | *Place order* | C4 rejects with 1 error | "⚠ The priority URGENT is not active" | `tc06c20ServerRejectsWithOneError` |
| TC06C-21 | *Place order* | C4 fails (Conjeture) | "⚠ The order could not be created: The server cannot be reached"; list and form unchanged | `tc06c21ServerFailsWhileOrdering` |
| TC06C-22 | *Place order*, then search, select, order | Valid form; C4 denies | "⚠ You do not have permission to order procedures." in red; form, table and search box disabled; *Back* enabled; nothing more reaches C3 or C4 (Q8) | `tc06c22DeniedWhileOrdering` |
| TC06C-23 | constructor, *Back* | C3 denies | The denied message; no patient listed; form, table and search box disabled; *Back* runs its action without asking; no call to C4 (Q8) | `tc06c23DeniedOnOpening` |
| TC06C-24 | select row | C4 denies the reading | The denied message; form and table disabled (Q8) | `tc06c24DeniedWhileReadingTheOrders` |
| TC06C-25 | select row | Reading rejected with "There is no patient with the id P-0001" | "⚠ There is no patient with the id P-0001"; form disabled | `tc06c25ValidationErrorWhileReadingTheOrders` |
| TC06C-26 | select row | Reading fails (Conjeture) | "⚠ The orders could not be read: The server cannot be reached"; form disabled | `tc06c26FailureWhileReadingTheOrders` |
| TC06C-27 | constructor | Search of C3 fails (Conjeture) | "⚠ The patients could not be read: The server cannot be reached"; no row; the search box stays enabled | `tc06c27FailureWhileReadingThePatients` |
| TC06C-28 | tables | Lucía selected | No cell of the two tables (9 + 8) is editable; the only buttons are *Search*, *Place order* and *Back*: nothing edits or cancels an order (D-039) | `tc06c28OrdersCannotBeEdited` |
| TC06C-29 | `displayOrders` | An order without date of `PET-BODY` and priority `VIP` (not in C2), and an order that is not a `DiagnosticOrder` (Conjeture) | Rows `"" · PET-BODY · VIP · Staging` and `22/11/2026 · Head CT · "" · Control` (the id when the name is unknown) | `tc06c29UnusualOrdersAreListed` |
| TC06C-30 | *Back* | Lucía, empty form | Runs its action; no question (Q11) | `tc06c30BackWithEmptyForm` |
| TC06C-31 | *Back* | Lucía, indication typed; answer No | "Discard the unsaved changes?" asked; *Back* does not run; the text is kept (Q11) | `tc06c31BackWithIndicationAnsweredNo` |
| TC06C-32 | *Back* | Lucía, only the justification typed; answer Yes | The question asked; *Back* runs (Q11) | `tc06c32BackWithJustificationAnsweredYes` |
| TC06C-33 | *Back* | No patient; indication text set | No question; *Back* runs (Q11: an order without patient is not a change) | `tc06c33BackWithTextButNoPatient` |
| TC06C-34 | select row | Lucía with indication typed, then Mario; answer No | The question asked; Lucía still selected, row 0 selected again, text kept; Mario's orders not read (Q11) | `tc06c34OtherPatientAnsweredNo` |
| TC06C-35 | select row | Lucía with justification typed, then Mario; answer Yes | The question asked; Mario selected, form emptied, his orders (none) read (Q11) | `tc06c35OtherPatientAnsweredYes` |
| TC06C-36 | select row | Lucía with empty form, then Mario | No question; Mario selected | `tc06c36OtherPatientWithEmptyForm` |
| TC06C-37 | buttons | Open the screen | *Place order* Alt+P, *Search* Alt+H, *Back* Alt+B (Q10) | `tc06c37Mnemonics` |
| TC06C-38 | `IU_Welcome` | Physician with `ORDER_PROCEDURES` and the action | Buttons *Order procedure*, *Log out*; Alt+O; the click runs the action once (Q9) | `tc06c38UserWithOrderProceduresSeesTheButton` |
| TC06C-39 | `IU_Welcome` | Every permission and action | *View audit log*, *Manage roles*, *Configure system*, *Configure integrations*, *Manage patients*, *Order procedure*, *Log out*; the button runs only its own action (Q9) | `tc06c39OrderWithTheOtherButtons`, `tc06c39OrderProcedureRunsOnlyItsOwnAction` |
| TC06C-40 | `IU_Welcome` | Admissions staff (`MANAGE_PATIENTS`); a user with no permission | No *Order procedure* (Q9) | `tc06c40UserWithoutOrderProceduresDoesNotSeeTheButton` |
| TC06C-41 | `IU_Welcome` | `ORDER_PROCEDURES` but `null` action | Only *Log out* (Q9) | `tc06c41WithoutTheActionNobodySeesTheButton` |
| TC06C-42 | `IU_Welcome` | The constructor of UC12 with every permission | The buttons of UC12, no *Order procedure* (Q9: the old constructors do not change) | `tc06c42OlderConstructorDoesNotShowTheButton` |
| TC06C-43 | `onSelectPatient` | `HC-000000`: a part of every identifier (Conjeture) | "⚠ There is no patient with the identifier HC-000000"; no patient; no call to C4 | `tc06c43PartialIdentifier` |
| TC06C-44 | select row | The permission of C3 is revoked after opening, then Lucía is selected (Conjeture; F-01) | "⚠ You do not have permission to order procedures." in red; no patient selected; form, table, search box and *Search* disabled; *Back* enabled; no call to C4 (Q8) | `tc06c44PermissionLostBeforeSelecting` |
| TC06C-45 | *Place order*; constructor | Valid form and C4 denies; C3 denies on opening (F-02) | The denied message; *Search* and *Place order* disabled; *Back* enabled (Q8) | `tc06c45SearchDisabledAfterDeniedOrder`, `tc06c45SearchDisabledWhenDeniedOnOpening` |
| TC06C-46 | `onSubmitOrder`, *Place order* | Just opened, empty form; Lucía with only the indication `"  "` (F-03) | A `LineBorder` of red (180, 0, 0) on test type, priority, indication and justification; then only on the indication (Q6) | `tc06c46EmptyFormBordersTheFourFields`, `tc06c46OnlyIndicationBlankBordersOnlyIt` |
| TC06C-47 | *Place order*, select row | Lucía, indication and justification empty: then the indication typed and ordered again; both typed and ordered (success); Mario selected (F-03) | Before: indication and justification bordered. Then only the justification; after the success none; after selecting Mario none (Q6, answer of Testing) | `tc06c47BordersClearedBeforeANewValidation`, `tc06c47BordersClearedAfterASuccess`, `tc06c47BordersClearedOnReload` |
| TC06C-48 | *Place order* | Valid form; C4 rejects with 2 errors (F-03) | The two messages in the message area; no field bordered (Q6, answer of Testing: the server's messages only appear in the message area) | `tc06c48ServerErrorsDoNotBorderTheFields` |
| TC06C-49 | labels | Open the screen (F-04) | The labels *Test type*, *Priority*, *Clinical indication*, *Justification* show Alt+T, Alt+R, Alt+C, Alt+J and their `labelFor` is their own list or field (Q10) | `tc06c49FieldMnemonics` |

Postcondition of every case: nothing is stored except by `orderDiagnosticProcedure` of the fake (TC06C-17, 18); after
a rejected, denied or failed order, or an error of the client, no order is created and the list of the patient is the
same; the screen is never shown.

## 5. Not covered / Not defined

**Defects found** in the review of `feature/UC06-client` (#204). F-01 to F-04 are **fixed** by Implementation in
`c227441` (F-01) and `4fd6204` (F-02 to F-04), steps 9 to 11 of `doc/plans/UC06-client.md`, and checked by TC06C-44 to
49. F-05 is **decided by Design** (PR #204, 23/11/2026): the priority is shown as `URGENT · Urgent` (id · name, the
separator of the test types) in the list and in the column *Priority*; the expected values of TC06C-03, 06, 07, 17, 18
and 19 and the priorities chosen in the other cases follow it.
- **F-01 (fixed, `c227441`) – The denied access is hidden when the permission is lost while selecting
  (TC06C-44).** `findPatient` caught the `AccessDeniedException`, called `handleDenied()` and returned `null`, and
  `onSelectPatient` then wrote "⚠ There is no patient with the identifier HC-0000001" over the denied message.
  Q8 says the denied access shows "You do not have permission to order procedures.".
- **F-02 (fixed, `4fd6204`) – *Search* stayed enabled after the denied access.** `handleDenied()` disabled the
  form, the table and the search box, but not the button *Search*. Q8 says "leaves only *Back* enabled" (TC06C-45).
- **F-03 (fixed, `4fd6204`) – No red border on the fields with errors.** Q6 says "the field of each error gets a red
  border"; the screen only wrote the message (TC06C-46 to 48).
- **F-04 (fixed, `4fd6204`) – No Alt keys for the fields.** Q10 gives Alt+T, Alt+R, Alt+C and Alt+J; the labels of
  the form had no `setLabelFor` nor `setDisplayedMnemonic` (TC06C-49).
- **F-05 (decided by Design, PR #204, 23/11/2026; fixed, `61e7246`) – The priority was shown only by its name.**
  *Usability decisions* says "the priority is shown by its name, with its id next to it"; the list and the table showed
  only the name. Design decided `URGENT · Urgent` in the list and in the column *Priority* (TC06C-03, 06, 17); an order
  whose priority is not in C2 (TC06C-29, `VIP`) is not defined by Design, so its expected value is not changed.

**Not defined** (no expected result in the plan or the specification):
- The order of the list of orders (the client shows them in the order of C4; newest first is not said).
- The maximum length of the indication and the justification (Q5 of the server plan: none, pending confirmation by
  Requirements); no case of a very long text.
- Whether a priority or test type is chosen by default (the code selects the first item of each list; the default
  priority of the test type in C2 is not used).
- What happens if *Place order* is pressed twice quickly.
- Q4 says the patient is read with `getPatient`; the code finds it with `searchPatients` by its identifier. The
  behaviour is the same for the user, so no case checks the call.

**Not covered here:** the rows of *Usability decisions* that need a desktop (layout, colours seen by a person, focus,
Tab order) are left to the manual check of step 8 of the plan; the real services of C2, C3 and C4 (they have their own
tests in the server modules).
