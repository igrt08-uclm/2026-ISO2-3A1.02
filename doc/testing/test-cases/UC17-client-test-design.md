# Test design · UC17 – Client (C2 v1.0.0) · #39

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#39 and the answers Q1–Q10 of `doc/plans/UC17-client.md`.

Code under test (`client-c2-configuration`, branch `develop`): the screen `IU_ConfigureParameters` with its tabs
`TestTypePanel`, `ParameterGroupPanel` (protocols, appointment policies, priorities) and `SchedulePanel`, and the
button *Configure system* of `IU_Welcome` (`client-c1-access`).

The screen is built with `FakeSystemParameterService` (in memory, no window): it can be told to deny every call
(`denyAll()`, `AccessDeniedException`) or to reject the next change with a list of errors (`rejectNextChange`,
`ValidationException`), as the server does. The confirmation of a deactivation is answered by the test (yes or no).
The user acts through the buttons (`doClick()`), the text fields and the selection of the table.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| `IU_Welcome` (constructor) | `permissions`, `onConfigureSystem` | With `CONFIGURE_SYSTEM` and an action | `[CONFIGURE_SYSTEM]`, every permission |
| | | Without `CONFIGURE_SYSTEM` | `[MANAGE_ROLES]`, `[]` |
| | | Conjeture: no action | `null` action; the constructor of UC18 |
| every tab | selection of the table | No row / active row / inactive row | *New* / `CT-HEAD`, `P1`, `Room MR-1` / `XR-HAND`, a deactivated row |
| | answer of the server | Accepted / `AccessDeniedException` / `ValidationException` | normal fake / `denyAll()` at opening and while open / `rejectNextChange(…)` |
| `TestTypePanel.save` | duration text | Whole number | `"25"`, `" 25 "`, `"0"` (sent: the server validates it, Q6) |
| | | Not a whole number | `"abc"`, `"20.5"` |
| | | Conjeture | `""`, `"   "` |
| | priority chosen | Active priority / none | `HIGH` / index `-1` (sent as `""`) |
| `TestTypePanel` (priority list) | priority of the test type | Active / inactive but listed / not listed by the server | `HIGH` / `OLD` / `GONE` |
| `ParameterGroupPanel.save` | id typed with *New* | New id / id already in the table | `P1` (empty table), `P2`, `A1`, `LOW` / `P1`, `A1` |
| | | Conjeture | `" P1 "` (trimmed) |
| | `kind` (message) | Starts with a consonant / with a vowel | `protocol`, `priority` / `appointment policy` |
| `SchedulePanel.save` | start and end text | `HH:mm` valid | `08:00`, `" 15:00 "`, `09:30`, `21:00`, `20:00` |
| | | Not `HH:mm` | `8`, `08.30`, `15.00` |
| | | Hour out of range | `25:00`, `24:00` (Limit Val.) |
| | | Conjeture: empty | `""`, `"  "` (sent as no time) |
| | room typed with *New* | New / already has a schedule | `Room CT-1` / `Room MR-1` |
| `deactivate` (each tab) | answer to the confirmation | Yes / no | `true` / `false` |

Outputs: what is sent to the fake (`lastRequest`, `lastSaved`, `lastDeactivated`, `lastCall`, `changes`), the
status message (green "✔ …" or red "⚠ …"), the rows of the table with their state, and which controls are enabled.

## 2. Combination of the values
- **Each use** for every class of section 1: every value is used at least once.
- Save combines selection × answer of the server: (no row, accepted) TC17C-06, -15; (no row, rejected) TC17C-08,
  -18, -21; (row, accepted) TC17C-09, -15, -23; (row, denied) TC17C-25. Pairwise would also need (no row, denied)
  and (row, rejected); the code of the two catches is the same for both, so they add no coverage.
- Deactivate combines answer × answer of the server: (no) TC17C-11, -17; (yes, accepted) TC17C-10, -17, -23; (yes,
  denied) TC17C-25; (yes, rejected) TC17C-26. "No" never calls the server, so these are all the combinations.
- Maximum number of test cases: there is no maximum, because the text fields accept any `String`; the domain is
  divided into the classes of section 1.

## 3. Coverage (white box)

`TestTypePanel`, `ParameterGroupPanel` and `SchedulePanel` have the same structure, so one row covers the three when
the same decision is in each.

