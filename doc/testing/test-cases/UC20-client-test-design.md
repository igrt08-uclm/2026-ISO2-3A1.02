# Test design · UC20 – Client (C2 v1.1.0, patch 1.1.2) · #42 – Manage the integrations with the hospital systems

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#42, the answers Q1–Q15 of `doc/plans/UC20-client.md`, R-05 of #122 (patch C2 1.1.2, #124, #139) and batch 2 of
`doc/plans/UC12-testing-fixes.md` (F-17, F-18, F-19).

Code under test (`client-c2-configuration` 1.1.2, `origin/develop` 25b0b41): the screen `IU_ManageIntegrations` (its
constructor, the public methods `displayIntegrations`, `showOperationResultSuccess`, `showOperationResultError`,
`showTestResult`, and the actions of its buttons and rows: *Save* → `onSaveConfiguration`, *Test connection* →
`onTestConnection`, *Revert*, *Back*, pressing a row → `changeRow`), and the button *Configure integrations* of
`IU_Welcome` (`client-c1-access`).

**Test data (precondition of every case).** The screen is built with a fake `IIntegrationManagementService` (in
memory, no window) with the four integrations in the order of the server: SSO (simulated, enabled, `timeout=30`, no
endpoint), EHR (simulated, disabled), Admissions (simulated, enabled, `http://admissions.hospital.test`, `timeout=15`
and `retries=2`) and RIS/PACS (real, enabled, `https://pacs.hospital.test`). The fake can deny every call
(`AccessDeniedException`), reject the next save with a list of errors (`ValidationException`) and answers the
connection test as the server does (3 ms unless a case says otherwise). Every confirmation is answered by the test
(accept or cancel), which also records the question. The screen opens with the first row (SSO) selected.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| constructor (`load`) | answer of `getIntegrations` | The four integrations | the test data |
| | | Other order, status `ERROR` | RIS/PACS (`ERROR`), SSO |
| | | No integrations | empty list (Limit Val.) |
| | | Denied | `AccessDeniedException` |
| | | Conjeture | SSO with a `null` endpoint |
| *Save* (`onSaveConfiguration`) | endpoint (text field) and mode (radio buttons) | Empty, simulated mode | `""`; `"   "` |
| | | Empty, real mode | `""` |
| | | `http://` or `https://` (any case) followed by a host | `http://sso.hospital.test`, `https://sso.hospital.test:8443/api`, `HTTP://sso.hospital.test`, `Https://sso.hospital.test` |
| | | Right scheme, no host | `http://` (Limit Val.), `https://` (Limit Val.), `HTTPS://`, `http://:8443`, `http:///path`, `http://user@` |
| | | Wrong scheme | `ftp://sso.hospital.test`, `sso.hospital.test`, `http:/sso.hospital.test` (Limit Val.), `https:sso.hospital.test` (Limit Val.), `httpx://sso` |
| | | Conjeture | `"   https://sso.hospital.test  "` (spaces around); `ftp://…` in simulated mode |
| | settings (text area, one `key=value` per line) | Valid lines | `timeout=45`, `timeout=45\nrealm=hospital` |
| | | Blank lines, spaces, `=` inside the value | `"  zeta = 1 \n\n   \nalpha=2\n url = http://x.test/?a=b \n"` |
| | | No settings | `""`, `"\n\n"`, `"   "` (Limit Val.) |
| | | Line without `=` | `retries 2` (line 2), `bad`, `timeout` |
| | | Empty key | `=5`, `"  = 5"`, `=` (Limit Val.) |
| | | Empty value | `timeout=`, `"timeout=   "` (Limit Val.) |
| | | Repeated key | `timeout=30` and `" timeout = 40"` |
| | *Enabled* and mode against the saved integration | Disabling | SSO enabled → disabled |
| | | Switching to real | admissions simulated → real |
| | | Both at once | SSO enabled simulated → disabled real |
| | | No delicate change | only settings; enabling EHR; RIS/PACS real → real (endpoint), real → simulated |
| | answer to the confirmation | Accept / cancel | `true`, `false` |
| | answer of `configureIntegration` | Saved | the fake stores it |
| | | Rejected | `ValidationException` with 1 or 2 errors |
| | | Denied | `AccessDeniedException` (permission revoked while open) |
| | | Conjeture | every error of the screen at once (bad endpoint, line without `=`, repeated key, empty key on line 5); missing endpoint and a bad line |
| *Test connection* (`onTestConnection`, `showTestResult`) | answer of `testConnection` for the selected row | Connection OK | 3 ms; 0 ms (Limit Val.) |
| | | Disabled | EHR |
| | | Real mode | RIS/PACS |
| | | Not configured | admissions marked as not configured |
| | | Denied | permission revoked while open |
| | | Conjeture | test after saving a change; test with no selected row (`showTestResult` called directly) |
| row selection (`changeRow`) | state of the form | Clean | press other rows, or the same row again |
| | | Unsaved changes | settings changed |
| | answer to "Discard the unsaved changes?" | Accept / cancel | `true`, `false` |
| | | Conjeture | rows pressed after a denial |
| unsaved changes (`isDirty`, `updateControls`) | field changed | Each field alone | endpoint, settings, *Enabled*, mode |
| | | Back to the saved values | undo by hand; only spaces added; *Enabled* clicked twice |
| *Revert* | state | Unsaved changes | every field changed |
| | | After an error message | error of the screen; error of the server |
| *Back* | (none) | — | one click |
| `IU_Welcome` | `CONFIGURE_INTEGRATIONS` and action | Permission and action | user with the permission; administrator with every permission |
| | | Permission, no action | `null` action |
| | | No permission | other permissions; none |
| | | Older constructors | 5 and 6 arguments |

