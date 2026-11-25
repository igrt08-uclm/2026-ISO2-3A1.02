# Test design · UC13 – Server (C5 v1.0.0) · #131 – Manage the appointment schedule

Designed by Testing (Adam Gouram) following lesson T.04 (Testing Management): parameters of each method, equivalence
classes, boundary values and error guessing, combination of the values, and coverage of the code. Expected results
come from #130 and #131, the *Decisions* of `doc/plans/UC13-server.md` (permission, audit, free slots, booking,
reschedule, cancel, ids), `doc/plans/C5-design.md` (Q5, Q7, Q8, Q12, sections 2 and 5), the flow of UC13 and D37, D38,
D40, D41 of `doc/AN-C5-DECISIONS.md`, the Javadoc of `IAppointmentService` and `IAppointmentRepository`, D-022 and
D-040, and, for TC13S-67 to TC13S-76, the answers of #208 (Javier: `schedulesOf` rethrows the
`AccessDeniedException` of C2; Testing: the seed data of C5) and, for the rules of the slot (TC13S-16, TC13S-46,
TC13S-47, TC13S-49, TC13S-77 to TC13S-88), the group decision recorded in `c775ff4` (`doc/plans/UC13-server.md`,
Ignacio, 23/11/2026); nothing that those sources do not define is used as an expected result (section 5). The
first version of the cases (TC13S-01 to TC13S-32) was written by Ignacio Romero Torres on 23/11/2026; their ids and
expected results are kept, except the four changes of section 5.

Code under test (`server-c5-appointments` 1.0.0-SNAPSHOT, `feature/UC13-server` up to `b040e99`, merged into this
branch: `bcb06ae`, the plan `c775ff4` and the fixes of the defects of section 5, `c5f512d` (D1), `6a56ef9` (D5),
`6721cfd` (D2) and `53ecd94` (D6)): the seven operations of `IAppointmentService` and `loadSeedData` implemented by
`AppointmentService` (with the private `checkPermission`, `readActiveTestType`, `findPendingNeed`, `findAppointment`,
`slotErrors`, `startOnGrid`, `insideSchedule`, `schedulesOf`, `isFree`, `overlaps`, `nextAppointmentId`, `blank` and
`addFreeSlotsOfTheDay`, and the package-private `nextWorkingDay`), `AppointmentSeedData`, and
`InMemoryAppointmentRepository`.

**Test data (precondition of every case).** The service is built with an empty `InMemoryAppointmentRepository`, a
recording `IAuditRecorder` and the clock `Clock.fixed(2026-11-24T08:00:00Z, UTC)` (a Tuesday). The cases about the
past use a second service on the same doubles and repository with the clock at 24/11/2026 10:00 (`serviceAtTen()`).
- **Users (C1):** `lmartin` active with `MANAGE_APPOINTMENTS` (admissions staff, D37); `admin` and `mgarcia`
  (physician) active without it; `pnavarro` inactive; `nobody` does not exist.
- **Patients (C3):** `P-0001` (Lucía Ejemplo Prueba) and `P-0002` (Mario Demo Simulado) active; `P-0004` inactive.
- **Test types and schedules (C2, one schedule per room, keyed by the room):** `TT-01` Chest X-ray, active, Room 1,
  15 minutes, schedule Tuesdays 09:00-11:00 (eight slots); `TT-02` Blood test, active, Room 2, 30 minutes, Tuesdays
  09:00-13:00; `TT-03` inactive, Room 3, no schedule; `TT-04` active, Room 4, 15 minutes, whose only schedule
  (Tuesdays 09:00-11:00) is **inactive**.
- **C4:** a pending need `NEED-0001` of the diagnostic order `ORD-0001` (patient `P-0001`, `TT-01`, priority `URGENT`,
  ordered by `mgarcia` on 23/11/2026, due 23/11/2026). Some cases add `NEED-0002` of `ORD-0002` (`mgarcia`, priority
  `ROUTINE`) for another patient or test type, or of an order without priority (a follow-up order of UC07, D-040).
- **C2 that denies (TC13S-67 to TC13S-70):** the double of C2 can throw `AccessDeniedException` from `getSchedules`
  for `lmartin` (TC13S-67 to TC13S-69) or, like the real C2 for a requester without permission, for `null`
  (TC13S-70).
- **Seed (TC13S-71 to TC13S-76):** `loadSeedData()` on the empty repository with the clock of every case, or with the
  clock on Monday 23/11/2026 08:00 (TC13S-72) or Friday 27/11/2026 08:00 (TC13S-73); `nextWorkingDay` with the days
of TC13S-89.
- **Repository cases (TC13S-64 to TC13S-66):** five booked appointments at 09:00: `APT-0001` Room 1 `mgarcia`
  23/11/2026; `APT-0002` Room 1 `mgarcia` 24/11/2026; `APT-0003` Room 2 `jfernandez` 24/11/2026; `APT-0004` Room 1
  `mgarcia` 01/12/2026; `APT-0005` Room 1 `mgarcia` 02/12/2026.

