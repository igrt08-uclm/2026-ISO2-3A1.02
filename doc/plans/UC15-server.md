# Plan – UC15 – Server (#134)

| | |
|---|---|
| **Sub-issue** | #134 · branch `feature/UC15-server` |
| **Use case** | #132 · FR15 · C5 `1.1.0` |
| **Specification** | #132, #138 §6, [doc/AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) (D36, D37, D38, D40, D41, D42) and `doc/analysis/UC15-manage-waitlist-sequence.puml` |
| **Design** | [doc/design/C5-server-design.puml](../design/C5-server-design.puml), [doc/design/common/C5-common.puml](../design/common/C5-common.puml) (approved with [PR #169](https://github.com/igrt08-uclm/2026-ISO2-3A1.02/pull/169)) |
| **Plan approved by** | Ignacio Romero, 26/11/2026; changes of Q6 to Q10 and the cases TC15S-01 to TC15S-34 approved by Javier Villanueva Crespo, 26/11/2026 |

## Questions and answers

- **Q1 – Tests in this branch?** **A:** No. The member asked for the implementation only (26/11/2026): there is no test design table for UC15 yet, and a test may not invent its expected results (rule 17). The JUnit tests go in `feature/UC15-server-tests`, branched from this one, when Testing delivers the table. **Consequence:** the JaCoCo rule of D-033 (80 % lines, 70 % branches) fails while the new classes have no tests, so each step is checked with `mvn -B -ntp verify -Djacoco.skip=true`, and the pull request is not merged until the tests arrive.
- **Q2 – Version of `common`?** **A:** UC15 adds `WaitlistEntry`, `WaitlistStatus` and `IWaitlistService` to `common`, so it raises MINOR to `1.9.0-SNAPSHOT`; UC14 raises it to `1.10.0-SNAPSHOT`, and `v0.5.0` publishes `1.10.0` (D-036).
- **Q3 – Who creates the branches?** **A:** The agent (member's authorisation of 26/11/2026), as with `release/0.4.0`. Rule 7 of AGENTS.md is kept in spirit: one branch per sub-issue.
- **Q4 – Seed data?** **A:** `AppointmentSeedData` gains `waitlistEntries(day)` and `WaitlistService.loadSeedData()` loads it, as the design shows (`WaitlistService ..> AppointmentSeedData`).
- **Q5 – Where do the priorities come from?** **A:** From C2 (`ISystemParameterService.getPriorities`), as the design note N5 says: index in `getPriorities()` and then `addedAt` ascending. A tier that is not a configured priority is rejected (alternative flow 4b of the analysis).
- **Q6 – Who finishes the sub-issue?** **A:** Javier Villanueva Crespo takes over the code and the tests (26/11/2026). The tests still go in `feature/UC15-server-tests` (Q1), which the member creates from this branch; it is merged back into this one.
- **Q7 – Which entry does `proposePatientForOpening` return?** **A:** Design (Marcos Loro, 25/11/2026): the first entry of `getWaitlist` whose patient is active (`IPatientService.getPatient(...).isActive()`) and whose scheduling need is still pending in C4; `null` if there is none. The service receives `IPatientService`, as the design shows, and the Javadoc of `IWaitlistService` is corrected.
- **Q8 – Errors of `addToWaitlist`.** **A:** As its contract says, every reason comes together: the tier, the pending need and the need that already waits.
- **Q9 – Seed entry `WL-0002`.** **A:** It waits for `NEED-0003`, which is booked. It moves to `NEED-0004` (`P-0003`, `XRAY-CHEST`, `ROUTINE`), so the two seed entries are of different test types (Javier Villanueva Crespo, 26/11/2026; Testing owns the seed data and is told).
- **Q10 – Red tests.** **A:** The commit of the tests in `feature/UC15-server-tests` leaves red the cases of Q7, Q8 and Q9 until step 6 fixes the code; it is the only step without a green build.

## Test cases (S.04.01), approved by Javier Villanueva Crespo on 26/11/2026

Test data: `lmartin` has `MANAGE_APPOINTMENTS` and `mgarcia` has not; tiers `URGENT`, `PREFERENTIAL`, `ROUTINE` and `OLD` (inactive); patients `P-0001` and `P-0002` active, `P-0004` inactive.

| Case | Operation | Input | Expected result |
|---|---|---|---|
| TC15S-01 | the seven | requester without permission | access denied, `WAITLIST_ACCESS_DENIED` audited, nothing changes |
| TC15S-02 | consult | mixed tiers | ordered by tier |
| TC15S-03 | consult | same tier | oldest first |
| TC15S-04 | consult | removed, booked and other test type entries | not returned |
| TC15S-05 | consult | test type `null` or blank | validation error |
| TC15S-06 | add | valid data | `WAITING` entry `WL-0001`, `WAITLIST_CHANGED` audited |
| TC15S-07 | add | need and tier missing | both errors, nothing saved |
| TC15S-08 | add | unknown tier | validation error |
| TC15S-09 | add | inactive tier | validation error |
| TC15S-10 | add | need not pending | validation error |
| TC15S-11 | add | need already waiting | validation error |
| TC15S-12 | add | invalid tier and need not pending | both errors |
| TC15S-13 | change tier | valid | order changes, date kept, audited |
| TC15S-14 | change tier | entry and tier missing | both errors |
| TC15S-15 | change tier | unknown or not waiting entry | validation error |
| TC15S-16 | change tier | invalid tier | validation error, no change |
| TC15S-17 | remove | valid | `REMOVED`, kept, `WAITLIST_ENTRY_REMOVED` audited |
| TC15S-18 | remove | blank id | validation error |
| TC15S-19 | remove | unknown or already removed | validation error |
| TC15S-20 | openings | valid range | the free slots of UC13, audited |
| TC15S-21 | openings | blank test type | validation error |
| TC15S-22 | openings | reversed range | validation error of UC13 |
| TC15S-23 | propose | first entry valid | that entry |
| TC15S-24 | propose | first with inactive patient | the next one |
| TC15S-25 | propose | first with need already booked | the next one |
| TC15S-26 | propose | nobody waiting | `null` |
| TC15S-27 | propose | nobody valid | `null` |
| TC15S-28 | propose | test type and opening missing | both errors |
| TC15S-29 | fill | valid | appointment created, entry `BOOKED`, `OPENING_FILLED` audited |
| TC15S-30 | fill | entry and opening missing | both errors |
| TC15S-31 | fill | unknown or not waiting entry | validation error, no appointment |
| TC15S-32 | fill | opening already taken | validation error, entry still waiting |
| TC15S-33 | seed | empty repository, then with data | loaded only once; the needs of the entries are pending |
| TC15S-34 | model and repository | save, replace, find, getters | what was saved; `null` if it does not exist |

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
| `common/.../appointments/contracts/IWaitlistService.java` | change (step 6) | Javadoc of `proposePatientForOpening` (Q7) |
| `doc/testing/test-cases/UC15-server-test-design.md` | create (step 5) | design of the cases of this plan |
| `server/c5-appointments/src/test/java/.../service/WaitlistServiceTest.java`, `.../persistence/InMemoryWaitlistRepositoryTest.java` | create (step 5) | tests of the cases |
| `server/c5-appointments/src/test/java/.../service/TestDoubles.java`, `common/src/test/java/.../appointments/` | change (step 5) | test doubles and the model case |

## Steps

- [x] 1. `common`: `WaitlistEntry`, `WaitlistStatus`, `IWaitlistService`; `common` to `1.9.0-SNAPSHOT`. (8be6f37)
- [x] 2. `server`: `IWaitlistRepository`, `InMemoryWaitlistRepository`, `waitlistEntries()`; `server-c5-appointments` to `1.1.0-SNAPSHOT`. (8be6f37)
- [x] 3. `server`: `WaitlistService` (permission `MANAGE_APPOINTMENTS`, validations of the alternative flows, ordering by tier and date, audit `WAITLIST_CHANGED`, `WAITLIST_ENTRY_REMOVED`, `OPENING_FILLED`, and the booking of UC15 through `IAppointmentService`). (8be6f37)
- [x] 4. `server`: `WaitlistService` receives `IPatientService` (Q7). (31904cc)
- [ ] 5. `feature/UC15-server-tests`: tests TC15S-01 to TC15S-34 and their design.
- [ ] 6. Merge the tests; fix the proposal, the errors of `addToWaitlist`, the seed and the Javadoc (Q7 to Q9).
- [ ] 7. Verification and close of the plan.

## Verification

- [ ] Acceptance criteria of #134: the waitlist is ordered by priority tier and then by date; a cancelled appointment becomes an opening and filling it books through UC13.
- [ ] `mvn -B verify` is green: **blocked** by the coverage rule until the tests of `feature/UC15-server-tests` are merged into this branch. Until then, `mvn -B -ntp verify -Djacoco.skip=true` must be green (compile and the existing tests).

## Status

- **In progress:** nothing
- **Next step:** step 5: the member creates `feature/UC15-server-tests` from this branch; then `WaitlistServiceTest`
- **Open questions:** none
- **Last update:** 26/11/2026, Javier Villanueva Crespo
