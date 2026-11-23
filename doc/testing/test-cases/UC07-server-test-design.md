# Test design · UC07 – Server (C4 v1.1.0) · #71 – Order periodic follow-up procedures

Designed by Testing (Adam Gouram) following lesson T.04 (Testing Management): parameters of each method, equivalence
classes, boundary values and error guessing, combination of the values, and coverage of the code. Expected results
come from #69, #71, the answers Q2 to Q7 of `doc/plans/UC07-server.md` (and its *State for Adam*), the flow of UC07 in
`doc/AN-C4-DECISIONS.md:44-60`, D-039 and D-040; nothing that those sources do not define is used as an expected
result (section 5).

Code under test (`server-c4-orders` 1.1.0-SNAPSHOT, commit `b6d1e74` of `feature/UC07-server`): the operation added by
UC07, `IClinicalOrderService.orderFollowUp(String requester, FollowUpOrder order)`, implemented by
`ClinicalOrderService.orderFollowUp` with the private `validateFollowUp`, `checkFollowUpTestType` and
`createFollowUpSchedulingNeeds` (and the `checkPermission`, `checkPatient`, `blank`, `nextOrderId` and `nextNeedId` that
it shares with UC06), and the data class `FollowUpOrder` in `common` (`common` 1.7.0-SNAPSHOT). No other operation
changes (plan, *Decisions already taken*); the reads `getOrdersOfPatient` and `getPendingSchedulingNeeds` are used only
to observe the follow-up orders (Q7). The contract now has seven operations: TC06S-53
(`ClinicalOrderServiceTest.contractHasNoOperationToChangeAnOrder`, commit `d484736`) counts them and checks that none
edits or cancels an order, so D-039 still holds after UC07.

