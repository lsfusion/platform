---
title: 'New threads (NEWTHREAD, NEWEXECUTOR)'
---

The *new thread* operator allows you to execute an action in a thread other than the current one.

By default, an action is executed once in a new thread, immediately after the creation of this thread. However, if necessary, the action can be executed with a defined delay and/or performed continuously at a specified time interval.

If needed, a thread can return a value; it is written into a designated property after the thread completes, provided the enclosing pool operator waits for completion (see below).

The thread-creation operator itself does not open a new [change session](Change_sessions.md) or a separate SQL connection. The environment in which the body actually runs depends on the execution service (see below).

### Where threads execute

Beyond the thread itself, it is useful to specify where it runs — which pool or which user it belongs to, and whether to wait for it. The platform splits this into a separate operator — the *new execution service* operator: one operator defines the thread itself (which action, with what delay and period, where to write the return value), the other defines the service that executes it (on the application server or on a user's client connection; synchronously or asynchronously). The kind of service determines the execution location, paralleling the split used in [external-system calls](Access_to_an_external_system_EXTERNAL.md) and [internal calls](Access_to_an_internal_system_INTERNAL_FORMULA.md).

#### Server-side execution

A fixed-size server-side thread pool — thread actions run on the application server in its worker threads and share the change session with the calling code. Since change sessions are not thread-safe, the thread body is typically wrapped in a [new session](New_session_NEWSESSION_NESTEDSESSION.md); if a separate database transaction is required, a [new session on a separate SQL connection](New_session_NEWSESSION_NESTEDSESSION.md#newsql) is used. The pool size is given by an integer-typed expression. Delayed and periodic dispatches are served by the server-side pool scheduler.

#### Client-side execution

A client-side dispatcher tied to a user's [connection](User_IS_interaction.md) — thread actions are delivered to that connection and executed on the application server in their own fresh change session at the connection's navigator level, not bound to any opened form. The caller's session is not inherited, so wrapping the body in a new session is not needed. This mode lets the inner action use [user-interaction](User_IS_interaction.md) operators, including [opening forms](Open_form.md) in [interactive view](In_an_interactive_view_SHOW_DIALOG.md) — they target the UI of that connection; a form opened by such a thread gets its own session through the usual rules. Delayed and periodic dispatches are scheduled by the client-side timer in the browser or the desktop client.

#### Synchronization

Regardless of the execution location, the service operator supports two synchronization modes. In the synchronous mode it waits for the nested threads for which a future is registered to complete and writes their return values into properties of the current [session](New_session_NEWSESSION_NESTEDSESSION.md); in the asynchronous mode it returns as soon as all threads are dispatched. The synchronous mode may take a wait timeout; if some threads do not fit within it, the operator throws, but values written by threads that completed earlier are still applied and visible in an enclosing [`TRY ... CATCH`](TRY_operator.md).

### Language

To declare an action that executes another action in a new thread, use the [`NEWTHREAD` operator](NEWTHREAD_operator.md). To create an execution service and group nested `NEWTHREAD`s into it, use the [`NEWEXECUTOR` operator](NEWEXECUTOR_operator.md).

### Examples

```lsf
testNewThread () {
    // Server-side pool with wait for completion
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

    // Show a message to every other connected user
    FOR user(Connection conn) AND connectionStatus(conn) == ConnectionStatus.connectedConnection AND conn != currentConnection() DO {
        NEWEXECUTOR { NEWTHREAD MESSAGE 'Message'; } CLIENT conn NOWAIT;
    }

    // Periodic dispatch on the client side — runs while the connection stays alive
    NEWEXECUTOR {
        NEWTHREAD MESSAGE 'tick'; SCHEDULE PERIOD 10000 DELAY 5000;
    } CLIENT currentConnection() NOWAIT;

    // Collect a thread result with a wait timeout
    LOCAL res = INTEGER ();
    TRY {
        NEWEXECUTOR {
            NEWTHREAD { RETURN computeHeavy(); } TO res;
        } THREADS 1 WAIT 5000;
    } CATCH {
        MESSAGE 'did not complete within timeout: ' + messageCaughtException();
    }
}
```
