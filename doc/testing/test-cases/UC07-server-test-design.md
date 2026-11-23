# Test design – UC07 – Server (#71)

| | |
|---|---|
| **Use case** | #69 *UC07 – Order periodic follow-up procedures (C4 v1.1.0)* · FR07 |
| **Sub-issue** | #71 *UC07 – Server (C4 v1.1.0)* · branch `feature/UC07-server-tests` (from `feature/UC07-server`) |
| **Plan** | `doc/plans/UC07-server.md` (approved 23/11/2026) |
| **Specification** | flow of UC07 in `doc/AN-C4-DECISIONS.md:44-60`; D-040 (needs of the next 12 months); D-039 |
| **Designed by** | Ignacio Romero Torres, 23/11/2026 — for the review of Adam (Testing) |

## Test data
- **Clock:** fixed at 23/11/2026, so `creationDate` is 23/11/2026 and no case depends on the day it runs.
- **Users (C1):** `mgarcia` active physician with `ORDER_PROCEDURES`; `lmartin` active admissions staff with
  `MANAGE_APPOINTMENTS`; `admin` active with the permissions of C1 and C2 but not of C4; `jfernandez` active patient
  without permissions; `pnavarro` inactive physician with `ORDER_PROCEDURES`; `nobody` does not exist.
- **Patients (C3):** `P-0001`, `P-0002` and `P-0003` active; `P-0004` inactive; `P-9999` does not exist.
- **Test types (C2):** `TT-01` active and **not** eligible for follow-up; `TT-02` active and **eligible**; `TT-03`
  inactive and not eligible; `TT-99` does not exist.
- **Order of the valid case:** patient `P-0001`, test type `TT-02`, indication "Control of the nodule", justification
  "Follow-up of a known lesion", interval 6 months.

## Equivalence classes and boundary values
| Parameter | Classes / values | Cases |
|---|---|---|
| requester | active with `ORDER_PROCEDURES` · active without it · inactive · unknown · `null` | 01, 02, 03 |
| order | valid · `null` | 01, 04 |
| patient id | active · inactive · unknown · blank (`null`, `""`, `"   "`) | 01, 05-07 |
| test type id | active and eligible · active and not eligible · inactive · unknown · blank | 01, 08, 10, 11, 09 |
| recurrence interval | positive (12, 6, 5, 1) · 0 · negative | 01, 17-20, 12, 13 |
| indication / justification | filled · blank (`null`, `""`, `"   "`) | 01, 14, 15 |
| combinations | every error at once | 16 |

## Cases
| Case | What it checks | Expected result |
|---|---|---|
| TC07S-01 | The physician creates a valid follow-up order | `ORD-0001`, requester `mgarcia`, date 23/11/2026, interval 6, saved |
| TC07S-02 | An active user without `ORDER_PROCEDURES` tries it | `AccessDeniedException`, audit `ORDER_ACCESS_DENIED` on `orders`, C2 and C3 not called, nothing saved |
| TC07S-03 | An inactive, unknown or `null` requester tries it | `AccessDeniedException` and the audit entry, as TC07S-02 |
| TC07S-04 | The order is `null` | `ValidationException` with only `The order is required` |
| TC07S-05 | The patient id is blank | `The patient is required` |
| TC07S-06 | The patient does not exist | `There is no patient with the id P-9999` (message of C3) |
| TC07S-07 | The patient is inactive | `The patient P-0004 is not active` |
| TC07S-08 | The test type id is blank | `The test type is required` |
| TC07S-09 | The test type does not exist | `There is no test type with the id TT-99` (message of C2) |
| TC07S-10 | The test type is inactive | `The test type TT-03 is not active` (active is checked before eligible) |
| TC07S-11 | The test type is active but not eligible for follow-up | `The test type TT-01 is not eligible for follow-up` |
| TC07S-12 | The recurrence interval is 0 | `The recurrence interval must be a positive number of months` |
| TC07S-13 | The recurrence interval is negative | The same message as TC07S-12 |
| TC07S-14 | The clinical indication is blank | `The clinical indication is required` |
| TC07S-15 | The justification is blank | `The justification is required` |
| TC07S-16 | Every field is wrong at once | Every message, in the order of the plan: patient, test type, recurrence, indication, justification |
| TC07S-17 | Interval 6 (D-040) | Two pending needs, `NEED-0001` due 23/05/2027 and `NEED-0002` due 23/11/2027, both of `ORD-0001` |
| TC07S-18 | Interval 12 | One pending need, due 23/11/2027 |
| TC07S-19 | Interval 5 | Two pending needs, due at 5 and 10 months |
| TC07S-20 | Interval 1 | Twelve pending needs, from 1 to 12 months |
| TC07S-21 | The audit of a created order | Exactly one entry `FOLLOW_UP_ORDER_CREATED` on `ORDER:ORD-0001` |
| TC07S-22 | The ids of the follow-up orders | A diagnostic order takes `ORD-0001` and the follow-up takes `ORD-0002` (shared counter) |
| TC07S-23 | The reads of C4 | `getOrdersOfPatient` returns the diagnostic and the follow-up order; the follow-up has its interval |
| TC07S-24 | A rejected order does not use ids | Nothing saved, no need, no audit entry, and the next valid order is `ORD-0001` |
| TC07S-25 | The data sent by the client is ignored | The `orderId`, the `requester` and the `creationDate` of the request are replaced by the server |
| TC07S-26 | The needs are pending for C5 | `getPendingSchedulingNeeds` returns every need of the follow-up order |

## Map to the coverage of D-033
- Every line of `orderFollowUp`, `validateFollowUp`, `checkFollowUpTestType` and `createFollowUpSchedulingNeeds` is
  covered; the only branches left out would be the ones of an `AccessDeniedException` thrown by C2 or C3, which cannot
  happen with `ORDER_PROCEDURES` (#174, #175), as in UC06.
- The data class `FollowUpOrder` is covered by TC07S-01 and TC07S-23 (constructor and getter).

## Not covered here
- The client of UC07 (#70): its tests belong to its own plan.
- The 44 cases of UC06 are in `doc/testing/test-cases/UC06-server-test-design.md` and travel with the branch
  `feature/UC07-server-tests`, which includes them.
