---
slug: "/Class_change_CHANGECLASS_DELETE"
title: 'Class change (CHANGECLASS, DELETE)'
---

The *class change* operator creates an [action](Actions.md) that assigns a target [class](Classes.md) to all objects matching a condition. The condition is an expression of the same arguments as the object expression; it may be omitted, in which case it is considered to always hold. The target class must be a concrete [custom class](User_classes.md).

A related form of the operator — *deletion* — removes the matching objects from the system instead of assigning them another class.

When an object changes its class or is deleted, any data property whose stored value is no longer valid for the object — the object appears among the property's arguments or as its value, but does not belong to the property's declared classes — is automatically reset to `NULL`.

### Language

To declare an action that changes object classes, use the [`CHANGECLASS` operator](../language/CHANGECLASS_operator.md); for the deletion case use the [`DELETE` operator](../language/DELETE_operator.md).

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


```lsf
CLASS Document;
date = DATA DATE (Document);

CLASS ClosedDocument : Document;
// sets status to closed for all documents with a date older than 2 weeks
changeStatus()  {
    CHANGECLASS Document d TO ClosedDocument WHERE sum(date(d), 14) <= currentDate();
}
```
