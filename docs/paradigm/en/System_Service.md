---
slug: "/System_Service"
title: 'Service'
---

`Service` is a [system module](System_modules.md) that gathers the server's administration surface: database service and recalculation actions, virtual-machine and memory control, scheduled server restart, application-wide settings, per-user diagnostics and logging flags, per-user client GUI preferences, database scaling, and the server-settings API exported to clients. It is pulled in via `REQUIRE Service` (it does `REQUIRE System, Security, SystemEvents`; its own declarations live in `NAMESPACE Service`).

Most actions are declared through the `INTERNAL` operator over a Java implementation class, so they expose server operations that have no pure-`.lsf` body. The module also publishes the forms `maintenance`, `settings`, and `scaling` together with the matching navigator entries under the system folder.

### Database service

These single-threaded actions run database maintenance and recalculation directly. They are surfaced on the `maintenance` form.

| Action                          | What it does                                                                |
|---------------------------------|------------------------------------------------------------------------------|
| `serviceDBAction[]`             | runs the full database service routine                                       |
| `checkClassesAction[]`          | checks that stored object classes are consistent                             |
| `checkMaterializationsAction[]` | checks materialized property values against their definitions                |
| `checkIndicesAction[]`          | checks that the expected indices exist in the database                       |
| `checkTablesAction[]`           | checks the database tables                                                    |
| `recalculateClassesAction[]`    | recalculates stored object classes                                           |
| `recalculateAction[]`           | recalculates stored (materialized) property values                           |
| `recalculateFollowsAction[]`    | re-applies `RESOLVE`-style follows rules to bring data into a consistent state |
| `recalculateStatsAction[]`      | recalculates table and column statistics                                     |
| `overCalculateStatsAction[]`    | recalculates the extended statistics; `maxQuantityOverCalculate[]` caps how many distinct values are sampled |
| `updateStats[]`                 | refreshes the statistics the query optimizer uses                            |
| `analyzeDBAction[]`             | runs the database `ANALYZE`                                                   |
| `vacuumDBAction[]`              | runs the database `VACUUM`                                                    |
| `packAction[]`                  | physically removes rows marked as deleted                                     |

#### Multi-threaded variants

The heavier service and recalculation actions also come in a multi-threaded family, surfaced in the `multiThread` block of the `maintenance` form: `serviceDBMultiThreadAction`, `checkClassesMultiThreadAction`, `checkMaterializationsMultiThreadAction`, `recalculateClassesMultiThreadAction`, `recalculateMultiThreadAction`, `recalculateFollowsMultiThreadAction`, `recalculateStatsMultiThreadAction`, and `overCalculateStatsMultiThreadAction`. Each does the same work as its single-threaded counterpart but spreads the per-table work over several worker threads.

Every action in the family has three forms with the same name:

| Signature                | How it picks thread count and timeout                                                          |
|--------------------------|------------------------------------------------------------------------------------------------|
| `…MultiThreadAction[INTEGER, INTEGER]` | the base form: the first argument is the thread count (`NULL` lets the platform decide), the second is the per-property timeout in seconds (`NULL` for no timeout) |
| `…MultiThreadAction[INTEGER]`          | a timeout-only wrapper: calls the base form with `NULL` thread count and the given timeout |
| `…MultiThreadAction[]`                 | a thread-count-only wrapper: calls the base form with `threadCountMultiThread[]` and no timeout |

`threadCountMultiThread[]` is the local property the `maintenance` form binds the no-argument wrappers to, so the operator chooses the worker count on the form.

### Database modes

These flags switch how database service and recalculation run. Each is a stored property whose change is pushed into the server: the module declares a `set…` action through `INTERNAL`, a `refresh…` action that re-applies the current value, a `WHEN CHANGED` event that calls the refresh on every change, and an `onStarted` re-application so the value survives a restart. The `@defineMode` metacode generates this whole bundle for one flag.

