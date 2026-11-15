# Approved design

The approved diagrams of each iteration in a form that the agents can read, because they cannot open
`doc/ISO-Project.vpp`: the PlantUML text of each diagram (for example `C1-server-design.puml`) or, if a diagram was
drawn directly in Visual Paradigm, an exported image. The Visual Paradigm project is still the official model
(D-008, D-017): when a diagram changes there, its file here is updated in the same pull request (D-025).

## Differences from the design
The design diagrams show the design decisions; they are not redrawn every time the code changes. Only what changes
a contract is updated in Visual Paradigm, and the other differences found after the implementation are written here
(agreed with Design on 15/11/2026, #114, #127). The code is the reference for these points.

| Diagram | The diagram shows | The code has | Found in |
|---|---|---|---|
| `C2-client-design.png`, `C2-server-design.png` | One generic `SystemParameter` with a `ParameterGroup`, and `ISystemParameterService` with `getParameters(group)`, `getParameter`, `createParameter`, `updateParameter` and `deactivateParameter` | One class per group in `common.configuration.domain` (`TestType`, `Protocol`, `Schedule`, `AppointmentPolicy`, `Priority`) and typed operations for each one in `ISystemParameterService` (for example `getTestTypes`, `createTestType`, `updateTestType`, `deactivateTestType`, `saveProtocol`, `savePriority`) | D-02 of `doc/testing/defects-C1-C3.md`, #40 |
| `C2-client-design.png` | `requester : UserAccount` in the operations of `ISystemParameterService` | `requester : String` (the username), as in every other contract | #40 |
| `C2-client-design.png` | `IU_ConfigureParameters` with `selectedGroup` and one operation per action | `IU_ConfigureParameters` with the helper panels `TestTypePanel`, `SchedulePanel` and `ParameterGroupPanel` | #39 |
| `C2-server-design.png` | Four repositories: `ISystemParameterRepository`, `InMemorySystemParameterRepository`, `IIntegrationConfigurationRepository` and `InMemoryIntegrationConfigurationRepository` | One generic repository, `IConfigurationRepository<T>` and `InMemoryConfigurationRepository<T>`, used for every group and for the integrations, and the seed data in `ConfigurationSeedData` | #40, #43 |
| `C3-server-design.png` | `PatientService.addSeedData()` | `PatientService.loadSeedData()` | #46 |
