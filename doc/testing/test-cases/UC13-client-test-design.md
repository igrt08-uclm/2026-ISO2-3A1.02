# Test design · UC13 – Client (C5 v1.0.0) · #130 – Manage the appointment schedule

Designed by Testing (Adam Gouram) following lesson T.04 (Testing Management): parameters of each input, equivalence
classes, boundary values and error guessing, combination of the values, and coverage of the code. Expected results
come from #129, #130, the analysis of UC13 (`doc/AN-C5-DECISIONS.md`, main and alternative flows;
`doc/analysis/UC13-manage-appointments-sequence.puml`), the presentation section of `doc/plans/C5-design.md`, the
section *Decisions* of `doc/plans/UC13-client.md` and the decisions of Design in PR #210 (Marcos, 23/11/2026: (a) and
(b), see section 5). Where the plan gives no text, the text is the one of the code that was approved with it
(`IU_ManageAppointments`, commit `fb67a1e`).

Code under test (`client-c5-appointments` 1.0.0-SNAPSHOT, branch `feature/UC13-client`, `87b56ce`): the screen
`IU_ManageAppointments` (its public methods `loadData`, `onFindSlots`, `onBookSlot`, `onRescheduleAppointment`,
`onCancelAppointment`, `showErrors`, `showAccessDenied`, `showConfirmation`, `isCancelled`, the actions *Find slots*,
*Book*, *Move*, *Cancel appointment*, *Back* and the selection of a row), and the button *Manage appointments* of
`IU_Welcome` (`client-c1-access` 1.2.7-SNAPSHOT). Tests: `IU_ManageAppointmentsTest` and
`IU_WelcomeManageAppointmentsTest`.

**Test data (precondition of every case).** The screen is built for the requester `lmartin` (admissions staff) and is
never shown. One hand-written fake, in memory, `FakeAppointmentService` (C5):
- Pending needs: NEED-0001 of ORD-0001, P-0001 Lucía Ejemplo Prueba, `XRAY-CHEST` Chest X-ray, `URGENT`, ordered on
  20/11/2026, due on 27/11/2026; NEED-0002 of ORD-0003, P-0002 Mario Demo Simulado, `MRI-KNEE` Knee MRI, `ROUTINE`,
  ordered on 21/11/2026, **without due date** (Conjeture).
- Free slots, all on 30/11/2026: `XRAY-CHEST` XRAY-1 09:00–09:15 and 09:15–09:30; `MRI-KNEE` MRI-1 10:00–10:45;
  `CT-HEAD` CT-1 12:00–12:30. No slot on any other day.
- Appointments: APT-0001 P-0003 `XRAY-CHEST` XRAY-1 30/11/2026 08:00 BOOKED; APT-0002 P-0002 `MRI-KNEE` 11:00
  CANCELLED; APT-0003 P-0004 `CT-HEAD` CT-1 12:30 BOOKED. New appointments get APT-0004, APT-0005…
- It does not validate (so it shows that nothing is sent when the screen rejects a selection), records every call with
  its arguments and requester, removes a booked need from the pending ones, and can deny every call or only the reading
  of the appointments (`AccessDeniedException`), reject the next booking, move or cancellation, or every request of
  slots (`ValidationException`), and fail the reading of the needs (`ValidationException` or `RuntimeException`).

Every question of the screen (booking, cancelling) is answered by the test (Yes or No), which also records it.

## 1. Parameters and testing values