Outputs: the rows of the table (System, Mode, Enabled, Status), the form of the selected row, the message area
(green "✔ …" or red "⚠ …", or empty), the lines "SIMULATED: no real system is contacted" and "Unsaved changes: save or
revert to test the connection", the enabled state of the controls, the questions asked, and the requests sent to the
service (none when the screen finds an error or the user cancels). No exception leaves the screen: `AccessDeniedException`
and `ValidationException` are shown as messages.

## 2. Combination of the values

- **Maximum number of test cases.** There is no maximum for the endpoint and the settings (any text), so they are
  divided into the classes of section 1. With those classes, all the combinations of *Save* would be 4 endpoint
  classes (empty, with host, no host, wrong scheme) × 2 modes × 6 settings classes × 2 new *Enabled* × 2 saved
  *Enabled* × 2 saved modes × 2 answers to the question × 3 answers of the server = **2 304** cases; *Test
  connection* 6 answers (including no selection); pressing a row 2 states × 2 answers + denied = **5**; the welcome
  button 2 × 2 + 1 older constructor class = **5**.
- **All combinations** are used where two parameters decide the result together:
  - endpoint × mode: each endpoint class is tried in real mode (TC20C-16 to TC20C-18) and the empty and wrong-scheme
    classes also in simulated mode (TC20C-15, TC20C-19).
  - The confirmation depends on 4 booleans (saved and new *Enabled*, saved and new mode). Each use of the 4 outcomes
    (disable, real, both, no question) with 7 of the 16 combinations: enabled → disabled simulated → simulated
    (TC20C-22, TC20C-27), enabled → enabled simulated → real (TC20C-23), enabled → disabled simulated → real
    (TC20C-24), and enabled → enabled simulated → simulated, disabled → enabled simulated → simulated, enabled →
    enabled real → real, enabled → enabled real → simulated (TC20C-25).
  - unsaved changes × answer to "Discard the unsaved changes?": with changes and accept (TC20C-36), with changes and
    cancel (TC20C-37), without changes (TC20C-38, no question, so the answer does not matter); after a denial no
    question at all (TC20C-43).
