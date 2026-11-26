# Test design · UC15 – Client (C5 v1.1.0) · #133 – Manage waitlists, priority tiers and calendar openings

Cases approved by Javier Villanueva Crespo (26/11/2026) following lesson T.04 (Testing Management): inputs,
equivalence classes, boundary values and error guessing. Expected results come from #133, `doc/plans/UC15-client.md`
(cases and Q5 to Q8), design note N2 of `doc/design/C5-client-design.puml` and D-022; where the plan gives no text, the
text is the one of the screen.

**Test data (precondition of every case).** The screen is built for `lmartin`, never shown, with a clock at 30/11/2026
and three hand-written fakes: `FakeParameterService` (test types `XRAY-CHEST`, `MRI-KNEE` and `CT-HEAD`; tiers
`URGENT` and `ROUTINE`), `FakeAppointmentService` (two pending needs) and `FakeWaitlistService` (two entries of
`XRAY-CHEST`, none of the others, two openings on 30/11/2026, proposal `WL-0002`). The test answers every question.

## 1. Parameters and testing values

| Input | Equivalence classes | Testing values |
|---|---|---|
| Server answer | Allows / rejects / denies / fails (**Conjeture**) | usual data, two errors, denied, `IllegalStateException` |
| Waitlist | Empty / with entries | `MRI-KNEE`, `XRAY-CHEST` |
| Selection | Complete / missing | need, entry, opening or none |
| Question | Accepted / refused | yes, no |
| Day | Valid with 2, 1 or 0 openings (**Limit Val.**) / blank / badly written | 30/11/2026, 01/12/2026, empty, `2026-11-30`, `32/11/2026` |
| Proposal | Entry / nobody | `WL-0002`, `null` |

## 2. Combination of the values

Each use: one value that is not the usual one at a time. TC15C-28 and TC15C-29 repeat one answer over the six actions.

## 3. Test cases

The tests are in `IU_ManageWaitlistTest` (`client-c5-appointments`) and, for TC15C-32 and 33, in
`IU_WelcomeManageWaitlistTest` (`client-c1-access`). The case, its situation and its expected result are those of the
table of `doc/plans/UC15-client.md`; each test method names its case.

| ID | Situation | Expected result |
|---|---|---|
| TC15C-01 | open with data | test types, tiers, pending needs, waitlist of the first test type and today |
| TC15C-02 | empty waitlist | notice of the empty list |
| TC15C-03 | from an empty list to one with entries | entries and no notice |
| TC15C-04 | list with several entries | the order of the service |
| TC15C-05 | open and change the test type | one query each time |
| TC15C-06 | no permission when opening | message, actions disabled, *Back* enabled |
| TC15C-07 | the reading is rejected | its errors |
| TC15C-08 | the reading fails | the error |
| TC15C-09 | the list fails after changing the test type | empty table and error |
| TC15C-10 | valid add | confirmation and lists reloaded |
| TC15C-11 | add without a selected need | error, nothing sent |
| TC15C-12 | add rejected with two errors | both shown |
| TC15C-13 | change the tier | confirmation |
| TC15C-14 | change without a selected entry | error, nothing sent |
| TC15C-15 | remove and accept | removed and confirmation |
| TC15C-16 | remove and refuse | nothing sent |
| TC15C-17 | remove without a selected entry | error |
| TC15C-18 | openings of a valid day | listed, *1 opening* or *2 openings* |
| TC15C-19 | day without openings | notice |
| TC15C-20 | blank or badly written day | error, openings emptied, nothing sent |
| TC15C-21 | propose | row selected and message with the patient |
| TC15C-22 | empty proposal | notice, no row selected |
| TC15C-23 | propose without an opening | error |
| TC15C-24 | fill and accept | appointment confirmed, openings emptied, lists reloaded |
| TC15C-25 | fill and refuse | nothing sent |
| TC15C-26 | fill without an entry or an opening | error |
| TC15C-27 | fill rejected | reason shown, the entry stays |
| TC15C-28 | permission denied in an action | message and actions disabled |
| TC15C-29 | unexpected failure in each action | the error |
| TC15C-30 | change done and reload failed | error, no success message |
| TC15C-31 | *Back* | goes back |
| TC15C-32 | welcome screen with the permission | button *Manage waitlist* (Alt+W) that opens the screen |
| TC15C-33 | without the permission or the action | not shown |
| TC15C-34 | access keys | none repeated |

## 4. Coverage and open results

Measured on 26/11/2026, before the fixes of the plan: `IU_ManageWaitlist` 339/364 lines and 65/80 branches;
`client-c5-appointments` 94.3 % of lines and 84.5 % of branches; `client-c1-access` 84.4 % and 85.8 %.

Red until step 6 of the plan, because the screen still has the defects A to E: TC15C-03, 05, 09, 22 and 34.
