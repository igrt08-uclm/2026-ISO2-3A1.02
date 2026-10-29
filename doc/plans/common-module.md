# Plan – common module (#35)

| | |
|---|---|
| **Issue** | #35 *Add the common module to the Maven structure* · branch `feature/common-module` |
| **Decision** | D-016 (a third module, `common`, holds the shared contracts), D-014 (each module has its own version) |
| **Pending** | Ismael's answer in #34 *Ask Ismael about the estimate detail and the common module*; the team goes ahead, to be confirmed in Meeting 6 |
| **Plan approved by** | Javier, 29/10/2026 |

## Questions and answers
- **Q:** Version of `common`? **A:** `1.0.0-SNAPSHOT`, like `server` and `client` (Javier, 29/10/2026).
- **Q:** Which Java packages? **A:** Only the one of C1, `es.uclm.esi.iso2.common.access`, with a `package-info.java`; the
  packages of C2–C9 are created in their iterations (rule 3) (Javier, 29/10/2026).
- **Q:** A `SmokeTest` in `common`, like `server` and `client`? **A:** Yes (Javier, 29/10/2026).
- **Q:** Update `agents/architecture.md`, which says that `common` does not exist yet? **A:** Yes, in this pull request
  (Javier, 29/10/2026).

## Files
| File | Action | What |
|---|---|---|
| `pom.xml` | change | `common` first in `<modules>`; `common` `1.0.0-SNAPSHOT` in `<dependencyManagement>` |
| `common/pom.xml` | create | parent `radiodiagnostics`, artifactId `common`, version `1.0.0-SNAPSHOT`, JUnit for the tests |
| `common/src/main/java/es/uclm/esi/iso2/common/access/package-info.java` | create | package of C1 for its contracts and data classes |
| `common/src/test/java/SmokeTest.java` | create | same test as in `server` and `client` |
| `server/pom.xml`, `client/pom.xml` | change | dependency on `common` (version from the parent) |
| `agents/architecture.md` | change | the code structure shows `common`, which now exists |

## Steps
Tick each step with the hash of its commit (green `mvn -B verify`).
- [x] 1. This plan. `462270a`
- [ ] 2. Module `common`: `common/pom.xml`, `package-info.java`, `SmokeTest.java`, and `common` in the parent pom.
- [ ] 3. `server` and `client` depend on `common`.
- [ ] 4. `agents/architecture.md` describes the `common` module.

## Verification
- [ ] `mvn -B verify` is green and builds `common` before `server` and `client`.
- [ ] Every checkbox of #35 is done: `common` in the parent pom with its own version, `server` and `client` depend on
      it, one Java package per component (C1 for now), `mvn verify` passes in the CI workflow.

## Status
- **In progress:** nothing
- **Next step:** step 2, `common/pom.xml`
- **Open questions:** none
- **Last update:** 29/10/2026, Javier
