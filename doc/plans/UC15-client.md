# Plan – UC15 – Client (#133)

| | |
|---|---|
| **Sub-issue** | #133 · branch `feature/UC15-client` (branched from `feature/UC15-server`, because the screen needs its contract) |
| **Use case** | #132 · FR15 · C5 `1.1.0` |
| **Specification** | #132, [doc/AN-C5-DECISIONS.md](../AN-C5-DECISIONS.md) (D42) and `doc/analysis/UC15-manage-waitlist-sequence.puml` |
| **Design** | [doc/design/C5-client-design.puml](../design/C5-client-design.puml) (`IU_ManageWaitlist`, note N2) |
| **Plan approved by** | Ignacio Romero, 26/11/2026; Q4 to Q9 and the cases TC15C-01 to TC15C-34 approved by Javier Villanueva Crespo, 26/11/2026 |

## Questions and answers

- **Q1 – Tests in this branch?** **A:** No. The member asked for the implementation only (26/11/2026); there is no test design table for UC15, so the JUnit tests go in `feature/UC15-client-tests`, branched from this one, when Testing delivers it. The steps are checked with `mvn -B -ntp verify -Djacoco.skip=true` and the pull request waits for the tests (JaCoCo, D-033).
- **Q2 – How is the screen reached?** **A:** With a new button <em>Manage waitlist</em> (Alt+W) in `IU_Welcome`, shown only with `MANAGE_APPOINTMENTS`, after <em>Manage appointments</em>. The existing constructors keep their behaviour: the new action is `null` in them, so no other test changes.
- **Q3 – Which contracts does the screen use?** **A:** The three of the design: `IWaitlistService` (the operations), `IAppointmentService` (the pending needs) and `ISystemParameterService` (the test types and the tiers). It never uses a server class.
- **Q4 – Who finishes the sub-issue?** **A:** Javier Villanueva Crespo takes over the code and the tests (26/11/2026). The tests go in `feature/UC15-client-tests`, created from this branch on the member's request, and are merged back into it.
- **Q5 – Which defects are fixed?** **A:** (A) `Alt+R` is used twice, so the label *Tier* takes `Alt+E`; (B) the waitlist table keeps the previous list when a reload fails, so it is emptied; (C) the notice of an empty list stays after choosing a test type with entries, so the message is cleared; (D) filling the combos asks for the waitlist several times, each one audited, so it is asked once; (E) a `null` proposal shows a notice; (F) `Main` passes the patient service to `WaitlistService`.
- **Q6 – Text of the empty proposal.** **A:** *There is no patient who can fill this opening.* (Javier Villanueva Crespo, 26/11/2026; to be confirmed by Design).
- **Q7 – Version of `client-c1-access`.** **A:** `1.2.8-SNAPSHOT`, because the button changes `IU_Welcome`; the Coordinator is told in a comment of the pull request (#237, 26/11/2026).
- **Q8 – What is not changed?** **A:** The texts of the screen that Design has not reviewed (the confirmation of *Remove* shows the id of the patient) and the differences with the design that do not change a flow (`loadData` returns `boolean`, the constructor with a `Clock`, `onFindOpenings` is public).
- **Q9 – Red tests.** **A:** The commit of the tests leaves red the cases of the defects A to E until step 6 fixes them.

## Test cases (S.04.01), approved by Javier Villanueva Crespo on 26/11/2026

| Case | Situation | Expected result |
|---|---|---|
| TC15C-01 | open with data | test types, tiers, pending needs, waitlist of the first test type and today |
| TC15C-02 | empty waitlist | notice of the empty list |
| TC15C-03 | from an empty list to one with entries | entries and no notice |
| TC15C-04 | list with several entries | the order of the service |
| TC15C-05 | open and change the test type | one query each time |
| TC15C-06 | no permission when opening | message, actions disabled, *Back* enabled |
| TC15C-07 | C2 rejects the reading | its errors |
| TC15C-08 | the reading fails | the error |
| TC15C-09 | the list fails after changing the test type | empty table and error |
| TC15C-10 | valid add | confirmation and lists reloaded |
| TC15C-11 | add without a selected need | error, nothing sent |
| TC15C-12 | add rejected with two errors | both shown |
| TC15C-13 | change the tier | confirmation |
| TC15C-14 | change without a selected entry | error, nothing sent |
| TC15C-15 | remove and accept | removed and confirmation |
| TC15C-16 | remove and refuse | nothing sent |
| TC15C-17 | remove without a selected entry | error |
| TC15C-18 | openings of a valid day | listed, *1 opening* or *2 openings* |
| TC15C-19 | day without openings | notice |
| TC15C-20 | blank or badly written day | error, openings emptied, nothing sent |
| TC15C-21 | propose | row selected and message with the patient |
| TC15C-22 | empty proposal | notice, no row selected |
| TC15C-23 | propose without an opening | error |
| TC15C-24 | fill and accept | appointment confirmed, openings emptied, lists reloaded |
| TC15C-25 | fill and refuse | nothing sent |
| TC15C-26 | fill without an entry or an opening | error |
| TC15C-27 | fill rejected | reason shown, the entry stays |
| TC15C-28 | permission denied in an action | message and actions disabled |
| TC15C-29 | unexpected failure in each action | the error |
| TC15C-30 | change done and reload failed | error, no success message |
| TC15C-31 | *Back* | goes back |
| TC15C-32 | welcome screen with the permission | button *Manage waitlist* (Alt+W) that opens the screen |
| TC15C-33 | without the permission or the action | not shown |
| TC15C-34 | access keys | none repeated |

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
- [x] 4. Merge the server part; the patient service in `Main`; `client-c1-access` `1.2.8-SNAPSHOT`. (e16cd1a)
- [ ] 5. `feature/UC15-client-tests`: tests TC15C-01 to TC15C-34 and their design.
- [ ] 6. Merge the tests and fix the defects A to E.
- [ ] 7. Verification and close of the plan.

## Verification

- [ ] Acceptance criteria of #133: the diagrams and models are the ones of the design; the screen shows the waitlist ordered by tier and date, adds and changes entries, removes with confirmation, and fills an opening through UC13.
- [ ] `mvn -B verify` is green: **blocked** by the coverage rule until the tests of `feature/UC15-client-tests` are merged. Checked with `-Djacoco.skip=true`: BUILD SUCCESS, 1,348 tests green.

## Status

- **In progress:** nothing
- **Next step:** step 5, tests in `feature/UC15-client-tests`
- **Open questions:** confirmation of Design for the text of Q6
- **Last update:** 26/11/2026, Javier Villanueva Crespo
