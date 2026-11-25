# Plan – UC15 – Client (#133)

| | |
|---|---|
| **Sub-issue** | #133 · branch `feature/UC15-client` (branched from `feature/UC15-server`, because the screen needs its contract) |
| **Use case** | #132 · FR15 · C5 `1.1.0` |
| **Specification** | #132, [doc/AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) (D42) and `doc/analysis/UC15-manage-waitlist-sequence.puml` |
| **Design** | [doc/design/C5-client-design.puml](../design/C5-client-design.puml) (`IU_ManageWaitlist`, note N2) |
| **Plan approved by** | Ignacio Romero, 26/11/2026 |

## Questions and answers

- **Q1 – Tests in this branch?** **A:** No. The member asked for the implementation only (26/11/2026); there is no test design table for UC15, so the JUnit tests go in `feature/UC15-client-tests`, branched from this one, when Testing delivers it. The steps are checked with `mvn -B -ntp verify -Djacoco.skip=true` and the pull request waits for the tests (JaCoCo, D-033).
- **Q2 – How is the screen reached?** **A:** With a new button <em>Manage waitlist</em> (Alt+W) in `IU_Welcome`, shown only with `MANAGE_APPOINTMENTS`, after <em>Manage appointments</em>. The existing constructors keep their behaviour: the new action is `null` in them, so no other test changes.
- **Q3 – Which contracts does the screen use?** **A:** The three of the design: `IWaitlistService` (the operations), `IAppointmentService` (the pending needs) and `ISystemParameterService` (the test types and the tiers). It never uses a server class.

## Files

| File | Action | What |
|---|---|---|
| `client/c5-appointments/src/main/java/…/presentation/IU_ManageWaitlist.java` | create | screen of the design: `loadData`, `onSelectTestType`, `onAddEntry`, `onChangePriority`, `onRemoveEntry`, `onSelectOpening`, `onConfirmFillOpening`, `showErrors`, `showAccessDenied`, `showConfirmation` |
| `client/c1-access/src/main/java/…/presentation/IU_Welcome.java` | change | button <em>Manage waitlist</em> (Alt+W) and the constructor that receives its action |
| `client/c1-access/src/main/java/…/Main.java` | change | the `WaitlistService`, its seed data and the screen |
| `client/c5-appointments/pom.xml`, `pom.xml` | change | `client-c5-appointments` `1.1.0-SNAPSHOT` |
| `doc/testing/test-cases/UC15-client-test-design.md` | later | in `feature/UC15-client-tests` |

## Steps

- [x] 1. `IU_ManageWaitlist` with the ten methods of the design and the empty-state notices of note N2.
- [x] 2. `IU_Welcome`: <em>Manage waitlist</em> (Alt+W), only with `MANAGE_APPOINTMENTS`.
- [x] 3. `Main`: the service, its seed entries and the screen; `client-c5-appointments` to `1.1.0-SNAPSHOT`.
- [ ] 4. Hand the client part over to Testing in the pull request.

## Verification

- [ ] Acceptance criteria of #133: the diagrams and models are the ones of the design; the screen shows the waitlist ordered by tier and date, adds and changes entries, removes with confirmation, and fills an opening through UC13.
- [ ] `mvn -B verify` is green: **blocked** by the coverage rule until the tests of `feature/UC15-client-tests` are merged. Checked with `-Djacoco.skip=true`: BUILD SUCCESS, 1,348 tests green.

## Status

- **In progress:** nothing
- **Next step:** step 4, the handoff; then `feature/UC14-server` and `feature/UC14-client`
- **Open questions:** the test table of UC15 (Testing)
- **Last update:** 26/11/2026, Ignacio Romero
