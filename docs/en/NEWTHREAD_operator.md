---
title: 'NEWTHREAD operator'
---

The `NEWTHREAD` operator creates an [action](Actions.md) that executes another action in a [new thread](New_threads_NEWTHREAD_NEWEXECUTOR.md).

### Syntax

```
NEWTHREAD action
NEWTHREAD action SCHEDULE [PERIOD periodExpr] [DELAY delayExpr] [TO toPropertyId]
NEWTHREAD action CLIENT notificationPropertyId [TO toPropertyId]
NEWTHREAD action TO toPropertyId
```

### Description

The `NEWTHREAD` operator creates an action that executes another action in a new thread. When the operator is enclosed in a [`NEWEXECUTOR`](NEWEXECUTOR_operator.md) scope, the thread is taken from the execution service that scope establishes (a server-side pool or a client-side dispatcher); otherwise a free thread is created on the application server.

`SCHEDULE` runs the action with a delay and/or with periodic repetition. Inside `NEWEXECUTOR ... CLIENT` delayed and periodic dispatches are served by the client's timer; inside `NEWEXECUTOR ... THREADS` they are served by the server-side pool's scheduler. Without `PERIOD` the action fires once; with `PERIOD` it re-fires every period — a periodic thread cannot be joined to a waiting `NEWEXECUTOR ... WAIT` because it never completes.

`CLIENT` registers the action as a client notification and writes its id into `notificationPropertyId`. The form does not by itself deliver the notification to any connection — delivery is left to the calling code, which uses the recorded id to target the dispatch. An enclosing `NEWEXECUTOR ... WAIT` does not wait for such a bare `CLIENT p` — no future is registered for it. With `CLIENT p TO q`, however, the thread is awaited until the notification actually fires (when the calling code delivers the id) or until it is evicted by the retention timeout. For automatic delivery of an action on the client side, use a plain `NEWTHREAD action` inside `NEWEXECUTOR ... CLIENT conn` without the `CLIENT` clause.

`TO` stores the value returned by the inner action via `RETURN` into the given property. The inner action must declare a return value (through [`RETURN`](RETURN_operator.md)) and be written in block form — `NEWTHREAD { ... RETURN ...; } TO p` or with a wrapper such as `NEWTHREAD NEWSESSION { ... RETURN ...; } TO p`. A direct call `NEWTHREAD a(args) TO p` is parsed as an inner-action [`EXEC a(args) TO p`](Call_EXEC.md) and does not invoke this `NEWTHREAD ... TO` form. The form is allowed only inside `NEWEXECUTOR ... WAIT`: the write happens when the thread completes, and the stored values are then applied to the current session when `NEWEXECUTOR` exits. Combining `PERIOD` with `TO` is not allowed, because a periodic thread never completes.

The [change session](Change_sessions.md) in which the thread body runs depends on where it is dispatched, and this is the key difference between the two modes.

- Under **server-side dispatch** (a free thread or `NEWEXECUTOR ... THREADS`) the thread body runs in the same session as the calling code. Change sessions are not thread-safe — multiple threads sharing such a session's data concurrently will race, so the body is typically wrapped in [`NEWSESSION`](NEWSESSION_operator.md).
- Under **client-side dispatch** (`NEWEXECUTOR ... CLIENT`, as well as the actual firing of a notification registered through `CLIENT p`) the thread body runs on the application server, but not in the caller's session — it runs in its own fresh change session scoped to the client connection's navigator, not bound to any opened form. The action may use interactive operators (`MESSAGE`, [opening a form](Open_form.md), etc.) — they target the UI of that connection; a form opened by such an action gets its own session through the usual rules. Wrapping in `NEWSESSION` is not needed.

### Parameters

- `action`

    A [context-dependent action operator](Action_operators.md#contextdependent) that defines the action to be executed in the new thread.

- `periodExpr`

    An [expression](Expression.md) whose value is the action repetition period in milliseconds. The type is `INTEGER` or `LONG`, and the value must be strictly positive.

- `delayExpr`

    An expression whose value is the delay before the first execution in milliseconds. The type is `INTEGER` or `LONG`, and the value must not be negative. Without `DELAY` the first fire is immediate for a one-shot dispatch and for a server-side periodic dispatch; for a client-side periodic dispatch (`NEWTHREAD ... SCHEDULE PERIOD ...` inside `NEWEXECUTOR ... CLIENT`) the first fire without `DELAY` happens after `period`, and subsequent fires every `period`.

- `notificationPropertyId`

    [Id](IDs.md#propertyid) of a parameterless `INTEGER` property that receives the id of the registered client notification.

- `toPropertyId`

    [Id](IDs.md#propertyid) of a property that receives the value returned by the inner action via `RETURN`. The property's arity must match the return arity; if it exceeds it by one and the first parameter is of type `INTEGER`, that parameter receives the thread's position in the enclosing `NEWEXECUTOR`.

### Examples

```lsf
testNewThread () {
    // Server-side pool, wait for completion, capture the return value
    LOCAL r = INTEGER ();
    NEWEXECUTOR {
        NEWTHREAD { RETURN 42; } TO r;
    } THREADS 1 WAIT;
    MESSAGE 'r=' + r();   // r=42

    // Delayed one-shot dispatch on the server with result capture
    LOCAL r2 = INTEGER ();
    NEWEXECUTOR {
        NEWTHREAD { RETURN 99; } SCHEDULE DELAY 2000 TO r2;
    } THREADS 1 WAIT;
    MESSAGE 'r2=' + r2();   // r2=99 after about 2 seconds

    // Periodic dispatch on the client side
    NEWEXECUTOR {
        NEWTHREAD MESSAGE 'tick'; SCHEDULE PERIOD 10000 DELAY 5000;
    } CLIENT currentConnection() NOWAIT;

    // Register a client notification and save its id into notifId
    LOCAL notifId = INTEGER ();
    NEWTHREAD { MESSAGE 'on demand'; } CLIENT notifId;
    // notifId() now holds the id of the registered notification;
    // the calling code can later trigger its delivery to a client using this id
}
```
