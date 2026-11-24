# Test design · UC07 – Client (C4 v1.1.0) · #70 – Order periodic follow-up procedures

Designed by Testing (Adam Gouram) following lesson T.04 (Testing Management): parameters of each input, equivalence
classes, boundary values and error guessing, combination of the values, and coverage of the code. Expected results
come from #69, #70, D-022, D-039, D-040 and the decisions of `doc/plans/UC07-client.md` (*Only eligible test types are
offered*, *Recurrence interval*, *Messages of the client*, *Orders of the patient*, *Permission*, *Unsaved changes*);
the texts of the messages are the ones of the server of #71 (Q3 of `doc/plans/UC07-server.md`), and where the plan gives
no text, the text is the one of the code that was approved with it (`IU_OrderFollowUp`, branch `feature/UC07-client`,
commit `46a164b`). The layout follows `IU_OrderProcedure` ("Same layout", plan), so the keys of the buttons are those of
Q10 of `doc/plans/UC06-client.md`.

Code under test (`client-c4-orders` 1.1.0-SNAPSHOT): the screen `IU_OrderFollowUp` (its public methods `onSearch`,
`displayPatients`, `onSelectPatient`, `displayOrderForm`, `displayOrders`, `onSubmitOrder`, `showPatientInactive`,
`showValidationErrors`, `showOperationResultSuccess`, `showOperationResultError`, the actions *Search*, *Place order*,
*Back*, the selection of a row and the spinner of the interval), and the button *Order follow-up* of `IU_Welcome`
(`client-c1-access` 1.2.6-SNAPSHOT). Tests: `IU_OrderFollowUpTest` and `IU_WelcomeOrderFollowUpTest`; in this branch
also `IU_OrderProcedureInUc07Test` (section 4, last table).

**Test data (precondition of every case).** The screen is built for the requester `mgarcia` (physician) and is never
shown. Three hand-written fakes, in memory (their names do not clash with the fakes of UC06 in `feature/UC06-client`):
- `FollowUpPatientServiceFake` (C3): P-0001 `HC-0000001` Lucía Ejemplo Prueba (14/03/1980, active), P-0002
  `HC-0000002` Mario Demo Simulado (02/11/1975, active) and the invented P-0009 `HC-0000009` Inés Inactiva Inventada
  (no date of birth, **inactive**). It searches by a part of the identifier or of the name ignoring case, records every
  search, can deny every call and can fail every search.
- `FollowUpParameterServiceFake` (C2): the six test types of the seed data with their eligibility for follow-up
  (`XRAY-CHEST`, `MRI-KNEE`, `US-ABDOMEN`, `MAMMO-BILATERAL`, `DEXA-SPINE` eligible; `CT-HEAD` **not eligible**), plus
  the invented **deactivated** `XRAY-OLD` (eligible) and `CT-OLD` (not eligible); the priorities `URGENT`,
  `PREFERENTIAL`, `ROUTINE` (only used by the screen of UC06).
- `FollowUpOrderServiceFake` (C4): P-0001 has ORD-0001, a **follow-up** order of `DEXA-SPINE` every **6** months
  ("Osteoporosis treatment", 20/11/2026), and ORD-0002, a **diagnostic** order of `XRAY-CHEST` `ROUTINE` ("Persistent
  cough", 21/11/2026); the others have none. It does not validate (so it shows that nothing is sent when the client
  rejects the form), records every follow-up and diagnostic order sent, every requester and every reading, gives the
  ids ORD-0003…, and can deny every call (`AccessDeniedException`), reject the next order (`ValidationException`), fail
  the next order or every reading.

Every question "Discard the unsaved changes?" is answered by the test (Yes or No), which also records it.

## 1. Parameters and testing values

