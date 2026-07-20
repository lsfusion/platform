---
slug: "/Process_monitor"
title: 'Process monitor'
---

The *process monitor* is a built-in platform mechanism that shows the current state of the application server and the database server: active application server threads, active SQL queries, lock chains, and a set of actions for interrupting and forcibly terminating these processes.

### Monitor contents

The monitor is opened by the `Administration > System > Process monitor` form and consists of three tabs:

|Tab|What it shows|
|---|---|
|All processes|The full list of active server threads and active SQL queries, joined by logical process|
|Blocking processes|Processes holding locks that other processes are waiting for|
|Blocked processes|Processes waiting for locks to be released|

### Columns

Each monitor row combines two aspects of a single logical process: the server thread (Java) and the SQL query associated with it (if the process is working with the database at that moment). Some columns relate to the java thread, others to the database query.

#### Process identification

|Column|What it shows|
|---|---|
|`Process thread ID`|Identifier of the application server thread (for database processes with no java thread — `s` + the backend pid)|
|`Process ID (SQL)`|Identifier of the backend process on the database server|
|`Computer`|The computer the connection was created from|
|`User`|The user the process belongs to|
|`User address (SQL)`|Network address of the database connection client (this is the application server's address, not the end user's browser)|

#### Java thread

|Column|What it shows|
|---|---|
|`Java-thread name`|Name of the application server thread|
|`Status (Java)`|State of the java thread: `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`|
|`Process LSF stack trace`|The execution stack at the lsFusion level — the chain of actions/properties/forms running in the thread. It shows the application's logic rather than JVM frames; usually the most useful column for understanding what the process is actually doing|
|`Java-thread stack trace (Java)`|The raw JVM call stack of the thread at the moment of polling|
|`Java-thread stack trace (SQL)`|The java stack of the thread that issued the current SQL query (a snapshot attached to the database process)|
|`Thread allocated bytes`|The total amount of memory the thread has allocated over its entire lifetime (the counter only grows and does not decrease on garbage collection). This is not current usage but the thread's cumulative load on the heap|
|`Last thread allocated bytes`|The amount of memory the thread allocated during the last measurement interval — the allocation rate|
|`Process call date-time`|The moment the current call (task) started on the java thread|
|`Blocking (Java)`|The lock (monitor) the thread currently holds or is waiting for|

The `Thread allocated bytes` and `Last thread allocated bytes` columns are populated only when the `readAllocatedBytes` setting is enabled.

#### SQL query

|Column|What it shows|
|---|---|
|`Query (SQL)`|A shortened representation of the current SQL query|
|`Full query (SQL)`|The full query text; the `Copy full query into the clipboard` action copies it to the clipboard|
|`Debug Info (SQL)`|A breakdown of the parameters and context of the running query — the values bound into the SQL; helps understand what data it is operating on. Produced only for queries exceeding a threshold|
|`Start date-time (SQL)`|The moment the current query started on the database server (used for age-based highlighting)|
|`Status (SQL)`|The database session state (`active`, `idle`, `idle in transaction`, …), taken from `pg_stat_activity`|
|`Wait Event Type (SQL)`, `Wait Event (SQL)`|The category (`Lock`, `LWLock`, `IO`, `Client`, …) and the specific event the database backend is waiting on. They show exactly what the query is stuck on|
|`Status Message (SQL)`|An internal progress message for apply / event processing — a step of the form `event: 3/57 <property>`|
|`Query Timeout`|The timeout (in seconds) for the currently executing query|
|`Disabled nested loop`|The nested-loop plan is disabled for the session (`SET enable_nestloop=off`) — a planner optimization for heavy queries|

The query is highlighted with a background based on the age of the transaction it runs in: the longer the transaction holds its changes, the more aggressive the background, which visually separates short-lived operations from long or stuck ones.

#### Transaction

|Column|What it shows|
|---|---|
|`Active (SQL)`|Whether a query is currently running|
|`In transaction (SQL)`|The process is inside a transaction|
|`Transaction start date-time (SQL)`|The moment the transaction started; the longer it stays open, the more aggressive the row's background highlighting|
|`Attempt number (SQL)`|How many times the transaction has been restarted (deadlock, serialization conflict, the platform's "handled" conflicts). `0` — no retries; a value like `2(...)` — the 2nd or a later attempt is in progress|

### Locks

For each process, the monitor additionally shows the list of processes it blocks and the list of processes that block it. On the *Blocking* and *Blocked* tabs this relationship is expanded into a hierarchy: you can navigate from a lock-holding process to those waiting for it, and vice versa. The lock graph is built recursively — cyclic mutual locks (deadlock) are detected and flagged separately.

|Column|What it shows|
|---|---|
|`Blocking thread`, `Blocking thread ID`|The thread holding the lock the current process is waiting for|
|`Blocked process count`|How many processes (directly and transitively) are waiting for this process|
|`Blocking depth`|The process's depth in the lock-wait tree|
|`Deadlock`|A flag for participation in a cycle of mutual locks|

### Interruption and forced termination

Four actions are available for each process in the monitor:

|Action|What it does|When to use|
|---|---|---|
|Cancel Java-process|Sends the thread a cancellation request through the standard interruption mechanism; the process finishes its current operation and exits at the next interruption-flag check|A regular cancellation of a long or stuck user action|
|Kill Java-process|Forced termination of the thread|A last resort: the process does not respond to cancellation|
|Cancel SQL-process|Sends the database server a request to cancel the running query|A long or stuck query after which the process should continue normally|
|Kill SQL-process|Forced termination of the database process|A stuck database process interfering with others|

"Kill" is a last-resort operation: after it, the state of the application server and the database server may require a restart.

### Filter by process type

The monitor shows processes through one of four filters: all processes, all active processes, only active SQL queries, or only active application server threads. The "active" filters screen out processes idling outside a transaction or query and leave only those that actually load the server at the moment of polling. By default the monitor opens on active SQL queries. Idle system threads — the event thread, network I/O, JVM service threads — are filtered out of the list as uninformative.
