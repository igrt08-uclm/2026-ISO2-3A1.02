# Test design · UC13 – Server (C5 v1.0.0) · #131 – Manage the appointment schedule

Designed by Testing (Adam Gouram) following lesson T.04 (Testing Management): parameters of each method, equivalence
classes, boundary values and error guessing, combination of the values, and coverage of the code. Expected results
come from #130 and #131, the *Decisions* of `doc/plans/UC13-server.md` (permission, audit, free slots, booking,
reschedule, cancel, ids), `doc/plans/C5-design.md` (Q5, Q7, Q8, Q12, sections 2 and 5), the flow of UC13 and D37, D38,
D40, D41 of `doc/AN-C5-DECISIONS.md`, the Javadoc of `IAppointmentService` and `IAppointmentRepository`, D-022 and
D-040; nothing that those sources do not define is used as an expected result (section 5). The first version of the
cases (TC13S-01 to TC13S-32) was written by Ignacio Romero Torres on 23/11/2026; their ids and expected results are
kept.

Code under test (`server-c5-appointments` 1.0.0-SNAPSHOT, commit `387efac` of `feature/UC13-server`): the seven
operations of `IAppointmentService` implemented by `AppointmentService` (with the private `checkPermission`,
`readActiveTestType`, `findPendingNeed`, `findAppointment`, `slotErrors`, `insideSchedule`, `schedulesOf`, `isFree`,
`overlaps`, `nextAppointmentId`, `blank` and `addFreeSlotsOfTheDay`), and `InMemoryAppointmentRepository`.

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
- **Repository cases (TC13S-64 to TC13S-66):** five booked appointments at 09:00: `APT-0001` Room 1 `mgarcia`
  23/11/2026; `APT-0002` Room 1 `mgarcia` 24/11/2026; `APT-0003` Room 2 `jfernandez` 24/11/2026; `APT-0004` Room 1
  `mgarcia` 01/12/2026; `APT-0005` Room 1 `mgarcia` 02/12/2026.

The names, the rooms and the ids of C2 and C3 are invented test values.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
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
| | | Before the schedule | 08:00-08:15 |
| | | Ending after the schedule | 11:00-11:15 (Limit Val.: first slot after the end) |
| | | Day without schedule | 25/11 09:00-09:15 |
| | | Room with an inactive schedule | Room 4 09:00-09:15 |
| | | Past (clock 10:00) | 09:00-09:15; 09:45-10:00 (Limit Val.: last slot before now) |
| | | Start = now (clock 10:00) | 10:00-10:15 (Limit Val., accepted: "not in the past") |
| | | Taken | the same slot as a booked appointment; 09:10-09:25 over 09:00-09:15 (Conjeture: partial overlap) |
| | | Adjacent to an appointment | 09:15 after 09:00-09:15; 09:00 before 09:15-09:30 (Limit Val. of the overlap) |
| | | Freed by a cancellation | 09:00 after cancelling the appointment of 09:00 |
| `rescheduleAppointment` | `appointmentId` | Booked / rescheduled | `APT-0001` `BOOKED`; `APT-0001` `RESCHEDULED` |
| | | Cancelled | `APT-0001` after cancelling it |
| | | Unknown | `APT-9999` |
| | | Blank | `null` (Limit Val.), `"   "` |
| | `newSlot` | Free, inside the schedule | 10:00, 10:30 |
| | | Its own slot | 09:00 (Conjeture) |
| | | Not given / another room / past / outside the schedule / taken | `null`; Room 2; 09:00 at clock 10:00; 11:00-11:15; 10:00 of `APT-0002` |
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
with the requester, registered as `APPOINTMENT_ACCESS_DENIED` (D38); `ValidationException` with every reason; nothing
is saved.

## 2. Combination of the values

- **Requester × operation (all pairs).** Each of the six denied requester values with each of the seven operations
  (TC13S-01): 6 × 7 = 42 combinations, the maximum; the permitted requester `lmartin` is used in every other case.
- **Permission before validation.** A denied requester with a valid call never reaches C2, C3 or C4 (TC13S-02).
- **Fields of a booking (each use).** Each invalid class of `needId`, `slot` and the patient alone with the rest
  valid (TC13S-12 to TC13S-19, TC13S-43 to TC13S-51); each limit value of the slot alone (TC13S-17, TC13S-42 to
  TC13S-45). The conditions of the slot are isolated: a past slot is inside the schedule and free (TC13S-17), a slot
  outside the schedule is not past (TC13S-16, TC13S-45).
