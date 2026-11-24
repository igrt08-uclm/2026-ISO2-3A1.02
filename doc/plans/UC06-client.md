# Plan – UC06 – Client (#67)

| | |
|---|---|
| **Sub-issue** | #67 *UC06 – Client (C4 v1.0.0)* · branch `feature/UC06-client`, from `feature/UC06-server` (`b156b9a`), which already has the server of UC06 |
| **Use case** | #66 *UC06 – Order a diagnostic procedure (C4 v1.0.0)* · FR06 · component C4 1.0.0 |
| **Specification** | use case issue #66, sub-issue #67 and the wiki page *Requirements and Use Cases* |
| **Design** | `doc/design/C4-client-design.puml` (`IU_OrderProcedure`) · contracts: `IClinicalOrderService` (section 6 of #65, `doc/plans/UC06-server.md`) · `IPatientService` and `ISystemParameterService` |
| **Usability guide** | IPO I, as in `doc/plans/UC17-client.md` and `doc/plans/UC12-client.md` |
| **Plan approved by** | Ignacio Romero Torres, 23/11/2026 (the proposals Q1 to Q11 accepted as written) |

## Decisions already taken
- **Issue #67 (David, 01/11/2026):** the physician orders from an order screen; they select the test type, write the clinical indication and the justification and choose the priority; the screen lists the orders already placed for that patient with test type, priority, indication and date; the status of each order and its appointment belong to UC08 (Iteration 9); deactivated test types are not offered (D-022); if the patient is not active the screen does not allow ordering and explains why (UC12); if the user does not have the permission of the physician role, access is denied (UC18).
- **Design (Marcos and Denis, pull request #201):** one boundary class `IU_OrderProcedure` with `onSelectPatient(patientId)`, `displayOrderForm(testTypes, priorities, orders)`, `onSubmitOrder()`, `showPatientInactive(patient)`, `showValidationErrors(errors)`, `showOperationResultSuccess(msg)` and `showOperationResultError(msg)`. `IU_OrderFollowUp` is UC07 (#69–#71) and is **not** in this plan.
- **Server done (`feature/UC06-server`):** `IClinicalOrderService.orderDiagnosticProcedure(requester, order)` in `common`, `ClinicalOrderService` in `server-c4-orders` 1.0.0-SNAPSHOT, `common` 1.6.0-SNAPSHOT, and `ORDER_PROCEDURES` already exists in C1 1.2.4 for `PHYSICIAN` (`mgarcia`).
- **Testing:** the tests and the manual check are Testing's (Adam, rule 17); this plan writes no test.
- **Out of this version:** the screen of UC07, the status and the execution schedule of UC08 (D-013), and editing or cancelling an order (D-039).

## Questions and answers
Proposals of Implementation; **none is decided yet**: this plan is written to be approved, and no file changes before that approval (rule 4 of the cycle).
- **Q1 – Module, packages and versions (rule 9).** **Proposal:** a new module `client/c4-orders` (`artifactId` `client-c4-orders`, version `1.0.0-SNAPSHOT`, depends only on `common` and JUnit, as `client/c3-patients`), package `es.uclm.esi.iso2.client.orders.presentation`, added to `client/pom.xml` and to the `<dependencyManagement>` of the root pom. `client-c1-access` goes to `1.2.5-SNAPSHOT` and gets the dependencies on `server-c4-orders` and `client-c4-orders` in the last step, as UC12 did with C3. **A:** accepted (Ignacio, 23/11/2026): `client-c4-orders` `1.0.0-SNAPSHOT` and `client-c1-access` `1.2.5-SNAPSHOT`.
- **Q2 – The class.** **Proposal:** `IU_OrderProcedure`, a `JPanel` (so the tests create it without a display), which receives the `IClinicalOrderService`, the `IPatientService`, the `ISystemParameterService`, the username of the requester, a `Predicate<String>` to confirm (as the other screens) and the action of *Back*; the methods of the design keep their names. **A:** accepted (Ignacio, 23/11/2026).
- **Q3 – Layout.** **Proposal:** at the left a search box and the table of patients (*Identifier*, *Full name*, *Date of birth*), at the right the form of the order (test type, priority, clinical indication, justification), under it the table of the orders of the selected patient (*Date*, *Test type*, *Priority*, *Indication*) and the buttons *Place order* and *Back*; a line says how many patients were found and another one says what happened. When the screen opens it lists all the patients. **A:** accepted (Ignacio, 23/11/2026).
- **Q4 – Selecting the patient.** **Proposal:** a click on a row calls `onSelectPatient`; the screen reads the patient from C3 (`getPatient`) and, if it is not active, calls `showPatientInactive` and disables the form (the server would reject the order anyway); if it is active, it loads the test types and the priorities and shows the orders of that patient. **A:** accepted (Ignacio, 23/11/2026). **Deviation for Design:** `onSelectPatient` receives the health-card identifier that the table shows, not `patientId` as the design of `doc/design/C4-client-design.puml` says; the patient is read with `searchPatients(identifier)` and the exact identifier is compared, because C3 only offers the health card number to the screen.
- **Q5 – Test types and priorities.** **Proposal:** the list of test types offers only the active ones (`isActive()`, D-022) and shows `TT-01 · Chest X-ray`; the list of priorities offers the configured ones that are active (`getPriorities`, as the server checks). If C2 returns none, the form says so and *Place order* stays disabled. **A:** accepted (Ignacio, 23/11/2026).
- **Q6 – Validation in the client.** **Proposal:** the same rules and messages as the server (`doc/plans/UC06-server.md`, Q4–Q5): patient, test type, priority, indication and justification required; all the errors together, in red, with `⚠`, and nothing is sent; the field of each error gets a red border. **A:** accepted (Ignacio, 23/11/2026); the border is fixed in step 11 (F-03). **A of Testing (Adam, 23/11/2026):** the red border is only for the errors of the client (test type, priority, clinical indication and justification); it is cleared when the form is reloaded, after a success and before validating again; the messages of the server only appear in the message area.
- **Q7 – After a successful order.** **Proposal:** "Order ORD-0001 created." in green; the form is emptied (the patient stays selected) and the table of orders is loaded again; the order is placed for the selected patient with the requester of the session. **A:** accepted (Ignacio, 23/11/2026). **Deviation:** `ClinicalOrder` has no `getPriorityId`, so the column *Priority* is only filled when the order is a `DiagnosticOrder`.
- **Q8 – Errors of the server.** **Proposal:** `ValidationException` shows every message of the server together; `AccessDeniedException` shows "You do not have permission to order procedures." and leaves only *Back* enabled. **A:** accepted (Ignacio, 23/11/2026); the *Search* button is fixed in step 10 (F-02).
- **Q9 – Welcome screen and `Main`.** **Proposal:** `IU_Welcome` gets one more constructor with `Runnable onOrderProcedure` and the button *Order procedure* (Alt+O), shown only with `ORDER_PROCEDURES`; the current constructors stay unchanged, so the tests of C1 do not change. `Main` creates the `ClinicalOrderService` with the role service and the audit service of C1 plus the `SystemParameterService` and the `PatientService` it already builds, and shows the screen with the title "Order a diagnostic procedure". **A:** accepted (Ignacio, 23/11/2026).
- **Q10 – Keyboard.** **Proposal:** *Place order* Alt+P, *Search* Alt+H, *Back* Alt+B; the fields Alt+T (test type), Alt+R (priority), Alt+C (clinical indication) and Alt+J (justification). **A:** accepted (Ignacio, 23/11/2026); the Alt keys are fixed in step 11 (F-04).
- **Q11 – Unsaved changes.** **Proposal:** if the form has data and the user presses another patient or *Back*, the screen asks "Discard the unsaved changes?"; an empty form is not a change. **A:** accepted (Ignacio, 23/11/2026).

## Fixes after the review of Testing (23/11/2026)
The review of `feature/UC06-client` (#204) against `doc/testing/test-cases/UC06-client-test-design.md` found five
defects and four documents that disagree with the plan. They are the new steps 9 to 12. No test and no expected value
is changed here (rule 17): Adam writes TC06C-44 and updates the expected values that these fixes change.
- **F-01 – the denied access is hidden when the permission is lost while selecting (TC06C-44).** `findPatient`
  catches the `AccessDeniedException`, calls `handleDenied()` and returns `null`; `onSelectPatient` then writes
  “There is no patient with the identifier …” over the denied message. **Proposal:** remove the `catch` of
  `findPatient`, so the exception reaches the `catch` of `onSelectPatient`, which already calls `handleDenied()`:
  one place handles the denial. **Decided (Testing, 23/11/2026):** this way; the only requirement is that the
  denied message is seen (TC06C-44), whatever the mechanism.
- **F-02 – *Search* stays enabled after the denied access.** `handleDenied()` disables the form, the table and the
  search box, but not the button *Search* (Q8 says only *Back* stays enabled). **Proposal:** keep the button in a
  field (it is local in `buildBody`) and disable it in `handleDenied()`.
- **F-03 – no red border on the fields with errors (Q6).** **Proposal:** a line border of `ERROR_COLOR` on the fields
  of the errors of the client's own validation (test type, priority, clinical indication and justification); the
  borders are cleared when the form is shown again, after a success and before a new validation. The messages of a
  `ValidationException` of the server are free text, so only their text is shown. **Decided (Testing, 23/11/2026):**
  exactly that scope, and the borders are cleared on reload, after a success and before validating again.
- **F-04 – no Alt keys for the fields (Q10).** **Proposal:** `setLabelFor` + `setDisplayedMnemonic` on the labels
  *Test type* (Alt+T), *Priority* (Alt+R), *Clinical indication* (Alt+C) and *Justification* (Alt+J); `addField`
  receives the mnemonic as a parameter.
- **F-05 – the priority is shown only by its name.** The *Usability decisions* say “the priority is shown by its
  name, with its id next to it”. **Proposal:** `URGENT · Urgent` in the list and in the column *Priority* of the
  table, the same format as the test types (`id · name`, Q5). **Pending, Design (Marcos and Denis):** the plan
  contradicts itself (the usability table says “name with its id next to it”, the mockup shows only `URGENT`), so
  Design must decide the text; Implementation does not decide it. **Also pending, Testing:** the current cases expect
  the name only (TC06C-01, 03, 06, 17 and 18 and the column *Priority* of the table were taken from the code, not
  from the plan), so Testing uploads those expected values first and step 12 comes after; this branch does not touch
  a test (rule 17). **Decided by Design:** `id · name` (Denis, `61e7246`); Testing
  uploaded the expected values in `7967dcc` (`test(UC06): client cases TC06C-44..49 and the priority as id · name`).
- **Documents.** `agents/components.md` says `client-c1-access` is `1.2.4`; the pom is `1.2.5` (step 6).
  `IU_OrderProcedure.java:405` is 121 characters.
- **Status of the tests.** The 43 tests of Testing (commit `6883914`) are headless: they build the screen and never
  show it, so `mvn -B verify` runs them without a display. In this container they still fail with
  `Fontconfig head is null`, because it has neither `libfontconfig` nor fonts: that is a machine without fonts, not a
  machine without desktop. With fonts, the suite runs in `mvn -B verify`.

## Screen (low-fidelity prototype)
```
+------------------------------------------------------------------------------+
| Radiodiagnostics System - Order a diagnostic procedure                   _ X |
+------------------------------------------------------------------------------+
|  Patients                                                                    |
|  Search [____________________] [ Search ]   Test type  [TT-01 · Chest X-ray v]|
|  +--------------------------------------+   Priority   [URGENT            v] |
|  | Identifier   Full name        Birth   |   Indication [Chest pain         ] |
|  | HC-0000001 > Lucía Ejemplo..  14/03/80|   Justification [Rule out pneu...] |
|  | HC-0000002   Mario Demo Sim.. 02/11/75|                                    |
|  | HC-0000003   Elena Muestra..  25/07/01|                [ Place order ]     |
|  +--------------------------------------+                                    |
|  3 patients found.                                                           |
|  Orders of HC-0000001                                                        |
|  +------------------------------------------------------------------------+  |
|  | Date        Test type      Priority    Indication                       |  |
|  | 23/11/2026  Chest X-ray    URGENT      Chest pain                       |  |
|  +------------------------------------------------------------------------+  |
|  (!) The justification is required.                                          |
|                                                                   [ Back ]   |
+------------------------------------------------------------------------------+
```
Welcome screen: a new button *Order procedure* (Alt+O), visible only with `ORDER_PROCEDURES`.

## Usability decisions
| Principle (IPO I) | Decision |
|---|---|
| Visibility of the system status | A message area says what happened (created, errors, denied) and a line says how many patients were found |
| User's language | Plain names; dates as `dd/MM/yyyy`; the priority is shown by its name, with its id next to it |
| Control and freedom | *Back* returns to the welcome screen; nothing is deleted or cancelled (D-039) |
| Consistency and standards | The same message area, colours, icons, confirmations and *Back* as the screens of UC12, UC17 and UC20 |
| Error prevention | The patient must be active; only active test types and priorities are offered; the form is checked before the server is called; the questionnaire asks before it throws away unsaved data |
| Recognition rather than recall | The patients and the orders of the selected patient are always visible; each label is above its field |
| Flexibility and shortcuts | Enter searches; the Alt keys of Q10; Tab moves between fields |
| Minimalist design | One screen: patients and their orders at the left, the form at the right |
| Recognise and recover from errors | All the errors appear together, with an icon and the field marked, and the form keeps what the user typed |
| Gestalt: proximity and similarity | Search, patients and their orders together at the left; the form and *Place order* together at the right |
| Colour and readability | Errors have an icon and text, not only colour |

## Files
| File | Action | What | Step |
|---|---|---|---|
| `client/c4-orders/pom.xml` | create | `client-c4-orders` `1.0.0-SNAPSHOT`, depends on `common` and JUnit (Q1) — **rule 9** | 2 |
| `client/pom.xml`, `pom.xml` | change | module `c4-orders` and the managed version of `client-c4-orders` (Q1) — **rule 9** | 2 |
| `client/c4-orders/src/main/java/es/uclm/esi/iso2/client/orders/presentation/IU_OrderProcedure.java` | create | the screen of Q2 to Q8 and Q10-Q11; **new Java package** | 3–5 |
| `client/c1-access/pom.xml` | change | `1.2.5-SNAPSHOT` and the dependencies on `server-c4-orders` and `client-c4-orders` (Q1) — **rule 9** | 6 |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Welcome.java` | change | one more constructor and the button *Order procedure* (Q9) | 6 |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/Main.java` | change | the service and the navigation (Q9) | 6 |
| `agents/architecture.md`, `agents/components.md` | change | the tree with `client/c4-orders` and the versions — **rule 9** | 7 |
| `doc/plans/UC06-client.md` | change | this plan: Q1 to Q11 answered, the fixes F-01 to F-05 and the deviation of Q4 | 1, 9 |
| `agents/components.md` | change | `client-c1-access` `1.2.5`, as the pom already says (F-0, documents of the review) | 9 |
| `client/c4-orders/src/main/java/es/uclm/esi/iso2/client/orders/presentation/IU_OrderProcedure.java` | change | F-01 and F-02: the denied message survives a selection and *Search* is disabled; line 405 wrapped | 10 |
| `client/c4-orders/src/main/java/es/uclm/esi/iso2/client/orders/presentation/IU_OrderProcedure.java` | change | F-03 and F-04: red border of the fields with errors and the Alt keys of the labels | 11 |
| `client/c4-orders/src/main/java/es/uclm/esi/iso2/client/orders/presentation/IU_OrderProcedure.java` | change | F-05: the priority as `id · name` in the list and in the table | 12 |

No other file changes: nothing under `server/`, nothing of C1 beyond `IU_Welcome`, `Main` and the pom of the last step, no test (rule 17). The screen only imports `common` and Swing, never `server`; every class and public method has its Javadoc in English (rule 18).

## Steps
One step = one commit, with `mvn -B verify -Djacoco.skip=true` green before it (the coverage of `client-c4-orders` is only measured when Adam's tests exist; JaCoCo skips a module without tests).
- [x] 1. This plan (`docs(UC06): plan for the client part (#67)`). **Wait for the approval of the member (rule 4) before step 2.**
- [x] 2. Poms: module `client/c4-orders` and its managed version (Q1); the poms of `client-c1-access` wait for step 6.
- [x] 3. `IU_OrderProcedure`, the display: patient search and table, the form with the active test types and priorities, the table of orders of the selected patient, the lines of status, *Back* (Q3, Q4, Q5).
- [x] 4. `IU_OrderProcedure`, *Place order*: reading of the form, validation in the client, call to `orderDiagnosticProcedure`, messages, reload of the orders and the inactive patient (Q6, Q7, Q8).
- [x] 5. `IU_OrderProcedure`, unsaved changes and the denied access (Q8, Q11).
- [x] 6. `IU_Welcome` (button *Order procedure*) and `Main` (service and navigation); `client-c1-access` `1.2.5-SNAPSHOT` and its two dependencies (Q1, Q9).
- [x] 7. `agents/architecture.md` and `agents/components.md`.
- [x] 8. Hand the branch over to Testing: the test design and the tests of UC06-client (Testing's commit `6883914`, `test(UC06): T.04 test design and headless client tests TC06C-01..43 (#69)`); the pull request #204 (`Closes #67`) against `feature/UC06-server`.
- [x] 9. This plan (Q1 to Q11 answered, the answers of Testing to F-01, F-03 and F-05, the fixes and the deviation of Q4) and `agents/components.md` (`client-c1-access` `1.2.5`) — `90c3fd8` `docs(UC06): answers and state of the fixes of the review (#67)`.
- [x] 10. F-01 and F-02 in `IU_OrderProcedure`: the denied message is not overwritten by a selection and *Search* is disabled; line 405 wrapped — `c227441` `fix(UC06): keep the denied message when a patient is selected (#67)`.
- [x] 11. F-03 and F-04 in `IU_OrderProcedure`: the red border of the fields with errors and the Alt+T/R/C/J of the labels — `4fd6204` `fix(UC06): mark the fields with errors and add the Alt keys (#67)`.
- [x] 12. F-05 in `IU_OrderProcedure`: the priority as `id · name` in the list and in the table — `61e7246` `fix(UC06): show the id of the priority next to its name (F-05, Denis) (#67)`; Testing uploaded the expected values in `7967dcc`.
- [x] 13. Clear the selection, the form and the orders when there is no patient: `displayPatients` after a search and the failed selection in `onSelectPatient`; the read of the orders after creating one goes in its own `try` — `326d8ed` `fix(UC06): clear the form and the orders when no patient is selected (#204)`.
- [x] 14. Forget the previous patient when a selection fails: `clearSelection` empties the form, its texts, its
  borders and the orders, and the patient is only selected when its types and its orders were read — `a2f6942`
  `fix(UC06): forget the previous patient when a selection fails (#204)`.
- [x] 15. The reload after creating an order: decided by Design (#67, Marcos), no change. When the reload fails the
  error replaces the success, exactly as TC13C-46 does and as TC06C-53 and TC06C-54 expect; the id of the created
  order is not kept in the success line. The code already behaved this way (`ac200cf`).

## Verification
- [ ] Every acceptance criterion of #67: the physician creates an order for an active patient with an active test type and a configured priority and sees it in the list of the patient; invalid data is rejected with a message and nothing is created; deactivated test types are not offered; an inactive patient is not allowed to be ordered for; the denied access is explained.
- [ ] `IU_OrderProcedure` only imports `common` and Swing.
- [ ] No class of C1 changes except `IU_Welcome`, `Main` and the `pom.xml` of the last step.
- [ ] Each row of *Usability decisions* is checked in the running application (Testing).
- [ ] `mvn -B verify` is green with the tests of Testing (coverage included). They are headless and need no display and no desktop; they do need fontconfig and fonts installed in the machine.
- [ ] The CI of the pull request is green.

## Status
- **In progress:** nothing; the fixes of the review of Testing and of the review of the pull request are done (steps 9
  to 15).
- **Done:** `3eb55e2` approval of this plan, `2697fd2` the poms (step 2), `cef2828` the screen (steps 3 to 5, one commit because it is one class), `855e8ee` `IU_Welcome`, `Main`, the version 1.2.5 of `client-c1-access` and `agents/architecture.md` (steps 6 and 7); step 8 is Testing's commit `6883914` (test design and 43 headless tests), step 9 `90c3fd8` (the answers of Testing and `agents/components.md`), step 10 `c227441` (F-01 and F-02) and step 11 `4fd6204` (F-03 and F-04). Of the five defects of the review only F-05 is open, and it is blocked outside this branch.
- **Next step:** nothing in this branch. Testing (Adam) added the cases of steps 13 and 14 (`16918a7`, TC06C-55 to
  TC06C-61) and they pass; the message of the reload is decided by Design (step 15).
- **Tests of Testing:** they are headless (they build the screen and never show it), so they need no display and no desktop, but they do need fontconfig and fonts in the machine. `mvn -B verify` is green in the local machine (Maven 3.9.9, JDK 21, 23/11/2026): BUILD SUCCESS, all coverage checks met, 38 tests of `IU_OrderProcedureTest` and 104 of `client-c1-access`. Two traps of the environment, not of the code: without fonts the Swing components throw `Fontconfig head is null` (it happened in a container that had no `libfontconfig`), and with Maven 3.8.7 the old Surefire does not run JUnit 6 and reports 0 tests, which makes JaCoCo fail — the build needs Maven 3.9.x.
- **Deviations:** steps 3 to 5 are one commit; `agents/components.md` still says 1.2.4 for `client-c1-access` (fixed in step 9); `IClinicalOrderService.getOrdersOfPatient` returns `ClinicalOrder`, which has no `getPriorityId`, so the column *Priority* is only filled when the order is a `DiagnosticOrder` — the contract of `common` is not changed here, to keep #202 as it was reviewed; `onSelectPatient` takes the health-card identifier (Q4).
- **Not checked here:** the application was not started and the tests of the client could not run in this container (no fonts). The manual usability check is Testing's.
- **Open questions:** TC06C-29 (the priority VIP) is still open for Design: the case and the *Usability decisions* do
  not agree on whether a VIP priority is offered, and this plan does not decide it. Everything else of the review is
  closed; F-05 is decided (`id · name`), implemented in step 12 and its expected values are in `7967dcc`.
- **Last update:** 24/11/2026, Ignacio — step 15 closed as decided by Design (#67, Marcos): the error replaces the
  success when the reload fails, no change. `mvn -B -ntp verify` green with the tests of Adam.