| Property                      | What it controls                                                                       |
|-------------------------------|-----------------------------------------------------------------------------------------|
| `disableTILMode[]`            | disables the transaction-isolation-level mode (`REPEATABLE READ`) for service operations; built with `@defineMode` |
| `hostnameServerComputer[]`    | the host name of the `Computer` treated as the server (over `serverComputer`); built with `@defineMode` |
| `reupdateMode[]`              | turns on the re-update mode used while recalculating; wired with its own `setReupdateMode[BOOLEAN]` / `refreshReupdateMode[]` pair |
| `singleTransaction[]`         | runs the service routine inside a single transaction                                    |

### Virtual machine and memory

These actions inspect and reclaim server JVM resources, surfaced in the `virtualMachine` block of the `maintenance` form.

| Action / property             | What it does                                                                            |
|-------------------------------|-----------------------------------------------------------------------------------------|
| `runGarbageCollector[]`       | forces a JVM garbage collection                                                          |
| `getVMInfo[]`                 | collects current virtual-machine information                                             |
| `makeHeapDump[]`              | writes a JVM heap dump                                                                   |
| `makeProcessDumpAction[]`     | writes a process dump                                                                    |
| `dropLRU[]`                   | drops cached values by the least-recently-used policy; calls `dropLRUCustom[DOUBLE, BOOLEAN]` with `dropLRUPercent[]` (the fraction to drop) and `randomDropLRU[]` (drop at random instead of by age) |

### Server restart

The module controls a graceful server restart and a login lock. `restartPushed[]` records that a restart was requested; `notRestartPushed[]` is its negation and drives which control the form shows.

| Property / action                     | What it does                                                                   |
|---------------------------------------|---------------------------------------------------------------------------------|
| `scheduledRestart[]`                  | flag: when set, requesting a restart actually restarts the server               |
| `forbidLogin[]`                       | flag: when set, requesting a restart blocks new logins (a pending restart)       |
| `restartServer[]`                     | abstract list action that performs the restart request: under `scheduledRestart[]` runs `restartServerAction[]`, under `forbidLogin[]` runs `setPendingRestartAction[]`, sets `restartPushed[]`, and applies |
| `cancelRestartServer[]`               | abstract list action that undoes the request: runs `cancelRestartServerAction[]` and `resetPendingRestartAction[]`, clears `restartPushed[]`, and applies |
| `restartServerAction[]` / `cancelRestartServerAction[]` | the underlying server restart and cancel-restart actions       |
| `setPendingRestartAction[]` / `resetPendingRestartAction[]` | set / clear the server's pending-restart flag             |
| `isServerRestarting[]`                | native flag: the server is in the process of restarting; `isNotServerRestarting[]` is its negation |

### Settings

The `Setting` mechanism stores named application-wide settings whose value can be overridden per [user role](System_Authentication.md). A `Setting` object is identified by its `name[Setting]` (`ISTRING`); `setting[ISTRING]` looks a setting up by name.

A setting carries three stored values and resolves them by `OVERRIDE`, taking the first non-empty one:

| Property                        | Role in resolution                                                            |
|---------------------------------|--------------------------------------------------------------------------------|
| `baseValue[Setting, UserRole]`  | the value set for a specific role — checked first                              |
| `baseValue[Setting]`            | the value set for the setting as a whole — checked next                        |
| `defaultValue[Setting]`         | the built-in default — checked last                                            |

So `value[Setting, UserRole]` resolves to `baseValue[Setting, UserRole]`, then `baseValue[Setting]`, then `defaultValue[Setting]`; `value[Setting]` (no role) resolves to `baseValue[Setting]`, then `defaultValue[Setting]`. `overBaseValue[Setting, UserRole]` is the role-or-global base value (without the default).

To read a setting by name from logic: `valueSetting[ISTRING]` returns the resolved string value of the named setting, and `valueSettingBoolean[ISTRING]` returns whether that value equals `'true'`.

