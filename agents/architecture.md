# Architecture, build and tests

## Code structure (D-016)
Since D-032, each component is a Maven submodule of `server` and `client`, with its own version.
```
radiodiagnostics/        parent pom, es.uclm.esi.iso2, product version (0.2.0-SNAPSHOT during It2), Java 21, JUnit 6.1.3
├── common/              shared contracts (interfaces and data classes), with its own version
├── server/              aggregator of the server submodules (one per component)
│   ├── c1-access/       server of C1 (artifactId `server-c1-access`, version 1.2.2-SNAPSHOT), depends on common
│   ├── c2-configuration/ server of C2 (artifactId `server-c2-configuration`, version 1.1.0-SNAPSHOT), depends only on common
│   └── c3-patients/     server of C3 (artifactId `server-c3-patients`, version 1.0.0-SNAPSHOT), depends only on common
└── client/              aggregator of the client submodules (one per component)
    ├── c1-access/       client of C1 (artifactId `client-c1-access`, version 1.2.2-SNAPSHOT), depends on common, server-c1-access and,
    │                    because its `Main` starts the whole program, on server-c2-configuration and client-c2-configuration
    └── c2-configuration/ client of C2 (artifactId `client-c2-configuration`, version 1.1.0-SNAPSHOT), depends only on common
```
- One Java package per component in each module, e.g. `es.uclm.esi.iso2.server.access` for C1. A component's package
  is created in its own iteration; for now `common` only has `es.uclm.esi.iso2.common.access` (C1).
- Client and server talk through **interfaces (contracts)** in `common` (D-016, #35): the server implements them and
  the client uses them. The communication is simulated with direct Java calls, not a network. The version of
  `common` is set once in the `<dependencyManagement>` of the parent pom.
- The client starts the whole program: only `es.uclm.esi.iso2.client.Main` imports `server`, to create the services.
  The screens (`IU_` classes, Swing, D-030) only use `common`.
- External hospital systems (SSO, EHR, admissions, RIS/PACS) are simulated behind interfaces. The SSO is `IAuthenticatorSystem`,
  in `common`, because C1 uses it to log in and C2 to test the connection; the gateways of the EHR, the admissions
  system and the RIS/PACS are in `server-c2-configuration` (package `integration`), each with its simulator.
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
- When a use case is delivered, the version of its component goes up (D-014, D-021, D-032). C1 is in `server/c1-access` (`server-c1-access`,
  1.2.2-SNAPSHOT after the patches #109 and #112) and `client/c1-access` (`client-c1-access`, 1.2.2-SNAPSHOT after UC20-client). A component does not depend
  on the module of another one: what they share is a contract in `common`, for example `IAuditRecorder` or `IAuthenticatorSystem`.
  Changing a `pom.xml` needs the member's approval (rule 9 in [AGENTS.md](../AGENTS.md)).