- **Each use** for the rest: every invalid class of the settings once with the other fields valid (TC20C-11 to
  TC20C-13), every answer of the connection test once (TC20C-29 to TC20C-32), every field alone for the unsaved changes
  (TC20C-34). TC20C-20 combines several invalid classes on purpose (error guessing: every error together, in order).
- **Pairwise** is not used: the fields that do not decide the result together (for example the settings and the
  mode) are covered by each use.

## 3. Coverage (white box)

Java evaluates `&&` and `||` in short circuit, so a condition that is not evaluated is written "–". A pair of cases
shows MC/DC for a condition when that condition changes, the outcome changes, and every other condition that is
evaluated in both cases keeps its value. Measured with the JaCoCo report of `mvn -B verify` on `origin/develop`
(25b0b41): `IU_ManageIntegrations` 292/296 lines and 166/177 branches. `IU_Welcome` is in `client-c1-access`; its
decision is analysed from the tests only.

**Constructor and `load`**
- `catch (AccessDeniedException)` of `getIntegrations`: TC20C-39.
- `row < 0 && !entries.isEmpty()` (A, B): (T, T) → T on opening (TC20C-01); (T, F) → F with no integrations (TC20C-01
  `noIntegrations`); (F, –) → F after *Save*, which selects the saved row (TC20C-27). MC/DC reached.
- `row >= 0`: true TC20C-01; false TC20C-01 `noIntegrations`.
- Row listener `!getValueIsAdjusting() && !loading && !restoring && !denied`: `loading` true during the reload after
  *Save* (TC20C-27); `restoring` true when the selection goes back (TC20C-37); `denied` true (TC20C-43); all four
  true when a row is pressed (TC20C-04). The condition `getValueIsAdjusting()` true is **not covered** (see below).

**`displayIntegrations`**: `isEnabled() ? "Yes" : "No"` both in TC20C-01; `systemName` 4 branches (TC20C-01);
`statusName` 3 branches, "Error" in TC20C-01 `rowsFollowTheOrderOfTheServer`.

**`SimulatedRenderer`**: `column == MODE_COLUMN && "SIMULATED".equals(value)`: (T, T) → T EHR mode, (T, F) → F RIS/PACS
"REAL", (F, –) → F the name column (TC20C-02). `!selected`: true for the other rows, false for the selected SSO row
(TC20C-02). MC/DC reached.

**`showSelected`, `updateSimulatedLine`, `updateControls`**
- `selected == null ? "" : …` and `selected != null && mode == REAL`: `null` with no integrations (TC20C-01); REAL with
  RIS/PACS and SIMULATED with SSO (TC20C-04).
- `selectedEntry() != null && rbSimulated.isSelected()`: (T, T) SSO, (T, F) RIS/PACS (TC20C-03); (F, –) no integrations
  (TC20C-01) and denied (TC20C-39).
- `editable = !denied && selectedEntry() != null`: (T, T) TC20C-04; (F, –) TC20C-39; (T, F) TC20C-01 `noIntegrations`.
- `changed = editable && isDirty(…)`: (T, T) TC20C-34; (T, F) TC20C-04; (F, –) TC20C-39.

**`isDirty`** (4 conditions joined by `||`: endpoint, settings, *Enabled*, mode differ). Each condition alone true
after *Revert* (TC20C-34 `anyChangeIsUnsaved`), all false (TC20C-04, TC20C-34 `undoingByHandIsClean`): MC/DC reached.
`saved.getEndpoint() == null ? "" : …`: true TC20C-42, false TC20C-04.

**`changeRow`**: `newRow != shownRow && shownRow >= 0 && shownRow < entries.size() && isDirty(…) && !confirm.test(…)`.
Outcome true (go back) TC20C-37; false with the question accepted TC20C-36 and with no changes TC20C-38. `isDirty`
true/false and the answer true/false are covered, so the decision is covered; the first three conditions are never
false (**not covered**, see below), so condition coverage and MC/DC are not reached for them.

