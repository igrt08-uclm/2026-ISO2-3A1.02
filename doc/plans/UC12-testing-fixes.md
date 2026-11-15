# Plan – Fixes of the findings of Testing (#122) – UC12 and C2

| | |
|---|---|
| **Sub-issue** | #45 *UC12 – Client (C3 v1.0.0)* (batch 1, pull request #121) · batch 2 has no sub-issue: it is a patch of already merged code (D-036) |
| **Source** | `doc/testing/defects-C1-C3.md` (#122, Adam), the review of David in #120, #121 and #122 (R-01 to R-05) |
| **Branches** | batch 1: `feature/UC12-client` (Adam already merged `develop` into it) · batch 2: a new branch from `develop`, created by Javier (rule 7) |
| **Plan approved by** | Javier, 15/11/2026 |

## What is fixed in each batch
**Batch 1 – today, to unblock #121 (David approves when it is pushed and the build is green):**
F-06, F-07, F-08, F-09, F-10 (screen of the patients) and R-02, R-03, R-04 (rules of UC12, server and screen).

**Batch 2 – also today (Javier, 15/11/2026: every finding of David and Adam is fixed today), a patch in its own branch:** R-05 and F-19 (C2) and the findings that Adam left to Implementation: F-11, F-12, F-14, F-16, F-17 and F-18.

**Findings decided by others (Q9):** F-13 (Marcos and Denis), F-15 (David) and F-20 (Ignacio and David) are fixed in batch 2 with the proposal of Q9; D-01 to D-03 are diagrams (Design, #114). R-01 needs no change (no fixed format).

## Decisions already taken
- **Javier, 15/11/2026:** F-09 the error line goes in the centre of the footer and breaks into lines if it is long; F-10 the label *Search* keeps Alt+H and the button *Search* has no shortcut (Enter searches); two batches of commits.
- **David (#121):** F-06 the call to `table.clearSelection()` in `onNewPatient` is done with `restoring = true`; F-08 `Back` is `if (confirmDiscard()) { onBack.run(); }`.
- **David (#122, confirmed R-01 to R-05):** the identifier is compared ignoring case, all the spaces and `-`; the search ignores accents; e-mail with no empty part; date of birth not before 01/01/1900; phone typed with spaces but only its 9 to 15 digits count; endpoint with a host after `http://`/`https://` and the scheme ignoring case.
- The tests are Adam's: no step writes them. Some of his tests may fix the old behaviour (for example the double question): if one fails, I tell Javier and Adam, I do not edit it.

## Batch 1 – steps (branch `feature/UC12-client`)
Each step is one commit and `mvn -B verify -Djacoco.skip=true` is green before it.

- [x] **1. Plan.** This file, committed with the approval of Javier. (The branch was already updated with `git pull --ff-only`: `62095bc`.) Commit 8e9c565.
- [x] **2. F-06, F-07, F-08 (`IU_ManagePatients`).** Commit 35acffc.
  - F-06: in `onNewPatient`, `table.clearSelection()` runs with `restoring = true`, so the listener does not ask a second time.
  - F-07: `changeRow` does nothing when the selected row is still the shown one (dragging away and back no longer reloads the form).
  - F-08: *Back* asks with `confirmDiscard()` and leaves only if the user accepts.
- [ ] **3. F-09, F-10 (`IU_ManagePatients`).**
  - F-09: `lblStatus` goes in `BorderLayout.CENTER` of the footer and wraps its text in lines (HTML in the label with the width of the free space), so that a long error is read whole and does not go under *Back*.
  - F-10: `btnSearch` loses its mnemonic; the label keeps Alt+H.
- [ ] **4. R-02 (server).** `InMemoryPatientRepository.findByIdentifier` and the check "The identifier cannot be changed" in `PatientService` compare the identifiers ignoring case, all the spaces and `-` (`HC 0000001`, `HC-0000001` and `hc0000001` are the same patient). The identifier is saved as it was typed.
- [ ] **5. R-03 (server).** `InMemoryPatientRepository.search` ignores accents as well as case (`lucia` finds "Lucía"), with `Normalizer` of the JDK (no new dependency, rule 6).
- [ ] **6. R-04 (server and screen).** The same rules in `PatientService` and in `IU_ManagePatients`: e-mail `name@domain.ext` with no empty part (`ana@example.test.` and `ana@example..test` rejected); date of birth not before 01/01/1900, with the message "The date of birth cannot be before 01/01/1900". The phone is checked: it already ignores the spaces and counts only the digits, so it only changes if a case fails.
- [ ] **7. Hand over to Adam.** Update the lists of cases of `UC12-client.md` and `UC12-server.md` ("State for Adam"), add the commit hashes here and tell Adam what to test again (F-06, F-07, F-08, F-09, F-10, R-02 to R-04). **Push only when Javier asks** (rule 8).

## Questions and answers
- **Q1 (batch 1):** Should the search by identifier also ignore the spaces and `-` (so that `HC0000001` finds `HC-0000001`)? **Proposal:** no; the search only ignores case and accents, as David wrote in R-03. The text is also searched in the name, where the spaces matter ("Ana María"). **A:** approved as proposed (Javier, 15/11/2026)
- **Q2 (batch 2):** Branch and version. **Proposal:** a branch from `develop` with a name that Javier chooses (for example `fix/testing-findings-C2-C3`); the versions do not change (`server-c2` and `client-c2` stay at 1.1.0-SNAPSHOT, they are not released yet). **A:** approved as proposed (Javier, 15/11/2026)
- **Q3 (batch 2) – R-05:** the endpoint needs a host after `http://` or `https://`, and the scheme ignores case. **Proposal:** the endpoint is saved as it was typed (only stripped), and the message does not change: "The endpoint must start with http:// or https://" for a wrong scheme, and a new one, "The endpoint needs a host after http:// or https://". **A:** approved as proposed (Javier, 15/11/2026)
- **Q4 (batch 2) – F-11:** the counter of the ids is in the service. **Proposal:** `IPatientRepository` gets `nextPatientId()` (the repository knows the highest id it has), and `PatientService` uses it; the ids stay `P-0001`... and a service built with a repository that has patients never overwrites one. **A:** approved as proposed (Javier, 15/11/2026)
- **Q5 (batch 2) – F-12:** a blank identifier in a modification gives two messages. **Proposal:** only "The identifier is required"; "cannot be changed" is added only if the identifier is not blank. **A:** approved as proposed (Javier, 15/11/2026)
- **Q6 (batch 2) – F-14:** with `system = null` a user without permission gets `IllegalArgumentException`. **Proposal:** the permission is checked first (as in `PatientService`): a user without the permission gets `AccessDeniedException` and it is audited; the validation comes after. **A:** approved as proposed (Javier, 15/11/2026)
- **Q7 (batch 2) – F-16 and F-17:** `null` settings and `null` endpoint. **Proposal:** the server saves `null` settings as an empty map; the screen treats a `null` endpoint as `""` when it compares with the form. **A:** approved as proposed (Javier, 15/11/2026)
- **Q8 (batch 2) – F-18 and F-19:** screen of the integrations. **Proposal:** after an access denied the table ignores the clicks (and never asks "Discard the unsaved changes?"); *Revert* also clears the message area. **A:** approved as proposed (Javier, 15/11/2026)

- **Q9 (batch 2) – F-13, F-15, F-20:** F-13 audit `getPatient` and `modifyPatient` of an unknown id as `PATIENT_ACCESSED` (resource `PATIENT:<id>`); F-15 strip the keys and the values of the settings; F-20 reject a **new** test type with a deactivated priority ("The priority X is deactivated"; editing an old one keeps Q9 of UC17). **A:** approved as proposed (Javier, 15/11/2026); they are part of the patch of batch 2.

## Batch 2 – steps (a new branch from `develop`, created by Javier)
- [ ] **8.** Plan of this batch approved (the answers of Q2 to Q8) and the branch created by Javier.
- [ ] **9. R-05, F-14, F-15 is not mine, F-16 (`IntegrationManagementService`).**
- [ ] **10. F-17, F-18, F-19 (`IU_ManageIntegrations`).**
- [ ] **11. F-11, F-12 (`PatientService`, `IPatientRepository`, `InMemoryPatientRepository`).**
- [ ] **11b. F-13, F-15, F-20** as in Q9 (F-13 in `PatientService`, F-15 in `IntegrationManagementService`, F-20 in `SystemParameterService`).
- [ ] **12. Hand over to Adam and, if Javier asks, push and open the pull request** (the title and the text are shown to Javier first).

## State for Adam
To be filled in step 7 and step 12.
