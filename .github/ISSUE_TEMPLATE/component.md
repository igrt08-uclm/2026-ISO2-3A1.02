---
name: Component version
about: Parent issue for one version of a component. Its use cases are sub-issues.
title: "Creation of <Component> v<MAJOR.MINOR.PATCH>"
assignees: ''
---

## 1. Identification

| Field | Value |
|---|---|
| Component | <!-- e.g. C5 – Appointment planning --> |
| Version (SemVer) | <!-- e.g. v1.1.0 --> |
| Iteration | <!-- e.g. Iteration 5 --> |
| Planned delivery date | <!-- dd/mm/yyyy --> |

## 2. Objective

<!-- One or two sentences: what this version adds to the system and why it is delivered now. -->

## 3. Use cases included (sub-issues)

<!-- One sub-issue per use case. Keep 1 FR = 1 UC. Order by priority. -->

- [ ] #__ UCxx – <use case name> (priority __)

**Not included in this version (deferred):**
<!-- Use cases of this component planned for later versions. -->

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
- [ ] Interfaces match the contracts in section 6.
- [ ] Git tag created: `<component>-v<version>`

## 8. Effort estimation (hours)
[Click to see the table that describes the effort estimation of each UC in this component.](https://url/to/excel/page)

**Agent / model used:** GitHub Copilot

## 9. Risks and assumptions

<!-- Technical risks, simulated systems, open decisions. -->

## 10. Post-mortem (fill in when closing)

- Planned vs actual delivery date:
- Causes of any deviation:
- Corrective actions:
