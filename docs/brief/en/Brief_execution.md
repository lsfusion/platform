---
slug: "/Brief_execution"
title: 'Brief: execution'
---

## Stored data and materializations

The database stores [data (`DATA`) properties](../paradigm/Data_properties_DATA.md) — all but the [local](../paradigm/Data_properties_DATA.md#local) ones, which keep their values only within the [session](../paradigm/Change_sessions.md) — and calculated properties marked with the [`MATERIALIZED` option](../language/Property_options.md#persistent): their values sit in a table field, are updated automatically when the data they depend on changes, and are read straight from the database. Separate fields store an object's belonging to a class (`_CLASS_TableName`) and the table fullness flag (`_FULL_TableName`) — the latter only where fullness is not already guaranteed by what the table stores: a table holding class belonging for all descendants of its key class is marked full by itself and gets no such field.

A property can be materialized if and only if the number of object sets with a non-`NULL` value is finite. Materialization moves the cost from reading to writing: the value is not computed on every read, but is updated on every change of the source data. The mechanism is described in [materializations](../paradigm/Materializations.md).

```lsf
sum = GROUP SUM sum(OrderDetail od) BY order(od) MATERIALIZED;
```

**Analogy**: a materialized database view, refreshed as soon as the source data changes.

## Tables and database names

A table is declared by the [`TABLE` statement](../language/TABLE_statement.md):

```
TABLE name [dbName] (className1, ..., classNameN) [FULL | NODEFAULT];
```

The classes set the key fields `key0`, ..., `key(N-1)` — one per class, numbered from zero — the remaining fields hold property values. A property's table is set by the `TABLE` option; without it the property goes into the table closest by key classes, `NODEFAULT` excludes a table from that choice, and with no suitable table an `_auto_...` table is created. `FULL` means the table contains all objects of its key classes, and affects only query execution. The mechanism is described in [tables](../paradigm/Tables.md).

Names depend on the [naming policy](../paradigm/Launch_parameters.md#namingpolicy) (`db.namingPolicy`):

| Policy | Table | Field |
| --- | --- | --- |
| With signature (default) | `NameSpace_TableName` | `NameSpace_PropertyName_Class1_..._ClassN` |
| Without signature | `NameSpace_TableName` | `NameSpace_PropertyName` |
| Short | `TableName` | `PropertyName` |

## Indexes

An index on one property is created by the [`INDEXED` option](../language/Property_options.md#indexed), an index on an arbitrary list of fields of one table by the [`INDEX` statement](../language/INDEX_statement.md):

```
INDEX [dbName] [indexType] field1, ..., fieldN;
```

Only materialized properties can be indexed. A composite index takes both materialized properties and parameters referring to key fields; it must contain at least one materialized property, and all properties in it must be stored in one table and use the same set of parameters. The `LIKE` and `MATCH` types keep the usual index and try to add specialized ones — `LIKE` adds a `LIKE` index, `MATCH` on a string field adds both a `MATCH` and a `LIKE` one — which on string fields happens only when the current DB adapter has trigram / full-text support enabled, while on a single `TSVECTOR` field `MATCH` creates the specialized GIN index alone. A unique index on all keys of a table and indexes on the key suffixes `keyK`, ..., `keyN` are created automatically. The mechanism is described in [indexes](../paradigm/Indexes.md).

```lsf
orderDate = DATA DATE (Order) INDEXED;
INDEX supplier(Sku s, DATE d), s, price(s, d), d;
```

## Recomputing materializations

The [`RECALCULATE` operator](../language/RECALCULATE_operator.md) creates an action that recomputes the stored values of a materialized property from its definition:

```
RECALCULATE [CLASSES | NOCLASSES] propertyId(expr1, ..., exprN) [WHERE whereExpr]
```

Recomputing is reached for when the stored values may have diverged from the definition — after the property definition changed or after a direct data fix in the database. `WHERE` limits the argument sets to recompute, `CLASSES` recomputes only the class data, `NOCLASSES` only the values.

```lsf
recalculateSum() {
    RECALCULATE sum(Order o);
}
```