| Input | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| Opening (constructor, `loadData`) | answer of C5 | Allows, with data | 2 needs, 3 appointments |
| | | Denies the needs / only the appointments | `denyAll()`; `denyAppointments()` |
| | | Rejects (`ValidationException`) | "The orders of C4 could not be read" |
| | | Fails (Conjeture) | `IllegalStateException("The server cannot be reached")` |
| Need shown (Conjeture) | due date | Present / `null` | 27/11/2026 / empty cell |
| Day (text field) | value | Default | today (relative check: the day before or after the call) |
| | | Valid `dd/MM/yyyy` | `30/11/2026`; `"  30/11/2026 "` (trimmed, Conjeture) |
| | | Kept on reload | `01/12/2026` |
| | | Invalid (Conjeture) | `""`, `"   "`, `2026-11-30`, `32/11/2026`, `30/13/2026`, `1/12/2026` (one digit), `abc` |
| *Find slots* | selection | Need selected / none | Lucía, Mario / nothing |
| | answer of C5 | 2 slots / 1 slot (Limit Val.: singular) / 0 slots (Limit Val.) | Lucía 30/11; Mario 30/11; Lucía 01/12 |
| | | `ValidationException` (2 errors) / `AccessDeniedException` | "The test type XRAY-CHEST has no room", "The day 30/11/2026 is closed" / `denyAll()` |
| *Book* | selection | Need and slot / need only / slot only / none | Lucía + 09:15; Lucía; slot with the need cleared; — |
| | answer to the question | Yes / No | `true` / `false` |
| | answer of C5 | Ok / 1 error (Limit Val.) / 2 errors / denied | APT-0004; "The slot is no longer free"; "The patient P-0001 is not active", "The slot is in the past"; `denyAll()` |
| *Move* | selection | Appointment and slot (with a need also selected) / slot only / appointment only | APT-0001 + 09:00 |
| | answer of C5 | Ok / 1 error / denied | RESCHEDULED; "The slot is outside the schedule of the room"; `denyAll()` |
| *Cancel appointment* | selection | Appointment / none (a need selected) | APT-0003, APT-0001 / — |
| | answer to the question | Yes / No | `true` / `false` |
| | answer of C5 | Ok / 1 error / denied | CANCELLED; "The appointment APT-0001 is already cancelled"; `denyAll()` |
| `isCancelled` | status | BOOKED / RESCHEDULED / CANCELLED | each value of `AppointmentStatus` |
| `IU_Welcome` | permissions | With `MANAGE_APPOINTMENTS` / without (administrator, none) | — |
| | action | Given / `null` / older constructor (UC12) | — |

Outputs: the rows of the three tables, the text and colour of the message area (green for success, red with `⚠` for
errors), the calls sent to C5 and their arguments, the questions asked, the enabled state of the buttons and tables,
and whether *Back* runs its action.

## 2. Combination of the values
The actions only matter after the screen is open with data, so the suite is built with **each use**: every testing
value of section 1 appears in at least one case, and the rest takes the usual value (Lucía, 30/11/2026, the slot of
09:00 or 09:15, APT-0001). The pairs that the code combines in one decision are also covered **pairwise**: need ×
slot for *Book* (TC13C-09, 11), appointment × slot for *Move* (TC13C-20, 21), selection × answer to the question
(TC13C-09, 10, 24, 25, 26). Maximum number of test cases: the day is free text with no maximum, so the domain is divided
into the classes of section 1; with them the full product of the actions would be 3 day classes × 4 selections × 2
answers × 4 answers of C5 = 96 cases per action, which each use plus the pairs above reduce to the 32 cases of the
screen and the 5 of `IU_Welcome` (TC13C-33 to 37).

## 3. Coverage (white box)
Decisions of `IU_ManageAppointments` and the cases that make each condition true (T) and false (F):

