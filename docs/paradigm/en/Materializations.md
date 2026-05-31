---
slug: "/Materializations"
title: 'Materializations'
---

Almost any aggregated [property](Properties.md) in the platform can be *materialized*. In this case, the property will be stored in the database permanently and automatically updated when the data on which this property depends is changed. At the same time, when reading the values of the materialized property, these values will be read directly from the database, as if the property was [data](Data_properties_DATA.md) (and not calculated every time). Accordingly, all data properties are materialized by definition.

A property can be materialized if and only if for it there is a finite number of object collections for which the value of this property is not `NULL` (that is, the iteration operation for all of its non-`NULL` values is [correct](Set_operations.md#correct))

The stored values of a materialized property can be *recalculated* — recomputed from scratch from the property's definition. This is useful when those values may have diverged from the definition, for example after the property's definition changes or after a direct data fix.

### Language

To materialize a property, use the [`MATERIALIZED` option](../language/Property_options.md#persistent) in the property options. To recalculate the stored values of a materialized property, use the [`RECALCULATE` operator](../language/RECALCULATE_operator.md).

### Examples

```lsf
sum = GROUP SUM sum(OrderDetail od) BY order(od) MATERIALIZED;
date(OrderDetail od) = date(order(od)) MATERIALIZED;

 // such a property cannot be materialized, since it is not NULL for an infinite number of dates
lastDate(Customer customer, DATE date) = GROUP LAST date(Order order) IF customer(order) = customer AND date(order) < date ORDER order;
```
