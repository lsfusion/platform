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

### Parameters

- `expression1, expression2`

    [Expressions](Expression.md) whose values will be arguments for arithmetic operators.

### Examples

```lsf
sum(a, b) = a + b;
transform(a, b, c) = -a * (b (+) c);
```
