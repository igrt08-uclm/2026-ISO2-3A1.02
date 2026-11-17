# Approved design

The approved diagrams of each iteration in a form that the agents can read, because they cannot open
`doc/ISO-Project.vpp`: the PlantUML text of each diagram (for example `C1-server-design.puml`) or, if a diagram was
drawn directly in Visual Paradigm, an exported image. The Visual Paradigm project is still the official model
(D-008, D-017): when a diagram changes there, its file here is updated in the same pull request (D-025).

## Differences from the design
The design diagrams show the design decisions and aligned C1–C3 contracts/domain/service/repository/gateway types;
internal UI wiring and private helpers are omitted so the diagrams stay focused and readable (updated #144 after the
earlier #114/#127 notes). The code is the reference for these omitted details:

| Diagram | The diagram omits / abstracts | The code has | Found in |
|---|---|---|---|
| `C1-client-design.png` | Client startup wiring, navigation panels, and internal Swing state | `Main` launcher, `IU_Welcome` home/navigation screen, and internal Swing fields/listeners inside `IU_Login`, `IU_ManageRoles`, and `IU_Audit` | #144 |
| `C2-client-design.png` | Helper sub-panels inside `IU_ConfigureParameters` and internal Swing state | Package-private `TestTypePanel`, `SchedulePanel`, and `ParameterGroupPanel` (`Operations<T>`, `Adapter<T>`, `InactiveGreyRenderer`) inside `es.uclm.esi.iso2.client.configuration.presentation` | #39, #144 |
| `C3-client-design.png` | Internal Swing fields and private form/table helpers in `IU_ManagePatients` | Fields (`requester`, `patients`, `shownRow`) and package-private/private UI helpers (`onNewPatient`, `onSavePatient`, `onSearchRequested`, `changeRow`, `isDirty`, `readForm`) | #144 |
| `C1-server-design.png`, `C2-server-design.png`, `C3-server-design.png` | Private service helpers and injected-dependency constructor signatures | Private validation/authorisation helpers (such as `checkPermission`, `validate`, `saveNew` in `PatientService`) and dependency-injection constructors represented by `«use»` relationships | #144 |
