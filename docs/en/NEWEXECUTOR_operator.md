---
title: 'NEWEXECUTOR operator'
---

The `NEWEXECUTOR` operator creates an [action](Actions.md) that enables executing other actions in a [new thread pool](New_threads_NEWTHREAD_NEWEXECUTOR.md).

### Syntax

    NEWEXECUTOR action THREADS threadExpr

### Description

The `NEWEXECUTOR` operator creates an action that creates a new thread pool and executes the defined action in such a way that any action created with the [`NEWTHREAD` operator](NEWTHREAD_operator.md) will be executed in one of the threads of the created pool. 

### Parameters

- `action`

    A [context-dependent action operator](Action_operators.md#contextdependent) that defines an action to be executed.

- `threadExpr`

    An [expression](Expression.md) which value determines the number of threads in the pool. Must return the value of the `INTEGER` class. 

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
