# Test design · UC06 – Server (C4 v1.0.0) · #68 – Order a diagnostic procedure

Designed by Testing (Adam Gouram) following lesson T.04 (Testing Management): parameters of each method, equivalence
classes, boundary values and error guessing, combination of the values, and coverage of the code. Expected results
come from #66, #68, the answers Q4 to Q14 of `doc/plans/UC06-server.md` (and its *Cases to cover*), D-022 and D-039;
nothing that those sources do not define is used as an expected result (section 5).

Code under test (`server-c4-orders` 1.0.0-SNAPSHOT, commit `92d38bb` of `feature/UC06-server`): the six public methods
of `IClinicalOrderService` implemented by `ClinicalOrderService`, `InMemoryClinicalOrderRepository`,
`InMemorySchedulingNeedRepository` and the data classes of C4 in `common` (`ClinicalOrder`, `DiagnosticOrder`,
`SchedulingNeed`, `SchedulingNeedStatus`).

**Test data (precondition of every case).** The service is built with empty repositories, a recording
`IAuditRecorder` and the clock `Clock.fixed(2026-11-23T10:00:00Z, UTC)`, so every `creationDate` is 23/11/2026
(except TC06S-55, which uses the constructor with the system clock). The
users are the seed users of C1 1.2.4 with the permissions of #173: `mgarcia` (active, `ORDER_PROCEDURES`), `lmartin`
(active, `MANAGE_APPOINTMENTS`), `admin` (active, `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS`),
`jfernandez` (active, none), `pnavarro` (**inactive**, `ORDER_PROCEDURES`); `nobody` does not exist. The fake C3 has the
active patients `P-0001` to `P-0003` and the inactive `P-0004`. The fake C2 has the test types `TT-01`, `TT-02`
(active) and `TT-03` (deactivated) and the priorities `URGENT`, `PREFERENTIAL`, `ROUTINE` (active) and `OLD`
(deactivated). **`P-0004` and every id of the fake C2 catalogue (`TT-01`, `TT-02`, `TT-03`, `OLD`) are invented test
values**, not data of C2 or C3: the real C3 has no discharge yet (D-024) and the fakes let the tests count the calls to
C2 and C3. The valid order is `P-0001` / `TT-01` / `URGENT`, indication "Chest pain", justification "Rule out
pneumonia".

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| every method | `requester` | Active with `ORDER_PROCEDURES` | `mgarcia` |
| | | Active with `MANAGE_APPOINTMENTS` only | `lmartin` |
| | | Active without either permission | `admin`, `jfernandez` |
| | | Inactive (with `ORDER_PROCEDURES`) | `pnavarro` |
| | | Unknown | `nobody`; `""` (Conjeture) |
| | | Not given | `null` (Limit Val.) |
| `orderDiagnosticProcedure` | `order` | Valid | the valid order; also with `orderId` `ORD-0099`, `requester` `admin`, `creationDate` 01/01/2000 (ignored, Q4); an order already returned by the service (D-039, Conjeture) |
| | | Not given | `null` (Limit Val.) |
| | `order.patientId` | Active | `P-0001` |
| | | Blank | `null` (Limit Val.), `""` (Limit Val.), `"   "` |
| | | Inactive | `P-0004` |
| | | Unknown | `P-9999` |
| | `order.testTypeId` | Active | `TT-01`, `TT-02` |
| | | Blank | `null`, `""`, `"   "` |
| | | Deactivated | `TT-03` |
| | | Unknown | `TT-99` |
| | `order.priorityId` | Active and configured | `URGENT`, `PREFERENTIAL`, `ROUTINE` |
| | | Blank | `null`, `""`, `"   "` |
| | | Configured but deactivated | `OLD` |
| | | Not configured | `VIP`; `urgent` (Conjeture: case) |
| | `order.indication`, `order.justification` | Not blank (required, no maximum, Q5) | `Chest pain` / `Rule out pneumonia`; one character `a` / `b` (Limit Val.); with spaces at the ends (Conjeture) |
| | | Blank | `null`, `""` (Limit Val.), `"   "` |
| `getOrdersOfPatient` | `patientId` | With orders | `P-0001` (while `P-0002` also has an order) |
| | | Without orders | `P-0002` |
| | | Unknown | `P-9999` |
| | | Blank | `null`, `""`, `"   "` |
| `getOrder` | `orderId` | Existing | `ORD-0001` |
| | | Unknown or not given | `ORD-9999`, `null` |
| `getPendingSchedulingNeeds` | (state) | No needs / pending needs / a booked need | start; after one or two orders; after booking |
| `markSchedulingNeedBooked`, `markSchedulingNeedPending` | `needId` | Existing, in the other status | `NEED-0001` |
| | | Existing, already in that status | `NEED-0001` booked twice / made pending twice |
| | | Unknown or not given | `NEED-9999`, `null`, `""` (Conjeture) |
| (repositories) | `id` | Existing / unknown / saved twice | `ORD-0001`, `NEED-0001` already saved (Limit Val. of the counter) |

