---
slug: "/CONCAT_operator"
title: 'CONCAT operator'
---

The `CONCAT` operator creates a [property](../paradigm/Properties.md) that implements a string [concatenation](../paradigm/String_operators_plus_CONCAT_SUBSTRING.md).

### Syntax

```
CONCAT separatorExpr, concatExpr1, ..., concatExprN
```

### Description

The `CONCAT` operator creates a property that concatenates the values of `concatExpr1, ..., concatExprN`, in the order written, placing the `separatorExpr` separator between them. The handling of the separator and of `NULL` operands follows the [`CONCAT` concatenation](../paradigm/String_operators_plus_CONCAT_SUBSTRING.md).

The first operand is always the separator, even when it is meant as a prefix: `CONCAT 'Total: ', 'a', 'b'` gives `'aTotal: b'`, not `'Total: ab'`. Text at the start or at the end of the result is written as an ordinary operand with an empty separator (`CONCAT '', 'Total: ', 'a', 'b'`) or added with the [`+` operator](Arithmetic_operators.md) (`'Total: ' + CONCAT ', ', 'a', 'b'`).

### Parameters

- `separatorExpr`

    [Expression](Expression.md) whose value is used as the separator. It is most often a string literal but may be any string expression.

- `concatExpr1, ..., concatExprN`

    Expressions whose values are concatenated. At least one operand must be specified.

### Examples

```lsf
CLASS Person;
firstName = DATA STRING[100] (Person);
middleName = DATA STRING[100] (Person);
lastName = DATA STRING[100] (Person);

// if some part of the name is not specified, then this part will be skipped along with a space
fullName(Person p) = CONCAT ' ', firstName(p), middleName(p), lastName(p);     

// the heading is a separate operand with an empty separator: the first operand of CONCAT is always the separator,
// and CONCAT 'Person: ', firstName(p), lastName(p) would give 'JohnPerson: Smith'
caption(Person p) = CONCAT '', 'Person: ', fullName(p);
```
