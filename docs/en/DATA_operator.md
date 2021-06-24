---
title: 'DATA operator'
---

The `DATA` operator creates a [data property](Data_properties_DATA.md).

### Syntax

    DATA [LOCAL [NESTED]] returnClass (argumentClass1, ..., argumentClassN)

### Description

The `DATA` operator creates a data property. This [property operator](Property_operators_paradigm.md) cannot be used inside [expressions](Expression.md). The data property can be created local by specifying the keyword `LOCAL`. 

This operator cannot be used in the [`JOIN` operator](JOIN_operator.md) (inside `[ ]`), since a name must be specified for the data property.

### Parameters

- `LOCAL`

    A keyword that, when specified, creates a [local data property](Data_properties_DATA.md#local). 

- `NESTED`

    A keyword that, when specified, marks the local property as [nested](Session_management.md#nested) that is, all its changes will be visible in new sessions, and when these sessions are closed, changes to this property will be applied to the current session. Note that this behavior is similar to the behavior of a regular local property (not `NESTED`) when using the [`NEWSESSION` operator](NEWSESSION_operator.md) with the specified keyword `NESTED LOCAL` (or just `NESTED` if this local property is explicitly specified in the property list).

- `returnClass`

    [Class ID](IDs.md#classid) of the return value of a property. 

- `argumentClass1, ..., argumentClassN`

    A list of class IDs for property arguments. 

### Examples

```lsf
CLASS Item;
quantity = DATA LOCAL INTEGER (Item);

CLASS Country;
isDayOff = DATA BOOLEAN (Country, DATE);
```
