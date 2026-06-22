---
slug: "/DATA_operator"
title: 'DATA operator'
---

The `DATA` operator creates a [data property](../paradigm/Data_properties_DATA.md).

### Syntax

```lsf
DATA [LOCAL [NESTED [MANAGESESSION | NOMANAGESESSION]]] returnClass [(argumentClass1, ..., argumentClassN)]
```

### Description

The `DATA` operator creates a data property. This [property operator](../paradigm/Property_operators_paradigm.md) cannot be used inside [expressions](Expression.md). The data property can be created local by specifying the keyword `LOCAL`. 

For a local property, you can additionally specify `NESTED`. In this case, the property becomes [nested](../paradigm/Session_management.md#nested), and its values are preserved during session-management operations. If no additional modifier is specified after `NESTED`, the property is treated as nested both when creating a new session and when managing the current session. The `MANAGESESSION` modifier keeps the nested behavior only for `APPLY` / `CANCEL`, while `NOMANAGESESSION` keeps it only for `NEWSESSION`.

This operator cannot be used in the [`JOIN` operator](JOIN_operator.md) (inside `[ ]`), since a name must be specified for the data property.

### Parameters

- `LOCAL`

    A keyword that, when specified, creates a [local data property](../paradigm/Data_properties_DATA.md#local). 

- `NESTED`

    A keyword that can only be used after `LOCAL`. It marks the local property as [nested](../paradigm/Session_management.md#nested). Without additional modifiers, this means that the property is treated as nested both when [creating a new session](NEWSESSION_operator.md) and during `APPLY` / `CANCEL`.

- `MANAGESESSION` | `NOMANAGESESSION`

    Keywords that can only be used after `NESTED`.

    - `MANAGESESSION` means that the property is treated as nested only for operations that manage the current session (`APPLY`, `CANCEL`).
    - `NOMANAGESESSION` means that the property is treated as nested only when entering and leaving `NEWSESSION`.

- `returnClass`

    [Class ID](IDs.md#classid) of the return value of a property. 

- `argumentClass1, ..., argumentClassN`

    A list of class IDs for property arguments. An empty list (`()`) means the property has no arguments. Without the list at all, the argument classes are taken from the property declaration's typed parameters.

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