**Outputs.** `orderDiagnosticProcedure` returns the created `DiagnosticOrder` (`ORD-nnnn`, the requester of the
parameter, the date of the clock) and creates one `PENDING` need `NEED-nnnn` due on that date; it registers
`ORDER_CREATED` on `ORDER:<orderId>`. The reads return the list or the order; the changes of a need return nothing.
Exceptions: `AccessDeniedException` (with the requester), registered as `ORDER_ACCESS_DENIED` on `orders`,
`ORDER:<orderId>` or `SCHEDULING_NEED:<needId>` (Q7); `ValidationException` with every message of Q4 / Q13. The
accepted reads and the changes of a need are not audited (Q7).

## 2. Combination of the values

- **Requester class × operation (pairwise).** With two parameters, pairwise is every pair: 7 requester classes
  (`mgarcia`, `lmartin`, `admin`, `jfernandez`, `pnavarro`, unknown, `null`) × 6 operations = **42 combinations, the
  maximum number of cases** of this table. All 42 are covered:

  | Operation | `mgarcia` | `lmartin` | `admin`, `jfernandez` | `pnavarro`, unknown, `null` |
  |---|---|---|---|---|
  | `orderDiagnosticProcedure` | TC06S-01 | TC06S-02 | TC06S-02 | TC06S-03 |
  | `getOrdersOfPatient` | TC06S-28 | TC06S-45 | TC06S-45 | TC06S-45 |
  | `getPendingSchedulingNeeds` | TC06S-22 | TC06S-23 | TC06S-24 | TC06S-24 |
  | `getOrder` | TC06S-32, TC06S-47 | TC06S-47 | TC06S-24 | TC06S-24 |
  | `markSchedulingNeedBooked` | TC06S-47 | TC06S-36, TC06S-47 | TC06S-24 | TC06S-24 |
  | `markSchedulingNeedPending` | TC06S-47 | TC06S-37, TC06S-47 | TC06S-24 | TC06S-24 |

- **Fields of the order (each use).** Each invalid class alone with the rest of the order valid (TC06S-13 to TC06S-21,
  TC06S-25), then all at once (TC06S-26). The domain of every text field is any `String`, so there is no maximum
  number of cases: the classes of section 1 bound it.
- **Requester × invalid order.** A denied requester with an invalid order (TC06S-46) checks the order of the checks of
  Q4.
- **State of a need.** pending → booked → pending with the same need, and the two repeated changes (TC06S-36 to
  TC06S-38).

## 3. Coverage (white box)

Measured with JaCoCo in `mvn -B verify` (D-033: 80 % of lines, 70 % of branches of the bundle): `ClinicalOrderService`
111 of 113 lines and 44 of 44 branches. The constructor that uses the system clock (3 lines, missed before) is run by
TC06S-55. The 2 lines still missed are the calls to `deny` in `checkPermission` and `checkSchedulingPermission`: they
run in every denial case (TC06S-02, TC06S-03…), but `deny` always throws, and JaCoCo does not mark as covered a line
whose call never returns.

**Decision coverage** (each decision true and false):

