---
slug: "/Comparison_operators"
title: 'Comparison operators'
---

The `==`, `=`, `!=`, `<`, `>`, `<=`, `>=` operators create [properties](../paradigm/Properties.md) that implement [comparison operations](../paradigm/Comparison_operators_=_etc.md).

### Syntax

```
expression1 == expression2
expression1 = expression2
expression1 != expression2
expression1 < expression2
expression1 > expression2
expression1 <= expression2
expression1 >= expression2
```

### Description

The `=` and `==` forms are equivalent. Each operator takes two operands and cannot be chained — `expression1 < expression2 < expression3` is not valid. The evaluation order relative to other operators follows [operator priority](Operator_priority.md).

### Parameters

- `expression1, expression2`

    [Expressions](Expression.md) which values are the arguments of a comparison operator.

### Examples

```lsf
equalBarcodes = barcode(a) == barcode(b);
outOfIntervalValue1(value, left, right) = value < left OR value > right;
outOfIntervalValue2(value, left, right) = NOT (value >= left AND value <= right);
```
