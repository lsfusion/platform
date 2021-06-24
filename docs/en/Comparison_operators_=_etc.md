---
title: 'Comparison operators (=, >, <, ...)'
---

*Comparison operators* create [actions](Properties.md) which return the result of the comparison operation. The values of the created properties belong to the [built-in class](Built-in_classes.md) `BOOLEAN`.

The platform currently supports the following comparison operators:

|Operator   |Name                 |Description                                                                          |Example            |Result|
|-----------|---------------------|-------------------------------------------------------------------------------------|-------------------|------|
|`=` or `==`|Equality             |Takes two operands and returns `TRUE` if the operands are equal                      |`5 = 5` or `5 == 5`|`TRUE`|
|`!=`       |Inequality           |Accepts two operands and returns `TRUE` if the operands are not equal                |`3 != 5`           |`TRUE`|
|`>`, `<`   |Strict comparison    |Accepts two operands and returns `TRUE` if the strict comparison condition is met    |`3 > 5`            |`NULL`|
|`>=`, `<=` |Non-strict comparison|Accepts two operands and returns `TRUE` if the non-strict comparison condition is met|`4 <= 5`           |`TRUE`|

If one of the operands is `NULL`, all operators will return `NULL` as a result.

### Language

Description of [comparison operators](Comparison_operators.md).

### Examples


```lsf
equalBarcodes = barcode(a) == barcode(b);
outOfIntervalValue1(value, left, right) = value < left OR value > right;
outOfIntervalValue2(value, left, right) = NOT (value >= left AND value <= right);
```
