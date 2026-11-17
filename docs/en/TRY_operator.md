---
title: 'TRY operator'
---

The `TRY` operator creates an [action](Actions.md) that executes another action with [exception handling](Exception_handling_TRY.md).

### Syntax

```
TRY action [CATCH catchAction] [FINALLY finallyAction]
```

### Description

The `TRY` operator creates an action that executes another action and handles exceptions within it. 

An operator form without the `FINALLY` keyword creates an action that executes another action and intercepts errors that are thrown in it. In this case, the error is not passed anywhere.

An operator form with the `FINALLY` keyword creates an action that executes another action, intercepts errors that occur, then, regardless of whether an error was thrown or not, executes the action specified in the `FINALLY` block, and then, if an error was thrown and there was no `CATCH` block, passes this error to the top action in the stack.

An interruption of execution — when a [scheduler](Scheduler.md) task runs out of time, or when a process is canceled in the [process monitor](Process_monitor.md) — is not treated as an error: the `CATCH` block is not executed for it, the `FINALLY` block is, and the interruption is passed to the top action in the stack even if there is a `CATCH` block or no `FINALLY` block. This way an interruption also stops a loop that contains `TRY`.

### Parameters

- `action`

    A [context-dependent operator](Action_operators.md#contextdependent) that describes an action to be executed with exception handling.

- `catchAction`

    A context-dependent operator that describes an action to be executed if an error is thrown while executing the action. Here the error message will be written to the property `System.messageCaughtException[]`, the java error stack will be written to `System.javaStackTraceCaughtException[]`, and the LSF stack will be written to `System.lsfStackTraceCaughtException[]`.

- `finallyAction`

    A context-dependent operator that describes an action to be executed after the action being executed, regardless of whether or not an error has been thrown.

### Examples

```lsf
tryToImport(FILE f)  {
    TRY {
        LOCAL a = BPSTRING[10] (INTEGER);

        IMPORT XLS FROM f TO a = A;
    }
}

CLASS MyLock {
    lock 'Blocking'
}

singleDo ()  {
    NEWSESSION {
        lock(MyLock.lock);
        IF lockResult() THEN
        TRY {
            MESSAGE 'Lock Obtained';
        } CATCH {
            MESSAGE messageCaughtException();
        } FINALLY unlock(MyLock.lock);
    }
}
```
