---
slug: "/Brief_modularity"
title: 'Brief: modules'
---

## Modules and order

A *module* is a functionally complete part of a project: declarations of classes, properties, actions, forms, events, constraints ([modules](../paradigm/Modules.md)). One module is one `.lsf` file starting with a [module header](../language/Module_header.md): `MODULE`, `REQUIRE`, `PRIORITY`, `NAMESPACE`.

`REQUIRE` lists the modules the current one [depends](../paradigm/Modules.md#depends) on. The dependency is transitive, cycles are not allowed, and the initialization order is built from it: a module is initialized after all of its dependencies. Every module depends on the `System` module. The dependency also governs visibility: an element can only be found by name in a module that depends on it, so extending someone else's functionality is what the [extension](../paradigm/Extensions.md) technique is for, see [Brief: extensions](Brief_extensions.md).

A [project](../paradigm/Projects.md) is the set of modules and the accompanying files; by default every `.lsf` file on the application server's classpath is taken to be a module, and the `logics.includePaths`, `logics.topModule` and `logics.orderDependencies` launch parameters narrow that set and override the order.

**Analogy**: a package or an assembly.

```lsf
MODULE Sale;
REQUIRE System, Utils, Item;
NAMESPACE Sale;
```

## System modules

*System modules* are shipped with the platform — the standard library, which a project pulls in with `REQUIRE` and does not redefine ([system modules](../paradigm/System_modules.md)). The platform loads twelve of them itself — `System`, `Service`, `Reflection`, `Authentication`, `Security`, `SystemEvents`, `Email`, `Icon`, `Scheduler`, `Time`, `Utils`, `UserEvents` — but being loaded is not being depended on: only `System` is an implicit dependency of every module, and a declaration from any of the others needs a `REQUIRE`.

| Module | What for |
| --- | --- |
| [`System`](../paradigm/System_System.md) | root types, base classes, infrastructure |
| [`Utils`](../paradigm/System_Utils.md) | general-purpose helper properties and actions |
| [`Time`](../paradigm/System_Time.md) | date and time properties and operations |
| [`Authentication`](../paradigm/System_Authentication.md) | users, contacts, sign-in |
| [`Security`](../paradigm/System_Security.md) | roles and access policies |
| [`Service`](../paradigm/System_Service.md) | service actions and server settings |
| [`SystemEvents`](../paradigm/System_SystemEvents.md) | server-lifecycle events |
| [`UserEvents`](../paradigm/System_UserEvents.md) | programmatic access to a form's filters and orders |
| [`Reflection`](../paradigm/System_Reflection.md) | metadata about the navigator, forms, properties, tables |
| [`Scheduler`](../paradigm/Scheduler.md) | scheduled actions |
| [`Email`](../paradigm/System_Email.md) | sending and receiving email |
| [`Icon`](../paradigm/System_Icon.md) | UI icon catalogue |

Auxiliary modules: `Backup`, `Chat`, [`Eval`](../paradigm/Eval_EVAL.md), `Excel`, `Document` / `Word`, `Image` / `OpenCV`, `I18n`, `Integration`, `MasterData`, [`Numerator`](../paradigm/Utils_Numerator.md), `Hierarchy`, `Historizable`, `Geo`, `Printer` / `QZTray` / `Sound` / `Com`, `ProcessMonitor` / `Profiler`, `RabbitMQ` / `WebSocket`, `Messenger` with its `Telegram` / `Slack` / `Viber` / `Whatsapp` / `Skype`, `SQLUtils`, `DefaultData`, `Schedule`.

All of this is ordinary `.lsf` declarations, not language primitives: `lpad`, `currentDate`, `currentUser` are properties, and they should be searched for in the `paradigm` branch.