The names, the rooms and the ids of C2 and C3 are invented test values.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| Book / cancel | Repository | Save succeeds / fails (**Conjeture**) | Normal store / rejected save |
| Cancel | C4 rejection | Every message ends in `is already pending` / other / mixed | TC13S-92 / 93 / 94 |
| Free slots | Inclusive days | At / above maximum (**Limit Val.**) | 31 / 32 |
| Free slots | Active type duration | Zero / positive (**Limit Val.**) | 0 / 15 minutes |
| all seven | `requester` | Admissions staff, active, with `MANAGE_APPOINTMENTS` | `lmartin` |
| | | Active without `MANAGE_APPOINTMENTS` (physician, administrator) | `mgarcia`, `admin` |
| | | Inactive | `pnavarro` |
| | | Unknown | `nobody`; `""` (Conjeture) |
| | | Not given | `null` (Limit Val.) |
| `getFreeSlots` | `testTypeId` | Active, with an active schedule | `TT-01`, `TT-02` |
| | | Active, its room has only an inactive schedule | `TT-04` |
| | | Inactive | `TT-03` |
| | | Unknown | `TT-99` |
| | | Blank | `null` (Limit Val.), `""` (Limit Val.), `"   "` |
| | `fromDate`, `toDate` | Valid range | one day 24/11 to 24/11 (Limit Val.: `toDate == fromDate`); two days 24/11 to 25/11; two Tuesdays 24/11 to 01/12 |
| | | Day without schedule | 25/11/2026 (Wednesday) |
| | | `toDate` before `fromDate` | 24/11 to 23/11 (Limit Val.: one day before) |
| | | Missing | `fromDate` `null`; `toDate` `null` |
| | clock (state) | Before the schedule / in the middle of it | 08:00; 10:00 (slots 09:00-09:45 past, 10:00 = now, Limit Val.) |
| `bookAppointment` | `needId` | Pending | `NEED-0001`; `NEED-0002` (second in the list of C4) |
| | | Not pending (unknown or booked) | `NEED-9999`; `NEED-0001` after booking it |
| | | Blank | `null` (Limit Val.), `""` (Limit Val.), `"   "` |
| | | Of an inactive test type / of an inactive patient | `NEED-0002` of `TT-03`; `NEED-0002` of `P-0004` |
| | `slot` | Not given | `null` (Limit Val.) |
| | `slot.room` | The room of the test type | Room 1 |
| | | Another room | Room 2 |
| | `slot.start`, `slot.end` (same day) | Inside the schedule, free, not past | 09:00-09:15 (Limit Val.: start = schedule start); 10:45-11:00 (Limit Val.: end = schedule end) |
| | | Before the schedule (also off the grid) | 08:00-08:15 |
| | | After the schedule, on the grid | 12:00-12:15 |
| | | Ending after the schedule | 11:00-11:15 (Limit Val.: first slot after the end) |
| | | Start or end missing | start `null`; end `null` |
| | | End not after start | 09:15-09:15 (Limit Val.: end = start); 09:15-09:00 |
| | | Spanning two days | 24/11 23:55 to 25/11 00:10 |
| | | Length different from the duration (15) | 14 minutes, 16 minutes (Limit Val.); 30 minutes; 15 (Limit Val., accepted) |
| | | Start off the grid of the schedule | 09:01 (Limit Val.: one minute after a grid point); 09:15 (accepted) |
| | | Day without schedule | 25/11 09:00-09:15 |
| | | Room with an inactive schedule | Room 4 09:00-09:15 |
| | | Past (clock 10:00) | 09:00-09:15; 09:45-10:00 (Limit Val.: last slot before now) |
| | | Start = now (clock 10:00) | 10:00-10:15 (Limit Val., accepted: "not in the past") |
| | | Taken | the same slot as a booked appointment; 09:15-09:30 over 09:10-09:25 (Conjeture: partial overlap) |
| | | Adjacent to an appointment | 09:15 after 09:00-09:15; 09:00 before 09:15-09:30 (Limit Val. of the overlap) |
| | | Freed by a cancellation | 09:00 after cancelling the appointment of 09:00 |
| `rescheduleAppointment` | `appointmentId` | Booked / rescheduled | `APT-0001` `BOOKED`; `APT-0001` `RESCHEDULED` |
| | | Cancelled | `APT-0001` after cancelling it |
| | | Unknown | `APT-9999` |
| | | Blank | `null` (Limit Val.), `"   "` |
| | `newSlot` | Free, inside the schedule | 10:00, 10:30 |
| | | Its own slot | 09:00 (Conjeture) |
| | | Not given / another room / past / outside the schedule / taken | `null`; Room 2; 09:00 at clock 10:00; 11:00-11:15; 10:00 of `APT-0002` |
| | | Off the grid / longer than the duration | 10:01-10:16; 10:00-10:30 |
| `cancelAppointment` | `appointmentId` | Booked / rescheduled | `APT-0001` |
| | | Cancelled | `APT-0001` cancelled before |
| | | Unknown | `APT-9999` |
| | | Blank | `null` (Limit Val.), `""` (Limit Val.), `"   "` |
| | answer of C4 (state) | Need booked → pending | normal case |
| | | Need already pending (C4 throws `ValidationException`) | `NEED-0001` put back to `PENDING` in C4 before cancelling (Conjeture) |
| `getAppointmentsOfPatient` | `patientId` | With appointments / without | `P-0001` / `P-0002` |
| | | Blank | `null`, `""`, `"   "` |
| `getPendingSchedulingNeeds` | (state of C4) | Need of a diagnostic order / of an order without priority | `NEED-0001` (`URGENT`); `NEED-0002` of a follow-up-like order |
| `getAppointments` | (state) | One appointment | after a booking |
| `getFreeSlots`, `bookAppointment`, `rescheduleAppointment` | answer of C2 to `getSchedules` (state) | Schedules / `AccessDeniedException` for the requester / for `null` | normal case; `lmartin`; `null` (Conjeture) |
| `loadSeedData` | (state of the repository) | Empty / not empty | new service; after a booking; after a first load |
| | clock (state) | Day before a working day / Friday | Monday 23/11/2026 (D = 24/11); Friday 27/11/2026 (D = Monday 30/11) |
| `InMemoryAppointmentRepository.findByRoomAndDateRange` | `room`, `fromDate`, `toDate` | Inside the range; limits; outside; other room; room without appointments | 24/11 and 01/12 (Limit Val., included); 23/11 and 02/12 (Limit Val., excluded); Room 2; Room 3 |
| `findByDate` | `date` | Day with appointments in two rooms / day without | 24/11; 25/11 |
| `findByDateAndPhysician` | `date`, `physician` | Same day, each physician / other day | `mgarcia`, `jfernandez` on 24/11; `mgarcia` on 25/11 |

**Outputs.** `getFreeSlots` returns the `TimeSlot`s of the room of the test type, of its duration, from the start of
the active schedule while they fit, not in the past and not overlapping an appointment that is not cancelled, in
chronological order (plan, *Free slots*; contract). `bookAppointment` returns `APT-nnnn` `BOOKED` with the need, the
patient, the test type, the room and the physician of the order (Q6 of C5-design), marks the need `BOOKED` in C4 and
registers `APPOINTMENT_BOOKED`. `rescheduleAppointment` keeps the id and the need, sets the new slot and `RESCHEDULED`
and registers `APPOINTMENT_RESCHEDULED`. `cancelAppointment` sets `CANCELLED`, puts the need back to `PENDING` (D41)
and registers `APPOINTMENT_CANCELLED`. The reads register `APPOINTMENT_ACCESSED`. Exceptions: `AccessDeniedException`
with the requester, registered as `APPOINTMENT_ACCESS_DENIED` (D38), and `ValidationException` with every reason.
When C4 rejects a cancellation after the save, the repository already holds the cancelled appointment (`d933b30`).

## 2. Combination of the values

TC13S-90 to 97 vary one save, C4 response, range or duration at a time using the same permitted requester.

- **Requester × operation (all pairs).** Each of the six denied requester values with each of the seven operations
  (TC13S-01): 6 × 7 = 42 combinations, the maximum; the permitted requester `lmartin` is used in every other case.
- **Permission before validation.** A denied requester with a valid call never reaches C2, C3 or C4 (TC13S-02).
- **Fields of a booking (each use).** Each invalid class of `needId`, `slot` and the patient alone with the rest
  valid (TC13S-12 to TC13S-19, TC13S-43 to TC13S-51); each limit value of the slot alone (TC13S-17, TC13S-42 to
  TC13S-45). The conditions of the slot are isolated: a past slot is inside the schedule and free (TC13S-17), a slot
  outside the schedule is not past and is on the grid (TC13S-16, TC13S-45), each rule of the slot decided in
  `c775ff4` is broken alone by a slot inside the schedule and free (TC13S-77 to TC13S-84), and a partial overlap is on
  the grid (TC13S-49). A slot before the start of the schedule, on a day without schedule or of a room without an
  active schedule cannot be on the grid, so TC13S-46, TC13S-47 and TC13S-86 expect two messages for one input.
- **Reschedule × new slot (each use).** Each invalid class of the new slot with a booked appointment (TC13S-24,
  TC13S-53 to TC13S-56, TC13S-87, TC13S-88), and each state of the appointment with a valid slot (TC13S-21,
  TC13S-22, TC13S-59).
