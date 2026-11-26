# Test design of the use cases

One file per use case and side, designed by Testing as in lesson T.04 (Testing Management) and D-034: the parameters
of each method with their equivalence classes, boundary values and error guessing; how the values are combined (each
use or pairwise) and the maximum number of test cases; the coverage of the code (decision, condition/decision and
MC/DC); and the test cases, each with its input, precondition, expected result and the JUnit test that checks it.
Each file ends with what is not covered yet and the findings found while designing (see also
`doc/testing/defects-C1-C3.md`).

| Component | Use case | Files |
|---|---|---|
| C1 Access and security | UC18 Manage roles and permissions | `UC18-test-design.md` (manual cases and the automated tests that cover them) |
| | UC19 Audit system activity | `UC19-test-design.md` (manual cases and the automated tests that cover them) |
| | Patch 1.2.3 (#172), permission `CONSULT_AUDIT` | `C1-patch-1.2.3-test-design.md` |
| | Patch 1.2.4 (#173), permissions `ORDER_PROCEDURES` and `MANAGE_APPOINTMENTS` | `C1-patch-1.2.4-test-design.md` (added by #180) |
| C2 Configuration and integrations | UC17 Configure system parameters | `UC17-server-test-design.md`, `UC17-client-test-design.md` |
| | UC20 Configure integrations | `UC20-server-test-design.md`, `UC20-client-test-design.md` |
| C3 Patients | UC12 Manage patient demographic data | `UC12-server-test-design.md`, `UC12-client-test-design.md` |
| C5 Appointment planning | UC15 Manage waitlists | `UC15-server-test-design.md`, `UC15-client-test-design.md` |

The test design of each new patch or use case is added here with its pull request.
