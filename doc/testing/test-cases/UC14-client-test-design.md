# Test design · UC14 – Client (C5 v1.2.0) · #136 – Daily schedule and worklists screen

Cases approved by Javier Villanueva Crespo (26/11/2026) following lesson T.04 (Testing Management): parameters,
equivalence classes, boundary values and error guessing. Expected results come from #136, `doc/plans/UC14-client.md`
(cases, Q5, Q6 and Q8), D43 of `doc/AN-C5-DECISIONS.md` and note N3 of `doc/design/C5-client-design.puml`.

**Test data (precondition of every case).** `IU_DailySchedule` with `FakeDailyScheduleService`, the requester
`lmartin` and the clock `Clock.fixed(2026-11-24T08:00:00Z, UTC)`. The screen is built but never shown.

Messages that Design has not reviewed are checked by a keyword or by their ending, except the singular of the daily
schedule.

## 1. Parameters and testing values

| Parameter | Equivalence classes | Testing values |
|---|---|---|
| Day | Today / another / with spaces / badly written | `24/11/2026`, `25/11/2026`, `2026-11-24` |
| Number of appointments (**Limit Val.**) | None / one / several | 0, 1, 2 or 3 |
| Reading | Daily schedule / room / physician | the three buttons |
| Result of a reading | Correct / rejected / failed / denied | lines, two reasons, exception, access denied |
| Permission (welcome) | With / without | `MANAGE_APPOINTMENTS`, none |
| Actions (welcome) | Daily schedule given / not given | constructor of UC14, constructor of UC15 |

## 2. Combination of the values

Each use: one invalid value at a time with the rest valid. The invalid results (TC14C-12 to 15) are checked with each
of the three buttons.

## 3. Test cases

TC14C-01 to 16 are in `client/c5-appointments/src/test/java/.../presentation/IU_DailyScheduleTest.java`; TC14C-17 to 20
are in `client/c1-access/src/test/java/.../presentation/IU_WelcomeDailyScheduleTest.java`.

| ID | Situation | Expected result | Test |
|---|---|---|---|
| TC14C-01 | Open the screen | Today as the day, empty table, nothing read | `opens` |
| TC14C-02 | Schedule with two rooms | Six columns; rows grouped by room and in time order | `scheduleOfTwoRooms` |
| TC14C-03 | Schedule with one appointment | *1 appointment.* | `scheduleWithOneAppointment` |
| TC14C-04 | Day without appointments | Empty table and notice | `emptyDay` |
| TC14C-05 | Room worklist with two | Order of the service, *2 appointments* | `roomWorklist` |
| TC14C-06 | Room worklist with one | *1 appointment* | `roomWorklistWithOne` |
| TC14C-07 | Room worklist empty | Empty table and notice of the room | `emptyRoomWorklist` |
| TC14C-08 | Physician worklist with two | Order of the service, *2 appointments* | `physicianWorklist` |
| TC14C-09 | Physician worklist with one | *1 appointment* | `physicianWorklistWithOne` |
| TC14C-10 | Physician worklist empty | Empty table and notice of the physician | `emptyPhysicianWorklist` |
| TC14C-11 | Values with spaces around | Sent without them | `trimmed` |
| TC14C-12 | Badly written day, each button | Error, empty table, nothing sent | `badDay` |
| TC14C-13 | Rejected after a list | Both errors, empty table | `rejected` |
| TC14C-14 | Failed after a list | The error, empty table | `failed` |
| TC14C-15 | Denied after a list | Message, empty table, only *Back* enabled | `denied` |
| TC14C-16 | *Back* | Its action runs | `back` |
| TC14C-17 | Welcome with the permission | *Daily schedule* (Alt+D) runs its action | `shownWithThePermission` |
| TC14C-18 | Welcome without the permission | No button | `notShownWithoutThePermission` |
| TC14C-19 | Welcome with both actions | *Manage waitlist* and *Daily schedule* | `shownWithTheWaitlist` |
| TC14C-20 | Constructor of UC15 | *Manage waitlist* and no *Daily schedule* | `constructorWithoutTheAction` |

## 4. Coverage and open results

Measured on 26/11/2026, before step 6 of the plan: `IU_DailySchedule` 156/170 lines and 20/20 branches;
`client-c5-appointments` 94.0 % of lines and 86.0 % of branches; `client-c1-access` 83.6 % and 86.4 %.

Red before step 6 of the plan: TC14C-06, 09, 13, 14 and 15.

After step 6 (`cbf25b1`), all green: `IU_DailySchedule` 166/168 lines and 20/20 branches; `client-c5-appointments` 95.5 % of
lines and 86.0 % of branches.
