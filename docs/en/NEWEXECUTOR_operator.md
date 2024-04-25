---
title: 'NEWEXECUTOR operator'
---

The `NEWEXECUTOR` operator is the creation of an [action](Actions.md) that allows the execution of other actions in a [new thread pool](New_threads_NEWTHREAD_NEWEXECUTOR.md).

### Syntax

```
NEWEXECUTOR action THREADS threadExpr [syncType]
```

### Description

The `NEWEXECUTOR` operator creates an action that creates a new thread pool and executes the defined action in such a way that any action created with the [`NEWTHREAD` operator](NEWTHREAD_operator.md) will be executed in one of the threads of the created pool. 

### Parameters

- `action`

    A [context-dependent action operator](Action_operators.md#contextdependent) that defines an action to be executed.

- `threadExpr`

    An [expression](Expression.md) which value determines the number of threads in the pool. Must return the value of the `INTEGER` class. 

- `syncType`

    Synchronisation type. Specifies when the execution of `NEWEXECUTOR` action completes, allowing you to choose between synchronous and asynchronous approaches. Specified by one of the keywords:

    - `WAIT` - after all threads have completed execution. This value is used by default.
    - `NOWAIT` - immediately after all threads have been started.

### Examples

```lsf
testExecutor  {
    NEWEXECUTOR {
        FOR id(Sku s) DO {
            NEWTHREAD {
                NEWSESSION {
                    name(s) <- STRING[20](id(s)); // writing the code into the name in 10 threads
                    APPLY;
                }
            }
        }
    } THREADS 10;
}
```
