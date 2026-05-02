---
title: 'Change operators'
---

Change operators - a set of operators that determine various types of [property value changes](Change_operators_SET_CHANGED_etc.md). 

### Syntax

```
typeChange(propExpr)
```

### Description

Change operators create [properties](Properties.md) which determine whether a given type of change has occurred for a certain property in the current session. They are [context-dependent](Property_operators.md) property operators and can be used inside [expressions](Expression.md). Inside an [event handler](Events.md#change) they switch to event mode and report changes since the previous occurrence of that event.

### Parameters

- `typeChange`

    Type of the change operator. It is specified by one of the keywords:

    - `SET` — value became non-`NULL` (was `NULL` before)
    - `DROPPED` — value became `NULL` (was non-`NULL` before)
    - `CHANGED` — value changed in any direction
    - `SETCHANGED` — value changed and is now non-`NULL`
    - `DROPCHANGED` — value either dropped or changed between two non-`NULL` values
    - `SETDROPPED` — value either set or dropped (a `NULL`/non-`NULL` boundary was crossed)

- `propExpr`

    The [expression](Expression.md) whose change is checked.

### Examples

```lsf
CLASS Order;
CLASS Item;
quantity = DATA NUMERIC[14,2] (Item);
price = DATA NUMERIC[14,2] (Item);
sum = DATA NUMERIC[14,2] (Item);
posted = DATA BOOLEAN (Order);
status = DATA STRING[20] (Order);

// CHANGED — derived property recomputed when either operand changes (any → any non-equal)
sum(Item i) <- quantity(i) * price(i) WHEN CHANGED(quantity(i)) OR CHANGED(price(i));

// SET — fires only on NULL → value (initial assignment)
WHEN SET(posted(Order o)) DO
    MESSAGE 'Order has been posted';

// SETCHANGED — value changed and is now non-NULL
WHEN SETCHANGED(status(Order o)) DO
    MESSAGE 'Status is now: ' + status(o);

// DROPPED — fires only on value → NULL; PREV reads the value before clearing
WHEN DROPPED(status(Order o)) DO
    MESSAGE 'Status was: ' + PREV(status(o));

// DROPCHANGED — value either dropped or replaced by another non-NULL value
CONSTRAINT DROPCHANGED(status(Order o))
    MESSAGE 'Status cannot be cleared or changed once assigned';

// SETDROPPED — value crossed the NULL/non-NULL boundary in either direction
WHEN SETDROPPED(status(Order o)) DO
    MESSAGE 'Status was assigned or cleared';
```
