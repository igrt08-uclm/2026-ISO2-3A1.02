# Test design · UC06 – Server (C4 v1.0.0) · #68 – Order a diagnostic procedure

**Status: proposal.** Designed by the Implementation side following lesson T.04 (Testing Management): parameters of
each method, equivalence classes, boundary values and error guessing, combination of the values, and coverage of the
code. It has to be approved by Testing (Adam) before the tests are written; the expected results come from #66, #68,
section 6 of #65, the contract `IClinicalOrderService`, `doc/design/C4-server-design.puml` and
`doc/design/common/C4-common.puml` (pull request #201) and the answers Q4 to Q14 of `doc/plans/UC06-server.md`.

Code under test (`server-c4-orders` 1.0.0-SNAPSHOT, branch `feature/UC06-server`, commit `92d38bb`):
`ClinicalOrderService` (`orderDiagnosticProcedure`, `getOrdersOfPatient`, `getPendingSchedulingNeeds`, `getOrder`,
`markSchedulingNeedBooked`, `markSchedulingNeedPending`, the constructors and `checkPermission` /
`checkSchedulingPermission`), `InMemoryClinicalOrderRepository` (`findById`, `findByPatient`, `save`),
`InMemorySchedulingNeedRepository` (`findById`, `findPending`, `save`) and the data classes of C4 in `common`
(`ClinicalOrder`, `DiagnosticOrder`, `SchedulingNeed`, `SchedulingNeedStatus`).

## 1. Test data (precondition of every case)

A fake `IRoleManagementService` with the seed users of C1 1.2.4 and the permissions that #173 gives them:

| User | Active | Permissions |
|---|---|---|
| `mgarcia` | yes | `ORDER_PROCEDURES` (physician) |
| `lmartin` | yes | `MANAGE_APPOINTMENTS` (admissions staff) |
| `pnavarro` | **no** | `ORDER_PROCEDURES` (physician, deactivated) |
| `admin` | yes | `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` |
| `jfernandez` | yes | none (patient) |
| `nobody` | — (does not exist) | — |

A recording `IAuditRecorder` keeps every entry (author, action, resource). A fake `IPatientService` with the three
active patients of C3 1.0.1 (`P-0001` Lucía Ejemplo Prueba, `P-0002` Mario Demo Simulado, `P-0003` Elena Muestra
Ficticia) plus one inactive patient `P-0004`, because the real C3 has no discharge yet (D-024). A fake
`ISystemParameterService` with the catalogue of C2 1.1.2: active test types `TT-01` (Chest X-ray) and `TT-02` (Blood
test), a deactivated `TT-03`, and the active priorities `URGENT`, `PREFERENTIAL` and `ROUTINE` plus a deactivated
`OLD`. Every "unknown" id does not exist in those fakes. Unless a case says otherwise, the clock is
`Clock.fixed(2026-11-23T10:00:00Z, UTC)`, so the creation date of the orders is **23/11/2026**, and the service is built
with empty repositories. The valid order of the tests is `P-0001` / `TT-01` / `URGENT`, indication "Chest pain" and
justification "Rule out pneumonia".

## 2. Parameters and testing values

### 2.1 `orderDiagnosticProcedure(requester, order)`

| Parameter | Equivalence class | Testing values |
|---|---|---|
| `requester` | With `ORDER_PROCEDURES` | `mgarcia` |
| | Without it (active) | `lmartin`, `admin`, `jfernandez` |
| | Without it (inactive) | `pnavarro` |
| | Unknown | `nobody`, `null` |
| `order` | Valid | the valid order; also with `orderId`, `requester` and `creationDate` filled in (they are ignored) |
| | `null` | `null` (Limit Val.) |
| `order.patientId` | Valid and active | `P-0001` |
| | Blank | `null`, `""` (Limit Val.), `"   "` |
| | Inactive | `P-0004` |
| | Unknown | `P-9999` |
| `order.testTypeId` | Valid and active | `TT-01` |
| | Blank | `null`, `""`, `"   "` |
| | Deactivated | `TT-03` |
| | Unknown | `TT-99` |
| `order.priorityId` | Valid and active | `URGENT`, `PREFERENTIAL`, `ROUTINE` |
| | Blank | `null`, `""`, `"   "` |
| | Deactivated | `OLD` |
| | Not configured | `VIP`, `urgent` (case) |
| `order.indication` | Not blank | `Chest pain`; with spaces at the ends |
| | Blank | `null`, `""`, `"   "` |
| `order.justification` | Not blank | `Rule out pneumonia`; with spaces at the ends |
| | Blank | `null`, `""`, `"   "` |

Error guessing: an inactive patient with a valid test type; a valid patient with a deactivated test type; every
invalid class at once (to check that all the errors come together and in order); two orders in a row (the second gets
the next id); a repository that already has `ORD-0001` and `NEED-0001`.

### 2.2 The reads and the operations of C5

| Method | Parameter | Equivalence class | Testing values |
|---|---|---|---|
| `getOrdersOfPatient(requester, patientId)` | `patientId` | With orders / without orders / unknown | `P-0001`, `P-0002` (none), `P-9999` |
| | | Blank | `null`, `""`, `"   "` |
| `getOrder(requester, orderId)` | `orderId` | Existing / unknown / blank | `ORD-0001`, `ORD-9999`, `null` |
| `getPendingSchedulingNeeds(requester)` | — | Empty / with pending needs / with booked ones | start, after one order, after booking |
| `markSchedulingNeedBooked` / `markSchedulingNeedPending` | `needId` | Pending → booked; booked → pending; already booked; already pending; unknown / blank | `NEED-0001`, `NEED-9999`, `null` |

## 3. Combination of the values

- **Requester × method:** the six users are tried on the two groups of permissions: only `ORDER_PROCEDURES`
  (`orderDiagnosticProcedure`, `getOrdersOfPatient`) and `ORDER_PROCEDURES` or `MANAGE_APPOINTMENTS`
  (`getPendingSchedulingNeeds`, `getOrder`, `markSchedulingNeedBooked`, `markSchedulingNeedPending`), so that
  `lmartin` is accepted in the second group and denied in the first.
- **Validation:** each invalid class alone, with the rest of the order valid, and then all of them together, to check
  that the messages come together and in the order that Q4 fixes.
- **State of the needs:** pending → booked → pending again with the same `needId`, `orderId` and `dueDate`, and the
  two error branches (already booked, already pending).

## 4. Coverage to reach (D-033: 80 % of lines, 70 % of branches in `server-c4-orders`)

| Decision | True with | False with |
|---|---|---|
| `hasPermission(requester, ORDER_PROCEDURES)` in `checkPermission` | TC06S-01 | TC06S-02 to TC06S-03 |
| `hasPermission(..., ORDER_PROCEDURES) \|\| hasPermission(..., MANAGE_APPOINTMENTS)` | TC06S-22 (with `mgarcia`), TC06S-23 (with `lmartin`) | TC06S-24 |
| `errors.isEmpty()` in `orderDiagnosticProcedure` | TC06S-01 | TC06S-05 to TC06S-21 |
| `order == null` | TC06S-04 | TC06S-05 |
| `blank(patientId)` / `blank(testTypeId)` / `blank(priorityId)` / `blank(indication)` / `blank(justification)` | one case each | the valid order |
| `!patient.isActive()` / `!testType.isActive()` / `!priority.isActive()` | inactive cases | active cases |
| `priority == null` (not configured) | TC06S-14 | TC06S-13 |
| `need == null` / `need.getStatus() == status` in `changeStatus` | unknown and repeated cases | the two valid changes |
| `orders.findById(id) != null` / `needs.findById(id) != null` in the counters | repositories with `ORD-0001` and `NEED-0001` | the first order |

## 5. Test cases

### 5.1 Permission (security)

| ID | Technique | Input | Expected result | Source |
|---|---|---|---|---|
| TC06S-01 | Equivalence class | `mgarcia` + valid order | Order created; `ORDER_CREATED` on `ORDER:ORD-0001` | Q7, Q12 |
| TC06S-02 | Equivalence class | `lmartin`, `admin`, `jfernandez` | `AccessDeniedException`; `ORDER_ACCESS_DENIED` on `orders`; C2 and C3 not called; nothing saved | Q12, Q7 |
| TC06S-03 | Equivalence class | `pnavarro`, `nobody`, `null` | The same denial | Q12 |
| TC06S-22 | Equivalence class | `getPendingSchedulingNeeds` with `mgarcia` | The pending needs | Q12, Q13 |
| TC06S-23 | Equivalence class | `getPendingSchedulingNeeds` with `lmartin` | The pending needs | Q12, Q13 |
| TC06S-24 | Equivalence class | `getPendingSchedulingNeeds`, `getOrder`, `markSchedulingNeedBooked`, `markSchedulingNeedPending` with `admin`, `jfernandez`, `pnavarro`, `nobody`, `null` | `AccessDeniedException`; `ORDER_ACCESS_DENIED` on `orders`, `ORDER:<id>` or `SCHEDULING_NEED:<id>` | Q7, Q12 |

### 5.2 Creating a valid order

| ID | Technique | Input | Expected result | Source |
|---|---|---|---|---|
| TC06S-05 | Equivalence class | Valid order | `ORD-0001`; `requester` = `mgarcia`; `creationDate` = 23/11/2026; the fields as sent; saved | Q4, Q6 |
| TC06S-06 | Error guessing | Valid order with `orderId` `ORD-0099`, `requester` `admin` and `creationDate` 01/01/2000 | They are ignored: `ORD-0001`, `mgarcia`, 23/11/2026 | Q4, Q6 |
| TC06S-07 | Equivalence class | Second valid order | `ORD-0002` | Q6 |
| TC06S-08 | Equivalence class | Valid order | One need `NEED-0001`, `PENDING`, `orderId` `ORD-0001`, `dueDate` 23/11/2026 | Q6, D33 |
| TC06S-09 | Boundary value | `P-0001` with an order saved in a repository that already has `ORD-0001` and `NEED-0001` | `ORD-0002` and `NEED-0002` | Q6 |
| TC06S-10 | Equivalence class | `URGENT`, `PREFERENTIAL`, `ROUTINE` | Accepted, with its `priorityId` | Q4 |
| TC06S-11 | Boundary value | Indication and justification with spaces at the ends | Accepted; saved as sent | Q5 |
| TC06S-12 | Equivalence class | Valid order | C4 registers only `ORDER_CREATED` on `ORDER:ORD-0001`: it does not repeat the `PATIENT_ACCESSED` that C3 registers itself (the fake of C3 does not audit, so the case checks that C4 adds no `PATIENT_*` entry) | Q7, D34 |

### 5.3 Rejected orders (validation)

| ID | Technique | Input | Expected result | Source |
|---|---|---|---|---|
| TC06S-04 | Boundary value | `null` order | `ValidationException` with only "The order is required" | Q4 |
| TC06S-13 | Equivalence class | `patientId` `null`, `""`, `"   "` | "The patient is required" | Q4 |
| TC06S-14 | Equivalence class | `P-9999` | The message of C3: "There is no patient with the id P-9999" | Q4 |
| TC06S-15 | Equivalence class | `P-0004` (inactive) | "The patient P-0004 is not active" | Q4 |
| TC06S-16 | Equivalence class | `testTypeId` `null`, `""`, `"   "` | "The test type is required" | Q4 |
| TC06S-17 | Equivalence class | `TT-99` | "There is no test type with the id TT-99" | Q4 |
| TC06S-18 | Equivalence class | `TT-03` (deactivated) | "The test type TT-03 is not active" | Q4 |
| TC06S-19 | Equivalence class | `priorityId` `null`, `""`, `"   "` | "The priority is required" | Q4 |
| TC06S-20 | Equivalence class | `VIP`, `urgent` | "The priority VIP is not one of the configured priorities" | Q4 |
| TC06S-21 | Equivalence class | `OLD` (deactivated) | "The priority OLD is not active" | Q4 |
| TC06S-25 | Equivalence class | `null`, `""`, `"   "` indication / justification | "The clinical indication is required" / "The justification is required" | Q5 |
| TC06S-26 | Error guessing | Every invalid class at once | All the messages, in the order of Q4: patient, test type, priority, indication, justification | Q4 |
| TC06S-27 | Error guessing | Any rejected order | Nothing saved, no need created, no `ORDER_CREATED`; the next valid order gets `ORD-0001` | Q4, Q7 |

### 5.4 Reading the orders

| ID | Technique | Input | Expected result | Source |
|---|---|---|---|---|
| TC06S-28 | Equivalence class | `getOrdersOfPatient(mgarcia, P-0001)` with two orders of `P-0001` | The two, in the order of creation | Q13 |
| TC06S-29 | Equivalence class | `P-0002` (no orders) | Empty list; C3 not called | Q13 |
| TC06S-30 | Equivalence class | `P-9999` | Empty list; C3 not called | Q13 |
| TC06S-31 | Boundary value | `null`, `""`, `"   "` | `ValidationException` "The patient is required" | Q13 |
| TC06S-32 | Equivalence class | `getOrder(mgarcia, ORD-0001)` | The order, a `DiagnosticOrder` with its `priorityId` | Q13 |
| TC06S-33 | Equivalence class | `ORD-9999`, `null` | "There is no order with the id X" | Q13 |

### 5.5 Scheduling needs (the side of C5)

| ID | Technique | Input | Expected result | Source |
|---|---|---|---|---|
| TC06S-34 | Equivalence class | `getPendingSchedulingNeeds` with no order | Empty list | Q13 |
| TC06S-35 | Equivalence class | After two orders | `NEED-0001` and `NEED-0002`, in that order | Q13 |
| TC06S-36 | Equivalence class | `markSchedulingNeedBooked(lmartin, NEED-0001)` | `BOOKED`; it disappears from the pending ones | Q13, Q14 |
| TC06S-37 | Equivalence class | Booked → `markSchedulingNeedPending` | `PENDING` again, with the same `needId`, `orderId` and `dueDate`, and in its first place | Q13, Q14 |
| TC06S-38 | Error guessing | Booking an already booked need / making pending an already pending one | "The scheduling need NEED-0001 is already booked" / "… already pending" | Q13 |
| TC06S-39 | Equivalence class | `NEED-9999`, `null` | "There is no scheduling need with the id X" | Q13 |
| TC06S-40 | Equivalence class | Accepted operations | Nothing is audited | Q7 |

### 5.6 Data classes and repositories (to reach the coverage of D-033)

| ID | Technique | Input | Expected result | Source |
|---|---|---|---|---|
| TC06S-41 | Equivalence class | `DiagnosticOrder` and `SchedulingNeed` getters | Return what the constructor received; `ClinicalOrder` keeps the common fields | design |
| TC06S-42 | Equivalence class | `SchedulingNeedStatus.values()` | `PENDING`, `BOOKED` | design |
| TC06S-43 | Equivalence class | `InMemoryClinicalOrderRepository`: `findById` unknown, `findByPatient`, `save` twice with the same id | `null`; the orders of that patient; the second replaces the first | D-030 |
| TC06S-44 | Equivalence class | `InMemorySchedulingNeedRepository`: `findById` unknown, `findPending`, `save` twice | `null`; only the pending ones; the second replaces the first and keeps its place | D-030 |

## 6. Covered acceptance criteria of #68

| Criterion | Cases |
|---|---|
| An order is only created for an active patient and an active test type, with a configured priority | TC06S-05, TC06S-10, TC06S-15, TC06S-18, TC06S-21, TC06S-26 |
| Every created order is a pending scheduling need for C5 | TC06S-08, TC06S-35 |
| The creation is registered in the audit log; a denial too | TC06S-01, TC06S-02, TC06S-03, TC06S-24 |
| The interfaces match the contract of #65 | TC06S-22 to TC06S-40 |

## 7. Findings while designing (to ask Testing, not tested as errors)

1. The plan fixes `dueDate` = creation date for a diagnostic order (Q6) and leaves it pending confirmation of
   Requirements; if it changes, TC06S-08 changes with it.
2. There is no `loadSeedData()` in C4 (Q8, open question), so the tests start with empty repositories.
3. `getOrdersOfPatient` of an unknown patient returns an empty list without calling C3 (Q13): that is what TC06S-30
   checks, not an error.