- **Test type × range.** `TT-01` with each range class; `TT-02` and `TT-04` with one day; `TT-03`, `TT-99` and blank
  with one day. The dates have no maximum number of cases: the classes of section 1 bound them.
- **State (error guessing).** Book then cancel (TC13S-05), book then reschedule (TC13S-40, TC13S-58), reschedule then
  cancel (TC13S-62), reschedule twice (TC13S-59), cancel when C4 already has the need pending (TC13S-27), an id
  already in the repository (TC13S-52).

## 3. Coverage (white box)

Coverage through TC13S-99 (25/11/2026; before: `b040e99` with TC13S-90 to 97):
- `AppointmentService`: 204/207 lines and 128/128 branches. The three missed lines belong to the system-clock
  constructor (section 5).
- `AppointmentSeedData`: 7/7 lines. `InMemoryAppointmentRepository`: 26/26 lines and 4/4 branches.
- `server-c5-appointments`: 237/240 lines (98.8 %) and 132/132 branches (100 %).
- `common`: 252/287 lines (87.8 %) and 14/14 branches (100 %).

**Decision coverage** (each decision true and false):

| Decision | True | False |
|---|---|---|
| `!hasPermission(requester, MANAGE_APPOINTMENTS)` in `checkPermission` | TC13S-01, TC13S-02 | every other case |
| `blank(testTypeId)` / `!testType.isActive()` in `readActiveTestType` | TC13S-33 / TC13S-08, TC13S-51 | TC13S-03 |
| Last day beyond first + 30 days | TC13S-96 | TC13S-95 |
| Duration at most zero | TC13S-97 | TC13S-95 |
| Every C4 message ends in `is already pending` | TC13S-27, 92 | TC13S-93, 94 |
| `toDate.isBefore(fromDate)` | TC13S-06 | TC13S-03 (equal dates), TC13S-38 |
| `!day.isAfter(toDate)` (loop of days) | 1 iteration TC13S-03, 2 TC13S-38, 8 TC13S-39 | after the last day |
| `!daysOfWeek.contains(day)` in `addFreeSlotsOfTheDay` | TC13S-04, TC13S-38 (Wednesday) | TC13S-03 |
| `!start.plusMinutes(duration).isAfter(endOfDay)` (loop of slots) | 09:00 to 10:45 in TC13S-03; 09:00 to 12:30 in TC13S-36 | 11:00 in TC13S-03; 13:00 in TC13S-36 |
| `blank(needId)` / `slot == null` in `bookAppointment` | TC13S-13 / TC13S-14 | TC13S-09 |
| `needId.equals(need.getNeedId())` (loop of `findPendingNeed`) | first need TC13S-09; second TC13S-11, TC13S-19 | none matches: TC13S-12 |
| `!patient.isActive()` | TC13S-19 | TC13S-09 |
| `!errors.isEmpty()` after the slot checks | TC13S-15 to TC13S-19, TC13S-43 to TC13S-49, TC13S-79 to TC13S-86 | TC13S-09 |
| room of the slot is not the room of the test type | TC13S-15, TC13S-54 | TC13S-09 |
| `slot.getStart().isBefore(now)` | TC13S-17, TC13S-43, TC13S-55 | TC13S-09; TC13S-42 (start = now) |
| `!insideSchedule(...)` | TC13S-16, TC13S-45 to TC13S-47, TC13S-56, TC13S-86 | TC13S-09 |
| `!isFree(...)` | TC13S-18, TC13S-24, TC13S-49 | TC13S-09, TC13S-48, TC13S-50 |
| `blank(appointmentId)` in `rescheduleAppointment` / `cancelAppointment` | TC13S-23 / TC13S-60 | TC13S-21 / TC13S-25 |
| `newSlot == null` | TC13S-53 | TC13S-21 |
| `found == null` in `findAppointment` | TC13S-23b, TC13S-61 | TC13S-21, TC13S-25 |
| status `CANCELLED` in `rescheduleAppointment` / `cancelAppointment` | TC13S-22 / TC13S-26 | TC13S-21, TC13S-59 / TC13S-25, TC13S-62 |
| `catch (ValidationException)` of `markSchedulingNeedPending` | TC13S-27 | TC13S-25, TC13S-62 |
| `blank(patientId)` in `getAppointmentsOfPatient` | TC13S-29 | TC13S-28, TC13S-32 |
| `order instanceof DiagnosticOrder` | TC13S-30 (`URGENT`) | TC13S-63 (`""`) |
| `status != CANCELLED` (filter of `isFree`) | TC13S-18 | TC13S-05, TC13S-62 (cancelled, not counted) |
| `!appointmentId.equals(ignoredAppointmentId)` (filter of `isFree`) | TC13S-24 | TC13S-57 (its own slot) |
| `findById(id) != null` in `nextAppointmentId` | TC13S-52, TC13S-76 | TC13S-09, TC13S-11 |
| `appointments.findAll().isEmpty()` in `loadSeedData` | TC13S-71, first call of TC13S-75 | TC13S-74, second call of TC13S-75 |
| Saturday or Sunday (loop of `nextWorkingDay`) | TC13S-89 (Friday, Saturday: 2 and 1 iterations), TC13S-73 | TC13S-89 (Monday, Thursday, Sunday), TC13S-72 |
| `getSchedules` of C2 throws (`schedulesOf`, no `catch` since `bcb06ae`) | TC13S-67, TC13S-68, TC13S-69 | every other case that reads the schedules; TC13S-70 (C2 denies only `null`) |
| start or end `null` in `slotErrors` | TC13S-77, TC13S-78 | TC13S-09 |
| `!end.isAfter(start)` | TC13S-79 (end = start; end before start) | TC13S-09 |
| start and end on different days | TC13S-80 | TC13S-09 |
| length different from the duration | TC13S-82, TC13S-88 (longer); TC13S-81 (shorter) | TC13S-83 (= duration) |
| `!startOnGrid(...)` | TC13S-84, TC13S-87 (09:01, 10:01); TC13S-46, TC13S-47, TC13S-86 (no grid point) | TC13S-09 (09:00), TC13S-85 (09:15) |

**Condition/decision and MC/DC** of the compound decisions (Java evaluates them with short circuit; "–" = not
evaluated):

