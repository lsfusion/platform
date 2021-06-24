---
title: 'CHANGECLASS operator'
---

The `CHANGECLASS` operator creates an [action](Actions.md) that [changes objects classes](Class_change_CHANGECLASS_DELETE.md).

### Syntax

    CHANGECLASS expr TO className [WHERE whereExpr]

### Description

The `CHANGECLASS` operator creates an action that changes the class of objects for which a certain condition is met. This operator can add its local parameter, which will correspond to the objects being iterated. In this case, the `WHERE` block is required. This local parameter will not be a parameter of the action being created.

### Parameters

- `expr`

    An [expression](Expression.md) or [typed parameter](IDs.md#paramid). You can either use an already declared parameter as a typed parameter, or declare a new local parameter. When using an expression, new local parameters cannot be added.

- `className`

    The name of the class to which you want to change the object classes. A [composite ID](IDs.md#cid), since the class must be a [custom](User_classes.md) class.

- `whereExpr`

    An expression whose value is a condition for the created action. If not specified, it is considered equal to `TRUE`.

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
