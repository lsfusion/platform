---
slug: "/Arithmetic_operators"
title: 'Arithmetic operators'
---

The `+`, `-`, `*`, `/`, `(+)`, `(-)` operators create [properties](../paradigm/Properties.md) that implement [arithmetic operations](../paradigm/Arithmetic_operators_plus_minus_etc.md).

### Syntax

```
expression1 + expression2
expression1 - expression2
expression1 * expression2
expression1 / expression2
expression1 (+) expression2
expression1 (-) expression2
- expression1
```

### Description

The binary operators each take two operands and associate left to right; the unary minus takes a single operand. The evaluation order relative to other operators follows [operator priority](Operator_priority.md).

There are no dedicated operators for the remainder of division, integer division, or exponentiation — these operations are performed by the `mod[…, …]`, `divideInteger[…, …]`, and `power[…, …]` properties of the system module [`Utils`](../paradigm/System_Utils.md).

### Parameters

- `expression1, expression2`

    [Expressions](Expression.md) whose values will be arguments for arithmetic operators.

### Examples

```lsf
sum(a, b) = a + b;
transform(a, b, c) = -a * (b (+) c);
remainder(a, b) = mod(a, b); // remainder of division — a property of the Utils module
```
