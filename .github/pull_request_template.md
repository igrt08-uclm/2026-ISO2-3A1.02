Closes #

## Review checklist
Annex A of the theory report. Mark as n/a what does not apply to this change.

- [ ] **S. Security.** The change requires an authenticated user with the right role; every access to clinical data writes an audit entry (FR19); no credential, endpoint or patient data is logged.
- [ ] **U. Usability.** Each form validates formats and ranges in the client and the server, with a readable message; deactivating or cancelling asks for confirmation.
- [ ] **F. Function.** Each acceptance criterion of the issue is satisfied and identifiable in the diff; the alternative flows of the use case are handled, not only the happy path.
- [ ] **C. Contract.** The interfaces in `common` are respected; no client-to-server import outside `Main`; if a contract changed, the change is recorded and the version is MAJOR.
- [ ] **V. Version.** The module version was raised and matches the issue and the branch.
- [ ] **T. Tests.** New behaviour is covered; the build is green; no test is disabled or skipped to make it pass.
- [ ] **Q. Quality.** No dead code, no commented-out block, no duplicated logic that belongs in `common`; names say what the code does.
- [ ] **A. AI.** If a model generated part of the diff, it is stated in the issue, and the reviewer can explain every line.
- [ ] **D. Decision.** If the change decides something (a rule, a boundary, a behaviour not in the requirement), there is a decision entry.
