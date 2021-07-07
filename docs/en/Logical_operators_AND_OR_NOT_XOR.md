---
title: 'Logical operators (AND, OR, NOT, XOR)'
---

*Logical operators* create [properties](Properties.md) that consider their arguments as logical values of [class `BOOLEAN`](Built-in_classes.md) and whose return value is also a value of class `BOOLEAN`. If the value of an argument of an logical operator is not `NULL`, then the argument is treated as the value `TRUE` of class `BOOLEAN`, otherwise as `NULL`.

The platform currently supports the following logical operators:

|Operator|Name       |Description                                                               |Example        |Result|
|--------|-----------|--------------------------------------------------------------------------|---------------|------|
|`AND`   |Conjunction|Takes two operands and returns `TRUE` if both operands are non-`NULL`     |`TRUE AND TRUE`|`TRUE`|
|`OR`    |Disjunction|Takes two operands and returns `TRUE` if either operand is non-`NULL`     |`NULL OR TRUE` |`TRUE`|
|`NOT`   |Negation   |Takes one operand and returns `TRUE` if the operands is `NULL`            |`NOT TRUE`     |`NULL`|
|`XOR`   |Exception  |Takes two operands and returns `TRUE` if exactly one operand is non-`NULL`|`TRUE XOR TRUE`|`NULL`|

### Language

Description of [logical operator syntax](AND_OR_NOT_XOR_operators.md).

### Examples

```lsf
likes = DATA BOOLEAN (Person, Person);
likes(Person a, Person b, Person c) = likes(a, b) AND likes(a, c);
outOfInterval1(value, left, right) = value < left OR value > right;
outOfInterval2(value, left, right) = NOT (value >= left AND value <= right);
```
