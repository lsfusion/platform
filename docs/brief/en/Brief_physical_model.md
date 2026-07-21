---
slug: "/Brief_physical_model"
title: 'Brief: physical model'
---

## Physical model map

A detailed map of a single area — how data is stored in the database. The overall element map is in [Brief](Brief.md), and the mechanisms are described in [Tables](../paradigm/Tables.md), [Materializations](../paradigm/Materializations.md) and [Indexes](../paradigm/Indexes.md).

This article is not part of the set always passed to the assistant; it is requested when needed.

## What goes into the database

- Data ([`DATA`](../paradigm/Data_properties_DATA.md)) properties — always stored.
- Calculated properties with the `MATERIALIZED` option — stored and updated automatically when the data they depend on changes.
- An object's belonging to a class — stored like a data property, in the `_CLASS_TableName` field.
- The table fullness flag — stored, when needed, in the `_FULL_TableName` field.

A property can be materialized if and only if the number of object sets with a non-`NULL` value is finite.

## Tables

- A table is declared by the [`TABLE` statement](../language/TABLE_statement.md) with the list of its key classes: `TABLE skuStock (Sku, Stock);`.
- Key fields are named `key0`, ..., `keyN`; the remaining fields store property values.
- A property's table is set by the `TABLE` property option. If it is not set, the platform picks an existing table with the same number of keys and the closest key classes; the `NODEFAULT` option excludes a table from that choice.
- If no suitable table exists, a table named `_auto_<class ID>_..._<class ID>` is created.
- `FULL` means the table contains all existing objects of its key classes; it is used only to optimize execution (`INNER JOIN` instead of `LEFT JOIN`) and does not affect the logic.

## Names in the database

Table and field names depend on the naming policy set by the [`db.namingPolicy`](../paradigm/Launch_parameters.md#namingpolicy) launch parameter.

| Naming policy                 | Table name            | Field name                                        |
| ----------------------------- | --------------------- | ------------------------------------------------- |
| Full with signature (default) | `NameSpace_TableName` | `NameSpace_PropertyName_ClassName1_..._ClassNameN`|
| Full without signature        | `NameSpace_TableName` | `NameSpace_PropertyName`                          |
| Short                         | `TableName`           | `PropertyName`                                    |

The field name for a particular property can be specified explicitly.

## Indexes

- Only materialized properties can be indexed.
- An index on one property is set by the `INDEXED` option; an index on an arbitrary list of fields of one table is created by the [`INDEX` statement](../language/INDEX_statement.md).
- A composite index can include both materialized properties and parameters referring to key fields; it must contain at least one materialized property, and all properties in it must be stored in one table and use the same set of parameters.
- The `LIKE` and `MATCH` types create specialized indexes for the operators of the same names; `MATCH` on a string field works when the current database adapter has trigram/full-text support, and for a `TSVECTOR` field it creates a GIN index on that field itself.
- A unique index on all key fields of a table and indexes on the key suffixes `keyK`, ..., `keyN` are created automatically.

## Recalculation

The stored values of a materialized property are recomputed from its definition by the [`RECALCULATE` operator](../language/RECALCULATE_operator.md) — this is needed when the values may have diverged from the definition, for example after the definition changed or after a direct data fix.

## Examples

```lsf
TABLE sku (Sku) FULL;
TABLE skuDate (Sku, DATE);

price = DATA NUMERIC[10,2] (Sku, DATE);
sum = GROUP SUM sum(OrderDetail od) BY order(od) MATERIALIZED;

orderDate = DATA DATE (Order) INDEXED;
INDEX supplier(Sku s, DATE d), s, price(s, d), d;
```