| Decision | Conditions | Result | Case |
|---|---|---|---|
| `text == null \|\| text.isBlank()` (`blank`: need, test type, appointment, patient) | `null`, – | blank | TC13S-13, TC13S-23, TC13S-29, TC13S-33, TC13S-60 with `null` |
| | not `null`, `""` / `"   "` | blank | the same cases with `""` and `"   "` |
| | not `null`, not blank | not blank | TC13S-09, TC13S-21, TC13S-28, TC13S-03, TC13S-25 |
| `fromDate == null \|\| toDate == null` | T, – | required | TC13S-07 |
| | F, T | required | TC13S-35 |
| | F, F | valid | TC13S-03 |
| `insideSchedule`: day of the schedule `&&` `!start.isBefore(scheduleStart)` `&&` `!end.isAfter(scheduleEnd)` | T, T, T | inside | TC13S-09 (start = 09:00), TC13S-44 (end = 11:00) |
| | F, –, – | outside | TC13S-46 (Wednesday) |
| | T, F, – | outside | TC13S-86 (08:00) |
| | T, T, F | outside | TC13S-45 (11:00-11:15), TC13S-16 (12:00-12:15) |
| `schedulesOf` filter: `room.equals(resource) && isActive()` | T, T | kept | Room 1 in TC13S-03 |
| | F, – | dropped | the schedules of Room 2 and Room 4 in every case of `TT-01` |
| | T, F | dropped | Room 4 in TC13S-37, TC13S-47 (no schedule left: the loops of `startOnGrid` and `insideSchedule` do not run) |
| `!start.isBefore(now) && isFree(candidate, null)` in `addFreeSlotsOfTheDay` | T, T | offered | TC13S-03; TC13S-41 at 10:00 (start = now) |
| | F, – | not offered | TC13S-41 (09:00 to 09:45 at 10:00) |
| | T, F | not offered | TC13S-05, TC13S-40 |
| `overlaps`: `appointment.start < slot.end && slot.start < appointment.end` | T, T | taken | TC13S-18 (same slot), TC13S-49 (09:15-09:30 over 09:10-09:25) |
| | T, F | free | TC13S-48 (09:15 after 09:00-09:15) |
| | F, – | free | TC13S-50 (09:00 before 09:15-09:30) |
| `isInRange`: `!day.isBefore(fromDate) && !day.isAfter(toDate)` (repository) | T, T | in | `APT-0002` (= from), `APT-0004` (= to) in TC13S-64 |
| | F, – | out | `APT-0001` (from - 1 day) |
| | T, F | out | `APT-0005` (to + 1 day) |

MC/DC: in each row group, the case of the first row and the case where only one condition changes differ only in that
condition and change the result (for example, TC13S-07 / TC13S-03 for `fromDate == null`, TC13S-35 / TC13S-03 for
`toDate == null`; TC13S-46, TC13S-86 and TC13S-45 each against TC13S-09 for the three conditions of `insideSchedule`).
`checkPermission` has a single condition: `pnavarro` makes it true although he is a known user, because C1 answers
`false` for an inactive user. The other decisions have a single condition, so condition/decision and MC/DC are the
same as decision coverage.

## 4. Test cases

The tests are in `server/c5-appointments/src/test/java/es/uclm/esi/iso2/server/appointments/service/
AppointmentServiceTest.java` (nested classes `Permission`, `FreeSlots`, `Booking`, `Rescheduling`, `Cancelling`,
`ReadsAndViews`, `DeniedByC2`, `SeedData`, `SlotRules`) and, for TC13S-64 to TC13S-66, in
`.../persistence/InMemoryAppointmentRepositoryTest.java`. EC = equivalence class, BV = boundary value, EG = error
guessing. Dates are 2026 unless written; times are on 24/11/2026 (Tuesday) unless written.