| Decision | True | False |
|---|---|---|
| `!hasPermission(requester, ORDER_PROCEDURES)` in `checkPermission` | TC06S-02, TC06S-03, TC06S-45, TC06S-46 | TC06S-01, TC06S-28 |
| `!has(ORDER_PROCEDURES) && !has(MANAGE_APPOINTMENTS)` in `checkSchedulingPermission` | TC06S-24 | TC06S-22, TC06S-23, TC06S-47 |
| `!errors.isEmpty()` | TC06S-04, TC06S-13 to TC06S-21, TC06S-25 to TC06S-27 | TC06S-01 |
| `order == null` | TC06S-04 | TC06S-05 |
| `blank(patientId)` in `checkPatient` / in `getOrdersOfPatient` | TC06S-13 / TC06S-31 | TC06S-05 / TC06S-28 |
| `!patient.isActive()` | TC06S-15 | TC06S-05 |
| `catch (ValidationException)` of C3 / of C2 | TC06S-14 / TC06S-17 | TC06S-05 |
| `blank(testTypeId)` / `!testType.isActive()` | TC06S-16 / TC06S-18 | TC06S-05 |
| `blank(priorityId)` | TC06S-19 | TC06S-05 |
| `priorityId.equals(configured.getId())` (filter) | TC06S-10 | TC06S-20 (every priority) |
| `priority == null` | TC06S-20 | TC06S-10, TC06S-21 |
| `!priority.isActive()` | TC06S-21 | TC06S-10 |
| `blank(indication)` / `blank(justification)` | TC06S-25 | TC06S-05, TC06S-52 |
| `order == null` in `getOrder` | TC06S-33 | TC06S-32 |
| `need == null` in `changeStatus` | TC06S-39, TC06S-50, TC06S-51 | TC06S-36, TC06S-37 |
| `need.getStatus() == status` | TC06S-38 | TC06S-36, TC06S-37 |
| `orders.findById(id) != null` / `needs.findById(id) != null` (counters) | TC06S-09 | TC06S-01, TC06S-07 |

**Condition/decision and MC/DC** of the compound decisions (Java evaluates them with short circuit; "–" = not
evaluated). A = `hasPermission(requester, ORDER_PROCEDURES)`, B = `hasPermission(requester, MANAGE_APPOINTMENTS)`:

| Decision | A / first condition | B / second condition | Result | Case |
|---|---|---|---|---|
| `!A && !B` (`checkSchedulingPermission`) | A true (`!A` false) | – | accepted | TC06S-22, TC06S-47 with `mgarcia` |
| | A false | B true | accepted | TC06S-23, TC06S-47 with `lmartin` |
| | A false | B false | denied | TC06S-24 with `admin` |
| `text == null \|\| text.isBlank()` (`blank`) | `null` | – | blank | TC06S-13, TC06S-16, TC06S-19, TC06S-25, TC06S-31 with `null` |
| | not `null` | `""` or `"   "` (blank) | blank | the same cases with `""`, `"   "` |
| | not `null` | not blank | not blank | TC06S-05, TC06S-52 |

MC/DC of `!A && !B`: A alone changes the result between `mgarcia` (accepted) and `admin` (denied), with B false in
both; B alone changes it between `lmartin` (accepted) and `admin` (denied), with A false in both. MC/DC of `blank`: the
first condition alone between `null` (blank) and a valid text; the second alone between `""` / `"   "` (blank) and a
valid text, with the first false in both. `pnavarro` makes A false although he has `ORDER_PROCEDURES`, because C1
answers `false` for an inactive user. The other decisions (`priority == null`, `!priority.isActive()`, the
`isActive()` checks) have a single condition, so condition/decision and MC/DC are the same as decision coverage.

## 4. Test cases