**`onSaveConfiguration`**
- `selected == null`: never true (**not covered**).
- `rbReal.isSelected() ? REAL : SIMULATED`: true TC20C-16, false TC20C-15.
- `endpoint.isEmpty() && mode == REAL` (A, B): (T, T) → T TC20C-16; (T, F) → F TC20C-15; (F, –) → F TC20C-17. MC/DC:
  A by TC20C-16 / TC20C-17; B by TC20C-16 / TC20C-15.
- `else if (!endpoint.isEmpty())`: true TC20C-17, false TC20C-15.
- `!lower.startsWith("http://") && !lower.startsWith("https://")` (A, B): (T, T) → T TC20C-18, TC20C-19; (F, –) → F
  TC20C-17 `http://sso.hospital.test`, `HTTP://…`; (T, F) → F TC20C-17 `https://…:8443/api`, `Https://…`. MC/DC
  reached.
- `lower.startsWith("http://") ? 7 : 8`: both in TC20C-17.
- `!hasHost(…)`: true TC20C-17 (no host), false TC20C-17 (with host).
- `!errors.isEmpty()`: true TC20C-11 to TC20C-20; false TC20C-15.
- `question != null && !confirm.test(question)` (A, B): (T, T) → T TC20C-26; (T, F) → F TC20C-22; (F, –) → F TC20C-25.
  MC/DC reached.
- `catch (AccessDeniedException)` TC20C-40; `catch (ValidationException)` TC20C-28.

**`hasHost`** (same rule as the server)
- Loop `index >= 0 && index < end` (A, B): (T, T) for `/` in `http:///path` and `https://sso.hospital.test:8443/api`;
  (F, –) for `?` and `#` in every value. (T, F) **not covered**.
- `colon >= 0 ? …`: true `http://:8443`, `…:8443/api`; false `http://sso.hospital.test` (TC20C-17).
- `!(…).isBlank()`: both in TC20C-17.

**`readSettings`**
- Loop: 0 lines with text (TC20C-14), 1 line (TC20C-12), several (TC20C-10).
- `line.isEmpty()` (blank line skipped): true TC20C-10, TC20C-14; false TC20C-10.
- `separator < 0 ? "" : …`: true TC20C-11; false TC20C-10.
- `key.isEmpty() || value.isEmpty()` (A, B): (T, –) → T `=5`, `=`; (F, T) → T `timeout=`; (F, F) → F TC20C-10 (TC20C-12
  for the true cases). MC/DC reached.
- `settings.containsKey(key)`: true TC20C-13; false TC20C-10.

**`confirmationQuestion`**
- `disabling = saved.isEnabled() && !newEnabled`: (T, T) → T TC20C-22; (T, F) → F TC20C-23; (F, –) → F TC20C-25 (EHR).
- `toReal = saved.getMode() != REAL && newMode == REAL`: (T, T) → T TC20C-23; (T, F) → F TC20C-22; (F, –) → F
  TC20C-25 (RIS/PACS).
- `disabling && toReal`: (T, T) TC20C-24; (T, F) TC20C-22; (F, –) TC20C-23.
- `if (disabling)`: true TC20C-22, false TC20C-23; `toReal ? … : null`: true TC20C-23, false TC20C-25. MC/DC reached.

**`onTestConnection` and `showTestResult`**
- `selected == null` in `onTestConnection`: never true (**not covered**).
- `catch (AccessDeniedException)`: TC20C-41.
- `selected == null ? "the system" : …` in `showTestResult`: true TC20C-33 `showTestResultWithoutSelection`; false
  TC20C-29.
- `result.isSuccess()`: true TC20C-29; false TC20C-30.

**`selectedEntry`, `indexOf`, `formatSettings`**
- `row < 0 || row >= entries.size()` (A, B): (T, –) → T TC20C-01 `noIntegrations`; (F, F) → F TC20C-04. (F, T) **not
  covered**.
- `indexOf`, loop `system != null && index < entries.size()`: (F, –) on opening (TC20C-01); (T, T) after *Save*
  (TC20C-27). (T, F) (the saved system is not in the list read again) **not covered**.
