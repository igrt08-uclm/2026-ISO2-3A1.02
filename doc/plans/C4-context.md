# Context – C4 Clinical orders (#65)

> Read first in step 1 of the cycle in [AGENTS.md](../../AGENTS.md) (D-047). Every line cites its source (rule 12); if
> a line disagrees with its source, the source wins (rule 11): tell the member. Every pull request of C4 updates the
> lines of its own sub-issue, and the member who merges it checks that it did. Template: [CONTEXT-TEMPLATE.md](CONTEXT-TEMPLATE.md).

| | |
|---|---|
| **Component** | C4 Clinical orders · module `c4-orders` · UC06 → 1.0.0, UC07 → 1.1.0, UC08 → 1.2.0 ([agents/components.md](../../agents/components.md)) |
| **Iteration** | It4 · Milestone 3 · component issue #65 |
| **Last update** | 24/11/2026, David, pull request #213 (this proposal) |

## Decisions that apply
- D-013 – Order by dependencies: UC08 goes to It9, after C5.
- D-014, D-021, D-032 – One submodule per component with its own version: MINOR per use case, PATCH per fix.
- D-023 – C4 only writes its own data; it uses C2 and C3 through their interfaces.
- D-039 (AN-C4 D32) – Orders cannot be edited or cancelled.
- D-040 (AN-C4 D33) – A follow-up order creates the scheduling needs of the next 12 months, without a timer.
- AN-C4 D27 – It4 covers UC06 and UC07, without UC08. D28 – `TestType` and `Priority` are subclasses of `SystemParameter`.
- AN-C4 D29 – Permission `ORDER_PROCEDURES` for the Physician (patch of C1 merged in #180, #173).
- AN-C4 D30 – A failed permission check is registered in the audit log (`ACCESS_DENIED`).
- AN-C4 D31 – `ClinicalOrder` is abstract and records who asked for it (*to confirm*, #178). D34 – C2/C3 used through interfaces in design.

## Contracts (section 6 of #65)
| Operation | In one line |
|---|---|
| `orderDiagnosticProcedure`, `getOrder`, `getOrdersOfPatient` | Active patient (UC12), active test type and configured priority (UC17); `ORDER_PROCEDURES`; audited (UC19). |
| `orderFollowUp` | Indication, justification, interval in months; only eligible test types; needs of 12 months (D-040). |
| `getPendingSchedulingNeeds`, `markSchedulingNeedBooked`, `markSchedulingNeedPending` | For C5, also with `MANAGE_APPOINTMENTS`; `Appointment` stores `needId` (D-023). |
| `getOrderStatus`, `getExecutionSchedule` | UC08, deferred to It9 (D-013, D-023). |
| Uses `IPatientService.getPatient`, `ISystemParameterService.getTestType`, `getPriorities` | Provided by C3 and C2 (D-023). |

## Approved design (`doc/design/`, last change 2161422)
| Side | Classes | File |
|---|---|---|
| common | `ClinicalOrder` (abstract), `DiagnosticOrder`, `FollowUpOrder`, `SchedulingNeed`, `SchedulingNeedStatus`, `IClinicalOrderService` | `common/C4-common.puml` |
| server | `ClinicalOrderService`, `IClinicalOrderRepository`, `ISchedulingNeedRepository`, `InMemoryClinicalOrderRepository`, `InMemorySchedulingNeedRepository` | `C4-server-design.puml` |
| client | `IU_OrderProcedure`, `IU_OrderFollowUp` | `C4-client-design.puml` |

## Sub-issues
| Issue | Branch | Status |
|---|---|---|
| #68 UC06 – Server (C4 v1.0.0) | `feature/UC06-server` | merged in #202 (tests: #222) |
| #67 UC06 – Client (C4 v1.0.0) | `feature/UC06-client` | merged in #204 |
| #71 UC07 – Server (C4 v1.1.0) | `feature/UC07-server` | merged in #205 (tests: #206) |
| #70 UC07 – Client (C4 v1.1.0) | `feature/UC07-client` | merged in #207 |
| #74, #73 UC08 – Server, Client (C4 v1.2.0) | — | deferred (It9, Milestone 6) |
| #178 Record the physician who asked for an order (AN-C4 D31) | — | open |

## Links
- Use case issues: #66 (UC06), #69 (UC07), #72 (UC08) · analysis: [doc/AN-C4-DECISIONS.md](../AN-C4-DECISIONS.md),
  `doc/analysis/UC06-order-procedure-sequence.puml`, `UC07-order-followup-sequence.puml` · use cases: `doc/use-cases/C4-use-cases.puml`

## Discrepancies between sources
- #65, section 1 says "server and client 1.6.0 → 1.7.0" and section 3 says UC08 is "1.18.0"; the sub-issue titles and
  agents/components.md say C4 1.0.0, 1.1.0 and 1.2.0 (D-032). DECISIONS.md wins: #65 needs updating (its owner decides).
- AN-C4 D29 is still *To confirm* in `doc/AN-C4-DECISIONS.md`, but its patch of C1 is merged (#173, #180).
- #67, #68, #70 and #71 are closed, but *Implementation* and *Testing* are not ticked in their RADIT checklist.
