# Plan – UC14 – Server (#137)

| | |
|---|---|
| **Sub-issue** | #137 · branch `feature/UC14-server` |
| **Use case** | #135 · FR14 · C5 `1.2.0` |
| **Specification** | #135, #137, [doc/AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) (D43, D44) and `doc/analysis/UC14-daily-schedule-sequence.puml` |
| **Design** | [doc/design/C5-server-design.puml](../design/C5-server-design.puml), [doc/design/common/C5-common.puml](../design/common/C5-common.puml) |
| **Plan approved by** | Ignacio Romero, 26/11/2026 (steps 1 and 2); steps 3 to 6 and the cases TC14S-01 to TC14S-16 approved by Javier Villanueva Crespo, 26/11/2026 |

## Questions and answers

- **Q1 – Tests in this branch?** **A:** No. The member asked for the implementation only (26/11/2026) and there is no test design table for UC14 (rule 17). They go in `feature/UC14-server-tests`, branched from this one. Each step is checked with `mvn -B -ntp verify -Djacoco.skip=true`; the pull request waits for the tests (D-033). *Replaced by Q5.*
- **Q2 – Version of `common`?** **A:** `1.10.0-SNAPSHOT`: UC14 adds `ScheduleEntry` and `IDailyScheduleService` (D-036). `v0.5.0` publishes `1.10.0`; `server-c5-appointments` goes to `1.2.0-SNAPSHOT`.
- **Q3 – Which repository does it read?** **A:** The same `IAppointmentRepository` that UC13 fills, so the schedule sees the appointments that UC13 books. `Main` creates the repository and gives it to both services.
- **Q4 – Cancelled appointments?** **A:** They are never shown (D43): only `BOOKED` and `RESCHEDULED`, in time order, grouped by room, and inside each room by time.
- **Q5 – Who finishes the sub-issue?** **A:** Javier Villanueva Crespo takes over the code and the tests (26/11/2026). The tests go directly in `feature/UC14-server`, with the table below.
- **Q6 – When is the access registered?** **A:** At the end, as now and as the analysis sequence shows: if a patient or a test type cannot be read, nothing is returned and no access is registered (Javier Villanueva Crespo, 26/11/2026).
- **Q7 – Which statuses are shown?** **A:** The filter names `BOOKED` and `RESCHEDULED` (D43, Q4) instead of leaving out `CANCELLED`, so a new status is not shown by accident (Javier Villanueva Crespo, 26/11/2026).
- **Q8 – Constructor with four arguments.** **A:** It is removed: nothing uses it, it is not in the design and it builds an empty repository, against Q3 (Javier Villanueva Crespo, 26/11/2026).

## Test cases (S.04.01), approved by Javier Villanueva Crespo on 26/11/2026

Test data: `lmartin` has `MANAGE_APPOINTMENTS` and `mgarcia` has not; day 24/11/2026; `TT-01` (Room 1) and `TT-02` (Room 2), each with a preparation; the room of an appointment is that of its test type. Appointments, saved in this order:

| Id | Day and time | Room | Patient | Physician | Status |
|---|---|---|---|---|---|
| APT-0001 | 24/11 09:15 | Room 1 | P-0002 | `mgarcia` | `BOOKED` |
| APT-0002 | 24/11 09:00 | Room 1 | P-0001 | `mgarcia` | `RESCHEDULED` |
| APT-0003 | 24/11 10:00 | Room 2 | P-0001 | `jfernandez` | `BOOKED` |
| APT-0004 | 24/11 09:30 | Room 1 | P-0002 | `mgarcia` | `CANCELLED` |
| APT-0005 | 25/11 09:00 | Room 1 | P-0001 | `mgarcia` | `BOOKED` |
| APT-0006 | 26/11 09:00 | Room 1 | P-0001 | `mgarcia` | `CANCELLED` |

