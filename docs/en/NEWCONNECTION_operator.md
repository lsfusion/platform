---
title: 'NEWCONNECTION operator'
---

The `NEWCONNECTION` operator creates an [action](Actions.md) that executes another action with preserving SQL, TCP, DBF connections.

### Syntax

```
NEWCONNECTION action
```

### Description

The `NEWCONNECTION` operator creates an action that preserves SQL, TCP, and DBF connections, allowing the execution of `EXTERNAL SQL`, `EXTERNAL TCP`, and `EXTERNAL DBF` without the need to establish a new connection each time. All connections are closed at the end of the NEWCONNECTION operator's execution.

### Parameters

- `action`

    A [context dependent operator](Action_operators.md#contextdependent) that defines an action to be executed.

### Examples

```lsf
test {
    NEWCONNECTION {
        EXTERNAL SQL 'jdbc:postgresql://connection/string' EXEC 'first query'; // The first EXTERNAL creates a connection and does not close it        
        EXTERNAL SQL 'jdbc:postgresql://connection/string' EXEC 'second query'; // The second EXTERNAL uses the already established connection
    }
    // All created connections are closed at the end of NEWCONNECTION execution, regardless of whether there were errors during the execution
}
```
