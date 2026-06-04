---
slug: "/Process_monitor"
title: 'Process monitor'
---

The *process monitor* is a built-in platform mechanism that shows the current state of the application server and the database server: active application-server threads, active SQL queries, lock chains, and a set of actions for interrupting and forcibly terminating these processes.

### Monitor contents

The monitor is opened by the `Administration > System > Process monitor` form and has three tabs:

|Tab|What it shows|
|---|---|
|All processes|Full list of active application-server threads and active SQL queries, joined by logical process|
|Blocking processes|Processes holding locks that other processes are waiting on|
|Blocked processes|Processes waiting for locks to be released|

Each entry combines two aspects of a single logical process: the application-server thread (Java) and the SQL query associated with it (if the process is talking to the database at that moment).

### Application-server threads

For each active application-server thread the monitor shows: the thread id and name, its status (`RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`), the amount of memory the thread has allocated, information about the lock it holds, and the call stack (the Java stack at the moment of the snapshot). Idle system threads — the event loop, network I/O, JVM service threads — are filtered out as uninformative.

### SQL queries

For each active SQL query the monitor shows: the database process id, the user name, the client computer name, the transaction state (active, waiting, idle), the full query text and its abbreviated representation, the wait events, and the SQL call stack.

A query is highlighted by a background color that reflects the age of the transaction it runs in: the longer the transaction holds changes, the more aggressive the color, so that short operations are visually separated from long or hung ones.

### Locks

For each process the monitor additionally shows the list of processes it blocks and the list of processes that block it. On the *Blocking* and *Blocked* tabs this relation is expanded into a hierarchy: one can walk from a lock holder to the processes waiting on it, and back. The lock graph is built recursively — cyclic mutual locks (deadlocks) are detected and marked separately.

### Interruption and forced termination

For each process the monitor exposes four actions:

|Action|What it does|When to use|
|---|---|---|
|Cancel Java process|Sends the thread an interrupt request through the standard interruption mechanism; the process finishes the current operation and exits at the next interrupt-flag check|Routine cancellation of a long or hung user action|
|Kill Java process|Forcibly terminates the thread|Last resort: the process does not respond to cancellation|
|Cancel SQL process|Sends the database server a request to cancel the running query|A long or hung query after which the process should continue normally|
|Kill SQL process|Forcibly terminates the database process|A hung database process that is blocking others|

«Kill» is a last-resort operation: after it the application-server and database state may require a restart.

### Process-type filter

The monitor lists processes through one of four filters: all processes, all active processes, only active SQL queries, or only active application-server threads. The «active» filters drop processes idling outside a transaction or query, leaving only the ones that actually consume server resources at the snapshot moment. By default the monitor opens on active SQL queries.
