# Plan – Patch of C2 1.1.2 (#124)

| | |
|---|---|
| **Issue** | #124 *Align the endpoint rule of the screen of the integrations with the server (C2 v1.1.2)* · branch `feature/C2-patch-1.1.2` (created by Javier from `develop` `0b1e2d2`, 15/11/2026, after #125 and #126 were merged) |
| **Use case** | UC20 (screen): no new use case, it is a PATCH of C2 (D-014, D-032) |
| **Specification** | #124; R-05 of #122 (David); the rule of the server in the patch #123 (`doc/plans/C2-patch-1.1.1.md`, Q3) |
| **Plan approved by** | Javier, 15/11/2026 |

## Decisions already taken
- **David (#122, R-05):** the endpoint is `http://` or `https://` followed by a host, and the scheme ignores case.
- **Patch #123 (server):** messages "The endpoint must start with http:// or https://" (wrong scheme) and "The endpoint needs a host after http:// or https://" (no host); the host is the text after the scheme up to `/`, `?` or `#`, without `user@` and without `:port`; the endpoint is saved as written, only stripped.
- **Javier, 15/11/2026:** two patches (the screen was left out of #123, Q1 option B of David); this one is C2 **1.1.2**, after #123 is merged.
- NFR03 and `doc/plans/UC20-client.md`: the screen checks with the same rules and messages as the server.
- The tests are Adam's: no test is changed by the agent.

## Questions and answers
- **Q1 – Tests of Adam that check the old rule.** Two tests of `IU_ManageIntegrationsTest` fail with the new rule: **TC20C-17** accepts `http://` and `https://` alone, and **TC20C-18** rejects `HTTP://sso.hospital.test` (now accepted, the scheme ignores case). **Proposal:** Adam updates both in this branch (the same as in #123), with the same cases as TC20S-17 of the server. As in #123, the code can be pushed before his tests, with the build red only in those two (Javier decides). **A:** Adam updates TC20C-17 and TC20C-18 in this branch; the code is committed and pushed only after his tests are in the branch, with the build green (Javier, 15/11/2026)
- **Q2 – Versions (rule 9).** **Proposal:** `client-c2-configuration` from `1.1.0-SNAPSHOT` to `1.1.2-SNAPSHOT` in `client/c2-configuration/pom.xml` and in the managed version of the parent `pom.xml`, and its line in `agents/architecture.md`. **A:** 1.1.2 (Javier, 15/11/2026); the files, with the plan.

## Files
| File | Action | What |
|---|---|---|
| `client/c2-configuration/.../presentation/IU_ManageIntegrations.java` | change | the rule of the endpoint in `onSaveConfiguration`, the same as the server |
| `client/c2-configuration/pom.xml`, `pom.xml` | change | version `1.1.2-SNAPSHOT` (Q2) |
| `agents/architecture.md` | change | version of `client-c2-configuration` (Q2) |
| `doc/plans/C2-patch-1.1.2.md` | new | this plan |

## Steps
Each step is one commit and `mvn -B verify -Djacoco.skip=true` is green before it (or with the exception of Q1).
- [ ] **1. Plan.** This file, committed with the approval of Javier.
- [ ] **2. Rule of the endpoint in the screen** (`IU_ManageIntegrations`).
- [ ] **3. Versions** (Q2).
- [ ] **4. Hand over.** State for Adam, hashes here, push only when Javier asks; pull request `Closes #124`.

## State for Adam
To be filled in step 4.
