---
title: 'INDEX statement'
---

The `INDEX` statement creates a new [index](Indexes.md).

### Syntax

```
INDEX [dbName] [indexType] field1, ..., fieldN;
```

Here `field1, ..., fieldN` is a non-empty list of fields. Each element of this list can be written either as a [property](Properties.md) stored in this table:

```
propertyId(param1, ..., paramN)
```

or as a [typed parameter](IDs.md#paramid) referring to the corresponding key field:

```
param
```

### Description

The `INDEX` statement adds a new index by an ordered non-empty list of fields of one table. The list must contain at least one [materialized](Materializations.md) property. The table that the index should be built for is determined by the first property in the list. All other properties in the list must be stored in the same table and use the same set of parameters. Typed parameters specified separately in the list correspond to the key fields of that table.

### Parameters

- `dbName`

    [String literal](Literals.md#strliteral) that specifies the physical index name in the database. If omitted, the name is generated automatically.

- `indexType`

    Optional choice of a special index type. If omitted, a usual index is created.

    - `LIKE`

        For string fields, keeps the usual index and additionally tries to create a specialized GIN index for `LIKE` operations.

    - `MATCH`

        For string fields, keeps the usual index and additionally tries to create specialized indexes for `MATCH` and `LIKE`. The string `MATCH` index is built on `to_tsvector` with the current full-text search language.

        On a single field of type `TSVECTOR`, creates only the specialized GIN index by that field itself.

        Specialized `LIKE` / `MATCH` indexes for string fields are created only when the current DB adapter has the corresponding trigram/full-text support enabled. The usual index is created regardless of that support.

- `propertyId`

    The ID of the [property](IDs.md#propertyid) that should be stored in the table for which the index is being created.

- `param1, ..., paramN`

    A list of property parameters. Each element of the list is a [typed parameter](IDs.md#paramid).

    Parameter names in `param1, ..., paramN` must be distinct.

- `param`

    A [typed parameter](IDs.md#paramid) that determines the key field of the table.

### Examples

```lsf
INDEX customer(Order o);
number = DATA STRING (Order);
INDEX 'order_number_like' LIKE number(Order o);

searchVector = DATA TSVECTOR (Item);
INDEX 'item_search_match' MATCH searchVector(Item i);

date = DATA DATE (Order);
INDEX date(Order o), o;

INDEX supplier(Sku s, DATE d), s, price(s, d), d;
```
