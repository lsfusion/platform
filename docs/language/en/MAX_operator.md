---
slug: "/MAX_operator"
title: 'MAX operator'
---

The `MAX` operator creates a [property](../paradigm/Properties.md) that implements finding the [maximum](../paradigm/Extremum_MAX_MIN.md) value.

### Syntax 

```
MAX expr1, ..., exprN
```

### Description

The `MAX` operator creates a property whose value is the maximum among the values of the specified operands. The skipping of `NULL` operands and the determination of the result class follow the [extremum](../paradigm/Extremum_MAX_MIN.md).

### Parameters

- `expr1, ..., exprN`

    [Expressions](Expression.md) whose values the maximum is selected among. At least one operand must be specified.

### Examples

```lsf
date1 = DATA DATE(INTEGER);
date2 = DATA DATE(INTEGER);
maxDate (INTEGER i) = MAX date1(i), date2(i);

balance = DATA INTEGER (Item);
outcome 'Balance (non-negative)' (Item i) = MAX balance(i), 0;
```