All the tests are in `server/c4-orders/src/test/java/es/uclm/esi/iso2/server/orders/`. EC = equivalence class, BV =
boundary value, EG = error guessing. Every exception case also checks that the requester is in the
`AccessDeniedException` and that the only audit entry is the denial.

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC06S-01 | `orderDiagnosticProcedure` | EC: `mgarcia`, valid order | `ORD-0001`, `mgarcia`, 23/11/2026, saved; audit only `ORDER_CREATED` on `ORDER:ORD-0001` (Q6, Q7) | `physicianCreatesTheOrder` |
| TC06S-02 | `orderDiagnosticProcedure` | EC: `lmartin`, `admin`, `jfernandez`, valid order | `AccessDeniedException`; `ORDER_ACCESS_DENIED` on `orders`; C2 and C3 not called; no order, no need (Q4, Q7, Q12) | `activeUserWithoutPermissionIsDenied` |
| TC06S-03 | `orderDiagnosticProcedure` | EC/BV: `pnavarro`, `nobody`, `null`, `""` | The same denial; nothing saved | `inactiveUnknownOrNullRequesterIsDenied` |
| TC06S-04 | `orderDiagnosticProcedure` | BV: `mgarcia`, `null` order | `ValidationException` with only "The order is required" (Q4) | `nullOrder` |
| TC06S-05 | `orderDiagnosticProcedure` | EC: valid order | `ORD-0001`; patient, test type, priority, indication, justification as sent; `mgarcia`; 23/11/2026 (Q4, Q6) | `orderGetsItsOwnData` |
| TC06S-06 | `orderDiagnosticProcedure` | EG: valid order with `ORD-0099`, `admin`, 01/01/2000 | Ignored: `ORD-0001`, `mgarcia`, 23/11/2026 (Q4) | `clientDataIsIgnored` |
| TC06S-07 | `orderDiagnosticProcedure` | EC: a second valid order | `ORD-0002` (Q6) | `secondOrderGetsTheNextId` |
| TC06S-08 | `orderDiagnosticProcedure` | EC: valid order | One need `NEED-0001`, `ORD-0001`, `PENDING`, due 23/11/2026 (Q6) | `orderCreatesOnePendingNeed` |
| TC06S-09 | `orderDiagnosticProcedure` | BV: repositories that already have `ORD-0001` and `NEED-0001` | `ORD-0002` and `NEED-0002` (Q6) | `idsSkipTheExistingOnes` |
| TC06S-10 | `orderDiagnosticProcedure` | EC: `URGENT`, `PREFERENTIAL`, `ROUTINE` | Accepted with that `priorityId` (Q4) | `everyActivePriorityIsAccepted` |
| TC06S-11 | `orderDiagnosticProcedure` | EG: indication and justification with spaces at the ends | Accepted and saved as sent. **Error guessing, pending Requirements (plan open question 6)**: the code keeps the spaces, C3 removes them | `textsWithSpacesAreAccepted` |
| TC06S-12 | `orderDiagnosticProcedure` | EC: valid order | C4 registers only `ORDER_CREATED` on `ORDER:ORD-0001`; C3 is called once (C4 does not repeat `PATIENT_ACCESSED`, D34 of AN-C4) | `createdOrderIsTheOnlyAuditEntry` |
| TC06S-13 | `orderDiagnosticProcedure` | EC/BV: patient `null`, `""`, `"   "` | "The patient is required" (Q4) | `blankPatient` |
| TC06S-14 | `orderDiagnosticProcedure` | EC: `P-9999` | "There is no patient with the id P-9999" (Q4) | `unknownPatient` |
| TC06S-15 | `orderDiagnosticProcedure` | EC: `P-0004` | "The patient P-0004 is not active" (Q4) | `inactivePatient` |
| TC06S-16 | `orderDiagnosticProcedure` | EC/BV: test type `null`, `""`, `"   "` | "The test type is required" (Q4) | `blankTestType` |
| TC06S-17 | `orderDiagnosticProcedure` | EC: `TT-99` | "There is no test type with the id TT-99" (Q4) | `unknownTestType` |
| TC06S-18 | `orderDiagnosticProcedure` | EC: `TT-03` | "The test type TT-03 is not active" (Q4) | `deactivatedTestType` |
| TC06S-19 | `orderDiagnosticProcedure` | EC/BV: priority `null`, `""`, `"   "` | "The priority is required" (Q4) | `blankPriority` |
| TC06S-20 | `orderDiagnosticProcedure` | EC/EG: `VIP`, `urgent` | "The priority X is not one of the configured priorities" (Q4) | `unknownPriority` |
| TC06S-21 | `orderDiagnosticProcedure` | EC: `OLD` | "The priority OLD is not active" (Q4) | `deactivatedPriority` |
| TC06S-22 | `getPendingSchedulingNeeds` | EC: `mgarcia`, after one order | One pending need (Q12) | `physicianReadsThePendingNeeds` |
| TC06S-23 | `getPendingSchedulingNeeds` | EC: `lmartin`, after one order | One pending need (Q12) | `admissionsStaffReadsThePendingNeeds` |
| TC06S-24 | the four operations of C5 | EC/BV: `admin`, `jfernandez`, `pnavarro`, `nobody`, `null`, `""` | `AccessDeniedException`; `ORDER_ACCESS_DENIED` on `orders`, `ORDER:ORD-0001`, `SCHEDULING_NEED:NEED-0001` (Q7, Q12) | `userWithoutSchedulingPermissionIsDenied` |
| TC06S-25 | `orderDiagnosticProcedure` | EC/BV: indication or justification `null`, `""`, `"   "` | "The clinical indication is required" / "The justification is required" (Q4, Q5) | `blankTexts` |
| TC06S-26 | `orderDiagnosticProcedure` | EG: every field wrong at once | The five messages together in the order of Q4 | `everyErrorTogether` |
| TC06S-27 | `orderDiagnosticProcedure` | EG: a rejected order, then a valid one | Nothing saved, no need, no audit; the valid one gets `ORD-0001` (Q4, Q6) | `rejectedOrderSavesNothing` |
| TC06S-28 | `getOrdersOfPatient` | EC: `mgarcia`, two orders of `P-0001` | `ORD-0001`, `ORD-0002` in that order (Q13) | `ordersOfAPatient` |
| TC06S-29 | `getOrdersOfPatient` | EC: `P-0002` without orders | Empty list; C3 not called (Q13) | `patientWithoutOrders` |
| TC06S-30 | `getOrdersOfPatient` | EC: `P-9999` | Empty list; C3 not called (Q13) | `unknownPatientGivesNoOrders` |
| TC06S-31 | `getOrdersOfPatient` | BV: `null`, `""`, `"   "` | "The patient is required" (Q13) | `blankPatientId` |
| TC06S-32 | `getOrder` | EC: `mgarcia`, `ORD-0001` | A `DiagnosticOrder` with `URGENT` (Q13) | `oneOrder` |
| TC06S-33 | `getOrder` | EC/BV: `ORD-9999`, `null` | "There is no order with the id X" (Q13) | `unknownOrder` |
| TC06S-34 | `getPendingSchedulingNeeds` | BV: no order | Empty list (Q13) | `noPendingNeeds` |
| TC06S-35 | `getPendingSchedulingNeeds` | EC: two orders, `lmartin` | `NEED-0001`, `NEED-0002` in that order (Q13) | `pendingNeedsInOrder` |
| TC06S-36 | `markSchedulingNeedBooked` | EC: `lmartin`, `NEED-0001` pending | `BOOKED`; not pending any more (Q13, Q14) | `bookingANeed` |
| TC06S-37 | `markSchedulingNeedPending` | EC: `lmartin`, `NEED-0001` booked | `PENDING` with the same `orderId` and `dueDate`, first of the pending ones (Q14) | `needMadePendingAgain` |
| TC06S-38 | both | EG: booked twice / made pending twice | "The scheduling need NEED-0001 is already booked" / "… already pending" (Q13) | `repeatedStatusChange` |
| TC06S-39 | `markSchedulingNeedBooked` | EC/BV: `NEED-9999`, `null` | "There is no scheduling need with the id X" (Q13) | `unknownNeed` |
| TC06S-40 | the four operations of C5 | EC: accepted calls | Nothing audited (Q7) | `c5OperationsAreNotAudited` |
| TC06S-41 | `DiagnosticOrder`, `SchedulingNeed` | EC: constructor and getters; order without id, requester and date | The values given; `null` where not given | `orderKeepsItsData`, `orderWithoutServerData`, `needKeepsItsData` |
| TC06S-42 | `SchedulingNeedStatus` | EC: `values()` | `PENDING`, `BOOKED` | `statusValues` |
| TC06S-43 | `InMemoryClinicalOrderRepository` | EC: unknown id; two patients; same id saved twice | `null`; only the orders of the patient; the second replaces the first | `unknownId`, `ordersOfOnePatient`, `saveReplaces` |
| TC06S-44 | `InMemorySchedulingNeedRepository` | EC: unknown id; pending and booked; same id saved twice | `null`; only the pending ones in the order of saving; replaced in its place | `unknownId`, `onlyPendingNeeds`, `saveReplacesAndKeepsThePlace` |
| TC06S-45 | `getOrdersOfPatient` | EC/BV: `lmartin`, `admin`, `jfernandez`, `pnavarro`, `nobody`, `null`, `""`; `P-0001` has an order | `AccessDeniedException`; only `ORDER_ACCESS_DENIED` on `orders` (Q7, Q12, Cases to cover 1) | `readingOrdersWithoutPermissionIsDenied` |
| TC06S-46 | `orderDiagnosticProcedure` | EG: `admin` with a `null` order, with `P-9999`, with a blank indication | `AccessDeniedException`, not `ValidationException`; only the denial on `orders` each time; C2 and C3 not called; nothing saved (Q4) | `permissionBeforeValidation` |
| TC06S-47 | `getOrder`, `markSchedulingNeedBooked`, `markSchedulingNeedPending` | EC (MC/DC of A and B): `mgarcia`, `lmartin`; one order | The order `ORD-0001`; `NEED-0001` booked, then pending; nothing audited (Q7, Q12) | `eitherPermissionIsAcceptedInC5Operations` |
| TC06S-48 | `getOrdersOfPatient` | EC: orders of `P-0001`, `P-0002`, `P-0001` | `ORD-0001`, `ORD-0003`: the order of `P-0002` is not returned (Cases to cover 5) | `ordersOfAnotherPatientAreExcluded` |
| TC06S-49 | `getOrdersOfPatient` | EC: `mgarcia`, `P-0001` with an order | Nothing audited (Q7) | `readingOrdersIsNotAudited` |
| TC06S-50 | `markSchedulingNeedPending` | EC/BV/EG: `NEED-9999`, `null`, `""`; `NEED-0001` exists | "There is no scheduling need with the id X" (Q13; `""` is an unknown id) | `unknownNeedCannotBeMadePending` |
| TC06S-51 | `markSchedulingNeedBooked` | EG: `""`; `NEED-0001` pending | "There is no scheduling need with the id " (Q13); `NEED-0001` still pending | `emptyNeedIdCannotBeBooked` |
| TC06S-52 | `orderDiagnosticProcedure` | BV: indication `a`, justification `b` (smallest not blank text) | Accepted with those texts (Q5) | `oneCharacterTextsAreAccepted` |
| TC06S-53 | `IClinicalOrderService` | EG: its methods, by reflection | The 6 operations of Q11; none starts with `update`, `edit`, `modify`, `cancel`, `delete` or `remove` (D-039, D-022) | `contractHasNoOperationToChangeAnOrder` |
| TC06S-54 | `orderDiagnosticProcedure` | EG: the order returned as `ORD-0001` is sent again | A new order `ORD-0002`; `ORD-0001` is the same object as before; two orders of `P-0001` (Q4, D-039) | `resentOrderCreatesANewOne` |
| TC06S-55 | constructor without `Clock`, `orderDiagnosticProcedure` | EC: service built with the system clock; `mgarcia`, valid order | `ORD-0001` dated today (the date of the system just before or just after the call, so that midnight does not make it fail); one pending need `NEED-0001` of `ORD-0001` due on that date (Q6) | `systemClockDatesTheOrderToday` |