| Method | Decision (conditions) | Decision / condition coverage | MC/DC |
|---|---|---|---|
| `IU_Welcome` | `onConfigureSystem != null && hasPermission(CONFIGURE_SYSTEM)` | (T,T): TC17C-02; (T,F): TC17C-03 other permission; (F,-): TC17C-03 no action | both covered |
| `IU_ConfigureParameters` (tab listener) | selected tab `== testTypes` | T: TC17C-14 back to *Test types*; F: TC17C-14 to *Priorities* / *Protocols* | single |
| `addTab` | `denied` | T: TC17C-24 (the tabs added after the denial); F: TC17C-01 | single |
| `load`, `readPriorities` | `catch AccessDeniedException` | thrown: TC17C-24, TC17C-25 (tab shown again); not: TC17C-01 | — |
| `load` | `isActive ? active : inactive`; `row >= 0` | active/inactive: TC17C-01, -27; row found: TC17C-06, -10; not: opening, TC17C-26 | single |
| `showSelected` | `selected == null`; `selected != null && isRequiresContrast()` (and the other checks / days) | null: TC17C-05 (*New*); (T,T): `CT-HEAD` contrast, Tuesday; (T,F): `CT-HEAD` consent, Monday | covered |
| `TestTypePanel.showPriorities` | `id != null && !id.isBlank() && noneMatch(active ids)` | (F): TC17C-05 (*New*), -14 nothing chosen; (T,F): TC17C-06 without priority (row reselected with `""`); (T,T,F): TC17C-05; (T,T,T): TC17C-13 `OLD`, `GONE` | each condition against TC17C-13 |
| | `orElse` (listed by the server or not) | `OLD`: TC17C-13; `GONE`: TC17C-13 | — |
| `refreshPriorities` | `chosen == null` | T/F: TC17C-14 (both tests) | single |
| priority renderer | `value == null` | T/F: TC17C-05 | single |
| `updateControls` | `editable = !denied && (selected == null \|\| isActive)` | denied: TC17C-24, -25; no row: TC17C-05; active: TC17C-05; inactive: TC17C-12, -17, -23 | `!denied`: TC17C-25 vs -05 (same active row); `selected == null`: *New* vs TC17C-12; `isActive`: TC17C-05 vs -12 |
| | Deactivate `= !denied && selected != null && isActive` | (F): TC17C-24; (T,F): TC17C-05 *New*; (T,T,T): TC17C-05; (T,T,F): TC17C-12 | covered |
| `TestTypePanel.save` | `NumberFormatException`; `selected == null` (create/update); `priority == null` | TC17C-07 / TC17C-06; TC17C-06 / -09; TC17C-06 (both) | single conditions |
| `ParameterGroupPanel.save`, `SchedulePanel.save` | `selected == null && indexOf(id) >= 0` | (T,T): TC17C-16, -22; (T,F): TC17C-15, -19; (F,-): TC17C-15 edit, -23 edit | both covered |
| | `article`: first letter is a vowel | T: TC17C-16 policy; F: TC17C-16 protocol | single |
| `SchedulePanel.save` | `DateTimeParseException`; `parseTime`: `text.isBlank()`; day `isSelected()` | TC17C-20 / -19; TC17C-21 / -19; T/F: TC17C-19 | single |
| every `save` | `catch AccessDeniedException` / `catch ValidationException` | TC17C-25 / TC17C-08, -18, -21 | — |
| every `deactivate` | `selected == null \|\| !confirm` | (F,T) no: TC17C-11, -17; (F,F) yes: TC17C-10, -17, -23; **(T,-) not covered** | **`selected == null` not covered** |
| | `catch AccessDeniedException` / `catch ValidationException` | TC17C-25 / TC17C-26 (test types, protocols, schedules) | — |
| `indexOf` | `id != null && index < size`; `equals(id)` | id null: opening; loop ends without finding: TC17C-15, -18, -19 (generic and schedule tabs); found: TC17C-06, -16, -22 | `TestTypePanel`: **`index < size` false with an id not covered** |
| `InactiveGreyRenderer` | `!selected`; `inactive` | TC17C-27 (selected and not; active and inactive) | single |

**Not covered (to add).** The expected result of each one is to be given by Testing:
1. `deactivate()` with no row selected (`selected == null` true) in the three tabs. The button is disabled then, and
   `doClick()` on a disabled button does not run its action, so `deactivateWithoutSelectionAsksNothing` checks the
   disabled button but never runs this branch (it cannot be reached from the screen).