| Decision | Condition | T | F |
|---|---|---|---|
| `loadData`: `txtDate.getText().isEmpty()` | — | TC13C-01, 02 (opening) | TC13C-02 (reload) |
| `loadData`: `getDueDate() == null` | — | TC13C-01 (Mario) | TC13C-01 (Lucía) |
| `loadData`: `catch` | `AccessDeniedException` | TC13C-15, 16 | — |
| | `ValidationException` | TC13C-18 | — |
| | `RuntimeException` | TC13C-19 | — |
| `onFindSlots`: `need == null` | — | TC13C-06 | TC13C-03 |
| `onFindSlots`: day not parsed | — | TC13C-07 | TC13C-03, 04 |
| `onFindSlots`: `slots.isEmpty()` | — | TC13C-05 | TC13C-03 |
| `onFindSlots`: `slots.size() == 1` | — | TC13C-04 | TC13C-03 |
| `onFindSlots`: `catch` | `AccessDeniedException` / `ValidationException` | TC13C-17 / TC13C-08 | — |
| `onBookSlot()`: `need == null \|\| slot == null` | need | TC13C-11 (none, slot only) | TC13C-09 |
| | slot | TC13C-11 (need only) | TC13C-09 |
| `onBookSlot()`: `!confirm` | — | TC13C-10 | TC13C-09 |
| `onBookSlot(String, TimeSlot)`: `catch` | denied / validation | TC13C-14 / TC13C-12, 13 | TC13C-09 |
| `onReschedule()`: `appointment == null \|\| slot == null` | appointment | TC13C-21 (slot only) | TC13C-20 |
| | slot | TC13C-21 (appointment only) | TC13C-20 |
| `onRescheduleAppointment`: `catch` | denied / validation | TC13C-23 / TC13C-22 | TC13C-20 |
| `onCancel()`: `appointment == null` | — | TC13C-26 | TC13C-24 |
| `onCancel()`: `!confirm` | — | TC13C-25 | TC13C-24 |
| `onCancelAppointment`: `catch` | denied / validation | TC13C-27 | TC13C-24 |
| `showErrors`: `size() == 1` | — | TC13C-12 | TC13C-13, 31 |
| `selectedNeed`, `selectedSlot`, `selectedAppointment`: `row < 0 \|\| row >= size()` | `row < 0` | TC13C-06, 11, 21, 26 | TC13C-09, 20, 24 |
| | `row >= size()` | — (see below) | TC13C-09, 20, 24 |
| `isCancelled`: `== CANCELLED` | — | TC13C-32 | TC13C-32 |

`IU_Welcome`: `onManageAppointments != null && hasPermission(MANAGE_APPOINTMENTS)`: (T,T) TC13C-33, (F,T) TC13C-36,
(T,F) TC13C-35; the older constructor passes `null` (TC13C-37).

Result of JaCoCo for `client-c5-appointments` (`mvn -B verify`, `87b56ce` with these tests): 100 % of the lines (218 of
218) and 93.5 % of the branches (43 of 46), above the 80 % / 70 % of D-033. Not covered: `row >= size()` true in the
three `selected…` methods (the tables and the lists are always filled together, so a selected row is always inside the
list).

## 4. Test cases