| Input | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| Select or search | Previous form | Active patient with a draft and orders | Lucía, both texts filled, two orders |
| Select | Read failure (**Conjeture**) | C2, C3 or C4 unavailable, denied or rejected | TC07C-49 to 53, 55 |
| Opening (constructor, `onSearch`) | state of C3 | Allows, with patients | the three patients |
| | | Denies | `denyAll()` before opening |
| | | Fails (Conjeture) | `IllegalStateException("The server cannot be reached")` |
| *Search* / Enter | search text | Finds all / one (Limit Val.: singular) / none (Limit Val.: 0) | `""`; `"  mario "` (spaces at the ends, Conjeture); `zzz` |
| Selected patient (row, `onSelectPatient`) | patient | Active, with orders | Lucía (1 follow-up + 1 diagnostic) |
| | | Active, without orders (Limit Val.: 0 orders) | Mario |
| | | Inactive | Inés (P-0009) |
| | | None selected | the screen just opened |
| | | Not found (Conjeture) | `HC-9999999`; a part of every identifier `HC-000000` |
| Reading the orders | answer of C4 | Ok / `AccessDeniedException` / `ValidationException` / failure | 2 orders; `denyAll()`; "There is no patient with the id P-0001"; `IllegalStateException` |
| Order shown in the list | kind of order | `FollowUpOrder` (interval shown) / other (`DiagnosticOrder`, empty interval) | ORD-0001 (6); ORD-0002 |
| | data (Conjeture) | No date / test type not in the catalogue | `null` date; `PET-BODY` |
| Test type (list) | active × eligible | Active and eligible (offered) | `XRAY-CHEST`, `MRI-KNEE`, `US-ABDOMEN`, `MAMMO-BILATERAL`, `DEXA-SPINE` |
| | | Active, not eligible (not offered) | `CT-HEAD` |
| | | Inactive, eligible (not offered) | `XRAY-OLD` |
| | | Inactive, not eligible (not offered) | `CT-OLD` |
| | | No active eligible one (Limit Val.: 0) | every eligible type deactivated |
| Recurrence interval (spinner, 0–12, step 1) | value set | Not positive: 0 (Limit Val., the minimum) | `0` |
| | | Valid: 1 (Limit Val.), 6 (default), 12 (the year of D-040) | `1`, `6`, `12` |
| | value typed in the box | Outside 0–12 | `-1`, `13` (**Limit Val.**), `121` |
| | | Not a number / too big (Conjeture) | `abc`, `99999999999` |
| | | In the range (**Limit Val.**) | `0`, `12` |
| | Arrows | At the minimum and maximum (**Limit Val.**) | Previous and next at 0 and at 12 |
| Clinical indication | text | Not blank / one character / empty / blank | `Meniscus repair`, `"  Meniscus repair "` (trimmed) / `a` / `""` / `"   "`, `"  "` |
| Justification | text | Not blank / one character / empty / blank | `Check the evolution`, `" Check the evolution  "` / `b` / `""` / `" "` |
| *Place order* | answer of C4 | Ok | ORD-0003 created |
| | | `ValidationException` (1 error, Limit Val.) | "The recurrence interval must be a positive number of months" |
| | | `ValidationException` (2 errors) | "The patient P-0001 is not active", "The test type MRI-KNEE is not eligible for follow-up" |
| | | `AccessDeniedException` | `denyAll()` |
| | | Failure (Conjeture) | `IllegalStateException("The server cannot be reached")` |
| *Back* / another patient | unsaved data | None / indication only / justification only / text but no patient | — |
| | answer to the question | Yes / No | `true` / `false` |
| `IU_Welcome` | permissions | With `ORDER_PROCEDURES` / without (admissions staff, none) | — |
| | action | Given / `null` / older constructor (UC06) | — |

Outputs: the rows of the two tables, the items of the list of test types, the value and bounds of the spinner, the
enabled state of the form, the text and colour of the message area (green for success, red with `⚠` for errors), the
follow-up order sent to `orderFollowUp`, the questions asked and whether *Back* runs its action.

## 2. Combination of the values