- `entries.get(index).getSystem() == system`: true and false in TC20C-27 `savedRowStaysSelected` (admissions is the
  third row).
- `formatSettings`, `settings == null ? "" : …`: false TC20C-04. True **not covered**.

**`IU_Welcome`**: `onConfigureIntegrations != null && hasPermission(permissions, CONFIGURE_INTEGRATIONS)` (A, B):
(T, T) → T TC20C-07; (T, F) → F TC20C-09 `userWithoutConfigureIntegrationsDoesNotSeeTheButton`; (F, –) → F TC20C-09
`withoutTheActionNobodySeesTheButton`, `olderConstructorsDoNotShowTheButton`. MC/DC reached.

### Not covered (to add)
1. **`hasHost`, condition `index < end` false** (line 418): no endpoint has a second separator after an earlier one.
   Case to add: real mode with `http://sso.hospital.test/api?x=1` → saved.
2. **`hasHost`, `?` and `#` as the end of the host** (error guessing): `http://?a=b` and `http://#top` → "⚠ The endpoint
   needs a host after http:// or https://.", nothing sent; `http://sso.hospital.test?a=b` → saved.
3. **Boundary value of the class "with host"**: a host of one character, `http://a` (adjacent to the limit
   `http://`), should be saved; no test uses it.
4. **`formatSettings` with `null` settings** (line 448): the server keeps `null` settings as `null` (finding 4 of the
   server, F-16), but no test opens the screen with such an integration. Case to add: the fake returns SSO with `null`
   settings → the settings area is empty, no "Unsaved changes", *Save* sends an empty map.
5. **`indexOf` without the saved system** (line 438): after *Save*, the list read again does not contain the saved
   system → the first row is selected. Needs a fake that drops the system after saving.
6. **`changeRow`, conditions `newRow != shownRow`, `shownRow >= 0` and `shownRow < entries.size()` false** (line 213):
   no test fires a row event for the same row, or with no row shown. They may not be reachable from the screen
   (the listener ignores events while loading or restoring); to decide with the team whether to add a case or keep
   them as defensive checks.
7. **Row listener with `getValueIsAdjusting()` true** (line 131): the tests select rows by code, never by dragging the
   mouse. A case could send a selection event with `valueIsAdjusting = true` and check that the form does not change.
8. **`selected == null` in `onSaveConfiguration` and `onTestConnection`** (lines 262, 329), and **`row >= entries.size()`
   in `selectedEntry`** (line 434): *Save* and *Test connection* are disabled when no row is selected, and the table
   always has as many rows as `entries`, so they are not reachable through the screen (defensive checks).
9. **`DocumentListener.changedUpdate`** (lines 254–255): a plain text field never fires it (only attribute changes of
   styled documents do), so it is never run.
10. **`addLabel`, `row == 1 ? 12 : 0` false** (line 498): both labels are in row 1; layout code only.