`pushSetting[STRING, STRING]` and `popSetting[STRING]` temporarily set and restore a setting value on the server. `writeDefaultSettings[]` seeds the default values; `updateSetting[Setting, UserRole, BOOLEAN]` and the helper `updateGlobalSetting` push a changed value into the running server (the system user, role-less users, and each role), driven by `WHEN CHANGED` on `baseValue` and re-applied in `onInit`. The platform ships a number of named settings (read through `valueSetting` / `valueSettingBoolean` across the platform); the seeded defaults are not listed here.

### Per-user diagnostics

These per-`User` flags turn on extra logging and query diagnostics for a single user. Each follows the same pattern: a stored `DATA BOOLEAN (User)` property, a `set…` action declared through `INTERNAL` that pushes the value into the server, a `refresh…` action re-applying the current value, a `WHEN CHANGED` event refreshing on every change, and an `onStarted` re-application. They are shown on the per-user logging tab of the `settings` form.

| Property                          | What it enables for that user                                              |
|-----------------------------------|----------------------------------------------------------------------------|
| `explainAnalyzeMode[User]`        | `EXPLAIN ANALYZE` logging of executed queries (`turnExplainAnalizeOnCurrentUser[]` turns it on for the current user) |
| `loggerDebugEnabled[User]`        | debug-level server logging                                                 |
| `explainTemporaryTablesEnabled[User]` | inclusion of temporary tables in query explains                        |
| `remoteLoggerDebugEnabled[User]`  | debug-level logging of remote calls                                        |
| `remoteExLogEnabled[User]`        | logging of remote-call execution                                           |
| `remotePausableLogEnabled[User]`  | logging of pausable remote calls                                           |
| `explainAppEnabled[User]`         | allocation (memory) explains                                               |
| `volatileStatsEnabled[User]`      | use of volatile statistics for that user's queries                         |

### Per-user runtime settings

These per-user properties tune query execution and client behavior. `execEnv[User]` (class `TypeExecEnv`, with objects `materialize`, `disablenestloop`, `none`) selects the query execution environment and pushes the selected `id[TypeExecEnv]` into the server through the same `set…` / `refresh…` / `WHEN CHANGED` pattern as the diagnostics flags; `nameExecEnv[User]` is its caption.

| Property                      | What it controls                                                                |
|-------------------------------|----------------------------------------------------------------------------------|
| `useBusyDialogCustom[CustomUser]` | forces the busy dialog for that user                                         |
| `useRequestTimeout[CustomUser]`   | applies the request timeout for that user                                    |
| `devMode[CustomUser]`             | development mode for that user (debugging)                                    |
| `transactTimeout[CustomUser]`     | the transaction timeout (in seconds) for that user                           |

`turnCacheStatsOff[]` turns off statistics caching for the server.

### Client logs and dumps

The module lets an administrator pull logs and a thread dump from a connected user's client, shown on the per-user logging tab of the `settings` form.

| Action                          | What it does                                                                |
|---------------------------------|------------------------------------------------------------------------------|
| `requestUserLogs[CustomUser]`   | asks each connected client of the user to upload its logs into `fileUserLogs[Connection]`; warns if the user is not connected |
| `openUserLogs[CustomUser]`      | opens the uploaded logs of the user's connections                            |
| `requestThreadDump[CustomUser]` | asks each connected client of the user to upload a thread dump into `fileThreadDump[Connection]` |
| `openThreadDump[CustomUser]`    | opens the uploaded thread dump                                               |

### Client GUI per user

The `settings` form lets per-user (and default) client GUI preferences be set. Colors are stored per `User`: `selectedRowBackgroundColor[User]`, `selectedCellBackgroundColor[User]`, `focusedCellBackgroundColor[User]`, `focusedCellBorderColor[User]`, `tableGridColor[User]`; the `override…` variants (`overrideFocusedCellBorderColor[]`, `overrideTableGridColor[]`) take the current user's value first and the application default otherwise. `resetWindowsLayout[]` resets the saved window layout.

