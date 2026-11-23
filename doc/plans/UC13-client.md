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
- **Versions (rule 9):** new module `client-c5-appointments` `1.0.0-SNAPSHOT`; `client-c1-access` goes to
  `1.2.5-SNAPSHOT` and depends on `server-c4-orders`, `server-c5-appointments` and `client-c5-appointments`, because
  `Main` starts the services.
- **Tests:** the tests of the screen are Adam's (rule 17) and go in `feature/UC13-client-tests`; this branch is the
  implementation. The screen exposes `getNeedsTable`, `getSlotsTable`, `getAppointmentsTable` and `getStatusMessage` for
  them.

## Files
| File | Action |
|---|---|
| `client/c5-appointments/pom.xml` | create the module |
| `client/c5-appointments/.../presentation/IU_ManageAppointments.java` | create the screen |
| `client/pom.xml`, `pom.xml` | the module and its managed version |
| `client/c1-access/pom.xml` | `1.2.5-SNAPSHOT` and the three dependencies |
| `client/c1-access/.../access/presentation/IU_Welcome.java` | the button *Manage appointments* with `MANAGE_APPOINTMENTS` |
| `client/c1-access/.../client/Main.java` | the service and the navigation |
| `doc/plans/UC13-client.md` | this plan |

## Verification
- [x] `mvn -B verify -DskipTests -Djacoco.skip=true` green for `client-c1-access` and its tree.
- [ ] The usability of the screen walked in the running application (Testing: this machine has no desktop).
- [ ] The tests of the screen in `feature/UC13-client-tests`.

## Status
- **In progress:** nothing; the implementation is complete in `feature/UC13-client`.
- **Next step:** the pull request (#130) and, after it, the tests of the screen.
- **Open questions:** none. The wiring of `Main` will have to be rebased when the clients of UC06 (#204) and UC07 (#207)
  are merged, because all three add a button to `IU_Welcome` on the same branch point.
- **Last update:** 23/11/2026, Ignacio.