## 4. Test cases

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC20C-01 | constructor, `displayIntegrations` | Open the screen; also with the rows RIS/PACS (`ERROR`) and SSO; and with no integrations (Limit Val.) | Columns System, Mode, Enabled, Status; four rows "Single sign-on (SSO)", "Electronic health record (EHR)", "Admissions", "RIS/PACS" in the order of the server (no codes such as `RIS_PACS`); Mode `SIMULATED`/`REAL`, Enabled "Yes"/"No", Status "Active"/"Inactive" (and "Error"); cells not editable; the first row selected; empty message area. With no integrations the table is empty and *Save* and *Test connection* are disabled | `listsTheFourIntegrations`, `rowsFollowTheOrderOfTheServer`, `noIntegrations` |
| TC20C-02 | `SimulatedRenderer` | Mode column | "SIMULATED" in bold and dark amber; "REAL" and the other columns plain | `simulatedIsBoldAndAmberInTheTable` |
| TC20C-03 | `updateSimulatedLine` | Select a simulated row, a real row, switch the radio buttons | The line "SIMULATED: no real system is contacted" (bold, amber) appears only while the mode of the form is simulated | `simulatedLineOnlyInSimulatedMode` |
| TC20C-04 | `showSelected`, `updateControls` | Select each row | The form shows its endpoint, its settings one `key=value` per line in their order, *Enabled* and the mode; *Save* and *Test connection* enabled, *Revert* disabled, no "Unsaved changes"; nothing asked | `selectingARowFillsTheForm` |
| TC20C-05 | *Back* | Press *Back* | The action of *Back* runs; nothing is sent | `backRunsItsAction` |
| TC20C-06 | constructor | Keyboard | *Save* Alt+S, *Test connection* Alt+T, *Revert* Alt+R, *Back* Alt+B, *Enabled* Alt+N, labels Endpoint Alt+E, Settings Alt+G, Mode Alt+M (bound to *Simulated*) | `keyboardShortcuts` |
| TC20C-07 | `IU_Welcome` | Welcome screen of a user with `CONFIGURE_INTEGRATIONS` and the action | Button *Configure integrations* (Alt+I) shown, runs its action | `IU_WelcomeConfigureIntegrationsTest.userWithConfigureIntegrationsSeesTheButton` |
| TC20C-08 | `IU_Welcome` | Administrator with every permission and every action | Buttons in order: View audit log, Manage roles, Configure system, Configure integrations, Log out; each button runs only its own action | `IU_WelcomeConfigureIntegrationsTest.administratorSeesTheFiveButtonsInOrder`, `IU_WelcomeConfigureIntegrationsTest.configureIntegrationsRunsOnlyItsOwnAction` |
| TC20C-09 | `IU_Welcome` | User without `CONFIGURE_INTEGRATIONS`; no action given; the 5- and 6-argument constructors | No button *Configure integrations*; the other buttons as before | `IU_WelcomeConfigureIntegrationsTest.userWithoutConfigureIntegrationsDoesNotSeeTheButton`, `IU_WelcomeConfigureIntegrationsTest.withoutTheActionNobodySeesTheButton`, `IU_WelcomeConfigureIntegrationsTest.olderConstructorsDoNotShowTheButton` |
| TC20C-10 | *Save* (`readSettings`) | Settings `␣␣zeta = 1␣`, blank lines, `alpha=2`, `url = http://x.test/?a=b` | The server receives `{zeta=1, alpha=2, url=http://x.test/?a=b}` in that order (split at the first `=`, spaces removed, blank lines ignored); "✔ Integration … saved." | `readsTheSettings` |
| TC20C-11 | *Save* (`readSettings`) | Line 2 without `=` | "⚠ Line 2: use key=value." in red; nothing sent | `lineWithoutEquals` |
| TC20C-12 | *Save* (`readSettings`) | Limit Val.: `=5`, `␣␣=␣5`, `timeout=`, `timeout=␣␣␣`, `=` | "⚠ Line 1: use key=value."; nothing sent | `emptyKeyOrValue` |
| TC20C-13 | *Save* (`readSettings`) | Conjeture: `timeout=30` and `␣timeout␣=␣40` | "⚠ The setting timeout is repeated."; nothing sent | `repeatedKey` |
| TC20C-14 | *Save* (`readSettings`) | Limit Val.: empty text, only blank lines, only spaces | Sent with no settings (empty map) | `emptySettings` |
| TC20C-15 | *Save* | Empty endpoint, simulated mode | Saved | `emptyEndpointInSimulatedMode` |
| TC20C-16 | *Save* | Empty endpoint, real mode | "⚠ The endpoint is required in real mode."; no question, nothing sent | `emptyEndpointInRealMode` |
| TC20C-17 | *Save* | Limit Val. of the host, real mode: `http://sso.hospital.test`, `https://sso.hospital.test:8443/api`, `HTTP://sso.hospital.test`, `Https://sso.hospital.test`; and `http://`, `https://`, `HTTPS://`, `http://:8443`, `http:///path`, `http://user@` | With a host: saved with that endpoint and mode REAL; without a host: "⚠ The endpoint needs a host after http:// or https://.", no question, nothing sent | `validEndpointInRealMode`, `endpointWithoutHostInRealMode` |
| TC20C-18 | *Save* | Real mode with `ftp://…`, no scheme, `http:/…`, `https:…` (Limit Val.), `httpx://…` (`HTTP://…` is accepted since the patch 1.1.2) | "⚠ The endpoint must start with http:// or https://."; no question, nothing sent | `invalidEndpointInRealMode` |
| TC20C-19 | *Save* | Conjeture: `ftp://…` in simulated mode | Same error; nothing sent | `badEndpointInSimulatedMode` |
| TC20C-20 | *Save* | Conjeture: bad endpoint + a line without `=` + a repeated key + an empty key (line 5); also empty endpoint in real + bad line | All errors in one red message, in order, each ending with a full stop; the form keeps what was typed; nothing sent | `everyErrorTogether`, `missingEndpointAndBadLineTogether` |
| TC20C-21 | *Save* | Conjeture: `␣␣␣https://sso.hospital.test␣␣` | Sent as `https://sso.hospital.test` | `endpointIsStripped` |
| TC20C-22 | *Save* (`confirmationQuestion`) | Disable an enabled integration, accept | "Disable the integration with Single sign-on (SSO)?" asked; saved disabled | `disablingAsks` |
| TC20C-23 | *Save* (`confirmationQuestion`) | Switch a simulated integration to real, accept | "Switch Admissions to real mode?" asked; saved REAL | `switchingToRealAsks` |
| TC20C-24 | *Save* (`confirmationQuestion`) | Both at once | One question: "Disable the integration with … and switch it to real mode?" | `bothAskOneQuestion` |
| TC20C-25 | *Save* (`confirmationQuestion`) | Change only the settings; enable a disabled one; real → simulated; change the endpoint of a real one | No question; saved | `otherChangesDoNotAsk` |
| TC20C-26 | *Save* | Cancel the question | Nothing sent; message area cleared; the form keeps the changes and "Unsaved changes" | `cancellingSendsNothing` |
| TC20C-27 | *Save* | Change endpoint, settings and *Enabled*, *Save*; and save another row | The server receives system, endpoint, settings, enabled, mode; "✔ Integration Single sign-on (SSO) saved." in green; the table is read again (Enabled "No", Status "Inactive"); the same row stays selected; no "Unsaved changes"; *Test connection* enabled again | `saveSendsTheForm`, `savedRowStaysSelected` |
| TC20C-28 | *Save* | The server rejects with two errors | "⚠ The setting timeout needs a value. The mode is required." in red; the form keeps the text; nothing stored | `serverErrorsTogether` |
| TC20C-29 | *Test connection* | Enabled simulated integration (3 ms; also 0 ms, Limit Val.) | "✔ Connection test of Single sign-on (SSO): Connection OK (3 ms)." in green | `connectionOk`, `connectionOkWithZeroMilliseconds` |
| TC20C-30 | *Test connection* | Disabled integration | "⚠ Connection test of Electronic health record (EHR): integration disabled." in red | `disabledIntegration` |
| TC20C-31 | *Test connection* | Real integration | "⚠ Connection test of RIS/PACS: real mode not available in this version." | `realModeNotAvailable` |
| TC20C-32 | *Test connection* | Conjeture: integration not configured in the server | "⚠ Connection test of Admissions: integration not configured." | `notConfigured` |
| TC20C-33 | *Test connection*, `showTestResult` | Conjeture: test after saving, then of another row; and `showTestResult` with no selected row | The test is of the selected system and of what is saved (after disabling: "integration disabled"); it sends no configuration | `testsTheSavedConfigurationOfTheSelectedSystem`, `testAfterDisabling`, `showTestResultWithoutSelection` |
| TC20C-34 | `isDirty`, `updateControls` | Change endpoint, settings, *Enabled* or the mode; then (Conjeture) undo by hand or add only spaces | Any change: "Unsaved changes: save or revert to test the connection", *Test connection* disabled, *Revert* enabled. Back to the saved values: clean again | `anyChangeIsUnsaved`, `undoingByHandIsClean` |
| TC20C-35 | *Revert* | Change every field and press *Revert* | The saved configuration is shown again (and the SIMULATED line); clean; nothing sent or asked | `revert` |
| TC20C-36 | `changeRow` | Unsaved changes, press another row, accept | "Discard the unsaved changes?" asked; the new row is shown; nothing sent | `otherRowAcceptDiscards` |
| TC20C-37 | `changeRow` | Same, cancel | The selection goes back to its row, with the changes and "Unsaved changes" | `otherRowCancelKeepsTheChanges` |
| TC20C-38 | `changeRow` | Press other rows without changes | No question | `otherRowWithoutChangesDoesNotAsk` |
| TC20C-39 | constructor | Open the screen when the server denies | "⚠ You do not have permission to configure the integrations." in red; empty table; *Save*, *Test connection*, *Revert* and every field disabled; *Back* enabled and works | `deniedWhenOpening` |
| TC20C-40 | *Save* | Conjeture: permission revoked while open, then *Save* | Same message; only *Back* enabled; nothing stored | `revokedThenSave` |
| TC20C-41 | *Test connection* | Conjeture: permission revoked while open, then *Test connection* | Same message; only *Back* enabled | `revokedThenTestConnection` |
| TC20C-42 | constructor, `isDirty` | Conjeture: the server returns SSO with a `null` endpoint; open the screen, press EHR and SSO again | Empty endpoint; no "Unsaved changes" line; *Test connection* enabled and *Revert* disabled; no question | `nullEndpointIsNotAnUnsavedChange` |
| TC20C-43 | `changeRow` | Conjeture: change the settings of SSO, the permission is revoked, *Save* (denied); then press admissions and RIS/PACS | The table is disabled; the form keeps SSO with its text; "Discard the unsaved changes?" is never asked (0 questions); the denial message stays; nothing sent | `afterADenialTheTableIgnoresTheRows` |
| TC20C-44 | *Revert* | Admissions with `ftp://x`, *Save* (error of the screen); and SSO with an error of the server; then *Revert* | The error ("⚠ The endpoint must start with http:// or https://." / the error of the server) disappears: empty message area; the saved configuration is shown again | `revertClearsTheMessage`, `revertClearsAnErrorOfTheServer` |

