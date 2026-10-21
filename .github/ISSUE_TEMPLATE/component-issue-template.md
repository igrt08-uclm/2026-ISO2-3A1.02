---
name: Component version
about: Parent issue for one version of a component. Its use cases are sub-issues.
title: "Creation of <Component> v<MAJOR.MINOR.PATCH>"
labels: component
assignees: ''
---

## 1. Identification

| Field | Value |
|---|---|
| Component | <!-- e.g. C5 – Appointment planning --> |
| Side | <!-- Client / Server / Both --> |
| Version (SemVer) | <!-- e.g. v1.1.0 --> |
| Change type | <!-- First release / MINOR (new functionality) / PATCH (fix) / MAJOR (breaking change) --> |
| Iteration | <!-- e.g. Iteration 5 --> |
| Milestone | <!-- e.g. Appointment planning v1.1.0 – delivery date --> |
| Responsible | <!-- @developer --> |
| Planned delivery date | <!-- dd/mm/yyyy --> |

## 2. Objective

<!-- One or two sentences: what this version adds to the system and why it is delivered now. -->

## 3. Use cases included (sub-issues)

<!-- One sub-issue per use case. Keep 1 FR = 1 UC. Order by priority. -->

- [ ] #__ FRxx – <use case name> (priority __)
- [ ] #__ FRxx – <use case name> (priority __)

**Not included in this version (deferred):**
<!-- Use cases of this component planned for later versions. -->

## 4. RADIT disciplines involved

- [ ] Requirements
- [ ] Analysis
- [ ] Design
- [ ] Implementation
- [ ] Tests

<!-- Briefly note what is done in each discipline in this version. -->

## 5. Dependencies

| Depends on | Required use cases / interfaces | Status |
|---|---|---|
| <!-- Component Cx v?.?.? --> | <!-- FRxx --> | <!-- Delivered / Simulated behind interface / Pending --> |

**Blocks:** <!-- components or issues that need this version -->

## 6. Contracts and interfaces

| Interface | Provided / Required | Direction (client→server, server→client) | Notes |
|---|---|---|---|
| <!-- I<Name> --> | <!-- Provided --> | <!-- client→server --> | <!-- external systems simulated? --> |

**Client–server compatibility:** <!-- e.g. Client 1.2.x works with Server ≥ 1.1.0 -->
**Breaking changes:** <!-- None / describe -->

## 7. Acceptance criteria (Definition of Done)

- [ ] All sub-issues (use cases) of this version are closed
- [ ] Each use case has its client GUI and its server logic and data
- [ ] Unit and integration tests designed and passing
- [ ] Access to clinical data emits audit events (FR19)
- [ ] Interfaces match the contracts in section 6
- [ ] CHANGELOG updated with the content of this version
- [ ] Use-case specifications, analysis and design models linked from the wiki
- [ ] Code reviewed (human review of AI-generated code)
- [ ] Git tag created: `<component>-v<version>`
- [ ] Milestone closed

## 8. Effort estimation (hours)

| Use case | Without AI agent | With AI agent | Actual | Notes |
|---|---|---|---|---|
| FRxx | | | | |
| FRxx | | | | |
| Integration and review of the component | | | | |
| **Total** | | | | |

**Agent / model used:** <!-- e.g. Claude Code, model and version -->

## 9. Risks and assumptions

<!-- Technical risks, simulated systems, open decisions. -->

## 10. Post-mortem (fill in when closing)

- Planned vs actual delivery date:
- Causes of any deviation:
- Corrective actions:
