# Architecture, build and tests

## Code structure (D-016)
D-032 changes it to one Maven submodule per component; this section is updated in the pull request that moves C1.
```
radiodiagnostics/        parent pom, es.uclm.esi.iso2, version 0.1.0-SNAPSHOT, Java 21, JUnit 6.1.3
├── common/              shared contracts (interfaces and data classes), version 1.0.0-SNAPSHOT
├── server/              server application, depends on common
└── client/              client application, depends on common and server
```
- One Java package per component in each module, e.g. `es.uclm.esi.iso2.server.access` for C1. A component's package
  is created in its own iteration; for now `common` only has `es.uclm.esi.iso2.common.access` (C1).
- Client and server talk through **interfaces (contracts)** in `common` (D-016, #35): the server implements them and
  the client uses them. The communication is simulated with direct Java calls, not a network. The version of
  `common` is set once in the `<dependencyManagement>` of the parent pom.
- The client starts the whole program: only `es.uclm.esi.iso2.client.Main` imports `server`, to create the services.
  The screens (`IU_` classes, Swing for now; the final technology is decided in Meeting 6) only use `common`.
- External hospital systems (SSO, EHR, admissions, RIS/PACS) are simulated behind interfaces.
- Each component is the only one that writes its data; the others use it through its interfaces. An operation of a
  component that is delivered later is declared as an interface now and has a simulated implementation (D-023).
- No use case deletes a record: records are deactivated or cancelled (D-022).
- "The engineer designs the interfaces; the AI implements the contracts" (assignment, p. 8).

## Build and test
- `mvn -B verify` builds and runs all the tests. The CI (`.github/workflows/maven.yml`) runs it on every push and
  pull request to `main` and `develop`; a pull request with a red build is not approved.
- Every use case has unit tests. The team designs the cases in a table and you write and run them, as in rule 17 of
  [AGENTS.md](../AGENTS.md) (assignment, p. 3; D-034).
- JaCoCo measures the coverage in `mvn verify` (#90): the report is in `target/site/jacoco` of each module, and the
  build fails below 80 % of lines or 70 % of branches in any module (D-033).
- When a use case is delivered, the version of its component goes up (D-014, D-021, D-032). Until C1 is moved to its
  submodules, that is the version of `server/pom.xml` or `client/pom.xml`.
  Changing a `pom.xml` needs the member's approval (rule 9 in [AGENTS.md](../AGENTS.md)).
