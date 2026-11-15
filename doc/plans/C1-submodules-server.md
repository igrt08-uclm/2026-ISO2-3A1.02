# Plan – C1 – Server submodule (#100)

> Moves the server of C1 to its own Maven submodule (D-032). It is a technical task: no Java class changes.

| | |
|---|---|
| **Sub-issue** | #100 *Move C1 to its server submodule (D-032)* · branch `feature/C1-submodules-server` |
| **Parent issue** | #93 *Move C1 to its two Maven submodules (D-032)* · next: #101 *Move C1 to its client submodule (D-032)* |
| **Component** | C1 Access and security · component version 1.2.0 (UC22 1.0.0, UC19 1.1.0, UC18 1.2.0) |
| **Specification** | D-032, D-033, `agents/components.md` (table of names and versions), `agents/architecture.md` |
| **Design** | none (no class or contract changes) |
| **Plan approved by** | Javier, 12/11/2026 |

## Starting point (checked on `develop`, `e1ed9d8`)
- `mvn -B verify` is green: `common` 16 tests, `server` 144, `client` 73 (233 in total), JaCoCo check passing in every module.
- `server/pom.xml` is `server` 1.3.0-SNAPSHOT with the code and the tests of C1 in `server/src`.
- The parent pom has `server` 1.3.0-SNAPSHOT in its `dependencyManagement`; `client/pom.xml` depends on it.
- JaCoCo is configured once in the parent pom and already runs on the aggregator poms without problems.

## Questions and answers
- **Q1:** What version does the pom of the submodule carry? **Proposal:** `1.2.0` (the version of C1 since v0.1.0; moving the code is not a new use case), as #92 and #93 say. **A:** approved as proposed (Javier, 12/11/2026)
- **Q2:** Does the aggregator `server/pom.xml` have its own version? **Proposal:** no, it inherits the product version (0.2.0-SNAPSHOT), because the versions are per component. **A:** approved as proposed (Javier, 12/11/2026)
- **Q3:** What happens with the version of `common` (1.3.0-SNAPSHOT today)? **Proposal:** do not touch it in this sub-issue (rule 2) and raise it with the team. **A:** approved as proposed (Javier, 12/11/2026)
- **Q4:** Parent of the submodule. **Proposal:** the product pom (`radiodiagnostics`, with `<relativePath>../../pom.xml</relativePath>`), so JaCoCo and the managed versions are inherited without going through the aggregator. **A:** approved as proposed (Javier, 12/11/2026)
- **Note (for #101, not for this plan):** `Main` stays in `client/c1-access`, which will depend on `server-c1-access`. When there are several client submodules, the place of `Main` has to be decided.

## Files
| File | Action | What |
|---|---|---|
| `server/src/**` (main and test, including `.gitkeep`) | move (`git mv`) | to `server/c1-access/src/**`, with no Java change (the packages stay as they are) |
| `server/c1-access/pom.xml` | create | `artifactId` `server-c1-access`, version `1.2.0`, parent `radiodiagnostics`, the dependencies that `server/pom.xml` has now (`common`, JUnit) |
| `server/pom.xml` | change | `packaging` `pom`, no own version, `<modules>` with `c1-access`, no dependencies |
| `pom.xml` (parent) | change | in `dependencyManagement`, `server` 1.3.0-SNAPSHOT becomes `server-c1-access` 1.2.0 |
| `client/pom.xml` | change | one line: the dependency `server` becomes `server-c1-access` (so that the build is green with only this pull request) |
| `README.md` | change | the link to `AuthenticatorSystemSimulator` points to `server/c1-access/src/...`, and the structure section |
| `agents/architecture.md` | change | the code structure and the note on versions (server side) |

## Steps
Tick each step with the hash of its commit (green `mvn -B verify`). Step 1 cannot be split: if the code moves without the poms, or the poms change without the dependency of the client, the build is red.
- [x] 1. Plan of this sub-issue (`docs(C1): plan for the server submodule (#100)`) – 16e77c4.
- [x] 2. Move the code and the tests to `server/c1-access`, create its pom, turn `server/pom.xml` into an aggregator, and update `dependencyManagement` and the dependency of `client/pom.xml`. – b65c7ed.
- [x] 3. Update `README.md` and `agents/architecture.md` (server side). – 962b913.

## Verification
- [x] `git diff --stat -M` shows the Java files only as renames (no content changes).
- [x] `mvn -B verify` is green: `common` 16, `server` 144 and `client` 73 tests, as before.
- [x] The JaCoCo check passes in `server-c1-access` (report in `server/c1-access/target/site/jacoco`).
- [x] The application still starts from `Main` (manual check with `mvn -B install -DskipTests` and the run command of the README). Checked by Javier on 12/11/2026: login, manage roles, audit and a user without permission.
- [ ] The CI of the pull request is green.

## Status
- **In progress:** nothing
- **Next step:** open the pull request of #100 (when Javier asks), then #101 on its own branch
- **Open questions:** out of this sub-issue (rule 2): the welcome screen has no "Are you sure you want to log out?" confirmation (found by Javier while testing, 12/11/2026); it is a fix of the client of C1 (PATCH, C1 1.2.1) for the team to decide.
- **Last update:** 12/11/2026, Javier