Postcondition of every case: a denied or rejected call saves nothing and registers only `ORDER_ACCESS_DENIED` (denied)
or nothing (rejected); an order is never changed after it is created (D-039).

## 5. Not covered / Not defined

**Changed expected value (when UC07 joins, #205/#206):** TC06S-53 expected 6 operations (Q11 of UC06); UC07 adds
`orderFollowUp`, so on the branches that carry UC07 it expects 7 operations. The second assertion (no operation
changes an order, D-039) is the same.

- **Not defined, so not tested:** a maximum length of the indication and the justification (Q5 says none, pending
  Requirements: plan open question 2); whether the spaces at the ends are kept (TC06S-11 tests the code, pending open
  question 6); the `dueDate` of a need if Requirements ties it to the priority (Q6, TC06S-08 would change); whether a
  physician may read the orders of another physician; whether the lists returned can be modified by the caller; the
  seed data of C4 (`loadSeedData()`, Q8: not decided, the service starts empty); `ORDER_ACCESS_DENIED` instead of the
  `ACCESS_DENIED` of note N2 (plan open question 3: if Design changes it, every denial case changes).
- **`""` as a requester or a need id** is not named by the plan: it is classified as an unknown user (C1 answers
  `false`) and as an unknown need (no need has that id); the message of TC06S-51 is the one of Q13 with an empty `X`.
- **Not covered at the service level:** that C3 registers `PATIENT_ACCESSED` (Cases to cover 3) belongs to C3; the
  fake C3 does not audit, so TC06S-12 only checks that C4 calls C3 once and adds no entry of its own.
- **Other iterations:** `orderFollowUp` and `FollowUpOrder` (UC07, #71) and editing or cancelling an order (D-039).
