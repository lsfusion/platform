---
slug: "/Materializations"
title: 'Materializations'
---

Almost any aggregated [property](Properties.md) in the platform can be *materialized*. In this case, the property will be stored in the database permanently and automatically updated when the data on which this property depends is changed. At the same time, when reading the values of the materialized property, these values will be read directly from the database, as if the property was [data](Data_properties_DATA.md) (and not calculated every time). Accordingly, all data properties are materialized by definition.

A property can be materialized if and only if for it there is a finite number of object collections for which the value of this property is not `NULL` (that is, the iteration operation for all of its non-`NULL` values is [correct](Set_operations.md#correct))

The stored values of a materialized property can be *recalculated* — recomputed from scratch from the property's definition. This is useful when those values may have diverged from the definition, for example after the property's definition changes or after a direct data fix.

### Recalculation {#recalculate}

Recalculating a materialized property compares its stored values with its definition and writes back the ones that differ. Several [working parameters](Working_parameters.md) shape how that is done.

`useRecalculateClassesInsteadOfInconsisentExpr` (`true` by default) first recalculates the classes of the table the property is stored in, and only then compares the values, so the comparison can use the ordinary expression of the property. With it off the classes are left alone and the comparison uses an expression that tolerates inconsistent class data, which adds joins and needs more memory. Recalculating the classes is the more invasive of the two, since it rewrites the class data of the whole table. When a recalculation is asked for without the classes, the parameter does not apply.

`recalculateMaterializationsMixedSerializable` (`false` by default) changes how a recalculation that runs in its own transaction holds its isolation level: the mismatched rows are collected first, outside the transaction, and only writing them back is done at the strictest level. It costs an extra temporary table and a second query, and it does nothing for a recalculation the administrator asked to run inside a single shared transaction.

`maxRecalculateTime` (`300000` milliseconds) is not a timeout - nothing is interrupted by it. An operation that took longer than it is added to the report the service action returns, so that the slow tables and properties can be seen; `0` puts practically every operation into it, since the comparison is strict.

`groupByTables` (`true` by default) groups the properties being recalculated by the table they are stored in: in a multi-threaded recalculation the properties of one table run one after another rather than at the same time, so they do not wait on each other's locks. It is also what updates, at server start, the statistics of the columns that the synchronization of the database schema has just created or moved. With it off that statistics update is skipped entirely, and queries over those columns keep their poor plans until the statistics are recalculated by other means, so it is better left on.

### Language

To materialize a property, use the [`MATERIALIZED` option](../language/Property_options.md#persistent) in the property options. To recalculate the stored values of a materialized property, use the [`RECALCULATE` operator](../language/RECALCULATE_operator.md).

### Examples

```lsf
sum = GROUP SUM sum(OrderDetail od) BY order(od) MATERIALIZED;
date(OrderDetail od) = date(order(od)) MATERIALIZED;

 // such a property cannot be materialized, since it is not NULL for an infinite number of dates
lastDate(Customer customer, DATE date) = GROUP LAST date(Order order) IF customer(order) = customer AND date(order) < date ORDER order;
```
