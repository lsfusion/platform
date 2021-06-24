---
title: 'Data properties (DATA)'
---

*Data property* is a [property](Properties.md) which value is stored in the database and may change through the execution of the [corresponding](Property_change_CHANGE.md) action. Each parameter and the value of a data property must belong to a certain specified [class](Classes.md). If a parameter does not belong to the specified class or is `NULL`, then the property value will return `NULL`. 

### Local data properties {#local}

Data properties can be *local*. Such properties retain their values only within the [session](Change_sessions.md), i.e. they are not saved to the database, which means when applying changes these values are reset to `NULL` by default.

### Language

To declare a primary property, use the [`DATA` operator](DATA_operator.md).

### Examples


```lsf
CLASS Item;
quantity = DATA LOCAL INTEGER (Item);

CLASS Country;
isDayOff = DATA BOOLEAN (Country, DATE);
```
