# Plan – UC14 – Client (#136)

| | |
|---|---|
| **Sub-issue** | #136 · branch `feature/UC14-client` (branched from `feature/UC14-server`, because the screen needs its contract) |
| **Use case** | #135 · FR14 · C5 `1.2.0` |
| **Specification** | #135, #136, [doc/AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) (D43, D44) |
| **Design** | [doc/design/C5-client-design.puml](../design/C5-client-design.puml) (`IU_DailySchedule`, note N3) |
| **Plan approved by** | Ignacio Romero, 26/11/2026 (steps 1 to 3); steps 4 to 7 and the cases TC14C-01 to TC14C-20 approved by Javier Villanueva Crespo, 26/11/2026 |

## Questions and answers

- **Q1 – Tests in this branch?** **A:** No. The member asked for the implementation only (26/11/2026) and there is no test design table for UC14 (rule 17). They go in `feature/UC14-client-tests`, branched from this one. The steps are checked with `mvn -B -ntp verify -Djacoco.skip=true`; the pull request waits for the tests (D-033). *Replaced by Q4.*
- **Q2 – How is the screen reached?** **A:** With a new button <em>Daily schedule</em> (Alt+D) in `IU_Welcome`, shown only with `MANAGE_APPOINTMENTS`, after <em>Manage appointments</em>. The previous constructors pass `null`, so no other behaviour changes.
- **Q3 – Which repository does `Main` give to the two services?** **A:** The same `InMemoryAppointmentRepository` for `AppointmentService` (UC13) and `DailyScheduleService` (UC14), so the schedule shows the appointments that UC13 books.
- **Q4 – Who finishes the sub-issue?** **A:** Javier Villanueva Crespo takes over the code and the tests (26/11/2026). The tests go directly in `feature/UC14-client`, with the table below.
- **Q5 – Which defects are fixed?** **A:** (A) the table keeps the previous appointments when a reading fails or is denied, so it is emptied; (B) the list `entries` is never read, so it is removed; (C) the order of the imports and the line longer than 120 characters of `Main` (Javier Villanueva Crespo, 26/11/2026).
- **Q6 – Which texts change?** **A:** Only the singular of the two worklists (*1 appointment*, as the daily schedule already says). The other texts stay and Design is asked to confirm them in the pull request (Javier Villanueva Crespo, 26/11/2026).
- **Q7 – Versions.** **A:** `client-c5-appointments` `1.2.0-SNAPSHOT`; `client-c1-access` `1.2.9-SNAPSHOT`, because the button changes `IU_Welcome` (agreed with the Coordinator in #237, 26/11/2026).
- **Q8 – How are the two buttons of `IU_Welcome` merged?** **A:** UC15 and UC14 added the same constructor. The one with <em>Manage waitlist</em> stays and passes `null` to a new one that also receives <em>Daily schedule</em>, so no existing test changes. `Main` gives the shared repository to the `AppointmentService` that the waitlist also uses (Q3).
- **Q9 – Red tests.** **A:** The commit of the tests leaves red the cases of the defect A and of Q6 until step 6 fixes them. After step 4 the coverage of D-033 can fail until the tests arrive, so that step is checked with `-Djacoco.skip=true`.

## Test cases (S.04.01), approved by Javier Villanueva Crespo on 26/11/2026

Test data: the screen with a double of `IDailyScheduleService`, the requester `lmartin` and the day 24/11/2026.

| Case | Situation | Expected result |
|---|---|---|
| TC14C-01 | open the screen | today as the day, empty table, nothing read |
| TC14C-02 | schedule of a day with two rooms | rows grouped by room and in time order, with room, start, end, patient, test type and preparation |
| TC14C-03 | schedule with one appointment | *1 appointment.* |
| TC14C-04 | day without appointments | empty table and notice |
| TC14C-05 | room worklist with two appointments | rows in the order of the service and *2 appointments* |
| TC14C-06 | room worklist with one appointment | *1 appointment* |
| TC14C-07 | room worklist empty | empty table and notice of the room |
| TC14C-08 | physician worklist with two appointments | rows in the order of the service and *2 appointments* |
| TC14C-09 | physician worklist with one appointment | *1 appointment* |
| TC14C-10 | physician worklist empty | empty table and notice of the physician |
| TC14C-11 | day, room and physician with spaces around | sent without them |
| TC14C-12 | badly written day, with each of the three buttons | error, empty table, nothing sent |
| TC14C-13 | a list is shown and the next reading is rejected, with each button | its errors and empty table |
| TC14C-14 | a list is shown and the next reading fails, with each button | the error and empty table |
| TC14C-15 | a list is shown and the next reading is denied | message, empty table, actions disabled, *Back* enabled |
| TC14C-16 | press *Back* | the action of *Back* runs |
| TC14C-17 | welcome screen with `MANAGE_APPOINTMENTS` | button *Daily schedule* (Alt+D) runs its action |
| TC14C-18 | welcome screen without the permission | no button |
| TC14C-19 | welcome screen with both actions | *Manage waitlist* and *Daily schedule* are shown |
| TC14C-20 | the constructor of UC15, without the new action | *Manage waitlist* and no *Daily schedule* |

## Files

| File | Action | What |
|---|---|---|
| `client/c5-appointments/src/main/java/…/presentation/IU_DailySchedule.java` | create (step 1), change (step 6) | screen of the design; defects A and B and the singular of Q6 |
| `client/c1-access/src/main/java/…/presentation/IU_Welcome.java` | change (steps 2 and 4) | button <em>Daily schedule</em>; merge of Q8 |
| `client/c1-access/src/main/java/…/Main.java` | change (steps 3 and 4) | shared repository, service and screen; merge of Q8 and layout (defect C) |
| `client/c5-appointments/pom.xml`, `pom.xml` | change (steps 3 and 4) | `client-c5-appointments` `1.2.0-SNAPSHOT` |
| `client/c1-access/pom.xml`, `pom.xml` | change (step 4) | `client-c1-access` `1.2.9-SNAPSHOT` |
| `doc/testing/test-cases/UC14-client-test-design.md`, `doc/testing/test-cases/README.md` | create, change (step 5) | design of the cases of this plan |
| `client/c5-appointments/src/test/java/…/presentation/IU_DailyScheduleTest.java`, `FakeDailyScheduleService.java` | create (step 5) | TC14C-01 to TC14C-16 |
| `client/c1-access/src/test/java/…/presentation/IU_WelcomeDailyScheduleTest.java` | create (step 5) | TC14C-17 to TC14C-20 |

## Steps

- [x] 1. `IU_DailySchedule`: the schedule per room and the worklists of a room or of a physician, in time order, with the empty-state notices. (25d76e9)
- [x] 2. `IU_Welcome`: <em>Daily schedule</em> (Alt+D), only with `MANAGE_APPOINTMENTS`. (25d76e9)
- [x] 3. `Main`: the shared repository, the service and the screen; `client-c5-appointments` to `1.2.0-SNAPSHOT`. (25d76e9)
- [x] 4. Merge `origin/develop` (UC15 and UC14 server): solve `Main`, `IU_Welcome` and the versions (Q7, Q8, defect C). (9dcae82)
- [x] 5. Tests TC14C-01 to TC14C-20 and their design (red on purpose, Q9). (3715645) Red on purpose: TC14C-06, 09, 13, 14 and 15.
- [x] 6. `IU_DailySchedule`: defects A and B and the singular of Q6. (cbf25b1)
- [x] 7. Verification and close of the plan.

## Verification

- [x] Acceptance criteria of #136: the screen has the operations of the design, shows the schedule per room and the worklists in time order (TC14C-02, 05, 08) and a notice when there are none (TC14C-04, 07, 10); cancelled appointments are left out by the server (TC14S-02 and 04). Still open in the issue: wiki links, human review of the code and review of the tests.
- [x] `mvn -B -ntp clean verify` is green on 26/11/2026 (`cbf25b1`): `client-c5-appointments` 95.5 % of lines and 86.0 % of branches, `client-c1-access` 83.6 % and 86.4 %.

## Status

- **In progress:** nothing
- **Next step:** push when the member asks, new description of #239 and review by another member
- **Open questions:** the confirmation of the texts of the screen (Design)
- **Last update:** 26/11/2026, Javier Villanueva Crespo