- **Reschedule × new slot (each use).** Each invalid class of the new slot with a booked appointment (TC13S-24,
  TC13S-53 to TC13S-56), and each state of the appointment with a valid slot (TC13S-21, TC13S-22, TC13S-59).
- **Test type × range.** `TT-01` with each range class; `TT-02` and `TT-04` with one day; `TT-03`, `TT-99` and blank
  with one day. The dates have no maximum number of cases: the classes of section 1 bound them.
- **State (error guessing).** Book then cancel (TC13S-05), book then reschedule (TC13S-40, TC13S-58), reschedule then
  cancel (TC13S-62), reschedule twice (TC13S-59), cancel when C4 already has the need pending (TC13S-27), an id
  already in the repository (TC13S-52).

## 3. Coverage (white box)

Measured with JaCoCo in `mvn -B verify` (D-033: 80 % of lines, 70 % of branches of the bundle):
`AppointmentService` 167 of 172 lines and 94 of 94 branches; `InMemoryAppointmentRepository` 26 of 26 lines and 4 of 4
branches; the bundle 193 of 198 lines (97 %) and 98 of 98 branches (100 %). The 5 missed lines are the constructor
with the system clock (lines 94-96) and the `catch` of `AccessDeniedException` in `schedulesOf` (lines 332-333), see
section 5.

**Decision coverage** (each decision true and false):

| Decision | True | False |
|---|---|---|
| `!hasPermission(requester, MANAGE_APPOINTMENTS)` in `checkPermission` | TC13S-01, TC13S-02 | every other case |
| `blank(testTypeId)` / `!testType.isActive()` in `readActiveTestType` | TC13S-33 / TC13S-08, TC13S-51 | TC13S-03 |
| `toDate.isBefore(fromDate)` | TC13S-06 | TC13S-03 (equal dates), TC13S-38 |
| `!day.isAfter(toDate)` (loop of days) | 1 iteration TC13S-03, 2 TC13S-38, 8 TC13S-39 | after the last day |
| `!daysOfWeek.contains(day)` in `addFreeSlotsOfTheDay` | TC13S-04, TC13S-38 (Wednesday) | TC13S-03 |
| `!start.plusMinutes(duration).isAfter(endOfDay)` (loop of slots) | 09:00 to 10:45 in TC13S-03; 09:00 to 12:30 in TC13S-36 | 11:00 in TC13S-03; 13:00 in TC13S-36 |
| `blank(needId)` / `slot == null` in `bookAppointment` | TC13S-13 / TC13S-14 | TC13S-09 |
| `needId.equals(need.getNeedId())` (loop of `findPendingNeed`) | first need TC13S-09; second TC13S-11, TC13S-19 | none matches: TC13S-12 |
| `!patient.isActive()` | TC13S-19 | TC13S-09 |
| `!errors.isEmpty()` after the slot checks | TC13S-15 to TC13S-19, TC13S-43 to TC13S-49 | TC13S-09 |
| room of the slot is not the room of the test type | TC13S-15, TC13S-54 | TC13S-09 |
| `slot.getStart().isBefore(now)` | TC13S-17, TC13S-43, TC13S-55 | TC13S-09; TC13S-42 (start = now) |
| `!insideSchedule(...)` | TC13S-16, TC13S-45 to TC13S-47, TC13S-56 | TC13S-09 |
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
| `findById(id) != null` in `nextAppointmentId` | TC13S-52 | TC13S-09, TC13S-11 |

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
| | T, F, – | outside | TC13S-16 (08:00) |
| | T, T, F | outside | TC13S-45 (11:00-11:15) |
| `schedulesOf` filter: `room.equals(resource) && isActive()` | T, T | kept | Room 1 in TC13S-03 |
| | F, – | dropped | the schedules of Room 2 and Room 4 in every case of `TT-01` |
| | T, F | dropped | Room 4 in TC13S-37, TC13S-47 (no schedule left: the loop of `insideSchedule` does not run) |
| `!start.isBefore(now) && isFree(candidate, null)` in `addFreeSlotsOfTheDay` | T, T | offered | TC13S-03; TC13S-41 at 10:00 (start = now) |
| | F, – | not offered | TC13S-41 (09:00 to 09:45 at 10:00) |
| | T, F | not offered | TC13S-05, TC13S-40 |
| `overlaps`: `appointment.start < slot.end && slot.start < appointment.end` | T, T | taken | TC13S-18 (same slot), TC13S-49 (09:10 over 09:00-09:15) |
| | T, F | free | TC13S-48 (09:15 after 09:00-09:15) |
| | F, – | free | TC13S-50 (09:00 before 09:15-09:30) |
| `isInRange`: `!day.isBefore(fromDate) && !day.isAfter(toDate)` (repository) | T, T | in | `APT-0002` (= from), `APT-0004` (= to) in TC13S-64 |
| | F, – | out | `APT-0001` (from - 1 day) |
| | T, F | out | `APT-0005` (to + 1 day) |

