# Plan – UC14 – Client (#136)

| | |
|---|---|
| **Sub-issue** | #136 · branch `feature/UC14-client` (branched from `feature/UC14-server`, because the screen needs its contract) |
| **Use case** | #135 · FR15 · C5 `1.2.0` |
| **Specification** | #135, [doc/AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) (D43, D44) |
| **Design** | [doc/design/C5-client-design.puml](../design/C5-client-design.puml) (`IU_DailySchedule`, note N3) |
| **Plan approved by** | Ignacio Romero, 26/11/2026 |

## Questions and answers

- **Q1 – Tests in this branch?** **A:** No. The member asked for the implementation only (26/11/2026) and there is no test design table for UC14 (rule 17). They go in `feature/UC14-client-tests`, branched from this one. The steps are checked with `mvn -B -ntp verify -Djacoco.skip=true`; the pull request waits for the tests (D-033).
- **Q2 – How is the screen reached?** **A:** With a new button <em>Daily schedule</em> (Alt+D) in `IU_Welcome`, shown only with `MANAGE_APPOINTMENTS`, after <em>Manage appointments</em>. The previous constructors pass `null`, so no other behaviour changes.
- **Q3 – Which repository does `Main` give to the two services?** **A:** The same `InMemoryAppointmentRepository` for `AppointmentService` (UC13) and `DailyScheduleService` (UC14), so the schedule shows the appointments that UC13 books.

## Files

| File | Action | What |
|---|---|---|
| `client/c5-appointments/src/main/java/…/presentation/IU_DailySchedule.java` | create | screen of the design: `onSelectDay`, `onSelectRoom`, `onSelectPhysician`, `showErrors`, `showAccessDenied` |
| `client/c1-access/src/main/java/…/presentation/IU_Welcome.java` | change | button <em>Daily schedule</em> (Alt+D) |
| `client/c1-access/src/main/java/…/Main.java` | change | the repository shared with UC13, the `DailyScheduleService` and the screen |
| `client/c5-appointments/pom.xml`, `pom.xml` | change | `client-c5-appointments` `1.2.0-SNAPSHOT` |
| `doc/testing/test-cases/UC14-client-test-design.md` | later | in `feature/UC14-client-tests` |

## Steps

- [x] 1. `IU_DailySchedule`: the schedule per room and the worklists of a room or of a physician, in time order, with the empty-state notices.
- [x] 2. `IU_Welcome`: <em>Daily schedule</em> (Alt+D), only with `MANAGE_APPOINTMENTS`.
- [x] 3. `Main`: the shared repository, the service and the screen; `client-c5-appointments` to `1.2.0-SNAPSHOT`.
- [ ] 4. Hand the client part over to Testing in the pull request.

## Verification

- [ ] Acceptance criteria of #136: the diagram and the models are the ones of the design; the screen shows the schedule per room and the worklists in time order and never shows a cancelled appointment.
- [ ] `mvn -B verify` is green: **blocked** by the coverage rule until the tests arrive. Checked with `-Djacoco.skip=true`: BUILD SUCCESS with the 1,348 existing tests green.

## Status

- **In progress:** nothing
- **Next step:** step 4, the handoff; then the four test branches of UC15 and UC14
- **Open questions:** the test tables of UC15 and UC14 (Testing)
- **Last update:** 26/11/2026, Ignacio Romero
