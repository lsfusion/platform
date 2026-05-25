---
slug: "/CHANGECLASS_operator"
title: 'CHANGECLASS operator'
---

The `CHANGECLASS` operator creates an [action](../paradigm/Actions.md) that [changes objects classes](../paradigm/Class_change_CHANGECLASS_DELETE.md).

### Syntax

```
CHANGECLASS expr TO className [WHERE whereExpr]
```

### Description

The `CHANGECLASS` operator creates an action that assigns the class `className` to the object given by `expr` for every set of arguments where `whereExpr` is not `NULL`.

The operator may introduce a local parameter in `expr`; in that case the `WHERE` block is required. Such a parameter corresponds to objects being iterated and is not a parameter of the created action.

### Parameters

- `expr`

    [Expression](Expression.md) or [typed parameter](IDs.md#paramid) for the object whose class is changed. As a typed parameter, you can both reference an already declared parameter and declare a new local parameter; as an expression, new local parameters cannot be added.

- `className`

    Name of the [custom class](../paradigm/User_classes.md) to which the object's class is changed. [Composite ID](IDs.md#cid). The class must be concrete.

- `whereExpr`

    Expression whose value is the condition under which the class is changed. If not specified, it is considered equal to `TRUE`.

### Examples

```lsf
CLASS Document;
date = DATA DATE (Document);

CLASS ClosedDocument : Document;
// sets status to closed for all documents with a date older than 2 weeks
changeStatus()  {
    CHANGECLASS Document d TO ClosedDocument WHERE sum(date(d), 14) <= currentDate();
}
```
