# Plan – C1 – Client submodule (#101)

> Moves the client of C1 to its own Maven submodule (D-032). It is a technical task: no Java class changes. It continues the server part (#100, pull request #104).

| | |
|---|---|
| **Sub-issue** | #101 *Move C1 to its client submodule (D-032)* · branch `feature/C1-submodules-client` (created from `feature/C1-submodules-server`) |
| **Parent issue** | #93 *Move C1 to its two Maven submodules (D-032)* · previous: #100 *Move C1 to its server submodule (D-032)* |
| **Component** | C1 Access and security · component version 1.2.0 (UC22 1.0.0, UC19 1.1.0, UC18 1.2.0) |
| **Specification** | D-032, D-033, `agents/components.md`, `agents/architecture.md` and the plan `doc/plans/C1-submodules-server.md` |
| **Design** | none (no class or contract changes) |
| **Plan approved by** | Javier, 12/11/2026 |

## Starting point
- The branch has the server branch merged (`7b458cb`): `server/c1-access` exists and `client/pom.xml` already depends on `server-c1-access`.
- `client/pom.xml` is `client` 1.3.0-SNAPSHOT, with 14 Java files in `client/src` (`Main`, 4 screens `IU_*`, 8 test classes and `SmokeTest`).
- Before this change: `mvn -B verify` is green with `common` 16, `server-c1-access` 144 and `client` 73 tests.
- The pull request of the server (#104) goes first. This one waits until #104 is merged and then `develop` is merged into this branch, so that it only shows the client (as in UC22 and UC18).

## Questions and answers
- **Q1:** Version of the submodule. **Proposal:** `1.2.0`, as in the server, `agents/components.md` and #93. **A:** approved as proposed (Javier, 12/11/2026)
- **Q2:** The aggregator `client/pom.xml`. **Proposal:** no version of its own (it inherits 0.2.0-SNAPSHOT), as `server/pom.xml`. **A:** approved as proposed (Javier, 12/11/2026)
- **Q3:** Where does `Main` live? **Proposal:** it stays in `client/c1-access`, which keeps depending on `common` and `server-c1-access` (only this submodule imports the server). When C2 has a client submodule, the place of `Main` is decided with the team; it is not decided here. **A:** approved as proposed (Javier, 12/11/2026)
- **Q4:** Command to run the application in the README. **Proposal:** `mvn -B -pl client/c1-access exec:java -Dexec.mainClass=es.uclm.esi.iso2.client.Main`, after `mvn -B install -DskipTests` as now. **A:** approved as proposed (Javier, 12/11/2026)
- **Q5:** The item of #101 "the component tag of the issue template is changed": `.github/ISSUE_TEMPLATE/component.md` already says that the git tag is the one of the product release (done by David in #92). **Proposal:** do not touch the templates (rule 9) and tick the item as done by #92. **A:** approved as proposed (Javier, 12/11/2026)
- **Q6:** The wiki page *Traceability and releases* already has the column of the component version (David); nothing for the agent to change (rule 8). **A:** approved as proposed (Javier, 12/11/2026)

## Files
| File | Action | What |
|---|---|---|
| `client/src/**` (main and test, including `.gitkeep`) | move (`git mv`) | to `client/c1-access/src/**`, with no Java change |
| `client/c1-access/pom.xml` | create | `artifactId` `client-c1-access`, version `1.2.0`, parent `radiodiagnostics` (`relativePath ../../pom.xml`), dependencies `common`, `server-c1-access` and JUnit (as `client/pom.xml` has now) |
| `client/pom.xml` | change | `packaging` `pom`, no own version, `<modules>` with `c1-access`, no dependencies |
| `README.md` | change | structure of `client/` and the command to run the application (Q4); the paragraph on submodules no longer says "its client follows" |
| `agents/architecture.md` | change | the code structure (client tree), remove the note "this section is updated in the pull request that moves C1", and the note on versions |
| `agents/components.md` | change | the note "until C1 is moved…" (it is moved now) |

## Steps
Tick each step with the hash of its commit (green `mvn -B verify`). Step 2 cannot be split.
- [ ] 1. Plan of this sub-issue (`docs(C1): plan for the client submodule (#101)`).
- [ ] 2. Move the code and the tests to `client/c1-access`, create its pom and turn `client/pom.xml` into an aggregator.
- [ ] 3. Update `README.md`, `agents/architecture.md` and `agents/components.md`.

## Verification
- [ ] `git diff --stat -M` shows the Java files only as renames.
- [ ] `mvn -B verify` is green: `common` 16, `server-c1-access` 144 and `client-c1-access` 73 tests, as before.
- [ ] The JaCoCo check passes in `client-c1-access`.
- [ ] The application starts with the new command of the README and the login, *Manage roles* and the audit log work (manual check by Javier).
- [ ] After #104 is merged: `develop` merged into this branch, and the pull request only shows the client.
- [ ] The CI of the pull request is green.

## Status
- **In progress:** step 2 (move the code to `client/c1-access` and change the poms)
- **Next step:** step 2 (move the code to `client/c1-access`)
- **Open questions:** none
- **Last update:** 12/11/2026, Javier