| Case | Operation | Input | Expected result |
|---|---|---|---|
| TC14S-01 | the three | `mgarcia` | access denied, `SCHEDULE_ACCESS_DENIED` registered, no patient or test type read |
| TC14S-02 | daily schedule | 24/11 | Room 1: APT-0002, APT-0001; Room 2: APT-0003; `SCHEDULE_ACCESSED` with the day |
| TC14S-03 | daily schedule | 24/11, line of APT-0003 | the appointment, the patient name, the test type name and its preparation |
| TC14S-04 | daily schedule | 26/11 | empty, access registered |
| TC14S-05 | daily schedule | 27/11 | empty, access registered |
| TC14S-06 | daily schedule | day `null` | one error, no access registered |
| TC14S-07 | room worklist | 24/11, Room 1 | APT-0002, APT-0001; `WORKLIST_ACCESSED` with the room and the day |
| TC14S-08 | room worklist | 24/11, a room without appointments | empty, access registered |
| TC14S-09 | room worklist | day `null` and room `null` | two errors, no access registered |
| TC14S-10 | room worklist | 24/11, blank room | one error, no access registered |
| TC14S-11 | physician worklist | 24/11, `mgarcia` | APT-0002, APT-0001; `WORKLIST_ACCESSED` with the physician and the day |
| TC14S-12 | physician worklist | 24/11, a physician without appointments | empty, access registered |
| TC14S-13 | physician worklist | day `null` and blank physician | two errors, no access registered |
| TC14S-14 | daily schedule | an appointment of an unknown patient | validation error, no access registered (Q6) |
| TC14S-15 | physician worklist | an appointment of an unknown test type | validation error, no access registered (Q6) |
| TC14S-16 | model | getters of `ScheduleEntry` | what was given |

The order of the rooms in the daily schedule and the order of two appointments at the same time are not in the design: no case checks them.

## Files

| File | Action | What |
|---|---|---|
| `common/src/main/java/…/appointments/domain/ScheduleEntry.java` | create (step 1) | entity of the design: the appointment, the patient, the test type and its preparation |
| `common/src/main/java/…/appointments/contracts/IDailyScheduleService.java` | create (step 1) | the three read operations of the design |
| `common/pom.xml`, `pom.xml` | change (step 1) | `common` `1.10.0-SNAPSHOT` |
| `server/c5-appointments/src/main/java/…/service/DailyScheduleService.java` | create (step 2), change (step 5) | `Control_DailySchedule`; filter of Q7 and constructor of Q8 |
| `server/c5-appointments/pom.xml`, `pom.xml` | change (step 2) | `server-c5-appointments` `1.2.0-SNAPSHOT` |
| `common/pom.xml`, `server/c5-appointments/pom.xml`, `pom.xml` | change (step 3) | solve the version conflicts with `develop`: `1.10.0` and `1.2.0` |
| `doc/testing/test-cases/UC14-server-test-design.md`, `doc/testing/test-cases/README.md` | create, change (step 4) | design of the cases of this plan |
| `server/c5-appointments/src/test/java/…/service/DailyScheduleServiceTest.java` | create (step 4) | TC14S-01 to TC14S-15 |
| `common/src/test/java/…/appointments/domain/ScheduleEntryTest.java` | create (step 4) | TC14S-16 |

## Steps

- [x] 1. `common`: `ScheduleEntry` and `IDailyScheduleService`; `common` to `1.10.0-SNAPSHOT`. (20a2a90)
- [x] 2. `server`: `DailyScheduleService`; `server-c5-appointments` to `1.2.0-SNAPSHOT`. (20a2a90)
- [x] 3. Merge `origin/develop` (UC15) and solve the versions; `mvn -B clean verify` green. (38a3412)
- [ ] 4. Tests TC14S-01 to TC14S-16 and their design.
- [ ] 5. `DailyScheduleService`: filter of Q7 and constructor of Q8.
- [ ] 6. Verification and close of the plan.

## Verification

- [ ] Acceptance criteria of #137: the daily schedule and the worklists only contain the booked appointments of that day, ordered by time; every access is registered in the audit log.
- [ ] `mvn -B clean verify` is green, with the coverage of D-033.

## Status

- **In progress:** step 4
- **Next step:** step 4
- **Open questions:** none for this branch. D43 and D44 are still *To confirm* in `doc/AN-C5-DECISIONS.md`
- **Last update:** 26/11/2026, Javier Villanueva Crespo