TC07C-49 to 57 hold Lucía's draft constant and vary the result of the next lookup, read or search.
The inputs of the form only matter after an active patient is selected, so the suite is built with **each use**: every
testing value of section 1 appears in at least one case, and the rest of the form takes a valid value (`MRI-KNEE`,
6 months, "Meniscus repair", "Check the evolution", Lucía). The pairs that the code combines in one decision are also
covered **pairwise**: active × eligible of the test type (the four classes, TC07C-05), interval × texts (TC07C-15, 16,
11, 19), blank indication × blank justification (TC07C-11 to 14, 19), patient selected × unsaved text (TC07C-32 to 38).
Maximum number of test cases: the interval has 13 values (0 to 12) and the texts have no maximum, so the domain is
divided into the classes of section 1; with them the full product of the form would be 4 patients × 5 test-type
classes × 6 interval classes × 4 × 4 texts = 1,920 cases, which each use plus the pairs above reduce to the 45 of
section 4.

## 3. Coverage (white box)
Decisions of `IU_OrderFollowUp` and the cases that make each condition true (T) and false (F):

| Decision | Condition | T | F |
|---|---|---|---|
| `onSearch`, `onSelectPatient`, `onSubmitOrder`: `denied` | — | TC07C-24 | every other case |
| `displayPatients`: `getDateOfBirth() == null` | — | TC07C-01 (Inés) | TC07C-01 (Lucía) |
| `displayPatients`: `size() == 1` | — | TC07C-02 (mario) | TC07C-01 (3), TC07C-02 (0) |
| `selectedRowChanged`: `row < 0 \|\| row == lastPatientRow` | `row < 0` | TC07C-56 | TC07C-03 |
| | `row == lastPatientRow` | TC07C-36 (row given back) | TC07C-38 |
| `selectedRowChanged`: `hasUnsavedChanges() && !confirm` | unsaved | TC07C-36, 37 | TC07C-38 |
| | `!confirm` | TC07C-36 (No) | TC07C-37 (Yes) |
| `onSelectPatient`: `patient == null` | — | TC07C-09, 45 | TC07C-03 |
| `onSelectPatient`: `!patient.isActive()` | — | TC07C-08 | TC07C-03 |
| `findPatient`: `identifier.equals(...)` | — | TC07C-03 | TC07C-45 |
| `displayOrderForm`: `isActive() && isFollowUpEligible()` (MC/DC) | active | TC07C-05 (`XRAY-CHEST`) | TC07C-05 (`XRAY-OLD`) |
| | eligible | TC07C-05 (`XRAY-CHEST`) | TC07C-05 (`CT-HEAD`) |
| `displayOrderForm`: `eligible > 0` / `eligible == 0` | — | TC07C-03 | TC07C-06 |
| `displayOrders`: `getCreationDate() == null` | — | TC07C-31 | TC07C-03 |
| `displayOrders`: `instanceof FollowUpOrder` | — | TC07C-03 (ORD-0001) | TC07C-03 (ORD-0002) |
| `validateForm`: `selectedPatient == null` | — | TC07C-10 | TC07C-11 |
| `validateForm`: no test type | — | TC07C-10 | TC07C-11 |
| `validateForm`: interval `<= 0` | — | TC07C-15, 16 | TC07C-11, 19, 20 |
| `validateForm`: indication blank | — | TC07C-11, 12, 13 | TC07C-14, 19 |
| `validateForm`: justification blank | — | TC07C-11, 12, 14 | TC07C-13, 19 |
| `onSubmitOrder`: `errors.isEmpty()` | — | TC07C-19, 20 | TC07C-10 to 16 |
| `showValidationErrors`: `size() == 1` | — | TC07C-13, 15, 22 | TC07C-11, 16, 21 |
| `onBackRequested`: `hasUnsavedChanges() && !confirm` | unsaved | TC07C-33, 34 | TC07C-32 |
| | `!confirm` | TC07C-33 (No) | TC07C-34 (Yes) |
| `hasUnsavedChanges` (MC/DC): `patient != null && (!indication.isBlank() \|\| !justification.isBlank())` | patient | TC07C-33 | TC07C-35 |
| | indication | TC07C-33 | TC07C-32 |
| | justification | TC07C-34 | TC07C-32 |
| `catch` of every operation | `AccessDeniedException` | TC07C-24, 25, 26, 46, 47, 48 | — |
| | `ValidationException` | TC07C-21, 22, 27 | — |
| | `RuntimeException` | TC07C-23, 28, 29 | — |

