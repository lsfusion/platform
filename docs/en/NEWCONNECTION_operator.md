---
title: 'NEWCONNECTION operator'
---

The `NEWCONNECTION` operator creates an [action](Actions.md) that executes another action while preserving [external connections](New_connection_NEWCONNECTION.md) (`SQL`, `TCP`, `DBF`) across [`EXTERNAL`](EXTERNAL_operator.md) calls.

### Syntax

```
NEWCONNECTION action
```

### Description

The `NEWCONNECTION` operator creates an action inside which every `EXTERNAL SQL`, `EXTERNAL TCP`, `EXTERNAL DBF` to the same endpoint reuses the previously opened connection instead of opening a new one on each call. An empty connection string (or an empty `host` for TCP) in a nested `EXTERNAL` resolves to the single already-open connection of that type; if there is not exactly one already-open connection of that type at this point (zero or more than one), the platform throws. Every connection opened inside the block is closed when the block exits, regardless of whether the inner action completed normally or threw.

### Parameters

- `action`

    A [context-dependent action operator](Action_operators.md#contextdependent) that defines the action to be executed.

### Examples

```lsf
test {
    NEWCONNECTION {
        EXTERNAL SQL 'jdbc:postgresql://erp/main' EXEC 'UPDATE stock SET qty = qty + 1';  // opens a connection and keeps it open
        EXTERNAL SQL 'jdbc:postgresql://erp/main' EXEC 'INSERT INTO audit (msg) VALUES (''sync'')'; // reuses the already opened connection
    }
    // all connections are closed here
}
```
