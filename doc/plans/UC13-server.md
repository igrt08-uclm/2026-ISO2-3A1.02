# Plan – UC13 – Server (#131)

| | |
|---|---|
| **Sub-issue** | #131 *UC13 – Server (C5 v1.0.0)* · branch `feature/UC13-server`, created from `feature/UC06-server` (`b156b9a`) because C5 1.0.0 only needs C4 1.0.0 and that branch is the one to which this pull request goes |
| **Use case** | #129 *UC13 – Manage the appointment schedule (book, reschedule and cancel) (C5 v1.0.0)* · FR13 · component C5 1.0.0 |
| **Specification** | #131; `doc/plans/C5-design.md` (contracts, model, permissions and audit of C5); D37, D38, D40, D43, D44; D-022, D-032, D-033, D-036 |
| **Design** | `doc/plans/C5-design.md`, sections *Target model* and `C5-server-design`; it is the design of Marcos for #144, still to be exported to VP (D-008, D-017) |
| **Plan approved by** | Ignacio Romero Torres, 23/11/2026 (he delegated the review to Javier for Implementation and Adam for Testing) |

## Decisions
- **Contract (rule 9):** the seven operations of `IAppointmentService` exactly as `C5-design.md` gives them, in
  `es.uclm.esi.iso2.common.appointments.contracts`, with the data classes `Appointment`, `AppointmentStatus`,
  `TimeSlot` and `PendingNeedView` in `...appointments.domain`. `common` is `1.8.0-SNAPSHOT` in every branch: UC06-server
  left it at `1.6.0-SNAPSHOT`, UC07 raises it MINOR to `1.7.0` and UC13 to `1.8.0`, all with `-SNAPSHOT` because
  nothing is released (D-036, decided with Javier on 24/11/2026).
- **Module (rule 9):** `server/c5-appointments`, `artifactId` `server-c5-appointments`, `1.0.0-SNAPSHOT`, depends only on
  `common`; packages `...server.appointments.persistence` and `...server.appointments.service`.
- **Permission:** every operation needs `MANAGE_APPOINTMENTS` (D37, already in C1 1.2.4 for `ADMISSIONS_STAFF`); a denied
  attempt is registered as `APPOINTMENT_ACCESS_DENIED` and throws `AccessDeniedException`.
- **Audit:** `APPOINTMENT_ACCESSED` (the two reads), `APPOINTMENT_BOOKED`, `APPOINTMENT_RESCHEDULED` and
  `APPOINTMENT_CANCELLED`, all with the resource `APPOINTMENT:<id>` except the reads (`appointments`, `PATIENT:<id>`).
- **Free slots (D40):** built from the active schedules of the room of the test type (C2) and its duration, in steps of
  that duration from the start of the schedule while they fit, skipping the slots of the past and the ones that overlap
  an appointment that is not cancelled.
- **Booking:** the need must be pending in C4; the test type must be active; the patient must be active; the slot must be
  of the room of the test type, inside its schedule, not in the past and free. Booking marks the need as booked in C4 and
  takes the physician of the order (D44).
- **Reschedule:** the appointment must exist and not be cancelled; the new slot is checked like a booking, ignoring the
  appointment itself; the id and the need are kept and the status becomes `RESCHEDULED`.
- **Cancel:** the appointment must exist and not be cancelled; the status becomes `CANCELLED` and the need is pending
  again in C4, so that it can be booked in another slot.
- **Ids:** `APT-0001`, `APT-0002`… with a counter that skips the ids the repository already has (pattern of D-037).
- **Tests:** the tests go in `feature/UC13-server-tests`, which forks from this branch (rule 17). The only test of this
  branch is `AppointmentModelTest`, in `common`, because without it the coverage of `common` falls below the 80 % of
  D-033 when the new data classes are added; Adam may move it to the branch of tests if he prefers.
