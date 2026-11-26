# Plan – UC13 – Client (#130)

| | |
|---|---|
| **Sub-issue** | #130 *UC13 – Client (C5 v1.0.0)* · branch `feature/UC13-client`, from `feature/UC13-server` (#208) |
| **Use case** | #129 *UC13 – Manage the appointment schedule (C5 v1.0.0)* · FR13 · `client-c5-appointments` `1.0.0-SNAPSHOT` |
| **Specification** | #130; `doc/plans/C5-design.md` (presentation section); `doc/plans/UC13-server.md` |
| **Plan approved by** | Ignacio Romero Torres, 23/11/2026 (review delegated to Javier for Implementation and Adam for Testing) |

## Decisions
- **Screen:** `IU_ManageAppointments` in `es.uclm.esi.iso2.client.appointments.presentation`, a `JPanel` like the other
  screens (D-030). On the left, the pending scheduling needs of C4 with the patient, the test type, the priority and the
  due date. On the right, a day (`dd/MM/yyyy`), the button *Find slots*, the free slots of the test type of the selected
  need, and the button *Book*; below, every appointment with *Move* (to the selected slot) and *Cancel appointment*.
- **Only the contract:** the screen uses `IAppointmentService` of `common` and never a class of `server`.
- **Messages:** the errors of `ValidationException` are shown as they come, joined in red; the success messages say the
  id of the appointment and its new moment; a denied access disables every action and explains it.
- **Confirmations:** booking and cancelling ask for confirmation with the `Predicate<String>` of the project.
- **Versions (rule 9):** `common` goes from `1.7.0-SNAPSHOT` to `1.8.0-SNAPSHOT` (UC07 raised it to 1.7.0 and this
  branch merges after it); new module `client-c5-appointments` `1.0.0-SNAPSHOT`; `client-c1-access` goes to
  `1.2.7-SNAPSHOT` (UC06-client took it to 1.2.5 and UC07-client to 1.2.6) and depends on `server-c4-orders`, `server-c5-appointments` and `client-c5-appointments`, because
  `Main` starts the services.
- **Tests:** the tests of the screen are Adam's (rule 17) and go in `feature/UC13-client-tests`; this branch is the
  implementation. The screen exposes `getNeedsTable`, `getSlotsTable`, `getAppointmentsTable` and `getStatusMessage` for
  them.
- **Move asks for confirmation (N-08): decided by the group.** *Move* asks "Move the appointment … to …?" before
  calling C5, like *Book* and *Cancel appointment*. Recorded by Ignacio, 24/11/2026; the code already did it and
  TC13C-20 and TC13C-45 check it.
- **Slots emptying (N-03, N-04): decided by the group.** After a booking, a move or a cancellation the slots table is emptied, and selecting a need or an appointment empties the slots, so a slot of another test type cannot be reused. Recorded by Ignacio, 23/11/2026, in answer to Adam's comment on #210 (commit 844405d).

## Files
| File | Action |
|---|---|
| `client/c5-appointments/pom.xml` | create the module |
| `client/c5-appointments/.../presentation/IU_ManageAppointments.java` | create the screen |
| `client/pom.xml`, `pom.xml` | the module and its managed version |
| `client/c1-access/pom.xml` | `1.2.7-SNAPSHOT` and the three dependencies |
| `client/c1-access/.../access/presentation/IU_Welcome.java` | the button *Manage appointments* with `MANAGE_APPOINTMENTS` |
| `client/c1-access/.../client/Main.java` | the service and the navigation |
| `doc/plans/UC13-client.md` | this plan |

## Verification
- [x] `mvn -B verify -DskipTests -Djacoco.skip=true` green for `client-c1-access` and its tree.
- [ ] The usability of the screen walked in the running application (Testing: this machine has no desktop).
- [x] The tests of the screen: they are in `develop` with #209 and #210 (`IU_ManageAppointmentsTest`, 46 tests with
      TC13C-51); `mvn -B clean verify` on this branch (25/11/2026) gives `BUILD SUCCESS` in the 14 modules and all
      coverage checks met; `IU_ManageAppointments` has 267/276 lines and 60/68 branches.

## Status
- **In progress:** nothing; the implementation is complete in `feature/UC13-client`.
- **Fixes from the review of Testing:**
  - `89f2344`: F-01, the confirmation is kept after reloading the data.
  - `69bed6c`: F-05, an injectable clock for the default day.
- **Next step:** Testing adds TC13C-43, 44 and the clock cases.
- **Review of the pull request (#210, 24/11/2026):** fixed in `ebf611a`: the two tables clear each other's selection,
  so *Find slots* and *Book* can no longer mix the test type of an appointment with the need of another one; the
  failures of `getFreeSlots`, `bookAppointment`, `rescheduleAppointment` and `cancelAppointment` are caught; the slots
  are emptied when `loadData` fails and when *Find slots* rejects its input; the count of slots restores the colour;
  the duplicated Javadoc is gone; the imports of `Main` are back in alphabetical order; the line 36 is wrapped; the
  row *Common* is in `agents/components.md`; `common` is `1.8.0-SNAPSHOT`. **Pending, for Testing:** the names instead
  of the ids (TC13C-01, 09, 20 and 40), removing `isCancelled` (TC13C-32) and the keys Alt+F/B/M/C/K and Alt+D
  (TC13C-29 expects Alt+B on *Back*); the code changes with the new values, not before (rule 17).
- **Review of C4 and C5 (25/11/2026), done by Implementation this once to close C4 and C5 (#130 reopened):** the label
  *Day* has the key Alt+D and points to its field, with the test TC13C-51 first (`3a8339e`) and the code after
  (`6864910`); an import of `AppointmentStatus` that nothing used is removed; TC13C-29 now expects Alt+K on *Back*
  in the design (the code has it since `0428ec0`) and TC13C-50 (one table clears the other) is in the design.
  `isCancelled` is already gone (`0428ec0`). Design answered in #130 on 25/11/2026: the ids stay in the table of
  appointments (N-05, no change of `common`), `onSelectNeed` is removed from the diagrams (F-06) and *Move* with a
  question is right (N-08).
- **Open questions:** N-03, N-04 and N-08 are resolved (decided by the group, see *Decisions*). N-02 is closed: the keys
  are Alt+F, Alt+B, Alt+M, Alt+C, Alt+K and Alt+D. N-05 and F-06 are closed by Design (see above). Still open: N-01,
  N-06 and N-07 (Adam, #210). The wiring of
  `Main` and the constructors of `IU_Welcome` will have to be rebased when the clients of UC06 (#204) and UC07 (#207)
  are merged, because the three add a button and a full constructor on the same branch point.
- **Last update:** 25/11/2026, Javier — Alt+D with its test TC13C-51, the design of TC13C-29 and TC13C-50, the
  decisions of Design and the Verification boxes ticked. Before: 24/11/2026, Ignacio (fixes of the review of the pull
  request; the keys, the names and `isCancelled` wait for Testing).
