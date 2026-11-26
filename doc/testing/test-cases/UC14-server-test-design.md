# Test design · UC14 – Server (C5 v1.2.0) · #137 – Prepare the daily schedule and manage the worklists

Cases approved by Javier Villanueva Crespo (26/11/2026) following lesson T.04 (Testing Management): parameters,
equivalence classes, boundary values and error guessing. Expected results come from #137,
`doc/plans/UC14-server.md` (cases, Q4 and Q6), the Javadoc of `IDailyScheduleService`, D43 of
`doc/AN-C5-DECISIONS.md` and `doc/analysis/UC14-daily-schedule-sequence.puml`.

**Test data (precondition of every case).** An `InMemoryAppointmentRepository`, the test doubles of C1 and C3
(`TestDoubles`) and a double of C2 that reads `TT-01` (Room 1) and `TT-02` (Room 2), each with a preparation.

- **Users:** `lmartin` has `MANAGE_APPOINTMENTS`; `mgarcia` has not.
- **Appointments**, saved in this order:

| Id | Day and time | Room | Patient | Physician | Status |
|---|---|---|---|---|---|
| APT-0001 | 24/11 09:15 | Room 1 | P-0002 | `mgarcia` | `BOOKED` |
| APT-0002 | 24/11 09:00 | Room 1 | P-0001 | `mgarcia` | `RESCHEDULED` |
| APT-0003 | 24/11 10:00 | Room 2 | P-0001 | `jfernandez` | `BOOKED` |
| APT-0004 | 24/11 09:30 | Room 1 | P-0002 | `mgarcia` | `CANCELLED` |
| APT-0005 | 25/11 09:00 | Room 1 | P-0001 | `mgarcia` | `BOOKED` |
| APT-0006 | 26/11 09:00 | Room 1 | P-0001 | `mgarcia` | `CANCELLED` |

Messages are checked by their number, not by the whole sentence.

## 1. Parameters and testing values

| Parameter | Equivalence classes | Testing values |
|---|---|---|
| `requester` | With / without the permission | `lmartin`, `mgarcia` |
| `date` | With appointments / only cancelled / without appointments / not given | 24/11, 26/11, 27/11, `null` |
| `room` | With / without appointments / not given / blank | `Room 1`, `Room 9`, `null`, blank |
| `physician` | With / without appointments / blank | `mgarcia`, `nobody`, blank |
| Data of an appointment | Readable / unknown patient / unknown test type | seed, `P-9999`, `TT-99` |

## 2. Combination of the values

Each use: one invalid value at a time with the rest valid. Two invalid values together only where the contract asks for
every reason (TC14S-09 and 13).

## 3. Test cases

The tests are in `server/c5-appointments/src/test/java/.../service/DailyScheduleServiceTest.java`; TC14S-16 is in
`common/src/test/java/.../appointments/domain/ScheduleEntryTest.java`.

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC14S-01 | the three | `mgarcia` | Access denied, `SCHEDULE_ACCESS_DENIED`, no patient or test type read | `deniedWithoutPermission` |
| TC14S-02 | `getDailySchedule` | 24/11 | Room 1: APT-0002, APT-0001; Room 2: APT-0003; `SCHEDULE_ACCESSED` with the day | `dailySchedule` |
| TC14S-03 | `getDailySchedule` | 24/11, line of APT-0003 | Appointment, patient name, test type name and preparation | `lineOfTheSchedule` |
| TC14S-04 | `getDailySchedule` | 26/11 | Empty, access registered | `dayWithOnlyACancelledAppointment` |
| TC14S-05 | `getDailySchedule` | 27/11 | Empty, access registered | `dayWithoutAppointments` |
| TC14S-06 | `getDailySchedule` | `null` | One error, no access registered | `dailyScheduleWithoutDay` |
| TC14S-07 | `getRoomWorklist` | 24/11, `Room 1` | APT-0002, APT-0001; `WORKLIST_ACCESSED` with the room and the day | `roomWorklist` |
| TC14S-08 | `getRoomWorklist` | 24/11, `Room 9` | Empty, access registered | `roomWithoutAppointments` |
| TC14S-09 | `getRoomWorklist` | `null`, `null` | Two errors, no access registered | `roomWorklistWithoutData` |
| TC14S-10 | `getRoomWorklist` | 24/11, blank | One error, no access registered | `roomWorklistWithBlankRoom` |
| TC14S-11 | `getPhysicianWorklist` | 24/11, `mgarcia` | APT-0002, APT-0001; `WORKLIST_ACCESSED` with the physician and the day | `physicianWorklist` |
| TC14S-12 | `getPhysicianWorklist` | 24/11, `nobody` | Empty, access registered | `physicianWithoutAppointments` |
| TC14S-13 | `getPhysicianWorklist` | `null`, blank | Two errors, no access registered | `physicianWorklistWithoutData` |
| TC14S-14 | `getDailySchedule` | APT-0007 of `P-9999` | Validation error, no access registered | `unknownPatient` |
| TC14S-15 | `getPhysicianWorklist` | APT-0007 of `TT-99` | Validation error, no access registered | `unknownTestType` |
| TC14S-16 | `ScheduleEntry` | Getters | What was given | `getters` |

## 4. Coverage and open results

Measured on 26/11/2026, before step 5 of the plan: `DailyScheduleService` 58/60 lines and 26/26 branches,
`ScheduleEntry` 10/10 lines; `server-c5-appointments` 98.1 % of lines and 99.1 % of branches; `common` 87.9 % and
100 %. The two lines without a test are the constructor that step 5 removes. No case was red.

Not covered: the design does not define the order of the rooms in the daily schedule or the order of two appointments
at the same time.