MC/DC: in each row group, the case of the first row and the case where only one condition changes differ only in that
condition and change the result (for example, TC13S-07 / TC13S-03 for `fromDate == null`, TC13S-35 / TC13S-03 for
`toDate == null`; TC13S-46, TC13S-16 and TC13S-45 each against TC13S-09 for the three conditions of `insideSchedule`).
`checkPermission` has a single condition: `pnavarro` makes it true although he is a known user, because C1 answers
`false` for an inactive user. The other decisions have a single condition, so condition/decision and MC/DC are the
same as decision coverage.

## 4. Test cases

The tests are in `server/c5-appointments/src/test/java/es/uclm/esi/iso2/server/appointments/service/
AppointmentServiceTest.java` (nested classes `Permission`, `FreeSlots`, `Booking`, `Rescheduling`, `Cancelling`,
`ReadsAndViews`) and, for TC13S-64 to TC13S-66, in `.../persistence/InMemoryAppointmentRepositoryTest.java`.
EC = equivalence class, BV = boundary value, EG = error guessing. Dates are 2026 unless written; times are on
24/11/2026 (Tuesday) unless written.

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
| TC13S-16 | `bookAppointment` | EC: 08:00-08:15 (not past at 08:00) | `The slot 24/11/2026 08:00 is not inside the schedule of the room Room 1` | `Booking.slotOutsideSchedule` | nothing saved |
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
| TC13S-46 | `bookAppointment` | EC: 25/11 09:00-09:15 | `The slot 25/11/2026 09:00 is not inside the schedule of the room Room 1` | `Booking.slotOnADayWithoutSchedule` | nothing saved |
| TC13S-47 | `bookAppointment` | EC: `NEED-0002` of `TT-04`, Room 4 09:00 | `The slot 24/11/2026 09:00 is not inside the schedule of the room Room 4` | `Booking.slotOfAnInactiveSchedule` | nothing saved |
| TC13S-48 | `bookAppointment` | BV: 09:00 booked, `NEED-0002` at 09:15 | Booked at 09:15 | `Booking.adjacentAfter` | two appointments |
| TC13S-49 | `bookAppointment` | EG: 09:00 booked, `NEED-0002` at 09:10-09:25 | `The slot 24/11/2026 09:10 is not free` | `Booking.partialOverlap` | one appointment |
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

Postcondition of every case: a denied call saves nothing and registers only `APPOINTMENT_ACCESS_DENIED`; a rejected
call saves nothing, does not change the need in C4 and registers nothing; no appointment is ever deleted (D-022).

## 5. Not covered / Not defined

- **To be tested when pushed (pending changes of Implementation, not in this branch):**
  - `schedulesOf` will stop catching `AccessDeniedException` of C2 (Javier's decision in #208). Today lines 332-333 are
    the only lines of the service not covered. No test of this design depends on the exception being swallowed (no
    double of C2 throws it), so none changes when `schedulesOf` rethrows; a case for the rethrow is added then.
  - `TimeSlot` `equals`/`hashCode`: the cases compare the start, end and room of the slots, not the objects.
  - `loadSeedData` / `AppointmentSeedData` with the seed given by Testing in #208.
- **Slot duration, grid and end after start, Not defined:** #131 says that the slots have the duration of the test
  type, but the code does not check that a booked slot lasts that duration, starts on the grid of the schedule or ends
  after it starts. No case asserts it. Every case uses slots of the duration of the test type on the grid, except
  TC13S-49 (09:10-09:25), which is off the grid to check a partial overlap: if the grid is validated, TC13S-49 gets a
  second message.
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
- **Not covered here:** the constructor with the system clock (lines 94-96; a test with it would depend on the day it
  is run); the client of UC13 (#130); UC15 (#134) and UC14 (#137); the order of the free slots of a room with two
  schedules, which cannot happen because C2 keeps one schedule per room (its key is the resource).
- **Defects found:** none; every expected result of section 4 passes against `387efac`.
