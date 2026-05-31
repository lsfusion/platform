---
slug: "/RECALCULATE_operator"
title: 'RECALCULATE operator'
---

The `RECALCULATE` operator creates an [action](../paradigm/Actions.md) that recalculates the stored values of a [materialized](../paradigm/Materializations.md) property.

### Syntax

```
RECALCULATE [CLASSES | NOCLASSES] propertyId(expr1, ..., exprN) [WHERE whereExpr]
```

### Description

The `RECALCULATE` operator creates an action that recomputes the stored values of a materialized property from its definition, for every set of arguments where `whereExpr` is not `NULL`. The argument list may introduce new local parameters; such parameters correspond to objects being iterated and are not parameters of the created action.

### Parameters

- `propertyId`

    [ID](IDs.md#propertyid) of the property whose stored values are recalculated. The property must be [materialized](../paradigm/Materializations.md).

- `expr1, ..., exprN`

    A list of [expressions](Expression.md) or [typed parameters](IDs.md#paramid) defining the arguments of the property. When using typed parameters, you can both reference already declared parameters and declare new local parameters; when using expressions, new local parameters cannot be added. The number of items in this list must equal the number of parameters of the property.

- `CLASSES | NOCLASSES`

    Keyword limiting what is recalculated. `CLASSES` recalculates only the property's class data, not its values; `NOCLASSES` recomputes only the values, assuming the class data is valid. If neither is specified, the values are recomputed (and the class data is refreshed when needed).

- `whereExpr`

    Expression whose value is the condition under which the property is recalculated. If not specified, it is considered equal to `TRUE`.

### Examples

```lsf
sum = GROUP SUM sum(OrderDetail od) BY order(od) MATERIALIZED;

// recompute all stored values of the materialized property
recalculateSum() {
    RECALCULATE sum(Order o);
}

// recompute only the values for one customer's orders, assuming the class data is valid
recalculateCustomerSum(Customer c) {
    RECALCULATE NOCLASSES sum(Order o) WHERE customer(o) = c;
}
```