Tests TC20C-01 to TC20C-06 and TC20C-10 to TC20C-44 are in `IU_ManageIntegrationsTest` (`client/c2-configuration`,
nested classes `Display`, `ReadingOfTheSettings`, `ValidationOfTheEndpoint`, `Confirmations`, `Save`,
`TestConnection`, `UnsavedChanges`, `Permission`, `FindingsOf122`); TC20C-07 to TC20C-09 are in
`IU_WelcomeConfigureIntegrationsTest` (`client/c1-access`). TC20C-42 to TC20C-44 come from the fixes after the
findings of #122 (batch 2 of `doc/plans/UC12-testing-fixes.md`, step 9, commit dc3571d); every question asked is
recorded by the confirmation of the test.

Postcondition of every case: nothing is stored in the service unless *Save* passes the checks of the screen, is
confirmed when it asks, and is accepted by the server; after a saved change the table is read again from the server;
a connection test, *Revert*, *Back* and pressing a row send no configuration; after a denial only *Back* can be used.

## Manual check (step 8 of the plan), with `admin` and `mgarcia`
- The four integrations are listed in the order SSO, EHR, admissions, RIS/PACS.
- SIMULATED is in bold amber in the table and the line above the form appears only in simulated mode.
- Editing a field shows "Unsaved changes", disables *Test connection* and enables *Revert*.
- *Save* with errors shows them all together and sends nothing.
- Switching to real mode or disabling asks first.
- *Test connection* gives OK with the time, "integration disabled" or "real mode not available in this version".
- Pressing another row with unsaved changes asks "Discard the unsaved changes?".
- `mgarcia` does not see *Configure integrations*.
