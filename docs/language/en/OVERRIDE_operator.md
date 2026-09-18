---
slug: "/OVERRIDE_operator"
title: 'OVERRIDE operator'
---

The `OVERRIDE` operator creates a [property](../paradigm/Properties.md) that implements the [selection](../paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#poly) of one of the values (polymorphic form).

### Syntax

```
OVERRIDE expr1, ..., exprN
```

### Description

The `OVERRIDE` operator creates a property whose value will be the value of one of the properties specified in the operator. Selection is made among properties with a non-`NULL` value. If multiple properties are non-`NULL`, the value of the first of these properties is selected.

Parentheses immediately after `OVERRIDE` group its first operand; they do not delimit the operand list, so `OVERRIDE(expr1, expr2)` is not accepted.

### Parameters

- `expr1, ..., exprN`

    List of [expressions](Expression.md) whose values will determine the value of the property.

### Examples

```lsf
CLASS Group;
markup = DATA NUMERIC[8,2] (Group);

markup = DATA NUMERIC[8,2] (Book);
group = DATA Group (Book);
overMarkup (Book b) = OVERRIDE markup(b), markup(group(b));
// a NULL markup is compared as 0
sameMarkup (Book b1, Book b2) = (OVERRIDE markup(b1), 0) = (OVERRIDE markup(b2), 0);

notNullDate (INTEGER i) = OVERRIDE date(i), 2010_01_01;
```
