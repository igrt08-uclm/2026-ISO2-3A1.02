# Plan – Patch of C2 1.1.1 (#123)

| | |
|---|---|
| **Issue** | #123 *Patch of C2: endpoint with a host and no deactivated priority for a new test type (C2 v1.1.1)* · branch `feature/C2-patch-1.1.1` (created by Javier from `develop` `e6c273a`, 15/11/2026) |
| **Use cases** | UC20 (R-05) and UC17 (F-20): no new use case, it is a PATCH of C2 (D-014, D-032) |
| **Specification** | #123; the findings R-05 and F-20 of `doc/testing/defects-C1-C3.md` (#122); the decision of David (Requirements) of 15/11/2026: in both the fix wins and Adam updates his tests |
| **Previous plans** | `doc/plans/UC20-server.md`, `UC20-client.md`, `UC17-server.md` (Q9), `UC12-testing-fixes.md` (step 11) |
| **Plan approved by** | Javier, 15/11/2026 |

## Decisions already taken
- **David, 15/11/2026:** R-05 and F-20: the fix wins, in a patch of C2 1.1.1 that does not block #121 (already merged). F-12, F-13 and F-16: the test wins, they stay as they are.
- **R-05 (David, #122):** the endpoint is `http://` or `https://` followed by a host (`http://` alone is rejected), and the scheme ignores case (`HTTPS://…` is accepted).
- **F-20 (David):** Q9 of UC17 only allowed a deactivated priority so that the test types that already existed did not break; a **new** test type cannot use it.
- **Javier, 15/11/2026:** the code must pass the tests of Adam; no test is changed by the agent. Adam updates TC20S-17 and `deactivatedPriorityIsAccepted` in this branch (he was told).
- The plan `UC12-testing-fixes.md` (step 11) still says that R-05 and F-20 stay open: this patch closes them.

## Questions and answers
- **Q1 – R-05 in the screen too?** The screen of the integrations checks the endpoint with the same rule as the server (NFR03, `UC20-client.md`), so today it accepts `http://` alone and rejects `HTTPS://…`. Its test TC20C-17 (Adam) requires that `http://` and `https://` alone are accepted. **Proposal:** change the screen too (same rule and same messages as the server), and Adam updates a **third** test, TC20C-17 (the two values `http://` and `https://` move to the rejected cases). `client-c2-configuration` then goes to `1.1.1-SNAPSHOT` as well, and #123 gets one more line (an edit in GitHub, only if Javier asks). **Option B:** the screen stays as it is; the server rejects it and the screen shows the error of the server; only `server-c2-configuration` changes. **A:** option B. David decided that Adam changes only TC20S-17 and `deactivatedPriorityIsAccepted` (not TC20C-17), and #123 only moves `server-c2-configuration`: the screen is not changed; the server rejects the endpoint and the screen shows its error (David, 15/11/2026). Note: the screen and the server do not check the endpoint with the same rule until a later change.
- **Q2 – F-20 in the screen?** The test-type panel already offers only the active priorities to a new test type (and the deactivated one only to a test type that already had it), so the screen needs no change. **Proposal:** no change in the screen for F-20. **A:** no change in the screen (deduced from the code and from #123).
- **Q3 – Messages and saving.** **Proposal:** R-05: "The endpoint must start with http:// or https://" stays for a wrong scheme, and the new "The endpoint needs a host after http:// or https://" is for a missing host; the host is the text after the scheme up to `/`, `?` or `#`, without a user (`user@`) and without a port (`:8443`); the endpoint is saved as it was written (only stripped). F-20: "The priority X is deactivated", only when the test type is new (no test type with that id exists yet); editing an existing one keeps Q9. **A:** as proposed: they are the messages of #123 and of the decision of David in #122.
- **Q4 – Versions (rule 9: changing a `pom.xml` needs the approval of Javier).** **Proposal:** `server-c2-configuration` from `1.1.0-SNAPSHOT` to `1.1.1-SNAPSHOT` in `server/c2-configuration/pom.xml` and in the managed version of the parent `pom.xml`; with Q1, the same for `client-c2-configuration`; `agents/architecture.md` (the line of each module) is updated with the new versions. `common` does not change (no contract changes). **A:** only `server-c2-configuration` to `1.1.1-SNAPSHOT` (in its pom and in the parent pom) and its line in `agents/architecture.md`, as #123 says; `client-c2-configuration` stays `1.1.0-SNAPSHOT` (Q1).

## Files
| File | Action | What |
|---|---|---|
| `server/c2-configuration/.../service/IntegrationManagementService.java` | change | R-05: scheme without case and host required (Q3) |
| `server/c2-configuration/.../service/SystemParameterService.java` | change | F-20: a new test type cannot use a deactivated priority (Q3) |
| `server/c2-configuration/pom.xml`, `pom.xml` | change | versions (Q4) |
| `agents/architecture.md` | change | versions of the modules (Q4) |
| `doc/plans/C2-patch-1.1.1.md` | new | this plan |

## Steps
Each step is one commit and `mvn -B verify -Djacoco.skip=true` is green before it. **The build is only green after Adam pushes his updated tests to this branch**: until then, steps 2 and 3 are written and checked with the tests of Adam that do not change, and they are committed when his tests are in the branch (`git pull --ff-only` first).

- [ ] **1. Plan.** This file, committed with the approval of Javier.
- [ ] **2. R-05 in the server** (`IntegrationManagementService`).
- [ ] **3. F-20** (`SystemParameterService`).
- [ ] **4. Versions** (poms and `agents/architecture.md`, Q4).
- [ ] **5. Hand over.** State for Adam below, the hash of each step here, and the push only when Javier asks (rule 8). The pull request (`Closes #123`) is opened by whoever Javier says.

## State for Adam
To be filled in step 5.