| ID | Method | Input (and precondition) | Expected result | Test | Postcondition |
|---|---|---|---|---|---|
| TC13S-01 | all seven | EC/BV: `admin`, `mgarcia`, `pnavarro`, `nobody`, `null`, `""` × each of the 7 operations | `AccessDeniedException` with the requester; the only audit entry is `APPOINTMENT_ACCESS_DENIED` on `appointments` (D37, D38) | `Permission.userWithoutPermissionIsDenied` | nothing saved |
| TC13S-02 | `bookAppointment` | EG: `admin`, valid need and slot | Denied; C2, C3 and C4 not called | `Permission.deniedAttemptDoesNotReachOtherComponents` | nothing saved |
| TC13S-03 | `getFreeSlots` | EC/BV: `TT-01`, 24/11 to 24/11 | Eight slots, 09:00 to 11:00, one every 15 minutes, Room 1 (plan, *Free slots*) | `FreeSlots.slotsOfTheSchedule` | no audit entry |
| TC13S-04 | `getFreeSlots` | EC: 25/11 (Wednesday) | No slots | `FreeSlots.dayWithoutSchedule` | — |
| TC13S-05 | `getFreeSlots` | EG: after booking 09:00; after cancelling it | Seven, without 09:00; then eight again | `FreeSlots.bookedAndCancelledSlots` | appointment `CANCELLED` |
| TC13S-06 | `getFreeSlots` | BV: 24/11 to 23/11 | `The last date cannot be before the first one` | `FreeSlots.wrongRange` | — |
| TC13S-07 | `getFreeSlots` | BV: `fromDate` `null` | `The date range is required` | `FreeSlots.missingRange` | — |
| TC13S-08 | `getFreeSlots` | EC: `TT-03` | `The test type TT-03 is not active` | `FreeSlots.inactiveTestType` | — |
| TC13S-09 | `bookAppointment` | EC: `NEED-0001`, Room 1 09:00-09:15 | `APT-0001` with the need, `P-0001`, `TT-01`, Room 1, physician `mgarcia`, 09:00, `BOOKED`, saved | `Booking.validBooking` | saved |
| TC13S-10 | `bookAppointment` | EC: valid booking | Need `BOOKED` in C4; audit only `APPOINTMENT_BOOKED` on `APPOINTMENT:APT-0001` | `Booking.needIsBookedInC4` | need booked |
| TC13S-11 | `bookAppointment` | EG: a second need after a booking | `APT-0002` | `Booking.secondBooking` | two appointments |
| TC13S-12 | `bookAppointment` | EC: `NEED-9999` | `The scheduling need NEED-9999 is not pending` | `Booking.needNotPending` | nothing saved |
| TC13S-13 | `bookAppointment` | EC/BV: need `null`, `""`, `"   "` | `The scheduling need is required` | `Booking.blankNeed` | nothing saved |
| TC13S-14 | `bookAppointment` | BV: slot `null` | `The slot is required` | `Booking.missingSlot` | nothing saved |
| TC13S-15 | `bookAppointment` | EC: Room 2 09:00 | `The room Room 2 is not the room of the test type TT-01` | `Booking.slotOfAnotherRoom` | nothing saved |
| TC13S-16 | `bookAppointment` | EC: 12:00-12:15 (after the schedule, on the grid, not past) | `The slot 24/11/2026 12:00 is not inside the schedule of the room Room 1` (changed, section 5) | `Booking.slotOutsideSchedule` | nothing saved |
| TC13S-17 | `bookAppointment` | EC: clock 10:00, 09:00-09:15 (inside the schedule and free) | Only `The slot 24/11/2026 09:00 is in the past` | `Booking.slotInThePast` | nothing saved |
| TC13S-18 | `bookAppointment` | EC: 09:00 already booked, `NEED-0002` | `The slot 24/11/2026 09:00 is not free` | `Booking.slotTaken` | one appointment |
| TC13S-19 | `bookAppointment` | EC: `NEED-0002` of `P-0004` | `The patient P-0004 is not active` | `Booking.inactivePatient` | nothing saved |
| TC13S-20 | `bookAppointment` | EG: rejected booking (Room 2) | Nothing saved, the need still `PENDING`, no audit entry | `Booking.rejectedBookingSavesNothing` | need pending |
| TC13S-21 | `rescheduleAppointment` | EC: `APT-0001` to 10:00 | Same id and need, `RESCHEDULED`, 10:00; audit `APPOINTMENT_BOOKED` and `APPOINTMENT_RESCHEDULED`; seven free slots | `Rescheduling.reschedule` | 09:00 free |
| TC13S-22 | `rescheduleAppointment` | EC: cancelled `APT-0001` | `The appointment APT-0001 is cancelled` | `Rescheduling.cancelledIsNotMovable` | still cancelled |
| TC13S-23 | `rescheduleAppointment` | EC/BV: id `null`, `"   "` | `The appointment is required` | `Rescheduling.blankAppointment` | — |
| TC13S-23b | `rescheduleAppointment` | EC: `APT-9999` | `There is no appointment with the id APT-9999` | `Rescheduling.unknownAppointment` | — |
| TC13S-24 | `rescheduleAppointment` | EC: to 10:00, taken by `APT-0002` | `The slot 24/11/2026 10:00 is not free` | `Rescheduling.rescheduleToTakenSlot` | `APT-0001` keeps 09:00 |
| TC13S-25 | `cancelAppointment` | EC: booked `APT-0001` | `CANCELLED`; need `PENDING` in C4 (D41); audit only `APPOINTMENT_CANCELLED` on `APPOINTMENT:APT-0001` | `Cancelling.cancel` | kept, not deleted (D-022) |
| TC13S-26 | `cancelAppointment` | EC: cancelled `APT-0001` | `The appointment APT-0001 is already cancelled` | `Cancelling.cancelTwice` | — |
| TC13S-27 | `cancelAppointment` | EG: booked `APT-0001` whose need C4 already has `PENDING` (C4 answers "already pending") | The cancellation succeeds: `CANCELLED`, need `PENDING`, `APPOINTMENT_CANCELLED` | `Cancelling.cancelAlreadyPendingNeed` | cancelled |
| TC13S-28 | `getAppointments`, `getAppointmentsOfPatient` | EC: after a booking, `P-0001` | One appointment each; `APPOINTMENT_ACCESSED` on `appointments` (and on `PATIENT:P-0001`) | `ReadsAndViews.reads` | unchanged |
| TC13S-29 | `getAppointmentsOfPatient` | EC/BV: `null`, `""`, `"   "` | `The patient is required` | `ReadsAndViews.blankPatient` | — |
| TC13S-30 | `getPendingSchedulingNeeds` | EC: `NEED-0001` | Need, order, patient and its name, test type and its name, `URGENT`, due 23/11/2026 | `ReadsAndViews.pendingNeedViews` | — |
| TC13S-31 | `getPendingSchedulingNeeds` | EG: after a booking | Empty | `ReadsAndViews.bookedNeedIsNotPending` | — |
| TC13S-32 | `getAppointmentsOfPatient` | EC: `P-0002` | Empty | `ReadsAndViews.patientWithoutAppointments` | — |
| TC13S-33 | `getFreeSlots` | EC/BV: test type `null`, `""`, `"   "` | `The test type is required` | `FreeSlots.blankTestType` | — |
| TC13S-34 | `getFreeSlots` | EC: `TT-99` | `ValidationException` of C2: `There is no test type with the id TT-99` (contract) | `FreeSlots.unknownTestType` | — |
| TC13S-35 | `getFreeSlots` | BV: `toDate` `null` | `The date range is required` | `FreeSlots.missingLastDate` | — |
| TC13S-36 | `getFreeSlots` | EC: `TT-02` | Eight slots of 30 minutes in Room 2: 09:00, 09:30 … 12:30, the last ending 13:00 | `FreeSlots.slotsOfSecondTestType` | — |
| TC13S-37 | `getFreeSlots` | EC: `TT-04` (only an inactive schedule) | No slots (plan: active schedules) | `FreeSlots.inactiveSchedule` | — |
| TC13S-38 | `getFreeSlots` | EC: 24/11 to 25/11 | The eight slots of 24/11 only | `FreeSlots.twoDayRange` | — |
| TC13S-39 | `getFreeSlots` | EC: 24/11 to 01/12 | Sixteen, chronological: the 8th starts 24/11 10:45, the 9th 01/12 09:00, the last ends 01/12 11:00 | `FreeSlots.twoTuesdays` | — |
| TC13S-40 | `getFreeSlots` | EG: `APT-0001` moved from 09:00 to 10:00 | Seven: 09:00 offered, 10:00 not (a rescheduled appointment is not cancelled) | `FreeSlots.rescheduledSlotIsOccupied` | — |
| TC13S-41 | `getFreeSlots` | BV: clock 10:00 | 10:00, 10:15, 10:30, 10:45 (the past skipped; 10:00 = now offered) | `FreeSlots.slotsOfThePastAreSkipped` | — |
| TC13S-42 | `bookAppointment` | BV: clock 10:00, 10:00-10:15 | `APT-0001` `BOOKED` at 10:00 ("not in the past") | `Booking.slotStartingNow` | saved |
| TC13S-43 | `bookAppointment` | BV: clock 10:00, 09:45-10:00 | Only `The slot 24/11/2026 09:45 is in the past` | `Booking.lastSlotBeforeNow` | nothing saved |
| TC13S-44 | `bookAppointment` | BV: 10:45-11:00 | Booked, ending 11:00 | `Booking.lastSlotOfTheSchedule` | saved |
| TC13S-45 | `bookAppointment` | BV: 11:00-11:15 | `The slot 24/11/2026 11:00 is not inside the schedule of the room Room 1` | `Booking.slotEndingAfterTheSchedule` | nothing saved |
| TC13S-46 | `bookAppointment` | EC: 25/11 09:00-09:15 | `The start of the slot must be on the grid of the schedule of the room Room 1`, `The slot 25/11/2026 09:00 is not inside the schedule of the room Room 1` (changed, section 5) | `Booking.slotOnADayWithoutSchedule` | nothing saved |
| TC13S-47 | `bookAppointment` | EC: `NEED-0002` of `TT-04`, Room 4 09:00 | `The start of the slot must be on the grid of the schedule of the room Room 4`, `The slot 24/11/2026 09:00 is not inside the schedule of the room Room 4` (changed, section 5) | `Booking.slotOfAnInactiveSchedule` | nothing saved |
| TC13S-48 | `bookAppointment` | BV: 09:00 booked, `NEED-0002` at 09:15 | Booked at 09:15 | `Booking.adjacentAfter` | two appointments |
| TC13S-49 | `bookAppointment` | EG: the repository has `APT-0009` of Room 1 at 09:10-09:25; `NEED-0001` at 09:15-09:30 | `The slot 24/11/2026 09:15 is not free` (changed, section 5) | `Booking.partialOverlap` | only `APT-0009` |
| TC13S-50 | `bookAppointment` | BV: 09:15 booked, `NEED-0002` at 09:00 | Booked at 09:00 | `Booking.adjacentBefore` | two appointments |
| TC13S-51 | `bookAppointment` | EC: `NEED-0002` of `TT-03` | `The test type TT-03 is not active` | `Booking.needOfAnInactiveTestType` | nothing saved, need `PENDING` |
| TC13S-52 | `bookAppointment` | EG: the repository already has `APT-0001` (Room 2) | The new appointment is `APT-0002` (plan, *Ids*) | `Booking.idSkipsExistingIds` | two appointments |
| TC13S-53 | `rescheduleAppointment` | BV: new slot `null` | `The slot is required` | `Rescheduling.missingNewSlot` | unchanged |
| TC13S-54 | `rescheduleAppointment` | EC: Room 2 10:00 | `The room Room 2 is not the room of the test type TT-01` | `Rescheduling.rescheduleToAnotherRoom` | keeps Room 1 |
| TC13S-55 | `rescheduleAppointment` | EC: clock 10:00, booked 10:30, to 09:00 | Only `The slot 24/11/2026 09:00 is in the past` | `Rescheduling.rescheduleToThePast` | keeps 10:30 |
| TC13S-56 | `rescheduleAppointment` | EC: to 11:00-11:15 | `The slot 24/11/2026 11:00 is not inside the schedule of the room Room 1` | `Rescheduling.rescheduleOutsideTheSchedule` | still `BOOKED` |
| TC13S-57 | `rescheduleAppointment` | EG: to its own slot 09:00 | Accepted: 09:00, `RESCHEDULED` (the appointment itself is ignored) | `Rescheduling.rescheduleOntoItsOwnSlot` | `RESCHEDULED` |
| TC13S-58 | `rescheduleAppointment` | EG: to 10:00 | The need is still `BOOKED` in C4 and not pending (the need is kept) | `Rescheduling.needStaysBookedAfterReschedule` | need booked |
| TC13S-59 | `rescheduleAppointment` | EC: a `RESCHEDULED` appointment to 10:30 | `APT-0001`, 10:30, `RESCHEDULED` | `Rescheduling.rescheduleTwice` | `RESCHEDULED` |
| TC13S-60 | `cancelAppointment` | EC/BV: id `null`, `""`, `"   "` | `The appointment is required` | `Cancelling.cancelBlankAppointment` | — |
| TC13S-61 | `cancelAppointment` | EC: `APT-9999` | `There is no appointment with the id APT-9999` | `Cancelling.cancelUnknownAppointment` | — |
| TC13S-62 | `cancelAppointment` | EG: `APT-0001` rescheduled to 10:00 | `CANCELLED`; need `PENDING`; eight free slots | `Cancelling.cancelRescheduled` | cancelled |
| TC13S-63 | `getPendingSchedulingNeeds` | EC: `NEED-0002` of an order that is not diagnostic (`P-0002`, `TT-02`, due 23/05/2027) | Second view: Mario Demo Simulado, Blood test, priority `""`, due 23/05/2027 (Javadoc of `PendingNeedView`, D-040) | `ReadsAndViews.needOfAnOrderWithoutPriority` | — |
| TC13S-64 | `findByRoomAndDateRange` | BV: Room 1, Room 2, Room 3, 24/11 to 01/12 | Room 1: `APT-0002`, `APT-0004` (both limits included); Room 2: `APT-0003`; Room 3: empty | `InMemoryAppointmentRepositoryTest.findByRoomAndDateRange` | unchanged |
| TC13S-65 | `findByDate` | EC: 24/11; 25/11 | `APT-0002`, `APT-0003`; empty | `InMemoryAppointmentRepositoryTest.findByDate` | unchanged |
| TC13S-66 | `findByDateAndPhysician` | EC: 24/11 `mgarcia`, 24/11 `jfernandez`, 25/11 `mgarcia` | `APT-0002`; `APT-0003`; empty | `InMemoryAppointmentRepositoryTest.findByDateAndPhysician` | unchanged |
| TC13S-67 | `getFreeSlots` | EC: C2 denies `getSchedules` to `lmartin`; `TT-01`, 24/11 | `AccessDeniedException` of `lmartin` reaches the caller (Javier, #208) | `DeniedByC2.freeSlotsWhenC2DeniesTheSchedules` | — |
| TC13S-68 | `bookAppointment` | EC: C2 denies `getSchedules` to `lmartin`; `NEED-0001`, 09:00 | `AccessDeniedException` of `lmartin` (Javier, #208) | `DeniedByC2.bookingWhenC2DeniesTheSchedules` | nothing saved, need `PENDING` |
| TC13S-69 | `rescheduleAppointment` | EC: `APT-0001` booked at 09:00; C2 denies `lmartin`; to 10:00 | `AccessDeniedException` of `lmartin` (Javier, #208) | `DeniedByC2.rescheduleWhenC2DeniesTheSchedules` | `APT-0001` keeps 09:00, `BOOKED` |
| TC13S-70 | `bookAppointment` | EG: C2 denies `null`, as the real C2 does for a requester without permission; `lmartin`, `NEED-0001`, 09:00 | `APT-0001` `BOOKED` (the permitted requester `lmartin` books a valid slot; TC13S-09) | `DeniedByC2.bookingWithAC2ThatDeniesANullRequester` (D1, fixed in `c5f512d`) | saved |
| TC13S-71 | `loadSeedData` | EC: empty repository | `APT-0001`, `APT-0002`, `APT-0003` of `NEED-0001`, `NEED-0002`, `NEED-0003`, `BOOKED`, `CANCELLED`, `RESCHEDULED`, all `CT-HEAD` in Room CT-1 (Testing, #208) | `SeedData.seedIsLoadedInAnEmptySystem` | three appointments |
| TC13S-72 | `loadSeedData` | BV: clock Monday 23/11/2026 08:00 | Every seed appointment on D = Tuesday 24/11/2026, the first working day after the start (Testing, #208) | `SeedData.seedOfAMondayIsOnTuesday` (D2, fixed in `6721cfd`) | three appointments |
| TC13S-73 | `loadSeedData` | BV: clock Friday 27/11/2026 08:00 | Every seed appointment on D = Monday 30/11/2026 (Testing, #208) | `SeedData.seedOfAFridayIsOnMonday` (D2, fixed in `6721cfd`) | three appointments |
| TC13S-74 | `loadSeedData` | EC: one appointment booked before | Nothing loaded: only `APT-0001` of `NEED-0001` (Testing, #208: only when the repository is empty) | `SeedData.seedIsNotLoadedWhenThereIsData` | one appointment |
| TC13S-75 | `loadSeedData` | EG: called twice | Three appointments; no audit entry (Testing, #208; C5-design Q12) | `SeedData.seedIsLoadedOnlyOnceAndNotAudited` | three appointments |
| TC13S-76 | `bookAppointment` | EG: after the seed, `NEED-0001`, 09:00 | `APT-0004` (Testing, #208: the ids continue; plan, *Ids*) | `SeedData.idsContinueAfterTheSeed` | four appointments |
| TC13S-77 | `bookAppointment` | BV: start `null`, end 09:15 | `ValidationException` with `The start and the end of the slot are required` | `SlotRules.startMissing` (D5, fixed in `6a56ef9`) | nothing saved |
| TC13S-78 | `bookAppointment` | BV: start 09:00, end `null` | `ValidationException` with `The start and the end of the slot are required` | `SlotRules.endMissing` (D5, fixed in `6a56ef9`) | nothing saved |
| TC13S-79 | `bookAppointment` | BV: 09:15-09:15 (end = start); EC: 09:15-09:00 (end before start) | `The end of the slot must be after its start` | `SlotRules.endNotAfterStart` | nothing saved |
| TC13S-80 | `bookAppointment` | EC: 24/11 23:55 to 25/11 00:10 (15 minutes) | `The slot must be inside a single day` | `SlotRules.slotOfTwoDays` | nothing saved |
| TC13S-81 | `bookAppointment` | BV: 09:00-09:14 (one minute shorter) | `The duration of the slot must match the duration of the test type TT-01` | `SlotRules.slotShorterThanTheDuration` (D6, fixed in `53ecd94`) | nothing saved |
| TC13S-82 | `bookAppointment` | BV: 09:00-09:16 (one minute longer) | `The duration of the slot must match the duration of the test type TT-01` | `SlotRules.slotLongerThanTheDuration` | nothing saved |
| TC13S-83 | `bookAppointment` | BV: 09:00-09:15 (= duration) | Booked, 09:00-09:15 | `SlotRules.lengthEqualToTheDuration` | saved |
| TC13S-84 | `bookAppointment` | BV: 09:01-09:16 (one minute off the grid) | `The start of the slot must be on the grid of the schedule of the room Room 1` | `SlotRules.startOffTheGrid` | nothing saved |
| TC13S-85 | `bookAppointment` | BV: 09:15-09:30 (second grid point) | Booked at 09:15 | `SlotRules.startOnTheGrid` | saved |
| TC13S-86 | `bookAppointment` | EC: 08:00-08:15 (before the schedule, not past at 08:00; the input of the first TC13S-16) | `The start of the slot must be on the grid of the schedule of the room Room 1`, `The slot 24/11/2026 08:00 is not inside the schedule of the room Room 1` | `SlotRules.slotBeforeTheSchedule` | nothing saved |
| TC13S-87 | `rescheduleAppointment` | EC: `APT-0001` at 09:00 to 10:01-10:16 | `The start of the slot must be on the grid of the schedule of the room Room 1` | `SlotRules.rescheduleOffTheGrid` | keeps 09:00 |
| TC13S-88 | `rescheduleAppointment` | EC: `APT-0001` at 09:00 to 10:00-10:30 | `The duration of the slot must match the duration of the test type TT-01` | `SlotRules.rescheduleWithAWrongDuration` | still `BOOKED` |
| TC13S-89 | `nextWorkingDay` | BV: Monday 23/11, Thursday 26/11, Friday 27/11; EC: Saturday 28/11, Sunday 29/11/2026 | Tuesday 24/11, Friday 27/11, Monday 30/11, Monday 30/11, Monday 30/11 (the first working day after the start, Testing, #208; `6721cfd`) | `SeedData.nextWorkingDay` | — |

The following cases use the review of #208 in the server plan (`b040e99`) and the corresponding change (`d933b30`).
The range includes both dates. TC13S-95 starts on Sunday and ends on the fifth scheduled Tuesday (eight slots each).
For TC13S-97 the room has no Wednesday schedule, so even a missing duration check cannot start an unbounded loop.

| ID | Method | Input (and precondition) | Expected result | Test | Postcondition |
|---|---|---|---|---|---|
| TC13S-90 | Book | Save fails | Storage error | `Booking.bookingSaveFails` | No appointment, need pending |
| TC13S-91 | Cancel | Save fails | Storage error | `Cancelling.cancelSaveFails` | Appointment and need booked |
| TC13S-92 | Cancel | Two already-pending messages | Success | `Cancelling.allPending` | Cancelled |
| TC13S-93 | Cancel | Unrelated C4 error | Same exception | `Cancelling.otherError` | Cancelled |
| TC13S-94 | Cancel | One suffix, one mid-message match | Same exception | `Cancelling.mixedErrors` | Cancelled |
| TC13S-95 | Free slots | 31 days | 40 slots, last ends at 11:00 on day 31 | `FreeSlots.accepts31Days` | Read only |
| TC13S-96 | Free slots | 32 days | Range error below | `FreeSlots.rejects32Days` | Read only |
| TC13S-97 | Free slots | Active TT-ZERO, zero minutes | Duration error below | `FreeSlots.zeroDuration` | Read only |
| TC13S-98 | Cancel | Booked `APT-0001`; its need is already `PENDING` and C4 would reject the call with “The need is broken” | `CANCELLED`; no error: C4 is not asked again | `Cancelling.alreadyPendingNeedIsNotMarkedAgain` | Appointment saved |
| TC13S-99 | Cancel | Booked `APT-0001`; C4 also lists `NEED-0002` as pending | `CANCELLED`; `NEED-0001` and `NEED-0002` both `PENDING` | `Cancelling.cancelWithOtherPendingNeeds` | Appointment saved |

TC13S-96 expects “The range cannot be longer than 31 days”. TC13S-97 expects “The test type TT-ZERO has no duration”.

Validation before saving leaves the appointment and need untouched. TC13S-93 and 94 exercise rejection after saving. TC13S-98 covers a need that is already pending: the service does not ask C4 and does not read its text.
Permission denial registers `APPOINTMENT_ACCESS_DENIED`. No appointment is deleted (D-022).

## 5. Not covered / Not defined

- TC13S-90 to 97 add expectations from `d933b30` and the review recorded in `b040e99`. The review defines the
  save order and propagated errors. Recovery or retry after a successful save followed by a C4 failure is unspecified.

- **Changed expected values after merging `bcb06ae` (#208):** none. No double of C2 threw `AccessDeniedException`
  before, so no case depended on the `catch` that Javier's decision removed; the rethrow is now TC13S-67 to TC13S-69.
- **Changed expected values after merging `c775ff4` (group decision on the rules of the slot).** `c775ff4` records
  in `doc/plans/UC13-server.md` that the group decided the checks of `slotErrors` (Ignacio, 23/11/2026): a slot is
  rejected if its start or end is `null`, its end is not after its start, it spans more than one day, its length is
  not the duration of the test type, or its start is not on the grid of the schedule of the room (`startOnGrid`). The
  four cases that were disabled because of the grid check are enabled again. Source of every change: group decision
  recorded in `c775ff4`; the text of the messages is taken from the code (`slotErrors` of `bcb06ae`), because the
  decision names the rules but not their messages.
  - TC13S-16: input 08:00-08:15 -> 12:00-12:15; expected `The slot 24/11/2026 08:00 is not inside the schedule of the
    room Room 1` -> `The slot 24/11/2026 12:00 is not inside the schedule of the room Room 1`. A slot before the start
    of the schedule is never on the grid, so the case now uses a slot after the schedule on the grid to keep a single
    reason; the old input is kept as TC13S-86 with its two messages.
  - TC13S-46: `The slot 25/11/2026 09:00 is not inside the schedule of the room Room 1` -> `The start of the slot must
    be on the grid of the schedule of the room Room 1`, `The slot 25/11/2026 09:00 is not inside the schedule of the
    room Room 1`. Two messages are accepted: a day without schedule has no grid, so no input isolates it.
  - TC13S-47: `The slot 24/11/2026 09:00 is not inside the schedule of the room Room 4` -> `The start of the slot must
    be on the grid of the schedule of the room Room 4`, `The slot 24/11/2026 09:00 is not inside the schedule of the
    room Room 4`. Two messages are accepted: a room without an active schedule has no grid.
  - TC13S-49: input 09:00 booked and 09:10-09:25 -> `APT-0009` of Room 1 at 09:10-09:25 already in the repository and
    09:15-09:30; expected `The slot 24/11/2026 09:10 is not free` -> `The slot 24/11/2026 09:15 is not free`. Two
    slots of 15 minutes on the same grid never overlap in part, so the existing appointment is off the grid and the
    new slot is on it, which keeps the overlap as the only reason.
- **Changed expected values after the earlier merge at `b3e7e58` (D1, D2, D5, D6):** none. TC13S-70, TC13S-72,
  TC13S-73, TC13S-77, TC13S-78 and TC13S-81 are back with the ids and expected values they had while out of the
  suite; TC13S-89 (`nextWorkingDay`) is new, with the rule of #208 (D is the first working day after the start).
- **New cases of the decided rules (TC13S-77 to TC13S-88).** One case or more per rule, alone, with a slot that is
  inside the schedule, free and not past (section 4). Expected results: the rule from `c775ff4`, the message from the
  code.
- **Defect D1: the grid check reads C2 with a `null` requester.** `startOnGrid` calls `schedulesOf(room, null)`, that
  is, `ISystemParameterService.getSchedules(null)`. The real `SystemParameterService` (C2 1.1.2) denies a `null`
  requester (`checkReadPermission`, `canConfigure(null)` is false), registers `ACCESS_DENIED` and throws
  `AccessDeniedException`, which `bcb06ae` now propagates: with the real C2 every booking and reschedule of a valid slot
  by `lmartin` fails with `AccessDeniedException` of `null`. TC13S-70 shows it with a double that denies `null`; it is
  **Fixed in `c5f512d`** (`startOnGrid` reads C2 with the requester); TC13S-70 is back in the suite and passes with its
  expected value unchanged.
- **Defect D2: the day of the seed.** The seed given by Testing in #208 puts the appointments on D, the first working
  day after the start; `AppointmentService.loadSeedData` passes `LocalDate.now(clock)`, the day of the start, to
  `AppointmentSeedData`, so a server started on a Saturday or a Sunday gets appointments on a day without schedule and
  one started after 10:00 gets them in the past. **Fixed in `6721cfd`** (`loadSeedData` passes
  `nextWorkingDay(LocalDate.now(clock))`); TC13S-72 and TC13S-73 are back in the suite and pass with their expected
  values unchanged, and TC13S-89 checks `nextWorkingDay` alone. The rest of the C5 seed matches #208 (TC13S-71,
  TC13S-74 to TC13S-76).
- **Defect D5: a slot with a `null` start or end throws `NullPointerException`.** The decided rule rejects it with
  `ValidationException`; `slotErrors` adds `The start and the end of the slot are required` but then reads
  `slot.getStart()` in the check of the past and `slot.getEnd()` in `insideSchedule`. **Fixed in `6a56ef9`**
  (`slotErrors` returns after that message); TC13S-77 and TC13S-78 are back in the suite and pass with their expected
  list unchanged: only that message, because the other checks need a start and an end.
- **Defect D6: a slot shorter than the duration is accepted.** The decided rule rejects a length different from the
  duration; the two conditions of the duration check (`end - duration > start`, `start + duration < end`) are both
  true only for a longer slot, so 09:00-09:14 is booked. **Fixed in `53ecd94`** (the end must be exactly the start
  plus the duration); TC13S-81 is back in the suite and passes with its expected value unchanged; the longer slot is
  still rejected (TC13S-82, TC13S-88).
- **C4 seed (`OrderSeedData`, `ClinicalOrderService.loadSeedData`), not tested here:** this branch has no tests of
  `server-c4-orders`, and one test there turns on its JaCoCo gate (80 % / 70 %) with the coverage of that test only.
  Found by reading `bcb06ae`, against #208: (D3) the needs were due on `LocalDate.now()`, the clock of the system,
  not on the clock of the service (fixed in `6ca690e`, `OrderSeedData.needs(day)`); (D4) the seed was loaded when
  `ORD-0001` was missing, not only when the repositories were empty, so a repository with other orders got the seed
  saved over them (fixed in `65273ef`: loaded only when none of the eight seed ids exists). C4 seed cases (D3, D4) go
  to the C4 test branch when UC06 and UC13 meet in develop. Cases to write there:
  - the seed needs are due on the day given by the clock of the service (fixed clock), not by the system clock (D3);
  - the seed is not loaded if any of `ORD-0001` to `ORD-0004` or `NEED-0001` to `NEED-0004` already exists, one case
    per kind of id (an order, a need), and it is loaded in empty repositories (D4);
  - after the seed, the next ids are `ORD-0005` and `NEED-0005`.
- **`TimeSlot` `equals`/`hashCode`:** decided not to add them (`4ce0879`, the plan no longer says they were added);
  nothing to test. The cases compare the start, end and room of the slots.
- **Reschedule of an appointment of an inactive patient, Not defined:** AN-C5 6b checks the patient in the booking, 9b
  only names the slot; the code does not check the patient when rescheduling. No case asserts it.
- **Cancelled appointments in `findByRoomAndDateRange`, `findByDate` and `findByDateAndPhysician`, Not defined:** the
  Javadoc of `findAll` and `findByPatient` says that cancelled ones are included; the Javadoc of these three does not
  say it, so TC13S-64 to TC13S-66 use only booked appointments.
- **Resource of the denied attempt:** the plan and C5-design section 5 name the action `APPOINTMENT_ACCESS_DENIED`
  but not its resource; TC13S-01 and TC13S-02 keep `appointments` from the first version of this design.
- **TC13S-17 changed its input:** the first version used a slot of 23/11/2026, which was also outside the schedule
  (Monday), so it did not isolate the past. It now uses a clock at 10:00 and a slot of 09:00 inside the schedule; the
  expected result is the same rule, with the date of the new slot, and it is now the only message.
- **Not covered here:** the constructor with the system clock (a test with it would depend on the day it
  is run); the times of the seed appointments, which #208 does not fix in this design; the client of UC13 (#130); UC15
  (#134) and UC14 (#137); the order of the free slots of a room with two schedules, which cannot happen because C2
  keeps one schedule per room (its key is the resource).
- **Defects found:** D1, D2, D5 and D6 (C5), fixed in `c5f512d`, `6721cfd`, `6a56ef9` and `53ecd94`; their cases
  (TC13S-70, TC13S-72, TC13S-73, TC13S-77, TC13S-78, TC13S-81) are back in the suite with no expected value changed,
  and TC13S-89 is new. D3 and D4 (C4, by reading), fixed in `6ca690e` and `65273ef`, tested in the C4 test branch.
  Every case of section 4 passes against `feature/UC13-server` up to `b040e99`, with no test disabled.
