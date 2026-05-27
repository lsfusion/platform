---
slug: "/MIN_operator"
title: 'MIN operator'
---

The `MIN` operator creates a [property](../paradigm/Properties.md) that implements finding the [minimum](../paradigm/Extremum_MAX_MIN.md) value.

### Syntax 

```
MIN expr1, ..., exprN
```

### Description

The `MIN` operator creates a property whose value is the minimum among the values of the specified operands. The skipping of `NULL` operands and the determination of the result class follow the [extremum](../paradigm/Extremum_MAX_MIN.md).

### Parameters

- `expr1, ..., exprN`

    [Expressions](Expression.md) whose values the minimum is selected among. At least one operand must be specified.

### Examples

```lsf
price1 = DATA NUMERIC[10,2] (Book);
price2 = DATA NUMERIC[10,2] (Book);
minPrice (Book b) = MIN price1(b), price2(b);

date = DATA DATE (INTEGER);
minDate (INTEGER i) = MIN date(i), 2001_01_01;
```
