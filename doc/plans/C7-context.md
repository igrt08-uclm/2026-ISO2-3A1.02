# Context – C7 Execution and reports (#193)

> Read first in step 1 of the cycle in [AGENTS.md](../../AGENTS.md) (D-047). Every line cites its source (rule 12); if
> a line disagrees with its source, the source wins (rule 11): tell the member. Every pull request of C7 updates the
> lines of its own sub-issue, and the member who merges it checks that it did. Template: [CONTEXT-TEMPLATE.md](CONTEXT-TEMPLATE.md).

| | |
|---|---|
| **Component** | C7 Execution and reports · module `c7-reports` · UC10 → 1.0.0, UC09 → 1.1.0 ([agents/components.md](../../agents/components.md)) |
| **Iteration** | It7 · Milestone 4 · component issue #193 |
| **Last update** | 25/11/2026, Denis Ionescu, pull request #225 (design) |

## Decisions that apply
- D-011 – Only UC10 and UC09 are modelled in It7 (AN-C7 D56).
- D-016, D-027 – RIS/PACS is simulated; no DICOM viewer.
- D-022 – Nothing is deleted: executions, reports and addenda are never removed.
- D-023 – C7 writes only its own data; it uses C2, C3 and C5 through their interfaces (AN-C7 D63).
- D-030 – Data in memory (`Map` repositories).
- D-036 – Changes in C1, C2, C3 and C5 are patches of those components ([C7-design.md](C7-design.md)).
- AN-C7 D57 – Permissions `RECORD_EXECUTIONS` (UC10) and `WRITE_REPORTS` (UC09) for the Physician.
- AN-C7 D58 – `ACCESS_DENIED` and every access and action are audited.
- AN-C7 D59/D60 – Worklist of the physician; in design, from `Appointment.physician` (C7-design Q3).
- AN-C7 D61, D62 – `ProcedureExecution` once per appointment; the appointment becomes `EXECUTED`.
- AN-C7 D64–D67 – `DiagnosticReport` DRAFT/VALIDATED, second opinion, `Addendum`, images through RIS/PACS.

## Contracts (section 6 of #193, as designed in [C7-design.md](C7-design.md))
| Operation | In one line |
|---|---|
| `getMyWorklist(requester, date)` | Booked/rescheduled appointments of the day whose physician is the requester. |
| `startExecution(requester, appointmentId)` | Appointment, patient and test type requirements for the form. |
| `recordExecution(requester, execution)` | Validates against the test type, stores once, marks the appointment `EXECUTED`. |
| `getMyReportableExecutions`, `openReport` | UC09: performed executions of the requester, with study images and report. |
| `saveDraft`, `getReportsToValidate`, `validateReport`, `addAddendum` | UC09: draft, second opinion by another physician, addenda. |
| Uses `getPhysicianWorklist`, `getAppointment`, `markAppointmentExecuted`, `getPatient`, `getTestType`, `getStudy` | Provided by C5, C3 and C2 (patches listed in C7-design.md). |

## Approved design (`doc/design/`)
| Side | Classes | File |
|---|---|---|
| common | `ProcedureExecution`, `ExecutionFormView`, `IProcedureExecutionService` (UC10); `DiagnosticReport`, `ReportStatus`, `Addendum`, `ReportableExecutionView`, `ReportWorkspaceView`, `IDiagnosticReportService` (UC09); `ImagingStudy` in `common/C2-common.puml` | `common/C7-common.puml` |
| server | `ProcedureExecutionService`, `DiagnosticReportService`, `IProcedureExecutionRepository`, `IDiagnosticReportRepository`, `IAddendumRepository` and their `InMemory*` classes | `C7-server-design.puml` |
| client | `IU_RecordExecution`, `IU_DiagnosticReports` | `C7-client-design.puml` |

## Sub-issues
| Issue | Branch | Status |
|---|---|---|
| #195 UC10 – Client (C7 v1.0.0) | `feature/UC10-client` | open |
| #196 UC10 – Server (C7 v1.0.0) | `feature/UC10-server` | open |
| #198 UC09 – Client (C7 v1.1.0) | `feature/UC09-client` | open |
| #199 UC09 – Server (C7 v1.1.0) | `feature/UC09-server` | open |
| #193 C7 design (UC10, UC09) | `feature/c7-design` | PR #225 open |

## Links
- Use case issues: #194 (UC10), #197 (UC09) · analysis: [AN-C7-DECISIONS.md](../analysis/AN-C7-DECISIONS.md),
  `doc/analysis/UC10-record-execution-sequence.puml`, `UC09-diagnostic-reports-sequence.puml` · plan: [C7-design.md](C7-design.md)

## Discrepancies between sources
- AN-C7 D59 proposes a new association `Patient * — 1 UserAccount`; the design uses the existing
  `Appointment.physician` instead (C7-design Q3). AN-C7 D59 stays *To confirm* until the team approves the design.
- UC10 depends on UC14 (`getPhysicianWorklist`), which is designed in C5 but not yet implemented.
