---
title: 'Data properties (DATA)'
---

*Data property* is a [property](Properties.md) which value is stored in the database and may change through the execution of the [corresponding](Property_change_CHANGE.md) action. Each parameter and the value of a data property must belong to a certain specified [class](Classes.md). If a parameter does not belong to the specified class or is `NULL`, then the property value will return `NULL`. 

### Local data properties {#local}

Data properties can be *local*. Such properties retain their values only within the [session](Change_sessions.md), i.e. they are not saved to the database, which means when applying changes these values are reset to `NULL` by default.

A regular local property is convenient as temporary storage inside one session or one interaction flow. If the value must survive session-management operations, the local property can be made [nested](Session_management.md#nested).

### Nested local data properties

A *nested local data property* is a local data property whose value is preserved by the platform during the required session-management operations.

The basic nested mode preserves the property value for all major session-management operations:

- when entering and leaving a [new session](NEWSESSION_operator.md);
- after [applying changes](Apply_changes_APPLY.md);
- after [canceling changes](Cancel_changes_CANCEL.md).

If preservation is required only for part of these operations, a restricted nested mode is used: either only for [applying changes](Apply_changes_APPLY.md) and [canceling changes](Cancel_changes_CANCEL.md), or only when entering and leaving a [new session](NEWSESSION_operator.md).

### Language

To declare a data property, use the [`DATA` operator](DATA_operator.md).

### Examples


```lsf
CLASS Item;
quantity = DATA LOCAL INTEGER (Item);

sessionOwners = DATA LOCAL NESTED MANAGESESSION INTEGER ();

CLASS Order;
selected = DATA LOCAL NESTED NOMANAGESESSION BOOLEAN (Order);

CLASS Country;
isDayOff = DATA BOOLEAN (Country, DATE);
```
