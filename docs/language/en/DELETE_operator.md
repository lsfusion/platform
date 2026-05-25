---
slug: "/DELETE_operator"
title: 'DELETE operator'
---

The `DELETE` operator creates an [action](../paradigm/Actions.md) that [deletes objects](../paradigm/Class_change_CHANGECLASS_DELETE.md).

### Syntax

```
DELETE expr [WHERE whereExpr]
```

### Description

The `DELETE` operator creates an action that removes from the system the object given by `expr` for every set of arguments where `whereExpr` is not `NULL`.

The operator may introduce a local parameter in `expr`; in that case the `WHERE` block is required. Such a parameter corresponds to objects being iterated and is not a parameter of the created action.

### Parameters

- `expr`

    [Expression](Expression.md) or [typed parameter](IDs.md#paramid) for the object to delete. As a typed parameter, you can both reference an already declared parameter and declare a new local parameter; as an expression, new local parameters cannot be added.

- `whereExpr`

    Expression whose value is the condition under which the object is deleted. If not specified, it is considered equal to `TRUE`.

### Examples

```lsf
// deleting an object
deleteObject(obj)  { DELETE obj; }

// deleting all inactive products
CLASS Article;
active = DATA BOOLEAN (Article);
deleteInactiveArticles()  {
    // a local parameter a is added corresponding to the objects to be iterated over
    DELETE Article a WHERE a IS Article AND NOT active(a);
}
```