| ID | Method/action | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC13C-01 | constructor | Open the screen | Needs `NEED-0001 · Lucía Ejemplo Prueba · Chest X-ray · URGENT` with the due date `27/11/2026`, and `NEED-0002 · Mario Demo Simulado · Knee MRI · ROUTINE` with an empty due date; appointments *Appointment*, *Patient*, *Test type*, *Start*, *Status*: `APT-0001 · P-0003 · XRAY-CHEST · 30/11/2026 08:00 · BOOKED`, `APT-0002 … CANCELLED`, `APT-0003 … 12:30 · BOOKED`; slots *Room*, *Start*, *End* empty; calls `needs`, `appointments` by `lmartin`; empty message; the five buttons enabled (#130, step 2 of the analysis) | `tc13c01OpeningListsTheNeedsAndTheAppointments` |
| TC13C-02 | constructor, `loadData` | Open; then the day `01/12/2026` and a reload | The day is today on opening (relative check, F-05); the written day is kept; the data are read again | `tc13c02DayStartsAsTodayAndIsKept` |
| TC13C-03 | *Find slots* | Lucía, `30/11/2026` | One request `XRAY-CHEST` from 30/11 to 30/11 by `lmartin`; slots `XRAY-1 · 30/11/2026 09:00 · 30/11/2026 09:15` and `… 09:15 · … 09:30`; "2 free slots."; nothing changed (step 4) | `tc13c03FindSlotsOfTheSelectedNeed` |
| TC13C-04 | *Find slots* | Mario, `"  30/11/2026 "` (Limit Val.: 1 slot; Conjeture: spaces) | Request `MRI-KNEE`; one slot `MRI-1 · 10:00 · 10:45`; "1 free slot." | `tc13c04OneFreeSlotAndTrimmedDay` |
| TC13C-05 | *Find slots* | Lucía, `30/11/2026` and then `01/12/2026` (Limit Val.: 0 slots) | Slots emptied; "⚠ There are no free slots on that day for that test type." in red | `tc13c05DayWithoutFreeSlots` |
| TC13C-06 | *Find slots* | No need selected | "⚠ Select a pending need before looking for slots." in red; no request of slots | `tc13c06FindSlotsWithoutNeed` |
| TC13C-07 | *Find slots* | Lucía; each invalid day of section 1 (Conjeture) | "⚠ The day must be written as dd/MM/yyyy" in red each time; no request of slots | `tc13c07InvalidDay` |
| TC13C-08 | *Find slots* | C5 rejects the slots with 2 errors | "⚠ The test type XRAY-CHEST has no room The day 30/11/2026 is closed" in red; no slot | `tc13c08ServerRejectsTheSlots` |
| TC13C-09 | *Book* | Lucía, slot of 09:15; answer Yes | Question "Book the patient Lucía Ejemplo Prueba on 30/11/2026 09:15?"; one booking by `lmartin` of NEED-0001 in XRAY-1 09:15–09:30; needs and appointments read again: only NEED-0002 pending, `APT-0004 · P-0001 · XRAY-CHEST · 30/11/2026 09:15 · BOOKED` listed, no need selected (steps 5–7; the message: F-01) | `tc13c09BookTheSelectedSlot` |
| TC13C-10 | *Book* | Mario, slot of 10:00; answer No | The question asked; nothing sent; lists unchanged (plan: *Confirmations*) | `tc13c10BookAnsweredNo` |
| TC13C-11 | *Book* | Nothing selected; need without slot; slot with the need cleared | "⚠ Select a pending need and one of its free slots." in red each time; no question; nothing sent | `tc13c11BookWithoutNeedOrSlot` |
| TC13C-12 | *Book* | C5 rejects with 1 error (Limit Val.) | "⚠ The slot is no longer free" in red; no reload; lists unchanged (6b) | `tc13c12ServerRejectsTheBookingWithOneError` |
| TC13C-13 | *Book* | C5 rejects with 2 errors | "⚠ The patient P-0001 is not active The slot is in the past" (plan: *Messages*, joined) | `tc13c13ServerRejectsTheBookingWithTwoErrors` |
| TC13C-14 | *Book* | C5 denies | "⚠ You do not have permission to manage appointments." in red; only *Back* enabled; the three tables disabled (6a) | `tc13c14DeniedWhileBooking` |
| TC13C-15 | constructor, *Back* | C5 denies on opening | The denied message in red; no rows; only *Back* enabled; tables disabled; only the needs asked; *Back* runs its action without a question (2a) | `tc13c15DeniedOnOpening` |
| TC13C-16 | constructor | Only the reading of the appointments is denied | The denied message; only *Back* enabled; no appointment | `tc13c16DeniedWhileReadingTheAppointments` |
| TC13C-17 | *Find slots* | C5 denies after opening | The denied message; only *Back* enabled; no slot | `tc13c17DeniedWhileFindingSlots` |
| TC13C-18 | constructor | The reading of the needs is rejected | "⚠ The orders of C4 could not be read" in red; no need; the actions stay enabled | `tc13c18ValidationErrorWhileReading` |
| TC13C-19 | constructor | The reading fails (Conjeture) | The message ends with "The data could not be read: The server cannot be reached"; no rows (the colour is F-04) | `tc13c19FailureWhileReading` |
| TC13C-20 | *Move* | Lucía selected, slot of 09:00, APT-0001 | One move of APT-0001 to XRAY-1 09:00 by `lmartin`; no question; no booking although a need is selected (decision (a)); reload shows `APT-0001 … 30/11/2026 09:00 · RESCHEDULED` (steps 8–10) | `tc13c20MoveTheSelectedAppointment` |
| TC13C-21 | *Move* | Slot without appointment; appointment without slot | "⚠ Select an appointment and one of the free slots." in red; nothing sent | `tc13c21MoveWithoutAppointmentOrSlot` |
| TC13C-22 | *Move* | C5 rejects | "⚠ The slot is outside the schedule of the room" in red; APT-0001 still BOOKED (9b) | `tc13c22ServerRejectsTheMove` |
| TC13C-23 | *Move* | C5 denies | The denied message; only *Back* enabled; tables disabled (9a) | `tc13c23DeniedWhileMoving` |
| TC13C-24 | *Cancel appointment* | APT-0003; answer Yes | Question "Cancel the appointment APT-0003?"; one cancellation by `lmartin`; reload shows APT-0003 CANCELLED (steps 11–13, D-022: listed, not deleted) | `tc13c24CancelAnsweredYes` |
| TC13C-25 | *Cancel appointment* | APT-0001; answer No | The question asked; nothing sent; still BOOKED | `tc13c25CancelAnsweredNo` |
| TC13C-26 | *Cancel appointment* | Only a need selected | "⚠ Select the appointment to cancel." in red; no question; nothing sent | `tc13c26CancelWithoutAppointment` |
| TC13C-27 | *Cancel appointment*, `onCancelAppointment` | C5 rejects; then C5 denies | "⚠ The appointment APT-0001 is already cancelled"; then the denied message and only *Back* enabled (12a) | `tc13c27CancelRejectedAndDenied` |
| TC13C-28 | tables | Lucía and her slots shown | No cell of the three tables is editable; each table selects a single row | `tc13c28CellsAreNotEditable` |
| TC13C-29 | buttons | Open the screen | Buttons *Find slots* Alt+F, *Book* Alt+B, *Move*, *Cancel appointment*, *Back* Alt+K (N-02) | `tc13c29ButtonsAndMnemonics` |
| TC13C-30 | *Back* | Lucía and a slot selected | Runs its action once; no question; no call to C5 | `tc13c30Back` |
| TC13C-31 | `showConfirmation`, `showErrors` | A confirmation; two errors | The confirmation in green; "⚠ First error Second error" in red (plan: *Messages*) | `tc13c31Callbacks` |
| TC13C-32 | `isCancelled` | BOOKED, RESCHEDULED, CANCELLED | `false`, `false`, `true` | `tc13c32IsCancelled` |
| TC13C-33 | `IU_Welcome` | Admissions staff with `MANAGE_PATIENTS`, `MANAGE_APPOINTMENTS` and the action | Buttons *Manage patients*, *Manage appointments*, *Log out*; Alt+A; the click runs the action once (plan: *Files*) | `tc13c33UserWithManageAppointmentsSeesTheButton` |
| TC13C-34 | `IU_Welcome` | Every permission and action | *View audit log*, *Manage roles*, *Configure system*, *Configure integrations*, *Manage patients*, *Manage appointments*, *Log out*; the button runs only its own action | `tc13c34OrderWithTheOtherButtons`, `tc13c34ManageAppointmentsRunsOnlyItsOwnAction` |
| TC13C-35 | `IU_Welcome` | Administrator without `MANAGE_APPOINTMENTS`; a user with no permission | No *Manage appointments* | `tc13c35UserWithoutManageAppointmentsDoesNotSeeTheButton` |
| TC13C-36 | `IU_Welcome` | `MANAGE_APPOINTMENTS` but `null` action | Only *Log out* | `tc13c36WithoutTheActionNobodySeesTheButton` |
| TC13C-37 | `IU_Welcome` | The constructor of UC12 with every permission | The buttons of UC12, no *Manage appointments* | `tc13c37OlderConstructorDoesNotShowTheButton` |

Postcondition of every case: nothing is stored except by `bookAppointment`, `rescheduleAppointment` and
`cancelAppointment` of the fake (TC13C-09, 20, 24); after a rejected or denied change, or an error of the screen,
nothing is changed in C5; the screen is never shown.

## 5. Not covered / Not defined

**Defects found** in the review of `feature/UC13-client` (`87b56ce`). No test of this branch fails because of them:
the cases that check the expected behaviour are kept outside the branch until Implementation fixes the code (they
fail now for these reasons only).
- **F-01 – The confirmation of a booking, a move or a cancellation is erased at once.** `onBookSlot`,
  `onRescheduleAppointment` and `onCancelAppointment` call `showConfirmation(...)` and then `loadData()`, which ends
  with `lblStatus.setText(" ")`, so the message area is empty after every success. The plan (*Messages*: "the success
  messages say the id of the appointment and its new moment") and steps 7, 10 and 13 of the analysis expect the
  confirmation. Expected: "Appointment APT-0004 booked on 30/11/2026 09:15.", "Appointment APT-0001 moved to
  30/11/2026 09:00.", "Appointment APT-0003 cancelled.", in green.
- **F-02 – Decision (a) of Design (PR #210) is not implemented.** `onFindSlots` always uses the selected need: with
  only an appointment selected it answers "Select a pending need before looking for slots.", and with a need and an
  appointment selected it asks for the test type of the need. Expected: with an appointment selected, *Find slots*
  asks C5 for the free slots of its `testTypeId`; with no appointment selected, the screen uses the selected need;
  *Move* needs an appointment and a slot and never a need (the last part already holds: TC13C-20, 21).
- **F-03 – Decision (b) of Design (PR #210) is not implemented.** The needs table has the columns *Need*, *Patient*,
  *Test type*, *Priority*, *Due*: the order date of `PendingNeedView.getOrderDate()` is not shown (step 2 of the
  analysis and Q7 of `doc/plans/C5-design.md` also ask for it). Expected: two date columns, the order date and then
  the due date.
- **F-04 – A failure of the reading is shown as a success.** The `catch (RuntimeException)` of `loadData` calls
  `showConfirmation`, so "The data could not be read: …" is green and without `⚠`. Expected (pending fix): the error
  style, in red. TC13C-19 checks only the text.
- **F-05 – The default day cannot be fixed in a test.** `loadData` writes `LocalDate.now()`; the pending fix is an
  injectable `Clock`. Its constructor is not decided yet, so no case checks it: TC13C-02 accepts the day before or
  after the call.
- **F-06 – The design lists `+onSelectNeed(needId: String): void`** (section 4 of `doc/plans/C5-design.md`, message 6
  of the analysis) and the screen has no such public method: the selection is the row of the table and the slots are
  asked with *Find slots*. Design decides whether the method is dropped from the design or added to the code.

**Not defined** (the plan does not say; Testing asks the team):
- **N-01 – Failures of the server outside the reading.** Only `loadData` catches `RuntimeException`; a failure of
  `getFreeSlots`, `bookAppointment`, `rescheduleAppointment` or `cancelAppointment` escapes from the button and
  nothing is shown. UC06 shows "… could not be …" in that case. Not tested.
- **N-02 – Keys.** The plan gives no Alt keys; TC13C-29 checks those of the code (Alt+F, Alt+B, Alt+K; Alt+A in
  `IU_Welcome`). *Move* and *Cancel appointment* have none, and the field *Day* has no key.
- **N-03 – Slots after a change.** After a booking or a move the slots table is not read again, so the slot just
  taken can be selected again (the server would reject it).
- **N-04 – Slots of another need.** After *Find slots* for one need, selecting another need keeps the slots of the
  first one, and *Book* sends the new need with a slot of another test type (the server would reject it). Related to
  decision (a).
- **N-05 – Content of the appointments table.** It shows the patient id and the test type id (not the names), and it
  lists the cancelled appointments (the rule "cancelled appointments are never shown" is of UC14, 2c).
- **N-06 – Lenient day.** `DateTimeFormatter.ofPattern("dd/MM/yyyy")` uses the SMART resolver, so `31/02/2026` is
  accepted as 28/02/2026. Not tested.
- **N-07 – Denied access.** The field *Day* stays enabled after a denied access; the plan says "disables every
  action", and the field is not an action.
- **Not covered:** the usability walk in the running application (plan, *Verification*: this machine has no desktop),
  and `row >= size()` of the `selected…` methods (section 3).
