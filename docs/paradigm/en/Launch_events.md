---
slug: "/Launch_events"
title: 'Launch events'
---

A launch event occurs when one of the platform's components starts. For each launch event the platform provides a predefined action that runs as the event's handler. A developer plugs initialization logic into this handler. Likewise, a graceful application server stop runs the `onStopping[]` handler, into which shutdown logic is plugged.

| Component          | Handler                                 | When the event occurs                                                                       |
| ------------------ | --------------------------------------- | ------------------------------------------------------------------------------------------- |
| Application server | `SystemEvents.onStarted[]`              | Once per application server start, before the server begins accepting client connections.   |
| Application server | `SystemEvents.onFinallyStarted[]`       | Once per application server start, at the end of the start: after `onStarted[]` and after the stage at which a full start synchronizes the [`Reflection`](System_Reflection.md) metadata with the database. |
| Application server | `SystemEvents.onStopping[]`             | Once per graceful application server stop (SIGTERM, Ctrl+C, service stop), before RMI, the scheduler and other services are stopped. Not run if the server start failed (including in `onFinallyStarted[]`). |
| Desktop client     | `SystemEvents.onDesktopClientStarted[]` | Once per desktop client launch, after the client has connected to the application server.   |
| Web client         | `SystemEvents.onWebClientStarted[]`     | Once per web client launch, after the client has connected to the application server.       |

The handlers are abstract actions of the sequential form declared in the system module [`SystemEvents`](System_SystemEvents.md). Application initialization logic is attached to them through [action extension](Action_extension.md). After a handler runs, the platform applies its changes to the database itself.

The [`Reflection`](System_Reflection.md) objects of forms, navigator elements, properties and actions are synchronized with the source code between `onStarted[]` and `onFinallyStarted[]`: in `onStarted[]` a lookup by canonical name finds only the objects that already existed at an earlier start, so initialization that needs them — seeding the permissions of the [security policy](Security_policy.md), for example — belongs in `onFinallyStarted[]`. Under a [light start](Launch_parameters.md) the synchronization is skipped, and elements added since the last full start are not found at all.

While `onStopping[]` runs, the server still accepts client connections and requests. The handler does not run when the process terminates abnormally (`kill -9`, a JVM crash, a power loss).

### Language

Handler implementations are added with the [`ACTION+` statement](../language/ACTION_plus_statement.md).

### Examples

```lsf
lastStarted 'Last server start' = DATA DATETIME ();

onStarted() + {
    lastStarted() <- currentDateTime();
}

onWebClientStarted() + {
    MESSAGE 'Welcome!';
}
```
