---
slug: "/Logical_operators_AND_OR_NOT_XOR"
title: 'Logical operators (AND, OR, NOT, XOR)'
---

*Logical operators* create [properties](Properties.md) that consider their operands as logical values of [class `BOOLEAN`](Built-in_classes.md) and whose return value is also a value of class `BOOLEAN`. If the value of an operand of a logical operator is not `NULL`, then the operand is treated as the value `TRUE` of class `BOOLEAN`, otherwise as `NULL`. The result is always either `TRUE` or `NULL`, never `FALSE`.

The platform currently supports the following logical operators:

|Operator|Name       |Description                                                               |Example        |Result|
|--------|-----------|--------------------------------------------------------------------------|---------------|------|
|`AND`   |Conjunction|Takes two operands and returns `TRUE` if both operands are non-`NULL`     |`TRUE AND TRUE`|`TRUE`|
|`OR`    |Disjunction|Takes two operands and returns `TRUE` if either operand is non-`NULL`     |`NULL OR TRUE` |`TRUE`|
|`NOT`   |Negation   |Takes one operand and returns `TRUE` if the operand is `NULL`             |`NOT TRUE`     |`NULL`|
|`XOR`   |Exception  |Takes two operands and returns `TRUE` if exactly one operand is non-`NULL`|`TRUE XOR TRUE`|`NULL`|

With more than two operands, `XOR` returns `TRUE` when an odd number of operands are non-`NULL`.

### Language

Description of [logical operator syntax](../language/AND_OR_NOT_XOR_operators.md).

### Examples

```lsf
likes = DATA BOOLEAN (Person, Person);
likes(Person a, Person b, Person c) = likes(a, b) AND likes(a, c);
outOfInterval1(value, left, right) = value < left OR value > right;
outOfInterval2(value, left, right) = NOT (value >= left AND value <= right);
```
