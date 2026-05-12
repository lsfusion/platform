---
title: 'New session (NEWSESSION, NESTEDSESSION)'
---

The new [session](Change_sessions.md) operator allows you to execute an action in a session different from the current one. 

As with other session management operators, you can explicitly specify [nested local properties](Session_management.md#nested) when creating a new session — this lets you list which local properties of the current session are migrated into the new one. When creating a [nested session](#nested) this is not needed — it copies the entire current session into the nested one anyway.

### Nested sessions {#nested}

It is also possible to create a new *nested* session. In this case, all changes that occurred in the current session are copied to the nested session (the same happens when [changes are discarded](Cancel_changes_CANCEL.md) in a nested session). At the same time, when you [apply changes](Apply_changes_APPLY.md) in the nested session, all changes are copied back to the current session (without being saved to the database). 

### New SQL connection {#newsql}

By default a new session uses the same SQL connection as the current one. When needed, a new session can be opened on a separate SQL connection — independent of the current session's connection. Actions inside it (`APPLY`, reads) can then proceed independently of the current session's transaction. This is useful when:

- an action dispatched from an event handler or a [thread](New_threads_NEWTHREAD_NEWEXECUTOR.md) must [apply](Apply_changes_APPLY.md) its changes independently of whether the current session is later applied or canceled;
- the latest committed database data must be read in parallel with a long-running transaction of the current session — on a separate connection the reader sees the committed state rather than the snapshot of the open transaction.

On a separate SQL connection, the new session inherits neither local properties nor class changes from the current one.

This option takes effect when the operator runs outside an already-running apply transaction of the current session: inside an `APPLY` transaction the platform falls back to recursive apply and does not open a separate SQL connection.

### Language

To create an action that executes another action in a new session, use the [`NEWSESSION` operator](NEWSESSION_operator.md) (for nested sessions, use the [`NESTEDSESSION` operator](NESTEDSESSION_operator.md)). Opening a new session on a separate SQL connection is enabled by the `NEWSQL` option of the [`NEWSESSION` operator](NEWSESSION_operator.md).

### Examples

```lsf
// NEWSESSION runs an action in a fresh session — outer-session changes are not visible inside
isolatedRun (Currency c)  {
    name(c) <- 'pending'; // change in the outer session
    NEWSESSION {
        // here name(c) returns the database value, not 'pending'
        NEW c2 = Currency;
        APPLY; // commits only the new Currency; name(c) stays pending in the outer session
    }
}

// NESTEDSESSION inherits the outer session — applying changes inside copies them back to the outer one
inheritedEdit (Sku s)  {
    name(s) <- 'temp'; // change in the outer session
    NESTEDSESSION {
        // sees name(s) == 'temp' from the outer session
        name(s) <- 'final';
        APPLY; // copies the change back to the outer session, not to the database
    }
}

// NEWSESSION NEWSQL opens a separate SQL connection, so the inner APPLY runs in its own transaction
backgroundLog (STRING msg)  {
    NEWSESSION NEWSQL {
        NEW e = LogEntry {
            text(e) <- msg;
            time(e) <- currentDateTime();
        }
        APPLY; // committed independently of the current session
    }
}
```
