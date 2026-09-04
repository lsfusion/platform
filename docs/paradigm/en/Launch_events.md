---
slug: "/Launch_events"
title: 'Launch events'
---

A launch event occurs when one of the platform's components starts. For each launch event the platform provides a predefined action that runs as the event's handler; a developer plugs initialization logic into this handler.

| Component          | Handler                                 | When the event occurs                                                                       |
| ------------------ | --------------------------------------- | ------------------------------------------------------------------------------------------- |
| Application server | `SystemEvents.onStarted[]`              | Once per application server start, before the server begins accepting client connections.   |
| Desktop client     | `SystemEvents.onDesktopClientStarted[]` | Once per desktop client launch, after the client has connected to the application server.   |
| Web client         | `SystemEvents.onWebClientStarted[]`     | Once per web client launch, after the client has connected to the application server.       |

The handlers are abstract actions of the sequential form declared in the system module [`SystemEvents`](System_SystemEvents.md). Application initialization logic is attached to them through [action extension](Action_extension.md). After a handler runs, the platform applies its changes to the database itself.

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
