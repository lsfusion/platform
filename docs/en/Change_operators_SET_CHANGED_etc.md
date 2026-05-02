---
title: 'Change operators (SET, CHANGED, ...)'
---

*Change operators* determine whether some types of changes have occurred for a given expression in the current session. All these operators are derived from the [previous value operator (`PREV`)](Previous_value_PREV.md), however, it is recommended to use them to improve readability and performance.

In the table below, `f` stands for the expression being checked (parameters omitted) and `PREV(f)` for its value at the start of the session.

|Operator     |Value                                                            |Description                   |
|-------------|-----------------------------------------------------------------|------------------------------|
|`SET`        |`f AND NOT PREV(f)`                                              |Value is set                  |
|`DROPPED`    |`NOT f AND PREV(f)`                                              |Value is reset                |
|`CHANGED`    |`(f OR PREV(f)) AND NOT f==PREV(f)`                              |Value is changed              |
|`SETCHANGED` |`f AND NOT f==PREV(f)`<br/>or<br/>`CHANGED(f) AND NOT DROPPED(f)`|Value is changed to non-`NULL`|
|`DROPCHANGED`|`CHANGED(f) AND NOT SET(f)`                                      |Value is either reset or changed from one non-`NULL` to another non-`NULL`|
|`SETDROPPED` |`SET(f) OR DROPPED(f)`                                           |Value is either reset or changed from `NULL` to non-`NULL`|

The first three operators (`SET`, `DROPPED`, `CHANGED`) are the basic change predicates; the remaining three are convenient combinations of those that cover cases cutting across the basic predicates.

:::warning
In [event mode](Events.md#change), these operators return changes from the point of the previous occurrence of the event (or rather, from the point at which all its handlers were completed) instead of changes since the start of the session.
:::

### Language

To declare a property using change operators, the following [syntax constructs](Change_operators.md) are used. 

### Examples

```lsf
CLASS Order;
status = DATA STRING (Order);

// Most common use: react to a transition inside an event handler. The chosen change
// operator picks which transitions fire — SET only on NULL → value, DROPPED only on
// value → NULL, CHANGED on either of those plus value → another value. Inside the
// handler PREV(f) reads f's value before the change.
WHEN CHANGED(status(Order o)) DO
    MESSAGE 'Status changed: ' + PREV(status(o)) + ' → ' + status(o);
```
