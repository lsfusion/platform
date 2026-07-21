---
slug: "/Rules_physical_model"
title: 'Rules: physical model'
---

## Scope

Recommendations for storing data in the database — [tables](../paradigm/Tables.md), [materializations](../paradigm/Materializations.md) and [indexes](../paradigm/Indexes.md). The common mandatory rules are in [Rules](Rules.md); this article is not part of the set always passed to the assistant and is requested when needed.

## Tables

1. Tables should be declared explicitly for every set of key classes in use, and properties with that set of parameters should be placed into them with the `TABLE` option. No logic should rely on an automatically created `_auto_...` table: its name is determined by the parameter classes, so changing a property's signature moves the data to a different table.

2. Properties with the same set of parameters that are usually read together should be stored in one table: reading them then requires no table join.

3. The `NODEFAULT` option should be used for narrow-purpose tables that properties may enter only explicitly.

4. The `FULL` option should be specified for a table that contains all objects of its key classes. It affects only how queries are executed, so it must not be specified for a table that is not filled for all objects.

5. The naming policy should be chosen at the start of a project. The short policy keeps database names readable, but with a large number of materialized properties it requires explicit field names to keep those names unique.

## Materializations

1. Aggregated properties that are read, or used in filter conditions, considerably more often than the data they depend on changes should be materialized.

2. Properties whose value is non-`NULL` for an infinite number of object sets should not be materialized — such a property cannot be materialized at all. The typical case is a property with a built-in class parameter, such as a date, that is not restricted by a condition.

3. Materializing a chain of intermediate properties multiplies the work done when data changes: the result that is actually read should be materialized, not every step of the computation.

4. A property that depends on frequently changing data and is read rarely should not be materialized — its stored values would be updated on every change.

5. After a materialized property's definition changes, or after a direct data fix in the database, the stored values should be recomputed with the [`RECALCULATE` operator](../language/RECALCULATE_operator.md).

## Indexes

1. Indexes should be created for properties used for filtering or search in forms and queries, and should not be created just in case: every index is updated whenever the values of its fields change.

2. Only materialized properties can be indexed, so an index on a calculated property requires materializing it — and that decision is made by the rules of the previous section, not for the sake of the index.

3. A composite index should be created when filtering uses several fields of one table at once; the order of fields in it should follow the order in which those fields are restricted.

4. An index duplicating the automatically created ones should not be created: the unique index on all key fields of a table and the indexes on the key suffixes already exist.

5. For fields searched with the `LIKE` and `MATCH` operators, the index types of the same names should be used instead of a plain index.

## Examples

```lsf
TABLE order (Order);
TABLE orderDetail (OrderDetail);
TABLE skuStock (Sku, Stock);

date = DATA DATE (Order) INDEXED;
sum (OrderDetail d) = quantity(d) * price(d) MATERIALIZED;
sum (Order o) = GROUP SUM sum(OrderDetail d) BY order(d) MATERIALIZED;

INDEX customer(Order o), date(o);
```