**Test data (precondition of every case).** The service is built with empty repositories, a recording
`IAuditRecorder` and the clock `Clock.fixed(2026-11-23T10:00:00Z, UTC)`, so every `creationDate` is 23/11/2026, except
in TC07S-30 (`2027-01-31T10:00:00Z`) and TC07S-31 (`2028-02-29T10:00:00Z`). The users are the seed users of C1 1.2.4
with the permissions of #173: `mgarcia` (active, `ORDER_PROCEDURES`), `lmartin` (active, `MANAGE_APPOINTMENTS`),
`admin` (active, `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS`), `jfernandez` (active, none), `pnavarro`
(**inactive**, `ORDER_PROCEDURES`); `nobody` does not exist. The fake C3 has the active patients `P-0001` to `P-0003`
and the inactive `P-0004`. The fake C2 has the test types `TT-01` (active, **not** eligible for follow-up), `TT-02`
(active, eligible) and `TT-03` (deactivated, not eligible). `P-0004` and the ids of the fake C2 catalogue are invented
test values, as in UC06. The valid order is `P-0001` / `TT-02`, indication "Control of the nodule", justification
"Follow-up of a known lesion", interval 6 months.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| `orderFollowUp` | `requester` | Active with `ORDER_PROCEDURES` | `mgarcia` |
| | | Active with `MANAGE_APPOINTMENTS` only (Q3: only `ORDER_PROCEDURES` is accepted) | `lmartin` |
| | | Active without either permission | `admin`, `jfernandez` |
| | | Inactive (with `ORDER_PROCEDURES`) | `pnavarro` |
| | | Unknown | `nobody`; `""` (Conjeture) |
| | | Not given | `null` (Limit Val.) |
| | `order` | Valid | the valid order; also with `orderId` `ORD-0099`, `requester` `admin`, `creationDate` 01/01/2000 (ignored, Q2) |
| | | Not given | `null` (Limit Val.) |
| | `order.patientId` | Active | `P-0001`, `P-0002` |
| | | Blank | `null` (Limit Val.), `""` (Limit Val.), `"   "` |
| | | Inactive | `P-0004` |
| | | Unknown | `P-9999` |
| | `order.testTypeId` | Active and eligible for follow-up | `TT-02` |
| | | Active, not eligible | `TT-01` |
| | | Inactive | `TT-03` |
| | | Unknown | `TT-99` |
| | | Blank | `null`, `""`, `"   "` |
| | priority | Not a parameter: `FollowUpOrder` has no priority (Q2, design of #201) | — (section 5) |
| | `order.recurrenceIntervalMonths` (`int`) | Not positive (invalid, Q3) | `0` (Limit Val.), `-1` (Conjeture, Limit Val. of the negatives), `-6` |
| | | Positive, divides 12 | `1` (Limit Val.), `3`, `4`, `6`, `12` (Limit Val.) |
| | | Positive, does not divide 12 | `5`, `11` (Limit Val.: last value with one need below 12) |
| | | Positive, greater than 12 | `13` (Limit Val.) — **Not defined** (section 5) |
| | `order.indication`, `order.justification` | Not blank | "Control of the nodule" / "Follow-up of a known lesion" |
| | | Blank | `null`, `""` (Limit Val.), `"   "` |
| | creation date (the `Clock` of the service, not a parameter of the call) | A day that exists in every month | 23/11/2026 |
| | | The last day of a 31-day month | 31/01/2027 (Limit Val.) |
| | | 29 February of a leap year | 29/02/2028 (Limit Val.) |
| | state | No orders / a diagnostic order before / a follow-up order before | start; after `orderDiagnosticProcedure`; after `orderFollowUp` |

**Outputs.** `orderFollowUp` returns the created `FollowUpOrder` (`ORD-nnnn` of the counter shared with the diagnostic
orders, the requester of the parameter, the date of the clock, the interval sent; Q2, Q5), saves it and creates one
`PENDING` need `NEED-nnnn` (counter shared with UC06) for every `k >= 1` with `k * interval <= 12`, due on
`creationDate.plusMonths(k * interval)` (Q4, D-040); it registers `FOLLOW_UP_ORDER_CREATED` on `ORDER:<orderId>` and
does not audit the needs (Q6). Exceptions: `AccessDeniedException` (with the requester), registered as
`ORDER_ACCESS_DENIED` on `orders`, before any call to C2 or C3 (Q3, Q6); `ValidationException` with every message of Q3
in the order order, patient, test type, interval, indication, justification; nothing is saved.

## 2. Combination of the values

- **Requester class (each use).** `orderFollowUp` is the only operation of UC07, so each requester class of section 1
  is used once with the valid order: `mgarcia` TC07S-01; `lmartin`, `admin`, `jfernandez` TC07S-02; `pnavarro`,
  `nobody`, `null`, `""` TC07S-03. 7 classes = 7 combinations, the maximum of this parameter.
- **Requester × invalid order.** A denied requester (`admin`) with a `null` order and with an order with every field
  wrong (TC07S-32) checks that the permission comes first (Q3).
- **Fields of the order (each use).** Each invalid class alone with the rest of the order valid (TC07S-04 to TC07S-15),
  then all at once (TC07S-16). The text fields have no maximum number of cases (any `String`): the classes of section
  1 bound them.
- **Interval × creation date.** Every interval value with 23/11/2026 (TC07S-17 to TC07S-20, TC07S-27 to TC07S-29), and
  the two month-end dates with the interval that reaches every month (1, TC07S-30) and the one that reaches the same
  day a year later (12, TC07S-31). The due dates are written as literal dates.
- **State (error guessing).** A diagnostic order, then a follow-up (TC07S-22, TC07S-23, TC07S-34); two follow-up orders
  in a row (TC07S-33); a rejected order, then a valid one (TC07S-24).

## 3. Coverage (white box)

Measured with JaCoCo in `mvn -B verify` (D-033: 80 % of lines, 70 % of branches of the bundle): `ClinicalOrderService`
152 of 157 lines and 62 of 62 branches, with the UC06 tests of the same module. The 5 missed lines are the constructor
that uses the system clock (not called: the tests need the fixed clock).

**Decision coverage** of the code of UC07 (each decision true and false):

| Decision | True | False |
|---|---|---|
| `!hasPermission(requester, ORDER_PROCEDURES)` in `checkPermission` | TC07S-02, TC07S-03, TC07S-32 | TC07S-01 |
| `!errors.isEmpty()` in `orderFollowUp` | TC07S-04 to TC07S-16 | TC07S-01 |
| `order == null` in `validateFollowUp` | TC07S-04 | TC07S-01 |
| `blank(patientId)` / `!patient.isActive()` / `catch` of C3 in `checkPatient` | TC07S-05 / TC07S-07 / TC07S-06 | TC07S-01 |
| `blank(testTypeId)` in `checkFollowUpTestType` | TC07S-08 | TC07S-01 |
| `catch (ValidationException)` of C2 | TC07S-09 | TC07S-01 |
| `!testType.isActive()` | TC07S-10 | TC07S-01, TC07S-11 |
| `!testType.isFollowUpEligible()` (only when active) | TC07S-11 | TC07S-01 |
| `getRecurrenceIntervalMonths() <= 0` | TC07S-12 (0), TC07S-13 (-1, -6) | TC07S-20 (1), TC07S-01 |
| `blank(indication)` / `blank(justification)` | TC07S-14 / TC07S-15 | TC07S-01 |
| `month <= 12` (loop of `createFollowUpSchedulingNeeds`) | every iteration of TC07S-17 to TC07S-20, TC07S-27 to TC07S-31 | after the last one: TC07S-18 (month 24), TC07S-29 (month 22), TC07S-20 (month 13) |
| `findById(id) != null` (counters, shared with UC06) | TC06S-09 | TC07S-01, TC07S-33, TC07S-34 |

Loop boundaries of `createFollowUpSchedulingNeeds`: one iteration (12, 11: TC07S-18, TC07S-29), two (6, 5), three (4),
four (3) and twelve (1: TC07S-20, TC07S-30), the maximum. Zero iterations happen only with an interval greater than 12
(13), whose expected result is not defined (section 5).

**Condition/decision and MC/DC** of the compound decisions (Java evaluates them with short circuit; "–" = not
evaluated). A = `hasPermission(requester, ORDER_PROCEDURES)`:

| Decision | First condition | Second condition | Result | Case |
|---|---|---|---|---|
| `text == null \|\| text.isBlank()` (`blank`, for patient, test type, indication, justification) | `null` | – | blank | TC07S-05, TC07S-08, TC07S-14, TC07S-15 with `null` |
| | not `null` | `""` or `"   "` | blank | the same cases with `""` and `"   "` |
| | not `null` | not blank | not blank | TC07S-01 |
| valid test type = `isActive()` and then `isFollowUpEligible()` (`if` / `else if`) | not active | – | "not active" | TC07S-10 (`TT-03`) |
| | active | not eligible | "not eligible for follow-up" | TC07S-11 (`TT-01`) |
| | active | eligible | accepted | TC07S-01 (`TT-02`) |

MC/DC of `blank`: the first condition alone changes the result between `null` (blank) and a valid text; the second
alone between `""` / `"   "` (blank) and a valid text, with the first false in both. MC/DC of the test type: `isActive`
alone changes the result between `TT-03` and `TT-02`; `isFollowUpEligible` alone between `TT-01` and `TT-02`, with
`isActive` true in both. `!A` has a single condition; `pnavarro` makes A false although he has `ORDER_PROCEDURES`,
because C1 answers `false` for an inactive user, and `lmartin` makes it false because `MANAGE_APPOINTMENTS` is not
accepted by `orderFollowUp` (Q3). The other decisions have a single condition, so condition/decision and MC/DC are the
same as decision coverage.

## 4. Test cases

All the tests are in `server/c4-orders/src/test/java/es/uclm/esi/iso2/server/orders/service/OrderFollowUpTest.java`,
except TC06S-53. EC = equivalence class, BV = boundary value, EG = error guessing. Every denial case also checks that
the requester is in the `AccessDeniedException` and that the only audit entry is the denial. The dates of the needs
are literal dates (D-040, Q4), never computed in the test.

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC07S-01 | `orderFollowUp` | EC: `mgarcia`, valid order | `ORD-0001`, `mgarcia`, 23/11/2026, interval 6, saved; audit only `FOLLOW_UP_ORDER_CREATED` on `ORDER:ORD-0001` (Q2, Q5, Q6) | `physicianCreatesTheOrder` |
| TC07S-02 | `orderFollowUp` | EC: `lmartin`, `admin`, `jfernandez`, valid order | `AccessDeniedException`; `ORDER_ACCESS_DENIED` on `orders`; C2 and C3 not called; no order, no need (Q3, Q6) | `activeUserWithoutPermissionIsDenied` |
| TC07S-03 | `orderFollowUp` | EC/BV: `pnavarro`, `nobody`, `null`, `""` | The same denial; nothing saved | `inactiveUnknownOrNullRequesterIsDenied` |
| TC07S-04 | `orderFollowUp` | BV: `mgarcia`, `null` order | `ValidationException` with only "The order is required" (Q3) | `nullOrder` |
| TC07S-05 | `orderFollowUp` | EC/BV: patient `null`, `""`, `"   "` | "The patient is required" (Q3) | `blankPatient` |
| TC07S-06 | `orderFollowUp` | EC: `P-9999` | "There is no patient with the id P-9999" (message of C3, Q3) | `unknownPatient` |
| TC07S-07 | `orderFollowUp` | EC: `P-0004` | "The patient P-0004 is not active" (Q3) | `inactivePatient` |
| TC07S-08 | `orderFollowUp` | EC/BV: test type `null`, `""`, `"   "` | "The test type is required" (Q3) | `blankTestType` |
| TC07S-09 | `orderFollowUp` | EC: `TT-99` | "There is no test type with the id TT-99" (message of C2, Q3) | `unknownTestType` |
| TC07S-10 | `orderFollowUp` | EC: `TT-03` | "The test type TT-03 is not active" (active is checked before eligible, Q3) | `inactiveTestType` |
| TC07S-11 | `orderFollowUp` | EC: `TT-01` (active, not eligible) | "The test type TT-01 is not eligible for follow-up" (Q3) | `notEligibleTestType` |
| TC07S-12 | `orderFollowUp` | BV: interval `0` | "The recurrence interval must be a positive number of months" (Q3) | `zeroInterval` |
| TC07S-13 | `orderFollowUp` | BV/EG: interval `-1` (Conjeture), `-6` | The same message as TC07S-12 (Q3) | `negativeInterval` |
| TC07S-14 | `orderFollowUp` | EC/BV: indication `null`, `""`, `"   "` | "The clinical indication is required" (Q3) | `blankIndication` |
| TC07S-15 | `orderFollowUp` | EC/BV: justification `null`, `""`, `"   "` | "The justification is required" (Q3) | `blankJustification` |
| TC07S-16 | `orderFollowUp` | EG: patient `null`, test type `""`, interval 0, indication `""`, justification `"  "` | The five messages together in the order of Q3: patient, test type, interval, indication, justification | `everyErrorTogether` |
| TC07S-17 | `orderFollowUp` | EC: interval 6, created 23/11/2026 | Two `PENDING` needs of `ORD-0001`: `NEED-0001` due 23/05/2027, `NEED-0002` due 23/11/2027 (Q4, D-040) | `sixMonths` |
| TC07S-18 | `orderFollowUp` | BV: interval 12 | One need due 23/11/2027 (Q4) | `twelveMonths` |
| TC07S-19 | `orderFollowUp` | EC: interval 5 | Two needs due 23/04/2027 and 23/09/2027 (Q4) | `fiveMonths` |
| TC07S-20 | `orderFollowUp` | BV: interval 1 | Twelve needs due 23/12/2026, 23/01/2027, 23/02/2027, 23/03/2027, 23/04/2027, 23/05/2027, 23/06/2027, 23/07/2027, 23/08/2027, 23/09/2027, 23/10/2027, 23/11/2027 (Q4) | `oneMonth` |
| TC07S-21 | `orderFollowUp` | EC: valid order | The only audit entry is `FOLLOW_UP_ORDER_CREATED` on `ORDER:ORD-0001`; no entry of C3 and no entry for the needs (Q6) | `createdOrderIsTheOnlyAuditEntry` |
| TC07S-22 | `orderFollowUp` | EG: a diagnostic order before | The follow-up order is `ORD-0002` (shared counter, Q5) | `sharedCounter` |
| TC07S-23 | `orderFollowUp`, `getOrdersOfPatient` | EC: a diagnostic and a follow-up order of `P-0001` | Both, a `DiagnosticOrder` and a `FollowUpOrder` with interval 6 (Q7) | `readsReturnBothOrders` |
| TC07S-24 | `orderFollowUp` | EG: a rejected order (`TT-01`), then a valid one | Nothing saved, no need, no audit; the valid one gets `ORD-0001` (Q3, Q5) | `rejectedOrderSavesNothing` |
| TC07S-25 | `orderFollowUp` | EG: valid order with `ORD-0099`, `admin`, 01/01/2000 | Ignored: `ORD-0001`, `mgarcia`, 23/11/2026 (Q2) | `clientDataIsIgnored` |
| TC07S-26 | `orderFollowUp`, `getPendingSchedulingNeeds` | EC: `lmartin` reads after a valid order | The two needs of the order (D-040: pending for C5) | `needsArePendingForC5` |
| TC07S-27 | `orderFollowUp` | EC: interval 3 | Four needs due 23/02/2027, 23/05/2027, 23/08/2027, 23/11/2027 (Q4) | `threeMonths` |
| TC07S-28 | `orderFollowUp` | EC: interval 4 | Three needs due 23/03/2027, 23/07/2027, 23/11/2027 (Q4) | `fourMonths` |
| TC07S-29 | `orderFollowUp` | BV: interval 11 | One need due 23/10/2027 (Q4) | `elevenMonths` |
| TC07S-30 | `orderFollowUp` | BV/EG: created 31/01/2027, interval 1 | Creation 31/01/2027; twelve needs due 28/02/2027, 31/03/2027, 30/04/2027, 31/05/2027, 30/06/2027, 31/07/2027, 31/08/2027, 30/09/2027, 31/10/2027, 30/11/2027, 31/12/2027, 31/01/2028 (Q4: `creationDate.plusMonths(k * n)`, each from the creation date, the last valid day when the month is shorter) | `monthEnd` |
| TC07S-31 | `orderFollowUp` | BV/EG: created 29/02/2028, interval 12 | Creation 29/02/2028; one need due 28/02/2029 (Q4) | `leapDay` |
| TC07S-32 | `orderFollowUp` | EG: `admin` with a `null` order, and with `P-9999` / `TT-01` / interval 0 / blank indication and justification | `AccessDeniedException`, not `ValidationException`; only the denial on `orders`; C2 and C3 not called; nothing saved (Q3) | `permissionBeforeValidation` |
| TC07S-33 | `orderFollowUp` | EG: interval 6 for `P-0001`, then interval 12 for `P-0002` | `NEED-0001`, `NEED-0002` of `ORD-0001` (23/05/2027, 23/11/2027) and `NEED-0003` of `ORD-0002` (23/11/2027) (Q4, Q5) | `twoFollowUpOrdersInARow` |
| TC07S-34 | `orderDiagnosticProcedure`, `orderFollowUp` | EG: a diagnostic order, then a follow-up with interval 6 | `NEED-0001` of `ORD-0001` due 23/11/2026 (UC06), `NEED-0002` and `NEED-0003` of `ORD-0002` due 23/05/2027 and 23/11/2027 (Q4: same counter as UC06) | `diagnosticAndFollowUpShareTheNeedCounter` |
| TC06S-53 | `IClinicalOrderService` | EG: its methods, by reflection | 7 operations after UC07 adds `orderFollowUp`; none starts with `update`, `edit`, `modify`, `cancel`, `delete` or `remove` (D-039, D-022) | `ClinicalOrderServiceTest.contractHasNoOperationToChangeAnOrder` |

Postcondition of every case: a denied call saves nothing and registers only `ORDER_ACCESS_DENIED`; a rejected call saves
nothing, creates no need and registers nothing; a created order is never changed afterwards and its needs stay
`PENDING` (D-039, D-040).

## 5. Not covered / Not defined

- **Interval greater than 12 months (13), Not defined:** D-040 and Q4 say that the needs of the next 12 months are
  generated, but not whether an interval of 13 is rejected, creates an order with no needs or creates one need. The
  code creates the order with no need (the loop does not run). No test asserts it: it is an open question of the team,
  and the case is added when Requirements answers it. A maximum interval is not defined either.
- **Priority of a follow-up order, Not defined:** `FollowUpOrder` has no priority (Q2) and the needs of C5 have none, so
  there is nothing to test; if Requirements adds one, it is a change of the contract.
- **Month-end dates:** they follow Q4 (`creationDate.plusMonths(k * n)` from the creation date, not from the previous
  need), so 31/01 + 1 month is 28/02 and 31/01 + 2 months is 31/03; if the team chooses another rule, TC07S-30 and
  TC07S-31 change.
- **`""` as a requester** is not named by the plan: it is classified as an unknown user (C1 answers `false`), as in UC06.
- **An `AccessDeniedException` thrown by C2 or C3** cannot happen with `ORDER_PROCEDURES` (#174, #175), so it is not
  tested, as in UC06.
- **Not covered here:** the constructor with the system clock (a test with it would depend on the day it is run); the
  client of UC07 (#70); the cases of UC06, which are in `UC06-server-test-design.md` of `feature/UC06-server-tests`; UC13 and UC15 of C5, which book
  the needs; editing or cancelling an order (D-039).
- **Defects found:** none; every expected result of section 4 passes against `b6d1e74`.
