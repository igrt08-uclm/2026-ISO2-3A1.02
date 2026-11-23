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
- **Recurrence interval:** a `JSpinner` from 0 to 120 months. The minimum is 0 and not 1 on purpose: with a minimum of
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

## Verification
- [x] Every acceptance criterion of #70 that the screen can satisfy: the form offers only eligible test types, it
      rejects a non-positive interval, it explains the inactive patient and the denied access, and it shows the orders of
      the patient with their interval.
- [x] `mvn -B verify -DskipTests -Djacoco.skip=true` green for `client-c1-access` and its tree.
- [ ] The usability decisions, walked in the running application (Testing: this machine has no desktop).
- [ ] `mvn -B verify` green with the tests of Adam (Testing).

## Status
- **In progress:** nothing; steps 1 to 3 are done and pushed in `feature/UC07-client` (pull request #207).
- **Done:** `da1ee8c` the screen, `74de1d5` the button *Order follow-up*, `Main` and the versions, `6c82beb` this plan.
  Correction of the review of 23/11/2026: the `JSpinner` of the interval goes from **0** to 120 months, because with a
  minimum of 1 the check `<= 0` of the screen never ran; the message of the design is now reachable (0 months) and the
  negative values are still impossible.
- **Next step:** step 4, the branch `feature/UC07-client-tests` for Adam: the tests of `IU_OrderFollowUp` in a machine
  with a display, and the manual walk of *Usability decisions*.
- **Open questions:** none.
- **Last update:** 23/11/2026, Ignacio — screen corrected after the review; the implementation is complete.
