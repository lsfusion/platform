---
title: 'EXCLUSIVE operator'
---

The `EXCLUSIVE` operator creates a [property](Properties.md) that implements a [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#exclusive) of one of the values (polymorphic form).

### Syntax

    EXCLUSIVE expr1, ..., exprN

### Description

The `EXCLUSIVE` operator creates a property whose value will be the value of one of the properties specified in the operator. It is assumed that for any set of parameters, at most one of the properties will be non-`NULL`. The value of the property will be the value of this single non-`NULL` property, or `NULL` if there are no such properties.

### Parameters

- `expr1, ..., exprN`

    List of [expressions](Expression.md) whose values will determine the value of the property.

### Examples

```lsf
background 'Color' (INTEGER i) = EXCLUSIVE RGB(255,238,165) IF i <= 5,
                                                   RGB(255,160,160) IF i > 5;

CLASS Human;

CLASS Male : Human;
CLASS Female : Human;

name(Human h) = EXCLUSIVE 'Male' IF h IS Male, 'Female' IF h IS Female;
```
