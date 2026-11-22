# Test design · UC20 – Server (C2 v1.1.0, patch 1.1.1) · #43 – Manage the integrations with the hospital systems

Designed by Testing following lesson T.04 (Testing Management): parameters of each method, equivalence classes,
boundary values and error guessing, combination of the values, and coverage of the code. Expected results come from
#41, #43, the contract `IIntegrationManagementService`, the answers of the plan `doc/plans/UC20-server.md` (Q4–Q12,
Q17 and the decisions of step 5), the texts of the validation errors of 8b11d74, R-05 of #122 (patch C2 1.1.1, #123,
#125) and batch 2 of `doc/plans/UC12-testing-fixes.md` (F-14, F-15, F-16).

Code under test (`server-c2-configuration` 1.1.1, `origin/develop` 25b0b41): `IntegrationManagementService`
(`getIntegrations(String requester)`, `getIntegration(String requester, HospitalSystemType system)`,
`configureIntegration(String requester, IntegrationConfiguration configuration)`,
`testConnection(String requester, HospitalSystemType system)`, `loadSeedData()`), the gateway simulators
`EhrGatewaySimulator`, `AdmissionsGatewaySimulator`, `RisPacsGatewaySimulator` (`ping()`), and the data classes of
UC20 in `common` (`IntegrationConfiguration`, `ConnectionTestResult`, `HospitalSystemType`, `IntegrationMode`,
`IntegrationStatus`).

**Test data (precondition of every case).** The fake `IRoleManagementService` of UC17, extended with the new
permission and one more user (`hasPermission` is the default method of the interface, the same rule as C1):

| User | Active | Permissions |
|---|---|---|
| `admin` | yes | `MANAGE_ROLES`, `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` |
| `mgarcia` | yes | none (physician) |
| `lmartin` | yes | `CONFIGURE_SYSTEM` only |
| `pnavarro` | **no** | `CONFIGURE_SYSTEM`, `CONFIGURE_INTEGRATIONS` |
| `nobody` | — (does not exist) | — |

A recording `IAuditRecorder` keeps every entry (author, action, resource). A `FakeHospitalSystem` double is used for
each of the four external systems (it implements `IAuthenticatorSystem`, `IEhrGateway`, `IAdmissionsGateway` and
`IRisPacsGateway`): it answers `ping()` with a fixed value, can wait some milliseconds, and counts the pings. Unless a
case says otherwise, the service starts empty and `loadSeedData()` gives the four integrations simulated, enabled,
`ACTIVE`, with an empty endpoint and no settings.

## 1. Parameters and testing values