SSL credentials for the external (HTTP) server are kept here as well, in two alternatives controlled by `useKeystore[]`:

- keystore: `keystore[]` plus `keystorePassword[]` and `keyPassword[]`, with the file triple `loadKeystore[]` / `openKeystore[]` / `resetKeystore[]`;
- PEM: `privateKey[]` and `chain[]` with their own load / open / reset triples, plus `privateKeyPassword[]`.

`computerSettings[]` collects per-`Computer` client settings (currently `textFieldPropertyEditorScannerSleep[Computer]`) as a `JSON` value for the client.

### Database scaling

A `DBServer` (abstract) is a database node with a `host[DBServer]` and an `snmpPort[DBServer]`. The concrete classes are `DBMaster` (the primary node; its `host` is the configured database server) and `DBSlave` (a replica; its `host` is `slaveHost[DBSlave]`). The `scaling` form lists the servers and their monitoring locals — `load`, `lsn`, `readyStatus`, `availability`, `lag`, `usedCpu`, `numberConnections` (all `DATA LOCAL` per `DBServer`, refreshed by `updateServersAction[]`).

`addSlave[DBSlave]` and `removeSlave[DBSlave]` register and unregister a replica on the running server; the async wrappers `asyncAddSlave` / `asyncRemoveSlave` run them in a new session and thread. Changing a slave's host re-registers it, and every slave is registered on `onStarted`.

### Server-settings API

Three actions export the server configuration to clients as JSON; all are declared `@@noauth` so they are reachable before sign-in. Each is an abstract `CASE` action with a default implementation that gathers the configuration and emits it with the `EXPORT JSON` operator.

| Action                          | What it exports                                                                         |
|---------------------------------|------------------------------------------------------------------------------------------|
| `getServerSettings[]`           | the pre-login configuration: application name and graphics, platform / API version, registration and two-factor flags, the no-auth resources, and the relevant `lsfParams` |
| `getInitSettings[]`             | the web-client init resources loaded on client startup                                   |
| `getClientSettings[]`           | the per-user client configuration: colors, fonts, locale, picker ranges, and the many `valueSetting` / `valueSettingBoolean`-driven client options |

`resetServerSettingsCacheAction[]` drops the cached server settings; it is fired by `WHEN CHANGED` on `lsfParams` and on the application name / graphics so the next API call rebuilds them.

### Forms and navigator

| Form          | Purpose                                                                                  |
|---------------|------------------------------------------------------------------------------------------|
| `maintenance` | database service, recalculation, restart control, and VM / memory actions                |
| `settings`    | general settings, the `Setting` table, per-user diagnostics, runtime, client GUI, and scaling-independent client options |
| `scaling`     | the database servers and their monitoring values                                         |

The navigator adds `settings` (first), `maintenance` (after `performance`), and `scaling` under the system folder.

### Language

- [`INTERNAL` operator](../language/INTERNAL_operator.md) — declares the service, mode, diagnostics, settings, and scaling actions over their Java implementation classes.

### See also

- [`System modules`](System_modules.md) — the general inventory of platform modules.
- [`Process monitor`](Process_monitor.md) — the running-process view this module's diagnostics and dumps support.
- [`Journals and logs`](Journals_and_logs.md) — the server logs the per-user logging flags feed.
- [`Working parameters`](Working_parameters.md) — the runtime parameters and settings the management surface configures.
- [`Backup and restore`](Backup_restore.md) — database backup, alongside the database service actions here.
- [`Reflection`](System_Reflection.md) — the metadata module that backs the administration forms.
- [`SystemEvents`](System_SystemEvents.md) — the server-lifecycle events module, pulled in via `REQUIRE`.
- [`Authentication`](System_Authentication.md) — users and roles, against which settings and GUI preferences are stored.
