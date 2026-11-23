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
- **Q1 – Module, packages and versions (rule 9).** **Proposal:** a new module `client/c4-orders` (`artifactId` `client-c4-orders`, version `1.0.0-SNAPSHOT`, depends only on `common` and JUnit, as `client/c3-patients`), package `es.uclm.esi.iso2.client.orders.presentation`, added to `client/pom.xml` and to the `<dependencyManagement>` of the root pom. `client-c1-access` goes to `1.2.5-SNAPSHOT` and gets the dependencies on `server-c4-orders` and `client-c4-orders` in the last step, as UC12 did with C3. **A:** pending.
- **Q2 – The class.** **Proposal:** `IU_OrderProcedure`, a `JPanel` (so the tests create it without a display), which receives the `IClinicalOrderService`, the `IPatientService`, the `ISystemParameterService`, the username of the requester, a `Predicate<String>` to confirm (as the other screens) and the action of *Back*; the methods of the design keep their names. **A:** pending.
- **Q3 – Layout.** **Proposal:** at the left a search box and the table of patients (*Identifier*, *Full name*, *Date of birth*), at the right the form of the order (test type, priority, clinical indication, justification), under it the table of the orders of the selected patient (*Date*, *Test type*, *Priority*, *Indication*) and the buttons *Place order* and *Back*; a line says how many patients were found and another one says what happened. When the screen opens it lists all the patients. **A:** pending.
- **Q4 – Selecting the patient.** **Proposal:** a click on a row calls `onSelectPatient`; the screen reads the patient from C3 (`getPatient`) and, if it is not active, calls `showPatientInactive` and disables the form (the server would reject the order anyway); if it is active, it loads the test types and the priorities and shows the orders of that patient. **A:** pending.
- **Q5 – Test types and priorities.** **Proposal:** the list of test types offers only the active ones (`isActive()`, D-022) and shows `TT-01 · Chest X-ray`; the list of priorities offers the configured ones that are active (`getPriorities`, as the server checks). If C2 returns none, the form says so and *Place order* stays disabled. **A:** pending.
- **Q6 – Validation in the client.** **Proposal:** the same rules and messages as the server (`doc/plans/UC06-server.md`, Q4–Q5): patient, test type, priority, indication and justification required; all the errors together, in red, with `⚠`, and nothing is sent; the field of each error gets a red border. **A:** pending.
- **Q7 – After a successful order.** **Proposal:** "Order ORD-0001 created." in green; the form is emptied (the patient stays selected) and the table of orders is loaded again; the order is placed for the selected patient with the requester of the session. **A:** pending.
- **Q8 – Errors of the server.** **Proposal:** `ValidationException` shows every message of the server together; `AccessDeniedException` shows "You do not have permission to order procedures." and leaves only *Back* enabled. **A:** pending.
- **Q9 – Welcome screen and `Main`.** **Proposal:** `IU_Welcome` gets one more constructor with `Runnable onOrderProcedure` and the button *Order procedure* (Alt+O), shown only with `ORDER_PROCEDURES`; the current constructors stay unchanged, so the tests of C1 do not change. `Main` creates the `ClinicalOrderService` with the role service and the audit service of C1 plus the `SystemParameterService` and the `PatientService` it already builds, and shows the screen with the title "Order a diagnostic procedure". **A:** pending.
- **Q10 – Keyboard.** **Proposal:** *Place order* Alt+P, *Search* Alt+H, *Back* Alt+B; the fields Alt+T (test type), Alt+R (priority), Alt+C (clinical indication) and Alt+J (justification). **A:** pending.
- **Q11 – Unsaved changes.** **Proposal:** if the form has data and the user presses another patient or *Back*, the screen asks "Discard the unsaved changes?"; an empty form is not a change. **A:** pending.

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
| `doc/plans/UC06-client.md` | create/update | this plan | 1, 8 |

No other file changes: nothing under `server/`, nothing of C1 beyond `IU_Welcome`, `Main` and the pom of the last step, no test (rule 17). The screen only imports `common` and Swing, never `server`; every class and public method has its Javadoc in English (rule 18).

## Steps
One step = one commit, with `mvn -B verify -Djacoco.skip=true` green before it (the coverage of `client-c4-orders` is only measured when Adam's tests exist; JaCoCo skips a module without tests).
- [ ] 1. This plan (`docs(UC06): plan for the client part (#67)`). **Wait for the approval of the member (rule 4) before step 2.**
- [ ] 2. Poms: module `client/c4-orders` and its managed version (Q1); the poms of `client-c1-access` wait for step 6.
- [ ] 3. `IU_OrderProcedure`, the display: patient search and table, the form with the active test types and priorities, the table of orders of the selected patient, the lines of status, *Back* (Q3, Q4, Q5).
- [ ] 4. `IU_OrderProcedure`, *Place order*: reading of the form, validation in the client, call to `orderDiagnosticProcedure`, messages, reload of the orders and the inactive patient (Q6, Q7, Q8).
- [ ] 5. `IU_OrderProcedure`, unsaved changes and the denied access (Q8, Q11).
- [ ] 6. `IU_Welcome` (button *Order procedure*) and `Main` (service and navigation); `client-c1-access` `1.2.5-SNAPSHOT` and its two dependencies (Q1, Q9).
- [ ] 7. `agents/architecture.md` and `agents/components.md`.
- [ ] 8. Hand the branch over to Testing: the manual check and the tests of UC06-client; the pull request (`Closes #67`) against `feature/UC06-server` or `develop`, as the member decides.

## Verification
- [ ] Every acceptance criterion of #67: the physician creates an order for an active patient with an active test type and a configured priority and sees it in the list of the patient; invalid data is rejected with a message and nothing is created; deactivated test types are not offered; an inactive patient is not allowed to be ordered for; the denied access is explained.
- [ ] `IU_OrderProcedure` only imports `common` and Swing.
- [ ] No class of C1 changes except `IU_Welcome`, `Main` and the `pom.xml` of the last step.
- [ ] Each row of *Usability decisions* is checked in the running application (Testing).
- [ ] `mvn -B verify` is green with the tests of Testing (coverage included).
- [ ] The CI of the pull request is green.

## Status
- **In progress:** nothing
- **Next step:** the approval of this plan (step 1 is written; steps 2 to 8 wait for a yes). Then step 2 (`client/c4-orders/pom.xml`, `client/pom.xml`, `pom.xml`).
- **Open questions:** every Q of the list above (Q1 to Q11).
- **Last update:** 23/11/2026, Ignacio — plan written, pending approval.
