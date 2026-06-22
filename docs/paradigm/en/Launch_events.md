---
slug: "/Launch_events"
title: 'Launch events'
---

A launch event occurs when one of the platform's components starts. For each launch event the platform provides a predefined action that runs as the event's handler; a developer plugs initialization logic into this handler.

| Component          | Handler                               | When the event occurs                                                                       |
| ------------------ | ------------------------------------- | ------------------------------------------------------------------------------------------- |
| Application server | `SystemEvents.onStarted`              | Once per application server start, before the server begins accepting client connections.   |
| Desktop client     | `SystemEvents.onDesktopClientStarted` | Once per desktop client launch, after the client has connected to the application server.   |
| Web client         | `SystemEvents.onWebClientStarted`     | Once per web client launch, after the client has connected to the application server.       |
