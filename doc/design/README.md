# Approved design

The official design class diagrams of each iteration live here as modular PlantUML (`.puml`) files (D-046; `doc/ISO-Project.vpp` was retired after Meeting 9, 22/11/2026, and is preserved in Git history):

- **`doc/design/common/`** holds the single-source-of-truth definitions (`!startsub`/`!endsub`) for shared styling (`_style.puml`), cross-component contracts (`shared-contracts.puml`), and each component's `common` domain/contract types (`C1-common.puml`–`C5-common.puml`).
- **`doc/design/Cn-{client,server}-design.puml`** includes the types it needs via `!includesub common/<file>!<TypeName>` and defines the component's `Presentation` or `server` classes, relationships, and notes.
- **Rendered previews (`.png`):** Existing Visual Paradigm `.png` exports (`C1`–`C5` client/server and `UC22-login-communication.png`) are kept untouched for Wiki compatibility (`It1`–`It3`). `.puml` is the single source of truth; whenever a diagram changes or a new iteration (`C6`–`C9`) adds diagrams, generate/update its `.png` preview directly from `.puml` using PlantUML CLI (`plantuml -tpng <file>.puml`, with Graphviz `dot`) or the VS Code / IntelliJ PlantUML extension (*Export Current Diagram*). Every diagram change is reviewed and approved in its pull request (D-008, D-025, D-046).

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
