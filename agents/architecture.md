# Architecture, build and tests

## Code structure (D-016)
```
radiodiagnostics/        parent pom, es.uclm.esi.iso2, version 0.1.0-SNAPSHOT, Java 21, JUnit 6.1.3
├── server/              server application
└── client/              client application
```
- One Java package per component in each module, e.g. `es.uclm.esi.iso2.server.access` for C1.
- Client and server talk through **interfaces (contracts)**; the communication is simulated with direct Java calls,
  not a network. A shared `common` module for those contracts is planned (#35) but **does not exist yet**.
- External hospital systems (SSO, EHR, admissions, RIS/PACS) are simulated behind interfaces.
- Each component is the only one that writes its data; the others use it through its interfaces. An operation of a
  component that is delivered later is declared as an interface now and has a simulated implementation (D-023).
- No use case deletes a record: records are deactivated or cancelled (D-022).
- "The engineer designs the interfaces; the AI implements the contracts" (assignment, p. 8).

## Build and test
- `mvn -B verify` builds and runs all the tests. The CI (`.github/workflows/maven.yml`) runs it on every push and
  pull request to `main` and `develop`; a pull request with a red build is not approved.
- Every use case has unit tests. The team designs the tests; you can write and run them (assignment, p. 3).
- When a use case is delivered, its module version goes up in `server/pom.xml` or `client/pom.xml` (D-014, D-021).
  Changing a `pom.xml` needs the member's approval (rule 9 in [AGENTS.md](../AGENTS.md)).
