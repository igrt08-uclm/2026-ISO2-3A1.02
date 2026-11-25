# Plan – UC15 – Server (#134)

| | |
|---|---|
| **Sub-issue** | #134 · branch `feature/UC15-server` |
| **Use case** | #132 · FR15 · C5 `1.1.0` |
| **Specification** | #132, #138 §6, [doc/AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) (D36, D37, D38, D40, D41, D42) and `doc/analysis/UC15-manage-waitlist-sequence.puml` |
| **Design** | [doc/design/C5-server-design.puml](../design/C5-server-design.puml), [doc/design/common/C5-common.puml](../design/common/C5-common.puml) (approved with [PR #169](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/pull/169)) |
| **Plan approved by** | Ignacio Romero, 26/11/2026 |

## Questions and answers

- **Q1 – Tests in this branch?** **A:** No. The member asked for the implementation only (26/11/2026): there is no test design table for UC15 yet, and a test may not invent its expected results (rule 17). The JUnit tests go in `feature/UC15-server-tests`, branched from this one, when Testing delivers the table. **Consequence:** the JaCoCo rule of D-033 (80 % lines, 70 % branches) fails while the new classes have no tests, so each step is checked with `mvn -B -ntp verify -Djacoco.skip=true`, and the pull request is not merged until the tests arrive.
- **Q2 – Version of `common`?** **A:** UC15 adds `WaitlistEntry`, `WaitlistStatus` and `IWaitlistService` to `common`, so it raises MINOR to `1.9.0-SNAPSHOT`; UC14 raises it to `1.10.0-SNAPSHOT`, and `v0.5.0` publishes `1.10.0` (D-036).
- **Q3 – Who creates the branches?** **A:** The agent (member's authorisation of 26/11/2026), as with `release/0.4.0`. Rule 7 of AGENTS.md is kept in spirit: one branch per sub-issue.
- **Q4 – Seed data?** **A:** `AppointmentSeedData` gains `waitlistEntries(day)` and `WaitlistService.loadSeedData()` loads it, as the design shows (`WaitlistService ..> AppointmentSeedData`).
- **Q5 – Where do the priorities come from?** **A:** From C2 (`ISystemParameterService.getPriorities`), as the design note N5 says: index in `getPriorities()` and then `addedAt` ascending. A tier that is not a configured priority is rejected (alternative flow 4b of the analysis).

## Files

| File | Action | What |
|---|---|---|
| `common/src/main/java/es/uclm/esi/iso2/common/appointments/domain/WaitlistEntry.java` | create | entity of the design (D42) |
| `common/src/main/java/es/uclm/esi/iso2/common/appointments/domain/WaitlistStatus.java` | create | `WAITING`, `BOOKED`, `REMOVED` |
| `common/src/main/java/es/uclm/esi/iso2/common/appointments/contracts/IWaitlistService.java` | create | the seven methods of the design (D42) |
| `common/pom.xml`, `pom.xml` | change | `common` `1.9.0-SNAPSHOT` |
| `server/c5-appointments/src/main/java/es/uclm/esi/iso2/server/appointments/persistence/IWaitlistRepository.java` | create | repository of the design |
| `server/c5-appointments/src/main/java/es/uclm/esi/iso2/server/appointments/persistence/InMemoryWaitlistRepository.java` | create | in-memory repository (D-030) |
| `server/c5-appointments/src/main/java/es/uclm/esi/iso2/server/appointments/service/WaitlistService.java` | create | `Control_ManageWaitlist` (D42) |
| `server/c5-appointments/src/main/java/es/uclm/esi/iso2/server/appointments/service/AppointmentSeedData.java` | change | `waitlistEntries(day)` |
| `server/c5-appointments/pom.xml`, `pom.xml` | change | `server-c5-appointments` `1.1.0-SNAPSHOT` |
| `doc/testing/test-cases/UC15-server-test-design.md` | later | in `feature/UC15-server-tests`, with the member's table |

## Steps

- [x] 1. `common`: `WaitlistEntry`, `WaitlistStatus`, `IWaitlistService`; `common` to `1.9.0-SNAPSHOT`. (8be6f37)
- [x] 2. `server`: `IWaitlistRepository`, `InMemoryWaitlistRepository`, `waitlistEntries()`; `server-c5-appointments` to `1.1.0-SNAPSHOT`. (8be6f37)
- [x] 3. `server`: `WaitlistService` (permission `MANAGE_APPOINTMENTS`, validations of the alternative flows, ordering by tier and date, audit `WAITLIST_CHANGED`, `WAITLIST_ENTRY_REMOVED`, `OPENING_FILLED`, and the booking of UC15 through `IAppointmentService`).
- [ ] 4. Hand the waitlist part over to Testing in the pull request.

## Verification

- [ ] Acceptance criteria of #134: the waitlist is ordered by priority tier and then by date; a cancelled appointment becomes an opening and filling it books through UC13.
- [ ] `mvn -B verify` is green: **blocked** by the coverage rule until the tests of `feature/UC15-server-tests` are merged into this branch. Until then, `mvn -B -ntp verify -Djacoco.skip=true` must be green (compile and the existing tests).

## Status

- **In progress:** nothing
- **Next step:** step 3, `WaitlistService.fillOpening` and the rest of the methods
- **Open questions:** the test table of UC15 (Testing); the branch `feature/UC15-server-tests` starts from this one when it exists
- **Last update:** 26/11/2026, Ignacio Romero
