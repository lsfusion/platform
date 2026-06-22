---
slug: "/System_modules"
title: 'System modules'
---

A *system module* is a [module](Modules.md) shipped with the platform. An application project pulls these modules in via `REQUIRE` and uses their declarations (classes, properties, actions, forms) as a ready-made standard library; the platform never expects them to be redefined.

System modules live under `server/src/main/lsfusion/` in the platform repository: core modules under the `system/` subfolder, additional utility modules under `utils/`. The `System` module is pulled into every project build automatically; the rest require an explicit `REQUIRE`.

### Inventory

#### Core (`system/`)

| Module          | Purpose |
|-----------------|---------|
| [`System`](System_System.md) | Root types, base classes, infrastructure. Pulled in automatically by every module |
| [`Time`](System_Time.md) | Date and time properties and operations |
| [`Authentication`](System_Authentication.md) | Users, contacts, sign-in, locale, passwords |
| [`Security`](System_Security.md) | Roles and access policies |
| [`Service`](System_Service.md) | Service actions, database monitoring, aggregation recalculation, settings |
| [`SystemEvents`](System_SystemEvents.md) | Server-lifecycle events, connections, exceptions, appearance |
| [`UserEvents`](System_UserEvents.md) | Programmatic access to a form's filters and orders |
| [`Reflection`](System_Reflection.md) | Metadata about the navigator, forms, properties, tables |
| [`Scheduler`](Scheduler.md) | Scheduled actions |
| [`Email`](System_Email.md) | Email accounts, sending, and receiving |
| [`Icon`](System_Icon.md) | UI icon catalogue and search |
| [`Utils`](System_Utils.md) | A collection of helper properties and actions: file system, encoding, strings, numbers, JSON, full-text search, and so on |

#### Auxiliary (`utils/`)

| Module            | Purpose                                                                    |
|-------------------|----------------------------------------------------------------------------|
| `Backup`          | Database backup and restore (see [Backup and restore](Backup_restore.md))  |
| `Chat`            | In-application chat (see [Chat](Chat.md))                                  |
| `Eval`            | Runtime execution of lsFusion code (see [`EVAL`](Eval_EVAL.md))            |
| `Excel`           | XLS / XLSX file handling                                                   |
| `Document` / `Word` | Printable documents and templates                                        |
| `Image` / `OpenCV`| Image processing                                                           |
| `I18n`            | Machine translation of strings                                             |
| `Integration`     | Generic integration actions                                                |
| `MasterData`      | Base templates for master-data classes                                     |
| `Numerator`       | Generic number generators                                                  |
| `Hierarchy`       | Hierarchical properties                                                    |
| `Historizable`    | Change-history storage                                                     |
| `Geo`             | Geographic data and operations                                             |
| `Printer` / `QZTray` / `Sound` / `Com` | Access to client-side hardware                                |
| `ProcessMonitor` / `Profiler` | Server management and monitoring                               |
| `RabbitMQ` / `WebSocket` / `messengers` | External transports                                       |
| `SQLUtils`        | Database service operations                                                |
| `DefaultData`     | Initial demo data                                                          |
| `Schedule`        | Calendars and working intervals                                            |

The full list follows the contents of `server/src/main/lsfusion/{system,utils}/` in the platform repository. Modules with a dedicated article link there.

### Language

- [Module header](../language/Module_header.md) — the `MODULE` / `REQUIRE` syntax used to pull in system modules.
