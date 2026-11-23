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
  `TimeSlot` and `PendingNeedView` in `...appointments.domain`. `common` goes from `1.7.0-SNAPSHOT` to `1.8.0-SNAPSHOT`, because UC07 takes it to `1.7.0-SNAPSHOT` and this branch merges after that one.
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
- [ ] Every acceptance criterion of #131: booking only in a free slot of the room of the test type, inside the schedule
      and not in the past, for an active patient; booking marks the need as booked in C4; rescheduling frees the old slot;
      cancelling changes the status and frees the slot; every operation checks the permission and is audited.
- [ ] `mvn -B verify` green for `common` and `server-c5-appointments`; `common` keeps at least 80 % of lines and 70 % of
      branches.
- [ ] The CI of the pull request is green (the workflow only runs against `main` and `develop`, so this is checked when
      the branch reaches `develop`).

## Status
- **In progress:** nothing; steps 1 to 3 are done.
- **Done:** the contract and the data classes in `common` `1.8.0`, the module `server-c5-appointments` `1.0.0`,
  the repository and `AppointmentService` with the seven operations, the free slots of D40 and the audit; the
  `AccessDeniedException` of `schedulesOf` propagates instead of returning an empty list (Javier's decision, #208);
  the three new validations of `slotErrors` (null start/end, end after start, same day, duration match, start on grid);
  the C4 calls in `bookAppointment` and `cancelAppointment` before saving; `AppointmentSeedData` and `OrderSeedData`
  with the data from Adam (#208); `loadSeedData()` in C4 and C5; `equals`/`hashCode` in `TimeSlot`; `common` at
  `1.8.0`.
- **State for Adam (Testing):** to cover in `feature/UC13-server-tests`: the permission and `APPOINTMENT_ACCESS_DENIED`;
  the seven operations with their messages; the free slots for a schedule of a room (days, duration, occupied and
  cancelled appointments, range with `toDate` before `fromDate`); the booking of a pending need, of a need already booked,
  of a slot of another room, outside the schedule, in the past and taken; the active and inactive patient; the reschedule
  of a booked, cancelled and unknown appointment; the cancellation and the need pending again in C4; the four audit
  actions.
- **Next step:** the branch `feature/UC13-server-tests` and, after that, the client (#130).
- **Last update:** 23/11/2026, Ignacio.