`IU_Welcome`: `onOrderFollowUp != null && hasPermission(ORDER_PROCEDURES)`: (T,T) TC07C-40, (F,T) TC07C-43,
(T,F) TC07C-42; the older constructor passes `null` (TC07C-44).

Result of JaCoCo for `client-c4-orders` (`mvn -B verify`, commit `8dd88e7` plus the tests, with the fixes of F-01 and
F-02): `IU_OrderFollowUp` 100 % of the lines (248 of 248) and 97.1 % of the branches (68 of 70); the module 98.9 % of
the lines (524 of 530) and 95.2 % of the branches (139 of 146), above the 80 % / 70 % of D-033. Not covered in
`IU_OrderFollowUp`: `row < 0` true in `selectedRowChanged` (a selection cleared by Swing while a row is selected) and
the `valueIsAdjusting` branch of the listener of the table (Swing only sends it while the mouse is dragged).

After R-01, TC07C-56 exercises the negative row index when a new search clears Lucía's selection. TC07C-54
and 57 exercise the unknown and inactive patient branches with a draft. TC07C-49 to 53 and 55 exercise read
exceptions; TC07C-03 exercises successful reads. R-01 adds no compound decision.

With TC07C-49 to 57, the coverage report shows 270/270 lines and 69/70 branches for `IU_OrderFollowUp` itself.
The module covers 546/552 lines (98.9 %) and 140/146 branches (95.9 %).
The run with the revised TC07C-17/18 gives the same coverage. Both cases fail against the 0–120 spinner at `cefde9a`:
the maximum is 120, the up arrow offers 13 after 12, and the editor accepts 13.

## 4. Test cases

