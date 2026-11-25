# Context – C6 Patient portal (#186)

> Read first in step 1 of the cycle in [AGENTS.md](../../AGENTS.md) (D-047). Every line cites its source (rule 12); if
> a line disagrees with its source, the source wins (rule 11): tell the member. Every pull request of C6 updates the
> lines of its own sub-issue, and the member who merges it checks that it did. Template: [CONTEXT-TEMPLATE.md](CONTEXT-TEMPLATE.md).

| | |
|---|---|
| **Component** | C6 Patient portal · module `c6-portal` · UC01 → 1.0.0, UC04 → 1.1.0, UC03 → 1.2.0 and UC05 → 1.3.0 in It8 ([agents/components.md](../../agents/components.md)) |
| **Iteration** | It6 · Milestone 4 · component issue #186 |
| **Last update** | 25/11/2026, Marcos Loro, branch `feature/c6-design` |

## Decisions that apply
- D-011, AN-C6 D45 – It6 covers UC01 and UC04 only; UC03 and UC05 are deferred to It8 (#186).
- D-014, D-021, D-032 – One submodule per component (`server/c6-portal`, `client/c6-portal`); `common` package `es.uclm.esi.iso2.common.portal`.
- D-022 – Appointments and reschedule requests are never deleted; status changes only.
- D-023, D-036 – C6 owns `RescheduleRequest`, `IncidentReport` and `SatisfactionSurvey`; changes in finished C1, C2, C4 and C5 go in separate D-036 PATCH PRs (listed in [C6-design.md](C6-design.md)).
- AN-C6 D46, D47 – Permission `USE_PATIENT_PORTAL` for `PATIENT` (C1 patch); every denied check (`ACCESS_DENIED`), access (`APPOINTMENTS_ACCESSED`) and action is audited (UC19).
- AN-C6 D48 – Server resolves the linked patient from `requester` via `IRoleManagementService.getLinkedPatientId(requester)` (`UserAccount.patientId`, C1 patch); client never supplies `patientId`.
- AN-C6 D50, D51 – UC01 lists upcoming non-cancelled appointments (`PatientAppointmentView`); confirmation is `Appointment.confirmedByPatient` (C5 patch, reset on reschedule).
- AN-C6 D52, D53 – `RescheduleRequest` (`PENDING`/`HANDLED`) does not move the appointment; C6 exposes pending-request methods for C5's future patch (`MANAGE_APPOINTMENTS`). Cancellation delegates to C5 and reverts `SchedulingNeed` to `PENDING` (C4/C5 patches).
- AN-C6 D54, D55 – `IncidentReport` (any own appointment) and `SatisfactionSurvey` (past non-cancelled own appointment, one per appointment, 4 `SurveyAspect` ratings 1..5, optional comments) are immutable once sent.

## Contracts (section 6 of #186, refined in [C6-design.md](C6-design.md))
| Operation | In one line |
|---|---|
| `getMyUpcomingAppointments(requester)` | UC01: upcoming non-cancelled own appointments as `PatientAppointmentView`; `USE_PATIENT_PORTAL`; audited. |
| `confirmAppointment(requester, appointmentId)` | UC01: delegates confirmation to C5 (`confirmedByPatient=true`, idempotent); `USE_PATIENT_PORTAL`; audited. |
| `requestReschedule(requester, appointmentId, preferredDates, reason)` | UC01: validates & stores `PENDING` `RescheduleRequest` (no duplicate pending); `USE_PATIENT_PORTAL`; audited. |
| `cancelAppointment(requester, appointmentId)` | UC01: delegates cancellation to C5 (status `CANCELLED`, reverts need to `PENDING`); `USE_PATIENT_PORTAL`; audited. |
| `getPendingRescheduleRequests(requester)`, `markRescheduleRequestHandled(requester, requestId)` | UC01/C5 handoff (#186 §9): admissions staff (`MANAGE_APPOINTMENTS`) read & handle reschedule requests. |
| `getCompletedProcedureHistory`, `getClinicalGuidelines` | UC03 and UC05, deferred to It8 (D-011, D45). |
| Uses `IRoleManagementService`, `IAuditRecorder`, `ISystemParameterService`, `IAppointmentService` | Provided by C1, C2 and C5 (D-023; D-036 patches tracked in [C6-design.md](C6-design.md)). |

## Approved design (`doc/design/`)
| Side | Classes | File |
|---|---|---|
| common | `PatientAppointmentView`, `RescheduleRequest`, `RescheduleRequestStatus`, `IPatientAppointmentService` | `common/C6-common.puml` |
| server | `PatientAppointmentService`, `IRescheduleRequestRepository`, `InMemoryRescheduleRequestRepository` | `C6-server-design.puml` |
| client | `IU_PatientAppointments` | `C6-client-design.puml` |

## Sub-issues
Status: *open* · *PR #n open* · *merged in #n* · *deferred (Itn)*.
| Issue | Branch | Status |
|---|---|---|
| #189 UC01 – Server (C6 v1.0.0) | `feature/UC01-server` | open (design in progress on `feature/c6-design`) |
| #188 UC01 – Client (C6 v1.0.0) | `feature/UC01-client` | open (design in progress on `feature/c6-design`) |
| #192 UC04 – Server (C6 v1.1.0) | `feature/UC04-server` | open (design in progress on `feature/c6-design`) |
| #191 UC04 – Client (C6 v1.1.0) | `feature/UC04-client` | open (design in progress on `feature/c6-design`) |
| UC03, UC05 (C6 v1.2.0, v1.3.0) | — | deferred (It8, Milestone 5) |

## Links
- Use case issues: #187 (UC01), #190 (UC04) · analysis: [AN-C6-DECISIONS.md](../analysis/AN-C6-DECISIONS.md), `doc/analysis/UC01-patient-appointments-sequence.puml`, `UC04-patient-feedback-sequence.puml` · design plan: [C6-design.md](C6-design.md) · use cases: `doc/use-cases/C6-use-cases.puml`

## Discrepancies between sources
- `#186` §6 drafts `getPatientAppointments(patientId)` and `submitIncidentReport(patientId, ...)`, whereas `#189` and AN-C6 D48 require resolving the patient from the authenticated `requester` (no client-supplied `patientId`). Design follows `#189` / D48 (`requester` first).
- `#186`–`#192` still have the *Analysis* RADIT box unticked although PR #217 (`feature/c6-analysis`) is merged (rule 22).