- **Not in this plan:** the client of UC13 (#130), UC15 (#134) and UC14 (#137), the seed data of C5, the waitlists and
  the daily schedule.

- **Slot validation (844405d): decided by the group.** `slotErrors` rejects a slot whose start or end is null, whose
  end is not after its start, that spans more than one day, whose duration does not match the duration of the test type,
  and whose start is not on the grid of the schedule of the room (`startOnGrid`). Recorded by Ignacio, 23/11/2026, in
  answer to Adam's comment on #210.

## Files
| File | Action | What |
|---|---|---|
| `common/pom.xml`, `pom.xml` | change | `common` `1.8.0-SNAPSHOT` and `server-c5-appointments` `1.0.0-SNAPSHOT` (rule 9) |
| `common/.../common/appointments/**` | create | `package-info`, the four data classes and `IAppointmentService` |
| `server/pom.xml` | change | module `c5-appointments` (rule 9) |
| `server/c5-appointments/pom.xml` | create | the module (rule 9) |
| `server/c5-appointments/.../persistence/` | create | `IAppointmentRepository` and `InMemoryAppointmentRepository` |
| `server/c5-appointments/.../service/AppointmentService.java` | create | the control of UC13 |
| `common/src/test/.../appointments/domain/AppointmentModelTest.java` | create | the data classes, to keep the coverage of `common` (see *Decisions*) |
| `agents/architecture.md`, `agents/components.md` | change | the tree and the versions (rule 9) |

## Verification
- [x] Every acceptance criterion of #131: booking only in a free slot of the room of the test type, inside the schedule
      and not in the past, for an active patient; booking marks the need as booked in C4; rescheduling frees the old slot;
      cancelling changes the status and frees the slot; every operation checks the permission and is audited (checked
      with the tests TC13S-01 to 99 in this branch; reviewed by Implementation on 25/11/2026).
- [x] `mvn -B verify` green for `common` and `server-c5-appointments`; `common` keeps at least 80 % of lines and 70 % of
      branches (25/11/2026, this branch: `BUILD SUCCESS` in the 14 modules, all coverage checks met).
- [x] The CI of the pull request is green (the workflow only runs against `main` and `develop`, so this is checked when
      the branch reaches `develop`): #208 reached `develop` on 24/11/2026.

## Status
- **In progress:** nothing.
- **Done:** the contract and the data classes in `common` `1.8.0-SNAPSHOT`, the module `server-c5-appointments` `1.0.0`,
  the repository and `AppointmentService` with the seven operations, the free slots of D40 and the audit; the
  `AccessDeniedException` of `schedulesOf` propagates instead of returning an empty list (Javier's decision, #208);
  the three new validations of `slotErrors` (null start/end, end after start, same day, duration match, start on grid);
  the C4 calls in `bookAppointment` and `cancelAppointment` before saving; `AppointmentSeedData` and `OrderSeedData`
  with the data from Adam (#208); `loadSeedData()` in C4 and C5; `TimeSlot` has no equals/hashCode: nothing compares
  slots (decided with Testing, 23/11/2026); `common` at `1.8.0-SNAPSHOT`.
- **Review of the pull request (#208, 24/11/2026):** `bookAppointment` and `cancelAppointment` save the appointment
  before calling C4, so a failed save no longer leaves the need marked; `cancelAppointment` only ignores the
  "already pending" of C4 and rethrows any other error (changed on 25/11/2026, step 8 below); the orphan Javadoc of `startOnGrid` is gone; `getFreeSlots`
  rejects a range of more than 31 days and `readActiveTestType` rejects a test type with a duration of 0, which used
  to loop for ever — `d933b30`.
- **Fixes from the review of Testing:**
  - Step 1 – D1 `c5f512d`: `startOnGrid` now reads the schedules with the requester.
  - Step 2 – D5 `6a56ef9`: `slotErrors` returns early when the start or the end is null.
  - Step 2b – D6 `53ecd94`: a slot shorter than the test type is rejected.
  - Step 3 – D2 `6721cfd`: the seed appointments are on the first working day after the start (`nextWorkingDay`).
  - Step 4 – D3 `6ca690e`: the C4 seed needs use the clock of the service.
  - Step 5 – D4 `65273ef`: the C4 seed loads only when none of its eight ids exists.
  - Step 6 – `4ce0879`: `TimeSlot` has no equals/hashCode (decision (b)); its Javadoc no longer says that a slot is
    identified by its room and its start, so the class does not promise an identity it does not have.
  - Seeds: checked on 24/11/2026 — the appointments are on the first working day after the start (`nextWorkingDay`)
    and `OrderSeedData` takes the date from the clock of the service, as Adam asked.
- **Review of C4 and C5 (25/11/2026), done by Implementation this once to close C4 and C5 (#131 reopened):**
  - Step 7 – the ids of the appointments use an `AtomicInteger`, as C4 does (`1412e4e`).
  - Step 8 – `cancelAppointment` no longer reads the text of C4: it asks C4 for the pending needs and, when the need is
    already pending, it does not mark it again (`releaseNeed`); the method `onlyAlreadyPending` is removed. First the
    test TC13S-98 (`ada5425`), then the code (`1412e4e`); TC13S-99 covers the other branch of the loop (`26ad7ed`).
    TC13S-27, 92, 93 and 94 still pass without changes; TC13S-92 and 94 no longer exercise the reading of the text.
    The cost is one more call to C4 in each cancellation.
  - The Javadoc of `startOnGrid` says "at least one active schedule", as the code does (`1412e4e`).
  - The "D41" of the test design is the decision D41 of `doc/AN-C5-DECISIONS.md`, not D-041: it stays as it is.
- **State for Adam (Testing):** to cover in `feature/UC13-server-tests`: the permission and `APPOINTMENT_ACCESS_DENIED`;
  the seven operations with their messages; the free slots for a schedule of a room (days, duration, occupied and
  cancelled appointments, range with `toDate` before `fromDate`); the booking of a pending need, of a need already booked,
  of a slot of another room, outside the schedule, in the past and taken; the active and inactive patient; the reschedule
  of a booked, cancelled and unknown appointment; the cancellation and the need pending again in C4; the four audit
  actions; the cases of D1, D2, D5, D6 (TC13S-70, 72, 73, 77, 78, 81).
- **Next step:** none. Testing added TC13S-67 to 97 (#209) and the pull request #208 was merged on 24/11/2026.
- **Open question (Javier):** `bookAppointment` and `cancelAppointment` still have no transaction: if C4 fails after the
  appointment is saved, the need keeps its old status. It is the order chosen on 24/11/2026 (save first), not changed.
- **Last update:** 25/11/2026, Javier — steps 7 and 8 and the Javadoc of `startOnGrid`, with the tests TC13S-98 and 99
  written by Implementation this once; the Verification boxes ticked. Before: 24/11/2026, Ignacio (Javadoc of
  `TimeSlot`, the seed dates and the fixes of the review of the pull request).
