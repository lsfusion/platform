---
title: 'ROUND operator'
---

`ROUND` operator creates a [property](Properties.md) that implements the [rounding operation](Rounding_operator_ROUND.md).

### Syntax 

```
ROUND(numExpr[, scaleExpr])
```

### Description

The `ROUND` operator creates a property whose value is a number rounded to a specified precision. The precision can be set to a negative value, which results in rounding the least significant digits in the integer part of the number.

### Parameters

- `numExpr`

    [Expression](Expression.md) whose value determines the number to be rounded. The value must belong to one of the [numerical classes](Built-in_classes.md).

- `scaleExpr`

    Expression whose value determines the number of digits to which the number is rounded. The value of the expression must belong to the `INTEGER` class. A positive value indicates the number of digits after the decimal point, a negative value indicates the number of digits before the decimal point, and zero indicates rounding to the nearest integer. If not specified, it defaults to zero.

### Examples

```lsf
number = DATA NUMERIC[10,3]();  //12345.678
rounded = ROUND(number());      //12346
rounded1 = ROUND(number(), 2);  //12345.68
rounded2 = ROUND(number(), -2); //12300.00

FORM roundTest
PROPERTIES() number, rounded, rounded1, rounded2;
```