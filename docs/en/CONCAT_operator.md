---
title: 'CONCAT operator'
---

The `CONCAT` operator creates a [property](Properties.md) that implements a string [concatenation](String_operators_+_CONCAT_SUBSTRING.md).

### Syntax

    CONCAT separatorString, concatExpr1, ..., concatExprN

### Description

The `CONCAT` operator creates a property that concatenates values using the `separatorString` separator. Here, `NULL` values are skipped and the separator is inserted only between non-`NULL` values.

### Parameters

- `separatorString`

    A [string literal](Literals.md#strliteral) to be used as a separator.

- `concatExpr1, ..., concatExprN`

    [Expressions](Expression.md) whose values are to be concatenated.

### Examples

```lsf
CLASS Person;
firstName = DATA STRING[100] (Person);
middleName = DATA STRING[100] (Person);
lastName = DATA STRING[100] (Person);

fullName(Person p) = CONCAT ' ', firstName(p), middleName(p), lastName(p);     // if some part of the name is not specified, then this part will be skipped along with a space
```
