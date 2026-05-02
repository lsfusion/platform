---
title: 'Previous value (PREV)'
---

The *previous value* operator creates a [property](Properties.md) that returns the value of the given expression at the beginning of the [change session](Change_sessions.md) (that is, the current value in the database, ignoring any changes that have been made in the session). Together with the [change operators](Change_operators_SET_CHANGED_etc.md) — which are derived from it — this operator is what makes session-local change tracking possible: it gives access to the "before" state that the current session is being compared against.

:::info
This operator always uses the start-of-session scope. For how that interacts with [event handling](Events.md#change), see the canonical event-mode section.
:::

### Language

To declare a property that returns a previous value, use the [`PREV` operator](PREV_operator.md). 

### Examples

```lsf
CLASS Order;
sum = DATA NUMERIC[10,2] (Order);

// Most common use: read the "before" value inside an event handler reacting to a
// change. PREV(sum(o)) returns the value at the start of the session — the database
// value, ignoring any in-session change. Change predicates (CHANGED, SET, DROPPED,
// ...) are derived from PREV as Boolean comparisons against the current value; PREV
// itself gives access to the actual previous value.
WHEN CHANGED(sum(Order o)) DO
    MESSAGE 'Sum changed: ' + PREV(sum(o)) + ' → ' + sum(o);
```
