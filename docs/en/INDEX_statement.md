---
title: 'INDEX statement'
---

The `INDEX` statement creates a new [index](Indexes.md).

### Syntax

    INDEX field1, ..., fieldN;

Each field of the `fieldi` [table](Tables.md) that the system should use to build an index can be described either by specifying a [property](Properties.md) stored in this table:

    propertyId(param1, ..., paramN)

or by specifying a typed parameter referring to the corresponding key field: 

    param

### Description

The `INDEX` statement adds a new index by an ordered list of fields of a certain table. The list must contain at least one property. The table that the index should be built for is determined by the first property in the list. Also, the parameters passed to this property are used to determine the correspondence of the parameters to the key fields of the table. Accordingly, all other properties in the list should have the same number of parameters and be stored in the same table as the first property. The parameters specified in the list will correspond to the key fields of the table.

### Parameters

- `propertyId`

    The ID of the [property](IDs.md#propertyid) that should be stored in the table for which the index is being created.

- `param1, ..., paramN`

    A list of property parameters. Each element of the list is a [typed parameter](IDs.md#paramid).

- `param`

    A typed parameter that determines the key field of the table.

### Examples

```lsf
INDEX customer(Order o);

date = DATA DATE (Order);
INDEX date(Order o), o;

INDEX name(Sku s), price(s, DATE d), d;
```