| ID | Method/action | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC07C-01 | constructor | Open the screen | Patients table *Identifier*, *Full name*, *Date of birth* with the 3 patients (`14/03/1980`, `02/11/1975`, empty for Inés); "3 patients found."; one search with `""`; no patient selected; form disabled; orders table *Date*, *Test type*, *Interval (months)*, *Indication* empty; *Back* enabled; no call to C4 | `tc07c01OpeningListsThePatients` |
| TC07C-02 | *Search*, Enter | `"  mario "` with the button; `zzz` with Enter | Mario only and "1 patient found."; then no row and "0 patients found."; searches `""`, `mario`, `zzz` (trimmed) | `tc07c02SearchFindsOneOrNone` |
| TC07C-03 | select row | Lucía (active, 2 orders) | Form enabled, texts empty, interval 6; orders `20/11/2026 · Bone densitometry · 6 · Osteoporosis treatment` and `21/11/2026 · Chest X-ray · "" · Persistent cough` (plan: interval read with `instanceof FollowUpOrder`, empty for UC06 orders); one reading of P-0001 by `mgarcia` | `tc07c03ActivePatientShowsTheOrdersWithTheInterval` |
| TC07C-04 | select row | Mario (active, no orders) | Form enabled; empty list of orders | `tc07c04PatientWithoutOrders` |
| TC07C-05 | `displayOrderForm` | Lucía; the catalogue with the four classes | Only `XRAY-CHEST · Chest X-ray`, `MRI-KNEE · Knee MRI`, `US-ABDOMEN · Abdominal ultrasound`, `MAMMO-BILATERAL · Bilateral mammography`, `DEXA-SPINE · Bone densitometry`, in the order of C2; not `CT-HEAD` (not eligible), `XRAY-OLD` (inactive), `CT-OLD` (both) (plan, #70 alt. 2, D-022) | `tc07c05OnlyActiveAndEligibleTestTypes` |
| TC07C-06 | `displayOrderForm` | Every eligible test type deactivated (`CT-HEAD` still active) | No test type; form disabled; "⚠ There is no active test type eligible for follow-up." in red; the orders are still listed (plan) | `tc07c06NoActiveEligibleTestType` |
| TC07C-07 | form | Open the screen | Labels *Test type*, *Interval (months)*, *Clinical indication*, *Justification*; no *Priority*, one list only (#69: a follow-up order has an interval instead) | `tc07c07FormHasNoPriority` |
| TC07C-08 | select row, *Place order* | Lucía, then Inés (inactive) | "⚠ The patient P-0009 is not active, so no order can be placed." in red; form disabled; orders table empty; the orders of Inés are not read; nothing sent (#70 alt. 3) | `tc07c08InactivePatientBlocksTheOrder` |
| TC07C-09 | `onSelectPatient` | `HC-9999999` (Conjeture) | "⚠ There is no patient with the identifier HC-9999999"; no patient; form disabled; no call to C4 | `tc07c09UnknownPatient` |
| TC07C-10 | `onSubmitOrder` | Just opened: no patient, empty list and fields, interval 6 | "⚠ Select a patient before placing an order. The test type is required The clinical indication is required The justification is required" in red; nothing sent | `tc07c10EmptyFormWithoutPatient` |
| TC07C-11 | *Place order* | Lucía; indication and justification empty | "⚠ The clinical indication is required The justification is required"; nothing sent (texts of #71) | `tc07c11EmptyIndicationAndJustification` |
| TC07C-12 | *Place order* | Lucía; `"   "` and `" "` | The same two errors; nothing sent | `tc07c12BlankIndicationAndJustification` |
| TC07C-13 | *Place order* | Indication `"  "`, justification valid | Only "⚠ The clinical indication is required"; the justification is kept | `tc07c13OnlyIndicationBlank` |
| TC07C-14 | *Place order* | Indication valid, justification `""` | Only "⚠ The justification is required"; the indication is kept | `tc07c14OnlyJustificationEmpty` |
| TC07C-15 | *Place order* | Lucía, `MRI-KNEE`, interval **0**, texts valid | "⚠ The recurrence interval must be a positive number of months" in red (the text of #71, plan); nothing sent; the interval (0) and the texts are kept | `tc07c15IntervalZeroIsRejected` |
| TC07C-16 | *Place order* | Lucía, interval 0, texts empty | "⚠ The recurrence interval must be a positive number of months The clinical indication is required The justification is required" (the order of the form); nothing sent | `tc07c16IntervalZeroAndEmptyTexts` |
| TC07C-17 | Spinner arrows | Lucía selected, at 0 and 12 | Range and neighbours below | `tc07c17SpinnerBoundaries` |
| TC07C-18 | Spinner editor | Type values listed below | Accept 0 and 12, reject the rest | `tc07c18TypedIntervals` |
| TC07C-19 | *Place order* | Lucía, `MRI-KNEE · Knee MRI`, interval **1**, `"  Meniscus repair "`, `" Check the evolution  "` | **Exactly one** `orderFollowUp` by `mgarcia` (no diagnostic order) with order id `null`, P-0001, `MRI-KNEE`, the texts trimmed, no date, interval 1; "Order ORD-0003 created." in green; fields emptied and interval back to 6; Lucía still selected; the orders read again, the new one last: `today · Knee MRI · 1 · Meniscus repair` (#70 criterion 3) | `tc07c19ValidOrder` |
| TC07C-20 | *Place order* | Mario, `DEXA-SPINE`, interval **12**, `a`, `b` (Limit Val.) | Sent with 12, `a` and `b`; "Order ORD-0003 created."; one order in the list, interval `12` (D-040) | `tc07c20TwelveMonthsAndOneCharacterTexts` |
| TC07C-21 | *Place order* | Valid form; C4 rejects with 2 errors | "⚠ The patient P-0001 is not active The test type MRI-KNEE is not eligible for follow-up" in red; the list is not read again; the form keeps the data and the interval (plan: errors shown as they come) | `tc07c21ServerRejectsWithTwoErrors` |
| TC07C-22 | *Place order* | C4 rejects with 1 error | "⚠ The recurrence interval must be a positive number of months" | `tc07c22ServerRejectsWithOneError` |
| TC07C-23 | *Place order* | C4 fails (Conjeture) | "⚠ The order could not be created: The server cannot be reached" in red; list and form unchanged | `tc07c23ServerFailsWhileOrdering` |
| TC07C-24 | *Place order*, then search, select, order | Valid form; C4 denies | "⚠ You do not have permission to order procedures." in red; form, table and search box disabled; *Back* enabled; nothing more reaches C3 or C4 (plan, *Permission*) | `tc07c24DeniedWhileOrdering` |
| TC07C-25 | constructor, *Back* | C3 denies | The denied message; no patient listed; form, table and search box disabled; *Back* runs its action without asking; no call to C4 | `tc07c25DeniedOnOpening` |
| TC07C-26 | select row | C4 denies the reading | The denied message; form and table disabled; *Back* enabled | `tc07c26DeniedWhileReadingTheOrders` |
| TC07C-27 | select row | Reading rejected with "There is no patient with the id P-0001" | "⚠ There is no patient with the id P-0001" in red; form disabled | `tc07c27ValidationErrorWhileReadingTheOrders` |
| TC07C-28 | select row | Reading fails (Conjeture) | "⚠ The orders could not be read: The server cannot be reached"; form disabled | `tc07c28FailureWhileReadingTheOrders` |
| TC07C-29 | constructor | Search of C3 fails (Conjeture) | "⚠ The patients could not be read: The server cannot be reached" in red; no row; the search box stays enabled | `tc07c29FailureWhileReadingThePatients` |
| TC07C-30 | tables | Lucía selected | No cell of the two tables (9 + 8) is editable; the only buttons are *Search*, *Place order* and *Back*: nothing edits or cancels an order (D-039) | `tc07c30OrdersCannotBeEdited` |
| TC07C-31 | `displayOrders` | A follow-up order without date of `PET-BODY` (not in C2), every 3 months (Conjeture) | Row `"" · PET-BODY · 3 · Staging` (the id when the name is unknown) | `tc07c31UnusualOrdersAreListed` |
| TC07C-32 | *Back* | Lucía, empty form | Runs its action; no question (plan, *Unsaved changes*) | `tc07c32BackWithEmptyForm` |
| TC07C-33 | *Back* | Lucía, indication typed; answer No | "Discard the unsaved changes?" asked; *Back* does not run; the text is kept | `tc07c33BackWithIndicationAnsweredNo` |
| TC07C-34 | *Back* | Lucía, only the justification typed; answer Yes | The question asked; *Back* runs | `tc07c34BackWithJustificationAnsweredYes` |
| TC07C-35 | *Back* | No patient; indication text set (Conjeture) | No question; *Back* runs (code: an order without patient is not a change, as Q11 of UC06) | `tc07c35BackWithTextButNoPatient` |
| TC07C-36 | select row | Lucía with indication typed, then Mario; answer No | The question asked; Lucía still selected, row 0 selected again, text kept; Mario's orders not read | `tc07c36OtherPatientAnsweredNo` |
| TC07C-37 | select row | Lucía with justification typed and interval 3, then Mario; answer Yes | The question asked; Mario selected, form emptied and interval back to 6, his orders (none) read | `tc07c37OtherPatientAnsweredYes` |
| TC07C-38 | select row | Lucía with empty form, then Mario | No question; Mario selected | `tc07c38OtherPatientWithEmptyForm` |
| TC07C-39 | buttons | Open the screen | *Place order* Alt+P, *Search* Alt+H, *Back* Alt+B (same layout as UC06, Q10 of its plan) | `tc07c39Mnemonics` |
| TC07C-40 | `IU_Welcome` | Physician with `ORDER_PROCEDURES` and both actions | Buttons *Order procedure*, *Order follow-up*, *Log out*; Alt+F; the click runs the action once (plan, *Permission*) | `tc07c40UserWithOrderProceduresSeesTheButton` |
| TC07C-41 | `IU_Welcome` | Every permission and action | *View audit log*, *Manage roles*, *Configure system*, *Configure integrations*, *Manage patients*, *Order procedure*, *Order follow-up*, *Log out*; the button runs only its own action | `tc07c41OrderWithTheOtherButtons`, `tc07c41OrderFollowUpRunsOnlyItsOwnAction` |
| TC07C-42 | `IU_Welcome` | Admissions staff (`MANAGE_PATIENTS`); a user with no permission | No *Order follow-up* | `tc07c42UserWithoutOrderProceduresDoesNotSeeTheButton` |
| TC07C-43 | `IU_Welcome` | `ORDER_PROCEDURES` but `null` action | *Order procedure* and *Log out* only (Javadoc of the constructor: "not at all if this is `null`") | `tc07c43WithoutTheActionNobodySeesTheButton` |
| TC07C-44 | `IU_Welcome` | The constructor of UC06 with every permission | The buttons of UC06, no *Order follow-up* | `tc07c44OlderConstructorDoesNotShowTheButton` |
| TC07C-45 | `onSelectPatient` | `HC-000000`: a part of every identifier (Conjeture) | "⚠ There is no patient with the identifier HC-000000"; no patient; no call to C4 | `tc07c45PartialIdentifier` |
| TC07C-46 | `onSelectPatient` | C3 denies after opening, then Lucía is selected (F-01) | "⚠ You do not have permission to order procedures." in red; no patient selected; form, table, search box and *Search* disabled; *Back* enabled; no call to C4 | `tc07c46PermissionLostBeforeSelecting` |
| TC07C-47 | `onSubmitOrder` | Valid form of Lucía, C4 denies the order (F-02) | The denied message; *Search* and *Place order* disabled; *Back* enabled | `tc07c47SearchDisabledAfterDeniedOrder` |
| TC07C-48 | `IU_OrderFollowUp` | C3 denies on opening (F-02) | The denied message; *Search* and *Place order* disabled; *Back* enabled | `tc07c48SearchDisabledWhenDeniedOnOpening` |

TC07C-49 to 57 start with Lucía selected, two orders, indication “Meniscus repair” and justification
“Check the evolution”. Every case expects both texts empty, the form disabled and zero order rows after the action.
The selection is empty except in TC07C-57, where the inactive patient is selected. Source for all nine cases:
R-01 in the client plan (`6a8179b`) and `9583556`.

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC07C-49 | Select | C2 offline | Orders read error (catalogue) | `tc07c49TestTypesOffline` |
| TC07C-50 | Select | C3 offline | Patient read error | `tc07c50PatientLookupFails` |
| TC07C-51 | Select | C4 rejects | Validation error | `tc07c51OrderReadRejected` |
| TC07C-52 | Select | C4 denies | Denied | `tc07c52OrderReadDenied` |
| TC07C-53 | Select | C4 offline | Orders read error (orders) | `tc07c53OrderReadFails` |
| TC07C-54 | Select | Unknown ID | Unknown patient | `tc07c54UnknownPatient` |
| TC07C-55 | Select | C3 denies | Denied | `tc07c55PatientLookupDenied` |
| TC07C-56 | Search | Search Mario | Only Mario listed | `tc07c56SearchClearsDraft` |
| TC07C-57 | Select | Select Inés | Inés selected, inactive error | `tc07c57InactivePatientClearsDraft` |

The error messages for TC07C-49 to 55 appear in red:

- TC07C-49: “⚠ The orders could not be read: Catalogue offline”.
- TC07C-53: “⚠ The orders could not be read: Orders offline”.
- TC07C-50: “⚠ The patient could not be read: Registry offline”.
- TC07C-51: “⚠ Patient unavailable”.
- TC07C-52 and 55: “⚠ You do not have permission to order procedures.”
- TC07C-54: “⚠ There is no patient with the identifier HC-9999999”.

**Cases of UC06 run again in this branch** (`IU_OrderProcedureInUc07Test`). `feature/UC07-client` was created before
the tests of UC06 were written, so `IU_OrderProcedure` (about half of the module) had no test here and the module did
not reach D-033. The class runs, with the fakes of UC07, the cases of `UC06-client-test-design.md` whose expected
result the fixes F-01 to F-05 of UC06 did not change (the priority is chosen by its position, never read as a text;
*Search* and the borders are not checked): TC06C-01, 03, 05, 07, 08, 09, 10, 11, 14, 17, 19, 21, 22, 23, 24, 25, 26,
27, 29, 31, 32, 34 and 35, with the expected results of that document. Two of them also check UC07 data: in TC06C-03
and TC06C-29 a follow-up order is listed in the screen of UC06 with an empty priority, and in TC06C-17 the screen of
UC06 sends a diagnostic order and never a follow-up. The class was also run against the fixed `IU_OrderProcedure` and
the tests of `feature/UC06-client` together (23 + 48 + 40 tests, green), so it can stay after the merge or be removed
then, as the team decides.

Postcondition of every case: nothing is stored except by `orderFollowUp` of the fake (TC07C-19, 20); after a rejected,
denied or failed order, or an error of the client, no order is created and the list of the patient is the same; the
screen is never shown.

## 5. Not covered / Not defined

**Changed expected values**

- TC07C-17: range 0–120 → 0–12, step 1. At 0 the previous and next values are null and 1.
  At 12 they are 11 and null (previously 119 and null at 120).
- TC07C-18: typed upper endpoint 120 → 12. The editor accepts 0 and 12. It rejects 13 as well as
  -1, 121, abc and 99999999999, keeping the previous value of 6 and sending no order.
- Source for both changes: David, [#207](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/issues/207), 24/11/2026.

**Defects found during review**
F-01 and F-02 were already found and fixed in the screen of UC06 (F-01 and F-02 of
`UC06-client-test-design.md`, commits `c227441` and `4fd6204`), but `IU_OrderFollowUp` was copied from the version
before those fixes.
- **F-01 – The denied access was hidden when the permission is lost while selecting.** Fixed in `da9a75d`, checked
  by TC07C-46. With C3 denying after opening, selecting Lucía showed "⚠ There is no patient with the identifier
  HC-0000001" instead of the denied message: `findPatient` caught the `AccessDeniedException`, called
  `handleDenied()` and returned `null`, and `onSelectPatient` then wrote its own error over it. The plan says "the
  screen explains a denied access". Now the exception reaches `onSelectPatient`: "⚠ You do not have permission to
  order procedures.", no patient selected, only *Back* enabled.
- **F-02 – *Search* stayed enabled after the denied access.** Fixed in `5732b3c`, checked by TC07C-46, 47 and 48.
  `handleDenied()` disabled the form, the table and the search box, but not the button *Search*. The plan says
  "leaves only *Back* enabled". Now *Search* is disabled after a denied opening, a denied selection and a denied
  order. No existing expected value changed: TC07C-24 and 25 did not check the button and stay as they were.
- **F-03 – Switching to an inactive patient kept the previous draft.** In `9583556`, confirming the switch clears
  the draft before displaying the inactive patient. TC07C-57 checks this transition.

**Not defined** (no expected result in the plan or the specification):
- R-01 defines the reset when a selected patient's lookup fails and when a new search displays its results.
  It gives no separate expected draft or order-table state for a failed search of the patient list before results
  reach `displayPatients`. TC07C-56 checks the successful list refresh.
- Values typed in the box of the spinner that the formatter of Swing reads in its own way: `6.5` is taken as 6 and
  `6,5` as 65 (with the locale `en_US`; with `es_ES` the separators are the other way round), while `+6` and `" 6 "`
  are refused. The range decision does not specify how to interpret these strings.
- The keys of the fields (UC06 has Alt+T, Alt+R, Alt+C, Alt+J after its F-04) and the red border of the fields with
  errors (UC06 Q6, F-03): the plan of UC07 says "Same layout" but does not ask for them, and `IU_OrderFollowUp` has
  neither. To confirm with Design whether they apply; no key is tested for the interval.
- Whether the success message says how many scheduling needs were created (D-040; the code only says "Order ORD-0003
  created.").
- The order of the list of orders (the client shows them in the order of C4).
- The maximum length of the indication and the justification; what happens if *Place order* is pressed twice quickly.
- The plan does not say how the patient is read; the code finds it with `searchPatients` by its identifier, as UC06.

**Not covered here:** the rows of *Usability decisions* that need a desktop (layout, colours seen by a person, focus,
Tab order) are left to the manual walk of the plan (*Verification*); the real services of C2, C3 and C4 (they have
their own tests in the server modules).
