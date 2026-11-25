# Plan – UC07 – Client (#70)

| | |
|---|---|
| **Sub-issue** | #70 *UC07 – Client (C4 v1.1.0)* · branch `feature/UC07-client`, created from `feature/UC06-client` (#204) with `feature/UC07-server` (#205) merged in, because neither is merged to `develop` yet |
| **Use case** | #69 *UC07 – Order periodic follow-up procedures (C4 v1.1.0)* · FR07 · `client-c4-orders` `1.1.0-SNAPSHOT` |
| **Specification** | #70, #69; alternative scenarios of #70 (interval not positive, test type not eligible, inactive patient, denied access); flow of UC07 in `doc/AN-C4-DECISIONS.md:44-60`; D-040 |
| **Design** | `doc/design/C4-client-design.puml` and the contract of `common` merged in #201 and implemented in #205 |
| **Plan approved by** | Ignacio Romero Torres, 23/11/2026 (he delegated the review to Javier for Implementation and Adam for Testing) |

## Decisions
- **Screen:** `IU_OrderFollowUp`, a new class in `es.uclm.esi.iso2.client.orders.presentation`, next to `IU_OrderProcedure`
  of UC06. Same layout: search of patients on the left, form and the orders of the patient on the right, a line of status
  and *Back*. It is a `JPanel` so the tests can create it without a display.
- **Only eligible test types are offered:** the screen asks C2 for the catalogue and shows the test types that are
  `isActive() && isFollowUpEligible()`. If there is none, the form is disabled and the reason is shown.
- **Recurrence interval:** a `JSpinner` from 0 to 12 months (David, #207, 24/11/2026: an interval of more than 12
  months is not offered and the server rejects it; it was 0 to 120 before). The minimum is 0 and not 1 on purpose: with a minimum of
  1 the check `<= 0` of the screen would never run and its message would be dead code. So the user can choose 0 and the
  screen shows `The recurrence interval must be a positive number of months`, which is the same text as the server of
  #71, and a negative value is still impossible.
- **Messages of the client:** the same texts as the server of #71 (Q3 of its plan), so the user reads the reason before
  the call and the server confirms it; the errors of `ValidationException` are shown as they come.
- **Orders of the patient:** the table shows the date, the test type, the interval and the indication. As in `IU_OrderProcedure`,
  `getOrdersOfPatient` returns `ClinicalOrder`, so the interval is read with `instanceof FollowUpOrder`; the orders of
  UC06 have an empty interval.
- **Permission:** the button of the welcome screen is only shown with `ORDER_PROCEDURES`; the screen explains a denied
  access (`AccessDeniedException`) and leaves only *Back* enabled.
- **Unsaved changes:** leaving the form with the indication or the justification written asks for confirmation.
- **Versions (rule 9):** `client-c4-orders` goes from `1.0.0-SNAPSHOT` to `1.1.0-SNAPSHOT` and `client-c1-access` from
  `1.2.5-SNAPSHOT` to `1.2.6-SNAPSHOT` (it gains the button of UC07).
- **Tests:** the tests of the screen are Adam's (rule 17) and go in a later branch, `feature/UC07-client-tests`; this
  branch is the implementation only, as with #204.

## Files
| File | Action | What |
|---|---|---|
| `client/c4-orders/src/main/java/es/uclm/esi/iso2/client/orders/presentation/IU_OrderFollowUp.java` | create | the screen |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Welcome.java` | change | the button *Order follow-up* (permission `ORDER_PROCEDURES`) |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/Main.java` | change | the navigation to the screen |
| `client/c4-orders/pom.xml`, `client/c1-access/pom.xml`, `pom.xml` | change | `1.1.0-SNAPSHOT` and `1.2.6-SNAPSHOT` (rule 9) |
| `doc/plans/UC07-client.md` | create | this plan |

No other file changes: nothing under `server/`, nothing of `common`, no other screen of C1.

## Steps
1. This plan.
2. `IU_OrderFollowUp`.
3. `IU_Welcome`, `Main` and the versions.
4. Branch `feature/UC07-client-tests` for Adam and the pull request.
- [x] F-02: *Search* disabled after a denied access (`5732b3c`).
- [x] F-01: the denied message is kept when a patient is selected (`da9a75d`).
- [x] R-01: `clearSelection` forgets the patient, its texts and its orders when the search fails, returns nobody or
  the reads of the types and the orders fail; the patient is only selected when the reads went well (`9583556`).
- [x] R-02: say which read fails: the message of the test types is “The test types could not be read: …”, as Design
  approved in #67 for the same screen of UC06 (Marcos, 25/11/2026). First the test, written by Implementation this
  once to close C4 and C5 (TC07C-49) — `7cb8ecb`; then the code — `92b2751`.
- [x] R-03: select the same patient again after a failed read: a failed read deselects the row of the table and
  forgets the last row (`clearFailedSelection`). First the test TC07C-58 — `7aefd2f`; then the code — `711bb47`.

## Verification
- [x] Every acceptance criterion of #70 that the screen can satisfy: the form offers only eligible test types, it
      rejects a non-positive interval, it explains the inactive patient and the denied access, and it shows the orders of
      the patient with their interval.
- [x] `mvn -B verify -DskipTests -Djacoco.skip=true` green for `client-c1-access` and its tree.
- [ ] The usability decisions, walked in the running application (Testing: this machine has no desktop).
- [x] `mvn -B verify` green with the tests of Adam (25/11/2026, this branch: `BUILD SUCCESS` in the 14 modules, all
      coverage checks met; `IU_OrderFollowUpTest` has 53 tests).

## Status
- **In progress:** nothing. The review of the pull request added R-01. **Next step:** nothing in this branch;
  Testing (Adam) added the cases of F-01, F-02 and R-01 (`bae659a`, TC07C-49 to TC07C-57) and they pass. R-02 and R-03
  were added on 25/11/2026 (TC07C-49 and TC07C-58). The interval of 13 months or more is decided (0 to 12).
- **Done:** `da1ee8c` the screen, `74de1d5` the button *Order follow-up*, `Main` and the versions, `6c82beb` this plan.
  Correction of the review of 23/11/2026: the `JSpinner` of the interval goes from **0** to 120 months, because with a
  minimum of 1 the check `<= 0` of the screen never ran; the message of the design is now reachable (0 months) and the
  negative values are still impossible.
- **Open questions:**
  1. Interval of 13 months or more: **closed.** David decided in #207 (24/11/2026) that it is rejected; the spinner
     goes from 0 to 12 months, the server rejects more than 12 (`6e8dd58`) and TC07C-17 and 18 and TC07S-35 and 36
     expect it.
  2. The defects of the review of Testing: F-01, the denied access was hidden when a patient was selected (the
     “There is no patient with the identifier …” message overwrote it), fixed in `da9a75d`; F-02, *Search* stayed
     enabled after a denial, fixed in `5732b3c`. Nothing is pending in the code; Testing adds the cases.
- **Last update:** 25/11/2026, Javier — R-02 and R-03 done with their tests TC07C-49 and TC07C-58, written by
  Implementation this once to close C4 and C5; the interval of 0 to 12 months written as decided; the Verification box
  of the build ticked. Before: 24/11/2026, Ignacio (F-01 and F-02 of the review of Testing, R-01 of the review of
  the pull request and `mvn -B -ntp verify` green with the tests of Adam, 75 in `client-c4-orders`, #207).
