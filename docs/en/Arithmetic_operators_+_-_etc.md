---
title: 'Arithmetic operators (+, -, *, ...)'
---

*Arithmetic operators* create [properties](Properties.md) whose value is the result of an arithmetic operation. The arguments of these operators must be properties whose values are instances of [number classes](Built-in_classes.md) . The platform currently supports the following arithmetic operators:

|Operator|Name|Description|Example|Result|
|--------|--------------|---|---|---|
|`+`     |Summation     |Takes two input operands and returns their sum|`3 + 5`|`8`|
|`-`     |Difference    |Accepts two input operands and returns their difference<br/>This operator also has a unary form, in which case the first operand is considered equal to `0`|`5 - 3`|`2`|
|`*`     |Multiplication|Accepts two input operands and returns their product|`3 * 5`|`15`|
|`/`     |Ratio         |Takes two input operands and returns their ratio|`15 / 3`|`5`|

All of these operators return `NULL` if one of the operands is `NULL` . It is also possible to use a special form of summation and difference operators with brackets, in which case `NULL` will be equivalent to `0`. The reverse is also true for these type of operators: if the result of an operator in such form is `0`, then `NULL` is returned (e. g., `5 (-) 5 = NULL`):

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
|`/`     |`NUMERIC[p1.IntegerPart + p1.Precision + p2.IntegerPart + p2.Precision, p1.Precision + p2.IntegerPart]`|
  

### Language

Description [of arithmetic operators](Arithmetic_operators.md).

### Examples

```lsf
sum(a, b) = a + b;
transform(a, b, c) = -a * (b (+) c);
```
