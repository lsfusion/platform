---
slug: "/Exception_handling_TRY"
title: 'Exception handling (TRY)'
---

The *exception handling* operator creates an [action](Actions.md) that runs another action with exception handling.

The operator has up to three parts:

-   the *main* action — always executed;
-   the *recovery* action — executed only if the main action raises an error; the error is consumed and execution continues normally after the operator;
-   the *cleanup* action — executed after the main action (and the recovery action, if both are specified) regardless of whether an error occurred.

If the main action raises an error and no recovery action is specified: when no cleanup action is specified either, the error is consumed silently; when a cleanup action is specified, the cleanup action runs and the error is then re-raised to the surrounding action.

Inside the recovery action, the error that triggered it is available through the system properties `messageCaughtException[]`, `javaStackTraceCaughtException[]`, and `lsfStackTraceCaughtException[]` — the error message, the Java stack trace, and the lsFusion stack trace, respectively.

### Language

To declare an exception handling action, use the [`TRY` operator](../language/TRY_operator.md).

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