2. `SchedulePanel.deactivate()` answered **no**: every test of the schedule tab answers yes.
3. Deactivate in the tabs *Appointment policies* and *Priorities*: the `deactivate` methods of `policiesPanel()` and
   `prioritiesPanel()` in `IU_ConfigureParameters` never run (TC17C-25 and TC17C-26 only use *Test types*,
   *Protocols* and *Schedules*, although the display name of TC17C-25 says "in every tab").
4. `selectedEntry()`: `row >= entries.size()` is never true in the three tabs.
5. `reload()` with no row selected (`selectedId()` / `selectedResource()` return `null`), in the three tabs.
6. The selection listener when `getValueIsAdjusting()` is true (only happens while dragging with the mouse).
7. `TestTypePanel.indexOf()` with an id that is not in the table (the loop ends because `index < size` is false).
8. Limit values of the times next to `24:00`: `23:59` and `00:00` (the last and the first valid values).

## 4. Test cases

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC17C-01 | `IU_ConfigureParameters` | EC valid: open the screen | Five tabs in this order: Test types, Protocols, Schedules, Appointment policies, Priorities; the table of test types lists the entries of the server, with the state "active" or "inactive" | `showsTheFiveTabsAndTheTestTypes` |
| TC17C-02 | `IU_Welcome` | EC valid: welcome screen of a user with `CONFIGURE_SYSTEM` | The button *Configure system* is shown and runs its action | `userWithConfigureSystemSeesTheButton`, `administratorSeesTheFourButtonsInOrder`, `configureSystemRunsOnlyItsOwnAction` |
| TC17C-03 | `IU_Welcome` | EC invalid: a user without `CONFIGURE_SYSTEM`, or with no action given | No button *Configure system* | `userWithoutConfigureSystemDoesNotSeeTheButton`, `withoutTheActionNobodySeesTheButton`, `theConstructorOfUc18DoesNotShowTheButton` |
| TC17C-04 | `IU_ConfigureParameters` | EC valid: press *Back* | The action of *Back* runs | `backRunsItsAction` |
| TC17C-05 | `TestTypePanel` | EC valid: select a row (and *New*: no row) | The form shows its data, the priority by its name; the id cannot be edited | `selectingARowShowsItsData`, `withoutSelectionTheFormIsEmpty` |
| TC17C-06 | `TestTypePanel.save` | EC valid: *New*, fill the form, *Save* (also with no priority chosen) | The server receives the test type; message "✔ Test type X saved."; the new row is selected | `newTestTypeIsSentAndSelected`, `newTestTypeWithoutPriorityIsSentWithEmptyPriority` |
| TC17C-07 | `TestTypePanel.save` | EC invalid (Limit Val.): duration `abc`, `20.5` or empty (Conjeture: spaces) | "⚠ The duration must be a whole number of minutes."; the server is **not** called | `durationThatIsNotAWholeNumberIsRejected` |
| TC17C-08 | `TestTypePanel.save` | EC invalid: the server rejects the save with two errors | Both errors shown together, each ending with a full stop, in one message | `twoErrorsOfTheServerAreShownTogether` |
| TC17C-09 | `TestTypePanel.save` | EC valid: edit a selected test type and *Save* | The server receives an update with the same id | `editingSendsAnUpdateWithTheSameId` |
| TC17C-10 | `TestTypePanel.deactivate` | EC valid: *Deactivate* and answer **yes** | The server deactivates it; "✔ … deactivated."; the row says "inactive"; *Save* and *Deactivate* disabled | `deactivateAnsweredYes` |
| TC17C-11 | `TestTypePanel.deactivate` | EC invalid: *Deactivate* and answer **no** | Nothing is sent to the server | `deactivateAnsweredNo` |
| TC17C-12 | `TestTypePanel` | EC valid: select an inactive entry (`XR-HAND`) | It can be read, but *Save* and *Deactivate* are disabled | `inactiveTestTypeCanOnlyBeRead` |
| TC17C-13 | `TestTypePanel` | Conjeture: priorities `HIGH` active and `OLD` inactive used by a test type (and `GONE`, not listed) | The list offers the active ones, plus the inactive one only for the test type that has it | `prioritiesOfferedAreTheActiveOnesPlusTheOneOfTheTestType`, `unknownPriorityIsOfferedByItsId` |
| TC17C-14 | `TestTypePanel.refreshPriorities` | Conjeture: save a new priority in its tab, then go back to *Test types* | The new priority appears in the list of the test-type form | `prioritySavedInItsTabAppearsInTheTestTypeForm`, `goingBackWithNothingChosenKeepsThePriorityEmpty` |
| TC17C-15 | `ParameterGroupPanel.save` | EC valid: create a protocol, a policy and a priority (and edit a protocol) | Messages "✔ Protocol P1 saved.", "✔ Appointment policy A1 saved.", "✔ Priority LOW saved." | `creatingAProtocol`, `creatingAnAppointmentPolicy`, `creatingAPriority`, `editingAProtocolKeepsItsId` |
| TC17C-16 | `ParameterGroupPanel.save` | EC invalid: *New* with an id that is already in the table | "⚠ A protocol with the id … already exists. Select it in the table to edit it."; the server is not called | `newWithAnExistingIdIsRejected`, `duplicatedAppointmentPolicyUsesAn` |
| TC17C-17 | `ParameterGroupPanel.deactivate` | EC valid: deactivate a protocol (yes; also no, and no row) | Row "inactive", buttons disabled | `deactivatingAProtocol`, `deactivatingAProtocolAnsweredNo`, `deactivateWithoutSelectionAsksNothing` |
| TC17C-18 | `ParameterGroupPanel.save` | EC invalid: the server rejects the save of a protocol | The errors are shown in red | `errorsOfTheServerAreShownInRed` |
| TC17C-19 | `SchedulePanel.save` | EC valid: room, Monday and Friday, `08:00`–`15:00`, *Save* (and select a schedule) | The server receives those days and times; the table shows "08:00 - 15:00" | `scheduleIsSentAndListed`, `selectingAScheduleShowsItsData` |
| TC17C-20 | `SchedulePanel.save` | EC invalid (Limit Val.): times `8`, `25:00`, `08.30` (also in the end time, and `24:00`) | "⚠ The times must be written as HH:mm, for example 08:30."; the server is not called | `timesThatAreNotHhMmAreRejected` |
| TC17C-21 | `SchedulePanel.save` | Conjeture: empty times | The client sends no time and shows the error of the server | `emptyTimesAreSentAsNoTime` |
| TC17C-22 | `SchedulePanel.save` | EC invalid: *New* with a room that already has a schedule | "⚠ A schedule for … already exists. Select it in the table to edit it." | `newWithARoomThatAlreadyHasAScheduleIsRejected` |
| TC17C-23 | `SchedulePanel.deactivate`, `save` | EC valid: deactivate a schedule (yes); edit a schedule | Row "inactive"; the room cannot be changed when editing | `deactivatingASchedule`, `whenEditingTheRoomCannotBeChanged`, `deactivateWithoutSelectionAsksNothing` |
| TC17C-24 | `IU_ConfigureParameters` | EC invalid: open the screen when the server denies | "⚠ You do not have permission to configure the system."; *New*, *Save* and *Deactivate* disabled in every tab; *Back* enabled | `deniedAtOpeningDisablesEverythingButBack` |
| TC17C-25 | every tab | Conjeture: the permission is revoked while the screen is open, then *Save* (also *Deactivate*, and showing *Test types* again) | Same message and every button disabled except *Back* | `permissionRevokedWhileOpenDisablesEverythingOnSave`, `permissionRevokedIsDetectedOnSaveInEveryTab`, `permissionRevokedIsDetectedOnDeactivate`, `permissionRevokedIsDetectedWhenTheTestTypesTabIsShown` |
| TC17C-26 | every tab | Conjeture: the server rejects a deactivation (the entry no longer exists) | The error is shown and the table is read again | `rejectedDeactivationShowsTheErrorAndReadsTheTableAgain`, `reloadKeepsTheSelectedTestType`, `reloadKeepsTheSelectedEntry`, `reloadKeepsTheSelectedSchedule` |
| TC17C-27 | `InactiveGreyRenderer` | EC valid: inactive rows of the table | Drawn in grey **and** with the word "inactive" | `inactiveRowsAreGreyAndSayInactive`, `inactiveRowsAreGreyInTheOtherTabs` |

Test classes: TC17C-01, -04, -24..27 in `IU_ConfigureParametersTest`; TC17C-05..14 in `TestTypePanelTest`; TC17C-15..18
in `ParameterGroupPanelTest`; TC17C-19..23 in `SchedulePanelTest`; TC17C-02 and -03 in `IU_WelcomeConfigureSystemTest`
(`client-c1-access`); the three `reload…` tests are also named TC17C-26.

Postcondition of every case: the screen is only built, never shown; when the client rejects an input (duration,
times, duplicated id, answer no) the fake receives nothing (`changes` stays 0); after a denial every button but
*Back* stays disabled.