| Method | Parameter | Equivalence classes | Testing values |
|---|---|---|---|
| every operation (`getIntegrations`, `getIntegration`, `configureIntegration`, `testConnection`) | `requester` | Active user with `CONFIGURE_INTEGRATIONS` | `admin` |
| | | Active user without any permission | `mgarcia` |
| | | Active user with `CONFIGURE_SYSTEM` but not `CONFIGURE_INTEGRATIONS` | `lmartin` |
| | | Inactive user with the permission | `pnavarro` |
| | | Unknown user | `nobody` |
| | | Conjeture | `null`; a denied user followed by `admin` on the same call |
| `getIntegrations` | (none; state of the repository) | Seed data | 4 integrations |
| | | Some systems stored, saved out of order | RIS/PACS, admissions, SSO |
| | | Nothing stored | empty repository (Limit Val.) |
| `getIntegration`, `testConnection` | `system` | A system of `HospitalSystemType` (4 values) | `SSO`, `EHR`, `ADMISSIONS`, `RIS_PACS` |
| | | System not stored | `EHR` when only SSO is stored; `RIS_PACS` in an empty service |
| | | No system | `null` (with and without the permission) |
| `configureIntegration` | `configuration` | An object | any of the values below |
| | | No object | `null` |
| | `configuration.system` | A system (4 values) | `SSO`, `EHR`, `ADMISSIONS`, `RIS_PACS` |
| | | No system | `null` |
| | `configuration.mode` | Simulated / real | `SIMULATED`, `REAL` |
| | | No mode | `null` |
| | `configuration.endpoint` | Empty (allowed only in simulated mode) | `""`, `null`, `"   "`, `"\t"` |
| | | `http://` or `https://` (any case) followed by a host | `http://ehr.hospital.test/api`, `https://ehr.hospital.test/api`, `HTTPS://ehr.hospital.test`, `Http://ehr.hospital.test`, `http://ehr.hospital.test:8443/api` (host with port and path) |
| | | Right scheme, no host | `http://` (Limit Val.), `https://` (Limit Val.), `HTTPS://`, `http://:8443`, `http:///path`, `http://user@`, `"  https://  "` |
| | | Wrong scheme | `ftp://ehr.hospital.test`, `ehr.hospital.test`, `http:/ehr.hospital.test`, `http:/` (one character short of `http://`, Limit Val.), `https:` (Limit Val.) |
| | | Conjeture | `"  https://ehr.hospital.test  "` (spaces around), `www.http://ehr.test` (scheme not at the start) |
| | `configuration.settings` | Valid keys and values | `timeout=30`; `timeout`, `application`, `facility` in that order |
| | | No settings | `null`, empty map (Limit Val.) |
| | | Key empty | `""`, `"  "`, `null` |
| | | Value empty | `""`, `"  "`, `null` (key `timeout`) |
| | | Conjeture | `" timeout "=" 30 "`, `application="  RIS"`, `"\tretries\t"="2\n"` (spaces, tabs, new line around) |
| | `configuration.enabled` | Enabled / disabled | `true`, `false` |
| | `configuration.status` | Any status (ignored by the server) | `ACTIVE`, `INACTIVE`, `ERROR`, `null` |
| `testConnection` | stored integration of `system` | Simulated, enabled, the gateway answers | seed data |
| | | Simulated, enabled, the gateway does not answer | `FakeHospitalSystem(false)` |
| | | Disabled | admissions disabled, in simulated and in real mode |
| | | Real mode, enabled | SSO with `https://sso.test` |
| | | Not stored | RIS/PACS in an empty service |
| | gateway response time | Answer after a delay | 50 ms (Limit Val. of `responseTimeMs ≥ 50`) |
| `loadSeedData` | (none; state of the repository) | Empty repository | new service |
| | | Repository with data | after the seed data; after a change; only SSO stored |
| `ping` of each simulator | (none) | One call / several calls | 1 call; 3 calls |

Outputs:
- `getIntegrations`: the list of integrations in the order of `HospitalSystemType` (empty if nothing is stored).
- `getIntegration`: the integration, or `null` if it is not stored.
- `configureIntegration`: the saved `IntegrationConfiguration` (endpoint, keys and values stripped; status `ACTIVE` if
  enabled, `INACTIVE` if not), and one audit entry `INTEGRATION_CHANGED` on `INTEGRATION:<SYSTEM>`.
