---
slug: "/AND_OR_NOT_XOR_operators"
title: 'AND, OR, NOT, XOR operators'
---

The `AND`, `OR`, `NOT`, `XOR` operators create [properties](../paradigm/Properties.md) that implement [logical operations](../paradigm/Logical_operators_AND_OR_NOT_XOR.md).

### Syntax

```
expression1 AND expression2
expression1 OR expression2
expression1 XOR expression2
NOT expression1
```

### Description

`AND`, `OR`, and `XOR` are infix operators taking two operands; `NOT` is a prefix operator taking a single operand. The evaluation order relative to other operators follows [operator priority](Operator_priority.md).

### Parameters

- `expression1, expression2`

    [Expressions](Expression.md) used as the operands.

### Examples

```lsf
likes = DATA BOOLEAN (Person, Person);
likes(Person a, Person b, Person c) = likes(a, b) AND likes(a, c);
outOfInterval1(value, left, right) = value < left OR value > right;
outOfInterval2(value, left, right) = NOT (value >= left AND value <= right);
```
