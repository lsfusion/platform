---
title: 'NEWEXECUTOR operator'
---

The `NEWEXECUTOR` operator creates an [action](Actions.md) that runs other actions in a [new execution service](New_threads_NEWTHREAD_NEWEXECUTOR.md) — a server-side thread pool or a client-side dispatcher.

### Syntax

```
NEWEXECUTOR action THREADS threadExpr [syncType]
NEWEXECUTOR action CLIENT connectionExpr [syncType]
```

where `syncType` is one of:

```
WAIT [timeoutExpr]
NOWAIT
```

### Description

The `NEWEXECUTOR` operator creates an action inside which every [`NEWTHREAD`](NEWTHREAD_operator.md) is dispatched by the execution service this operator establishes. In `THREADS` mode the service is a server-side thread pool of the given size: each nested thread's body shares the calling code's [change session](Change_sessions.md), and `NEWTHREAD ... SCHEDULE` uses the server-side scheduler. In `CLIENT` mode the service is a client-side dispatcher tied to the given connection: each nested thread's action is delivered to that connection and executed on the application server in its own fresh change session at the connection's navigator level, not bound to any opened form; interactive operators inside the thread target that connection's UI, and `NEWTHREAD ... SCHEDULE` uses the client-side timer.

`syncType` controls whether the operator waits for nested threads to complete. The default is `WAIT` without a timeout: the operator does not return until every nested thread for which the service registers a future has completed, after which the values written by [`NEWTHREAD ... TO p`](NEWTHREAD_operator.md) are applied to the current [session](New_session_NEWSESSION_NESTEDSESSION.md). [`NEWTHREAD ... CLIENT p`](NEWTHREAD_operator.md) without `TO` does not register a future and is not part of the wait. If a wait timeout is given and some threads do not fit within it, the operator throws; values from threads that completed earlier are still applied and are visible in an enclosing [`TRY ... CATCH`](TRY_operator.md).

### Parameters

- `action`

    A [context-dependent action operator](Action_operators.md#contextdependent) that defines the action to be executed.

- `threadExpr`

    An [expression](Expression.md) whose value is the server-side pool size. The type is `INTEGER`. If the value is `NULL` or 0, the pool size defaults to the number of available server processors.

- `connectionExpr`

    An expression whose value is the [connection](User_IS_interaction.md) whose interactive context is used to dispatch the nested `NEWTHREAD`s. The return class is `SystemEvents.Connection`. If the value is `NULL` (or does not resolve to a `SystemEvents.Connection` object), the operator exits without executing the inner action and without raising an error.

- `syncType`

    Synchronization type. One of the following keywords:

    - `WAIT` — synchronous execution: the operator waits for every nested `NEWTHREAD` for which a future is registered to complete and applies their `TO` results. May be followed by a timeout expression (see `timeoutExpr`).
    - `NOWAIT` — asynchronous execution: the operator returns as soon as all nested `NEWTHREAD`s are dispatched. Incompatible with [`NEWTHREAD ... TO`](NEWTHREAD_operator.md), which requires `WAIT`. `NEWTHREAD ... SCHEDULE PERIOD ...` requires this form because a periodic thread never completes.

    Without `syncType` the operator behaves as `WAIT` without a timeout.

- `timeoutExpr`

    An expression whose value is the wait timeout in milliseconds. The type is `INTEGER` or `LONG`, and the value must be strictly positive. The timeout is applied per wait step and is not a strict overall deadline for all threads — when several threads are awaited concurrently, the total wait time may exceed the configured value.

### Examples

```lsf
testExecutor {
    // Server-side pool of 10 threads, wait for completion
    NEWEXECUTOR {
        FOR id(Sku s) DO {
            NEWTHREAD {
                NEWSESSION {
                    name(s) <- STRING[20](id(s));
                    APPLY;
                }
            }
        }
    } THREADS 10 WAIT;

    // Fire-and-forget on the client side
    NEWEXECUTOR {
        NEWTHREAD MESSAGE 'Hello from client';
    } CLIENT currentConnection() NOWAIT;

    // Collect results from threads with a wait timeout
    LOCAL a = INTEGER ();
    LOCAL b = INTEGER ();
    TRY {
        NEWEXECUTOR {
            NEWTHREAD { RETURN computeFast(); } TO a;
            NEWTHREAD { RETURN computeSlow(); } TO b;
        } THREADS 2 WAIT 5000;
    } CATCH {
        // a / b may be partially populated if some threads missed the timeout
        MESSAGE 'timed out: ' + messageCaughtException();
    }
}
```