- `testConnection`: a `ConnectionTestResult` (`success`, `responseTimeMs`, message "Connection OK", "The system did not
  answer", "integration disabled", "real mode not available in this version" or "integration not configured").
- Exceptions: `AccessDeniedException(requester)` and one audit entry `INTEGRATION_ACCESS_DENIED` (resource
  `INTEGRATION:<SYSTEM>`, or `INTEGRATION` when there is no system) when the requester has no permission, always before
  any validation; `ValidationException` with the list of errors ("The integration is required", "The system is
  required", "The mode is required", "The endpoint is required in real mode", "The endpoint must start with http:// or
  https://", "The endpoint needs a host after http:// or https://", "A setting needs a key", "The setting <key> needs a
  value"), with nothing saved or audited; `IllegalArgumentException("The system is required")` for a `null` system in
  `getIntegration` and `testConnection` when the requester has the permission.

## 2. Combination of the values

- **Maximum number of test cases.** There is no maximum for `requester`, `endpoint` and the keys and values of the
  settings (any `String`), so their domains are divided into the classes of section 1. With those classes, all the
  combinations of `configureIntegration` would be 6 requesters × (1 `null` configuration + 5 systems × 4 endpoint
  classes × 3 modes × 2 enabled × 4 statuses × 5 settings classes) = 6 × 2 401 = **14 406** cases;
  `testConnection` 6 × 5 systems × 5 stored states = **150**; `getIntegration` 6 × 5 systems × 2 (stored or not) =
  **60**; `getIntegrations` 6 × 3 states = **18**; `loadSeedData` 2.
- **All combinations** are used where two parameters decide the result together:
  - `requester` × operation: the 6 requester classes × the 4 operations (TC20S-01 to TC20S-06, parameterised by the
    operation) = 24 cases; with `system = null`, the 4 denied requesters × `getIntegration` and `testConnection`
    (TC20S-59, TC20S-60).
  - `endpoint` × `mode`: every endpoint value of the classes "with host", "no host" and "wrong scheme" is run in real
    **and** in simulated mode (TC20S-17, TC20S-29); the empty class with both modes (TC20S-21, TC20S-28).
  - `enabled` × `status`: 2 × 4 = 8 combinations (TC20S-19).
- **Each use** for the rest: every invalid class is used once with the other parameters valid (TC20S-25 to
  TC20S-31), every system once for the audit resource, the ping and the failed ping (TC20S-24, TC20S-34, TC20S-35),
  and every stored state of `testConnection` once (TC20S-34 to TC20S-39). TC20S-32 and TC20S-33 combine several
  invalid classes on purpose (error guessing: every error together, in order).
- **Pairwise** is not used: the parameters that do not decide the result together (for example `settings` and
  `system`) are covered by each use.

## 3. Coverage (white box)

Java evaluates `&&` and `||` in short circuit, so a condition that is not evaluated is written "–". A pair of cases
shows MC/DC for a condition when that condition changes, the outcome changes, and every other condition that is
evaluated in both cases keeps its value. Measured with the JaCoCo report of `mvn -B verify` on `origin/develop`
(25b0b41): `IntegrationManagementService` 101/101 lines and 79/80 branches; the three simulators 100 % of lines.

**`checkPermission`** (called by every operation): `!hasPermission(requester, CONFIGURE_INTEGRATIONS)`, one condition.
True in TC20S-02 to TC20S-07, TC20S-59, TC20S-60; false in TC20S-01. Decision = condition/decision = MC/DC. The
conditions inside `hasPermission` (user exists, active, has the permission) belong to C1; the classes unknown,
inactive and without permission make each of them decide the denial (TC20S-05, TC20S-04, TC20S-02/03).

**`getIntegrations`**: no decision of its own (permission and sort). Sort with 0, 3 and 4 elements: TC20S-11, TC20S-10,
TC20S-09.

**`getIntegration`**
- `system == null ? RESOURCE : resourceOf(system)`: true TC20S-13, TC20S-59; false TC20S-12.
- `requireSystem`: `system == null` true TC20S-13, TC20S-61; false TC20S-12, TC20S-14.

**`configureIntegration`**
- `configuration == null ? null : configuration.getSystem()`: true TC20S-07 (second call), TC20S-25; false TC20S-15.
- `system == null ? RESOURCE : resourceOf(system)`: true TC20S-07 (`null` configuration), TC20S-26; false TC20S-15.
- `!errors.isEmpty()`: true TC20S-25 to TC20S-33; false TC20S-15.
- `configuration.getEndpoint() == null ? "" : strip()`: true TC20S-21 (`null`); false TC20S-20.
- `configuration.getSettings() != null`: true TC20S-15, TC20S-62; false TC20S-22.
- `configuration.isEnabled() ? ACTIVE : INACTIVE`: true and false in TC20S-19.

**`validate`** (private, reached through `configureIntegration`)
- `configuration == null`: true TC20S-25; false the rest.
- `configuration.getSystem() == null`: true TC20S-26, TC20S-32; false TC20S-27.
- `configuration.getMode() == null`: true TC20S-27, TC20S-32; false TC20S-26.
- `endpoint.isEmpty() && mode == REAL` (A, B):

  | A empty | B real | Outcome | Cases |
  |---|---|---|---|
  | T | T | T ("required in real mode") | TC20S-28 |
  | T | F | F | TC20S-15, TC20S-21, TC20S-27 (`null` mode) |
  | F | – | F | TC20S-16 |

  MC/DC: A by TC20S-28 / TC20S-16; B by TC20S-28 / TC20S-21.
- `else if (!endpoint.isEmpty())`: true TC20S-16, TC20S-29; false TC20S-15, TC20S-21.
- `!lower.startsWith("http://") && !lower.startsWith("https://")` (A, B):

  | A not http:// | B not https:// | Outcome | Cases |
  |---|---|---|---|
  | T | T | T ("must start with …") | TC20S-29 (`ftp://…`, `http:/`, `https:` …) |
  | F | – | F | TC20S-16 (`http://ehr.hospital.test/api`), TC20S-17 (`Http://…`) |
  | T | F | F | TC20S-16 (`https://ehr.hospital.test/api`), TC20S-17 (`HTTPS://…`) |

  MC/DC: A by TC20S-29 / TC20S-16 (`http://`); B by TC20S-29 / TC20S-16 (`https://`).
- `lower.startsWith("http://") ? 7 : 8`: true TC20S-17 (`http://:8443`); false TC20S-17 (`https://`).
- `!hasHost(…)`: true TC20S-17 (rejected values); false TC20S-16, TC20S-17 (accepted values).
- `configuration.getSettings() != null`: true TC20S-18; false TC20S-22.
- `key == null || key.isBlank()` (A, B): (T, –) → T TC20S-30 `null`; (F, T) → T TC20S-30 `""`, `"  "`; (F, F) → F
  TC20S-18. MC/DC: A by `null` / TC20S-18; B by `""` / TC20S-18.
- `value == null || value.isBlank()` (A, B): (T, –) → T TC20S-31 `null`; (F, T) → T TC20S-31 `""`, `"  "`; (F, F) → F
  TC20S-18. MC/DC: A by `null` / TC20S-18; B by `""` / TC20S-18.

**`hasHost`** (private, reached through `configureIntegration`)
- Loop over `/`, `?`, `#` with `index >= 0 && index < end` (A, B): (T, T) → T for `/` in `http:///path` and
  `http://ehr.hospital.test:8443/api`; (F, –) → F for `?` and `#` in every value, and for `/` in `http://`. The case
  (T, F) is **not covered** (see below), so decision coverage is reached but condition coverage, condition/decision and
  MC/DC of B are not.
- `colon >= 0 ? substring : authority`: true TC20S-17 (`http://:8443`, `http://ehr.hospital.test:8443/api`); false
  TC20S-17 (`http://`, `HTTPS://ehr.hospital.test`).
- `!(…).isBlank()`: true TC20S-17 (accepted), false TC20S-17 (`http://`, `http://:8443`, `http:///path`,
  `http://user@`).

**`testConnection`**
- `system == null ? RESOURCE : resourceOf(system)`: true TC20S-40, TC20S-60; false TC20S-34.
- `requireSystem`: true TC20S-40, TC20S-61; false TC20S-34.
- `configuration == null`: true TC20S-39; false TC20S-34.
- `!configuration.isEnabled()`: true TC20S-37 (simulated and real); false TC20S-38.
- `configuration.getMode() == REAL`: true TC20S-38; false TC20S-34.
- `answered ? "Connection OK" : "The system did not answer"`: true TC20S-34; false TC20S-35.
- `switch` of `ping` (4 branches): each system in TC20S-34 and TC20S-35.
- The order of the decisions (not stored → disabled → real) is shown by TC20S-37 in real mode (disabled wins over
  real).

**`loadSeedData`**: `integrations.findAll().isEmpty()` true TC20S-42; false TC20S-44, TC20S-45. Loop: 4 iterations
(TC20S-42).

**Data classes**: `IntegrationConfiguration` has one decision, `settings == null ? null : copy`: true TC20S-53 (and
TC20S-54), false TC20S-48 (2/2 branches). The simulators and `ConnectionTestResult` have no decisions.

### Not covered (to add)
1. **`hasHost`, condition `index < end` false.** No endpoint has a second separator after an earlier one, so the loop
   never finds a separator that is behind the end already found. Case to add: `http://ehr.hospital.test/api?x=1`
   (`?` after `/`) → accepted; the only branch that JaCoCo reports as missed (line 193).
2. **`hasHost`, `?` and `#` as the end of the host.** No endpoint has `?` or `#`, so `index >= 0` is never true for
   them (error guessing): `http://?a=b` and `http://#top` → "The endpoint needs a host after http:// or https://";
   `http://ehr.hospital.test?a=b` → accepted.
3. **Boundary value of the class "with host".** The adjacent value of the limit `http://` (no host) is a host of one
   character, `http://a` (and `https://a`): it should be accepted; no test uses a one-character host.

## 4. Test cases

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC20S-01 | all four | `admin` calls each of the four operations (`getIntegrations`, `getIntegration(EHR)`, `configureIntegration(ADMISSIONS …)`, `testConnection(RIS_PACS)`), after `loadSeedData()` | It works; only `configureIntegration` is audited (`INTEGRATION_CHANGED`); reads and the test are not | `administratorCanCallEveryOperation` |
| TC20S-02 | all four | `mgarcia` (active, no permission) calls each operation | `AccessDeniedException` with the requester; one audit entry `INTEGRATION_ACCESS_DENIED` with the resource of the operation; nothing saved; no gateway asked | `userWithoutPermissionIsDenied` |
| TC20S-03 | all four | `lmartin` (has `CONFIGURE_SYSTEM`, not `CONFIGURE_INTEGRATIONS`) calls each operation | Same as TC20S-02: the two permissions are separate | `configureSystemIsNotEnough` |
| TC20S-04 | all four | `pnavarro` (has the permission but is inactive) calls each operation | Same as TC20S-02 | `inactiveUserIsDenied` |
| TC20S-05 | all four | `nobody` (unknown) calls each operation | Same as TC20S-02 | `unknownUserIsDenied` |
| TC20S-06 | all four | Conjeture: `null` requester calls each operation | `AccessDeniedException`, no other exception; the audit entry has a `null` author | `nullRequesterIsDenied` |
| TC20S-07 | `configureIntegration` | Conjeture: `mgarcia` sends an invalid configuration (`ftp://`) and a `null` configuration | `AccessDeniedException`, not `ValidationException` (the permission comes first); resources `INTEGRATION:SSO` and `INTEGRATION` (no system) | `permissionIsCheckedBeforeValidation` |
| TC20S-08 | `testConnection` | Conjeture: `mgarcia` is denied a test, then `admin` makes the same call | The second call works; only the denial is audited | `denialLeavesNothingBehind` |
| TC20S-09 | `getIntegrations` | After the seed data | SSO, EHR, admissions, RIS/PACS, in that order | `integrationsAreInTheOrderOfTheSystems` |
| TC20S-10 | `getIntegrations` | Conjeture: empty service; save RIS/PACS, admissions and SSO in that order | Returned as SSO, admissions, RIS/PACS (order of `HospitalSystemType`, not of the saves) | `orderDoesNotDependOnTheSaves` |
| TC20S-11 | `getIntegrations` | Limit Val.: nothing stored | Empty list | `noIntegrationsGiveAnEmptyList` |
| TC20S-12 | `getIntegration` | `ADMISSIONS` after the seed data | The integration of admissions | `getIntegrationReturnsThatSystem` |
| TC20S-13 | `getIntegration` | `getIntegration(admin, null)` | `IllegalArgumentException` "The system is required"; nothing audited | `getIntegrationWithNullSystem` |
| TC20S-14 | `getIntegration` | `EHR` when only SSO is stored | `null` | `getIntegrationOfAMissingSystem` |
| TC20S-15 | `configureIntegration` | EHR, simulated, enabled, empty endpoint, settings `timeout=30` | Returned and stored with the same data and status `ACTIVE`; audit `INTEGRATION_CHANGED`, author `admin`, resource `INTEGRATION:EHR` | `simulatedWithEmptyEndpointIsSaved` |
| TC20S-16 | `configureIntegration` | Real mode with `https://ehr.hospital.test/api` and with `http://…` | Saved in mode `REAL` | `realWithValidEndpointIsSaved` |
| TC20S-17 | `configureIntegration` | Limit Val. of the host, in real and simulated mode: `http://`, `https://`, `HTTPS://`, `http://:8443`, `http:///path`, `http://user@`, `"  https://  "`; and `HTTPS://ehr.hospital.test`, `Http://ehr.hospital.test`, `http://ehr.hospital.test:8443/api` | Without a host: "The endpoint needs a host after http:// or https://"; with a host: accepted and saved as written (the scheme ignores case). Before the patch 1.1.1: `http://` alone was accepted | `endpointWithHostIsAccepted`, `endpointWithoutHostIsRejected` |
| TC20S-18 | `configureIntegration` | Settings `timeout`, `application`, `facility` in that order | Stored in the same order | `settingsKeepTheirOrder` |
| TC20S-19 | `configureIntegration` | Status sent `ACTIVE`, `INACTIVE`, `ERROR` or `null`, once enabled and once disabled | Enabled → `ACTIVE`, disabled → `INACTIVE`, whatever was sent; `ERROR` never stored | `statusIsSetByTheServer` |
| TC20S-20 | `configureIntegration` | Conjeture: endpoint `"  https://ehr.hospital.test  "` | Returned and stored as `https://ehr.hospital.test` | `endpointIsStripped` |
| TC20S-21 | `configureIntegration` | Limit Val. / Conjeture: simulated with endpoint `null`, `"   "`, `"\t"` | Accepted and stored as `""` | `blankEndpointInSimulatedModeIsSavedEmpty` |
| TC20S-22 | `configureIntegration` | Limit Val.: settings `null` and empty map | Accepted (`null` kept, empty stays empty) | `missingSettingsAreAccepted` |
| TC20S-23 | `configureIntegration` | Save RIS/PACS twice after the seed data | The second replaces the first; still 4 integrations; two `INTEGRATION_CHANGED` entries | `savingAgainReplaces` |
| TC20S-24 | `configureIntegration` | A valid save of each system | Audit resource `INTEGRATION:SSO`, `:EHR`, `:ADMISSIONS`, `:RIS_PACS` | `changeIsAuditedWithTheSystem` |
| TC20S-25 | `configureIntegration` | `configureIntegration(admin, null)` | `ValidationException` "The integration is required"; nothing saved or audited | `nullConfiguration` |
| TC20S-26 | `configureIntegration` | System `null` | `ValidationException` "The system is required"; nothing saved or audited | `nullSystem` |
| TC20S-27 | `configureIntegration` | Mode `null` | `ValidationException` "The mode is required"; nothing saved or audited | `nullMode` |
| TC20S-28 | `configureIntegration` | Limit Val.: real mode with endpoint `null`, `""`, `"   "` | `ValidationException` "The endpoint is required in real mode"; nothing saved or audited | `realModeNeedsAnEndpoint` |
| TC20S-29 | `configureIntegration` | Endpoint `ftp://…`, `ehr.hospital.test`, `http:/ehr…`, `http:/` (one char short, Limit Val.), `https:`, `www.http://…` (Conjeture), in real **and** simulated mode | `ValidationException` "The endpoint must start with http:// or https://"; nothing saved or audited | `endpointWithoutTheRightPrefix` |
| TC20S-30 | `configureIntegration` | A setting with key `""`, `"  "` or `null` | `ValidationException` "A setting needs a key"; nothing saved or audited | `settingWithoutKey` |
| TC20S-31 | `configureIntegration` | Setting `timeout` with value `""`, `"  "` or `null` | `ValidationException` "The setting timeout needs a value"; nothing saved or audited | `settingWithoutValue` |
| TC20S-32 | `configureIntegration` | Conjeture: system `null`, mode `null`, endpoint `ftp://x`, settings `""=30`, `facility=""`, `application=" "` | The 6 errors together, in that order; nothing stored | `everyErrorComesTogether` |
| TC20S-33 | `configureIntegration` | Conjeture: after the seed, EHR in real mode, no endpoint, `timeout=""` | 2 errors; the stored EHR is exactly as before | `invalidChangeKeepsTheStoredIntegration` |
| TC20S-34 | `testConnection` | Seeded (simulated, enabled) SSO, EHR, admissions, RIS/PACS | `success = true`, "Connection OK", time ≥ 0; only the gateway of that system is pinged, once; nothing audited | `simulatedEnabledIntegrationAnswers` |
| TC20S-35 | `testConnection` | The gateway of each system answers `false` | `success = false`, "The system did not answer"; that gateway pinged once | `gatewayThatDoesNotAnswer` |
| TC20S-36 | `testConnection` | Limit Val. of the time: the EHR waits 50 ms before answering | `responseTimeMs ≥ 50` (measured around `ping()`) | `responseTimeIsMeasured` |
| TC20S-37 | `testConnection` | Admissions disabled, in simulated and in real mode | `success = false`, "integration disabled", time 0, no ping (disabled comes first) | `disabledIntegration` |
| TC20S-38 | `testConnection` | SSO enabled in real mode | `success = false`, "real mode not available in this version", time 0, no ping | `realModeIsNotAvailable` |
| TC20S-39 | `testConnection` | RIS/PACS not stored | `success = false`, "integration not configured", time 0, no ping | `integrationNotConfigured` |
| TC20S-40 | `testConnection` | `testConnection(admin, null)` | `IllegalArgumentException` "The system is required"; no ping; nothing audited | `testConnectionWithNullSystem` |
| TC20S-41 | `testConnection` | Conjeture: test all four with OK, no answer, disabled and real | The stored configurations are identical before and after; no status becomes `ERROR`; no audit entry added | `testNeverChangesTheStoredSettings` |
| TC20S-42 | `loadSeedData` | Empty service built with the constructor that `Main` uses | 4 integrations in the order SSO, EHR, admissions, RIS/PACS; all simulated, enabled, `ACTIVE`, endpoint `""`, no settings; nothing audited | `seedDataIsLoaded` |
| TC20S-43 | `loadSeedData`, `testConnection` | Seed data with the real simulators; test every system | Every system gives "Connection OK" | `seedDataWithTheSimulatorsAnswers` |
| TC20S-44 | `loadSeedData` | Conjeture: called twice | Still 4 integrations | `seedDataTwice` |
| TC20S-45 | `loadSeedData` | Conjeture: seed, change EHR, seed again; and another service with only SSO stored, then seed | The changed EHR stays; the other service still has only SSO | `seedDataDoesNotOverwrite` |
| TC20S-46 | `ping` (simulators) | `EhrGatewaySimulator`, `AdmissionsGatewaySimulator`, `RisPacsGatewaySimulator` | `true` | `GatewaySimulatorsTest.simulatorsAlwaysAnswer` |
| TC20S-47 | `ping` (simulators) | Conjeture: three pings of each simulator | Always `true` | `GatewaySimulatorsTest.simulatorsAnswerEveryTime` |
| TC20S-48 | `IntegrationConfiguration` | Getters | Every field as given | `IntegrationConfigurationTest.keepsItsData` |
| TC20S-49 | `IntegrationConfiguration` | Conjeture: change the original settings map after creating the configuration | The configuration keeps its own copy | `IntegrationConfigurationTest.settingsAreCopied` |
| TC20S-50 | `IntegrationConfiguration` | Conjeture: `put` / `remove` on `getSettings()` | `UnsupportedOperationException`; the settings do not change | `IntegrationConfigurationTest.settingsAreUnmodifiable` |
| TC20S-51 | `IntegrationConfiguration` | Settings `zeta`, `alpha`, `mu` | Order kept | `IntegrationConfigurationTest.settingsKeepTheirOrder` |
| TC20S-52 | `IntegrationConfiguration` | Conjeture: empty and `null` keys and values | Kept as they are, so the server can report them | `IntegrationConfigurationTest.emptyValuesAreKept` |
| TC20S-53 | `IntegrationConfiguration` | Limit Val.: `null` map and empty map | `null` kept as `null`; empty stays empty | `IntegrationConfigurationTest.nullAndEmptySettings` |
| TC20S-54 | `IntegrationConfiguration` | Conjeture: every nullable field `null` | Kept as `null` (the server validates) | `IntegrationConfigurationTest.nullFieldsAreKept` |
| TC20S-55 | `ConnectionTestResult` | `ConnectionTestResult(true, 12, "Connection OK")` | Getters give those values | `ConnectionTestResultTest.successfulResult` |
| TC20S-56 | `ConnectionTestResult` | `ConnectionTestResult(false, 0, "integration disabled")` | Getters give those values | `ConnectionTestResultTest.failedResult` |
| TC20S-57 | `HospitalSystemType` | `values()` | SSO, EHR, ADMISSIONS, RIS_PACS in that order | `IntegrationEnumsTest.hospitalSystemTypes` |
| TC20S-58 | `IntegrationMode`, `IntegrationStatus` | `values()` | SIMULATED, REAL; ACTIVE, INACTIVE, ERROR | `IntegrationEnumsTest.modesAndStatuses` |
| TC20S-59 | `getIntegration` | `getIntegration(requester, null)` by `mgarcia`, `lmartin`, `pnavarro` (inactive) and `nobody` (unknown), after `loadSeedData()` | `AccessDeniedException` with the requester (not `IllegalArgumentException`); one audit entry `INTEGRATION_ACCESS_DENIED` on `INTEGRATION` | `getIntegrationWithNullSystemWithoutPermissionIsDenied` |
| TC20S-60 | `testConnection` | `testConnection(requester, null)` by the same four users | Same as TC20S-59; no gateway is asked | `testConnectionWithNullSystemWithoutPermissionIsDenied` |
| TC20S-61 | `getIntegration`, `testConnection` | Conjeture: `mgarcia` is denied `getIntegration(null)`, then `admin` calls `getIntegration(null)` and `testConnection(null)` | Both give `IllegalArgumentException` "The system is required" (the validation still comes after the permission); only the denial of `mgarcia` is audited; no gateway asked | `nullSystemWithPermissionStillFailsTheValidation` |
| TC20S-62 | `configureIntegration` | Conjeture: settings `" timeout "=" 30 "`, `application="  RIS"`, `"\tretries\t"="2\n"`, `facility=HGUCR` | Returned and stored as `timeout=30`, `application=RIS`, `retries=2`, `facility=HGUCR`, in that order; audited `INTEGRATION_CHANGED` on `INTEGRATION:EHR` | `settingsAreSavedStripped` |

Tests TC20S-01 to TC20S-45 and TC20S-59 to TC20S-62 are in `IntegrationManagementServiceTest`
(`server/c2-configuration`, nested classes `PermissionTests`, `ReadTests`, `ValidConfigurationTests`,
`InvalidConfigurationTests`, `ConnectionTestTests`, `SeedDataTests`, `FindingsOf122Tests`); TC20S-46 and TC20S-47 in
`GatewaySimulatorsTest` (`server/c2-configuration`); TC20S-48 to TC20S-58 in `IntegrationConfigurationTest`,
`ConnectionTestResultTest` and `IntegrationEnumsTest` (`common`). TC20S-59 to TC20S-62 come from the fixes after the
findings of #122 (batch 2 of `doc/plans/UC12-testing-fixes.md`, step 8, commit 347e4d0); F-16 stays as it was (Q7,
step 11): `null` settings are still saved as `null` and TC20S-22 does not change.

Postcondition of every case: a denied or rejected call leaves the repository as it was and audits only the denial (or
nothing, for a rejection); a valid `configureIntegration` leaves exactly one more audit entry and the integration of
that system replaced; `getIntegrations`, `getIntegration`, `testConnection` and `ping` change nothing (no status
becomes `ERROR`, nothing audited); `loadSeedData` changes the repository only if it was empty.

## Findings while designing (to ask Javier, not tested as errors)

1. **(Fixed by F-14 of #122, tested by TC20S-59 to TC20S-61.) `system = null` is checked before the permission.** In `getIntegration` and `testConnection`, a user without the
   permission who sends `system = null` gets `IllegalArgumentException`, not `AccessDeniedException`, and the denied
   access is **not** audited. The plan does not say which comes first. Not tested either way.
2. **(Fixed by R-05 in the patches C2 1.1.1 and 1.1.2.) The endpoint rule only checks the start.** `http://` and `https://` alone are accepted (TC20S-17), and the check is
   case sensitive, so `HTTPS://ehr.test` would be rejected. Both follow Q8 literally; to confirm with Javier.
3. **(Fixed by F-15, tested by TC20S-62.) Settings are not stripped.** The endpoint is stripped (step 5), but the keys and values of the settings are saved
   as they come (`" 30 "` stays with its spaces); only blank ones are rejected.
4. **`null` settings are stored as `null`**, not as an empty map, so a client that reads them must handle `null`.
5. The test double `FakeRoleManagementService`, shared with UC17, now also has `CONFIGURE_INTEGRATIONS` for `admin`
   and `pnavarro` and the user `lmartin`; the UC17 tests still pass unchanged.
