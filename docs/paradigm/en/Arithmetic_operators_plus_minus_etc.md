---
slug: "/Arithmetic_operators_plus_minus_etc"
title: 'Arithmetic operators (+, -, *, ...)'
---

*Arithmetic operators* create [properties](Properties.md) whose value is the result of an arithmetic operation. They primarily operate on values of [number classes](Built-in_classes.md). In addition, the sum and difference also operate on date/time values (described below), and the sum also concatenates strings (see [String operators](String_operators_plus_CONCAT_SUBSTRING.md)). The platform currently supports the following arithmetic operators:

|Operator|Name|Description|Example|Result|
|--------|--------------|---|---|---|
|`+`     |Summation     |Takes two input operands and returns their sum|`3 + 5`|`8`|
|`-`     |Difference    |Accepts two input operands and returns their difference<br/>This operator also has a unary form, in which case the first operand is considered equal to `0`|`5 - 3`|`2`|
|`*`     |Multiplication|Accepts two input operands and returns their product|`3 * 5`|`15`|
|`/`     |Ratio         |Takes two input operands and returns their ratio|`15 / 3`|`5`|

All of these operators return `NULL` if one of the operands is `NULL` . Division by zero also returns `NULL`. It is also possible to use a special form of summation and difference operators with brackets, in which case `NULL` will be equivalent to `0`. The reverse is also true for these type of operators: if the result of an operator in such form is `0`, then `NULL` is returned (e. g., `5 (-) 5 = NULL`):

|Operator|Name|Description|Example|Result|
|--------|----------|---|---|---|
|`(+)`   |Summation |Takes two input operands and returns their sum, treating `NULL` as `0`|`3 (+) 5`<br/>`3 (+) NULL`|`8`<br/>`3`|
|`(-)`   |Difference|Takes two input operands and returns their difference, treating `NULL` as `0`|`5 (-) 3`<br/>`5 (-) NULL`<br/>`5 (-) 5`|`2`<br/>`5`<br/>`NULL`|

### Determining the result class

The result class is determined as:

|Operator|Result|
|--------|---|
|`+`, `-`|[Common ancestor](Built-in_classes.md#commonparentclass) ("Numbers" family)|
|`*`     |`NUMERIC[p1.IntegerPart + p1.Precision + p2.IntegerPart + p2.Precision, p1.Precision + p2.Precision]`|
|`/`     |`NUMERIC[p1.IntegerPart + p2.Precision + s, s]`|

The `NUMERIC[ , ]` formulas for the product and the ratio apply only when at least one operand belongs to `NUMERIC[ , ]`; otherwise the result is the common ancestor ("Numbers" family) of the two operand classes, so the product or ratio of two integers is itself an integer — the ratio being integer (truncating) division. In the ratio formula `s` is the maximum `NUMERIC` scale (`32` by default).

The sum and the difference also operate on date/time values, where a whole number is counted in base units — days for `DATE`, seconds for `DATETIME`, `ZDATETIME`, and `TIME`:

|Operands|Result|
|--------|---|
|date/time value `+` / `-` a whole number|the same date/time class|
|`DATE` `-` `DATE`|`INTEGER` (number of days)|
|`DATETIME` / `ZDATETIME` / `TIME` `-` a value of the same class|`LONG` (number of base units)|

### Language

Description [of arithmetic operators](../language/Arithmetic_operators.md).

### Examples

```lsf
sum(a, b) = a + b;
transform(a, b, c) = -a * (b (+) c);
```
