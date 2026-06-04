---
slug: "/View_VIEW"
title: 'View (VIEW)'
---

The *view* operator creates a [property](Properties.md), defined on the objects of an object group, that returns `TRUE` when their object collection is included in the set of object collections currently displayed to the user in the group, and `NULL` otherwise.

### Language

To declare a property that determines whether a specified object collection is displayed to the user or not, use the [`VIEW` operator](../language/Object_group_operator.md).

### Examples

```lsf
CLASS Store;
name = DATA STRING[100] (Store);

FORM stores
    OBJECTS s = Store
;
countF 'Number of filtered warehouses' = GROUP SUM 1 IF [ VIEW stores.s](Store s);
orderF 'Order in an object group' (Store s) = PARTITION SUM 1 IF [ FILTER stores.s](s) ORDER [ ORDER stores.s](s), s;
setNameX 'Add X to name'()  {
    LOCAL k = INTEGER ();
    k() <- 0;
    FOR [ FILTER stores.s](Store s) ORDER [ ORDER stores.s](s) DO {
        k() <- k() + 1;
        name(s) <- 'X' + k() + name(s);
    }
}
```
