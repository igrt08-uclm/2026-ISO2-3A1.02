# Plan – UC14 – Server (#137)

| | |
|---|---|
| **Sub-issue** | #137 · branch `feature/UC14-server` |
| **Use case** | #135 · FR15 · C5 `1.2.0` |
| **Specification** | #135, [doc/AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) (D43, D44) and `doc/analysis/UC14-daily-schedule-sequence.puml` |
| **Design** | [doc/design/C5-server-design.puml](../design/C5-server-design.puml), [doc/design/common/C5-common.puml](../design/common/C5-common.puml) |
| **Plan approved by** | Ignacio Romero, 26/11/2026 |

## Questions and answers

- **Q1 – Tests in this branch?** **A:** No. The member asked for the implementation only (26/11/2026) and there is no test design table for UC14 (rule 17). They go in `feature/UC14-server-tests`, branched from this one. Each step is checked with `mvn -B -ntp verify -Djacoco.skip=true`; the pull request waits for the tests (D-033).
- **Q2 – Version of `common`?** **A:** `1.10.0-SNAPSHOT`: UC14 adds `ScheduleEntry` and `IDailyScheduleService` (D-036). `v0.5.0` publishes `1.10.0`; `server-c5-appointments` goes to `1.2.0-SNAPSHOT`.
- **Q3 – Which repository does it read?** **A:** The same `IAppointmentRepository` that UC13 fills, so the schedule sees the appointments that UC13 books. `Main` creates the repository and gives it to both services.
- **Q4 – Cancelled appointments?** **A:** They are never shown (D43): only `BOOKED` and `RESCHEDULED`, in time order, grouped by room, and inside each room by time.

## Files

| File | Action | What |
|---|---|---|
| `common/src/main/java/…/appointments/domain/ScheduleEntry.java` | create | entity of the design: the appointment, the patient, the test type and its preparation |
| `common/src/main/java/…/appointments/contracts/IDailyScheduleService.java` | create | the three read operations of the design |
| `common/pom.xml`, `pom.xml` | change | `common` `1.10.0-SNAPSHOT` |
| `server/c5-appointments/src/main/java/…/service/DailyScheduleService.java` | create | `Control_DailySchedule`: permission, ordering, audit, enrichment from C3 and C2 |
| `server/c5-appointments/pom.xml`, `pom.xml` | change | `server-c5-appointments` `1.2.0-SNAPSHOT` |
| `doc/testing/test-cases/UC14-server-test-design.md` | later | in `feature/UC14-server-tests` |

## Steps

- [x] 1. `common`: `ScheduleEntry` and `IDailyScheduleService`; `common` to `1.10.0-SNAPSHOT`.
- [x] 2. `server`: `DailyScheduleService`; `server-c5-appointments` to `1.2.0-SNAPSHOT`.
- [ ] 3. Hand the schedule part over to Testing in the pull request.

## Verification

- [ ] Acceptance criteria of #137: the daily schedule and the worklists only contain the booked appointments of that day, ordered by time; every access is registered in the audit log.
- [ ] `mvn -B verify` is green: **blocked** by the coverage rule until the tests arrive; checked with `-Djacoco.skip=true`.

## Status

- **In progress:** nothing
- **Next step:** step 3, the handoff; then `feature/UC14-client`
- **Open questions:** the test table of UC14 (Testing)
- **Last update:** 26/11/2026, Ignacio Romero
