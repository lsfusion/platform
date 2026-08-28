---
slug: "/Brief_sessions"
title: 'Brief: change sessions'
---

## What a session is

A [change session](../paradigm/Change_sessions.md) is where changes are accumulated locally instead of being written to the database right away. It holds changes of [data properties](../paradigm/Data_properties_DATA.md), including local ones, and changes of object classes — created and deleted objects, `CHANGECLASS`. Until the changes are applied they stay in this session; which of them another session can see is decided by the operator that opens it — `NESTEDSESSION` shows all of the upper session's changes, `NEWSESSION` reads from the database, carrying over the local properties its `NESTED` names — a list in brackets, or all of them with `NESTED LOCAL` — the class changes if `CLASSES` is given, and, listed or not, anything declared `DATA LOCAL NESTED` — together with how the properties themselves are declared, since a `DATA LOCAL NESTED` one travels on its own; an action's current session comes from the execution context — the form's session, the calling action's session, or the session supplied by the platform.

The value of a property at the start of the session is returned by the [`PREV` operator](../paradigm/Previous_value_PREV.md), and the [change operators](../paradigm/Change_operators_SET_CHANGED_etc.md) derived from it — `SET`, `DROPPED`, `CHANGED`, `SETCHANGED`, `DROPCHANGED`, `SETDROPPED` — answer what exactly changed in the session.

**Analogy**: an uncommitted database transaction that lives for as long as the user works with the form.

## NEWSESSION and NESTEDSESSION

The [`NEWSESSION` operator](../language/NEWSESSION_operator.md) runs the inner action in a separate [session](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md), isolated from the current one; the [`NESTEDSESSION` operator](../language/NESTEDSESSION_operator.md) runs it in a nested session, which copies the changes of the current session into itself and, on apply, copies them back. If either operator is executed during an [apply transaction](../paradigm/Apply_changes_APPLY.md) of the current session, no session is created at that moment — the inner action is deferred and executed in the current session inside the same transaction.

```
NEWSESSION [NEWSQL] [FORMS formId1, ..., formIdM] [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] action
NESTEDSESSION [SINGLE] action
```

The `NESTED` option lists the local properties to carry over, `NEWSQL` opens the session on a separate SQL connection. The two are mutually exclusive: written together they still parse, but `NEWSQL` wins and the whole `NESTED` clause is ignored.

A new session is reached for when the action is a unit of work of its own: a dialog, an import, file processing, a background record.

```lsf
logError (STRING message) {
    NEWSESSION NEWSQL {
        NEW e = LogEntry { text(e) <- message; }
        APPLY;
    }
}
```

## APPLY and CANCEL

The [`APPLY` operator](../language/APPLY_operator.md) [applies the changes](../paradigm/Apply_changes_APPLY.md) — writes what has been accumulated into the database, running the handlers of global [events](../paradigm/Events.md) and the checks of [constraints](../paradigm/Constraints.md) along the way. In a nested session it writes nothing to the database: the changes are copied back into the session the nested one is nested in. The [`CANCEL` operator](../language/CANCEL_operator.md) [cancels the changes](../paradigm/Cancel_changes_CANCEL.md) — clears the session, except during an apply transaction, where it cancels the running `APPLY` instead.

```
APPLY [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] [SERIALIZABLE] [action]
CANCEL [NESTED [nestedPropertySelector] [CLASSES]]
```

The outcome of an apply is read from the `System.canceled[]` and `System.applyMessage[]` properties.

**Analogy**: `COMMIT` and `ROLLBACK`.

## How changes are seen across sessions

Outside an apply transaction, what a new session sees is set by the operator that created it together with how the properties themselves are declared, between two ends: a session that inherits the upper session's uncommitted changes and one that ignores them and reads the committed state of the database. It is not a switch — `NESTED (...)` carries over the local properties it lists, and `CLASSES` carries over the class changes independently, so a session can inherit part of the state.

The operator is not the only place this is decided. A property declared `DATA LOCAL NESTED` carries over on its own, without being listed on the operator, and `MANAGESESSION` / `NOMANAGESESSION` on that declaration narrow it to `APPLY` / `CANCEL` or to `NEWSESSION` respectively ([`DATA`](../language/DATA_operator.md), [`NEWSESSION`](../language/NEWSESSION_operator.md)).

`NESTEDSESSION` is at one end — all changes of the upper session are visible. `NEWSESSION` is at the other — properties are read from the database, except those declared `DATA LOCAL NESTED`, which come along on their own — and its `NESTED` and `CLASSES` options widen that. `NEWSQL` narrows it instead: on a connection of its own nothing of the upper session comes along, declared `NESTED` or not.

Which combination gives what is described in [creating sessions](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md) and in the [`NEWSESSION` operator](../language/NEWSESSION_operator.md).
