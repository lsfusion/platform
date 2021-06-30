---
title: 'Filter (FILTER)'
---

The *filter* operator creates a [property](Properties.md) that returns `TRUE` if the object collection matches the current [filter](Form_structure.md#filters) of the specified object group, and `NULL` if it does not.

### Language

To declare a property that determines matching the filter use the [`FILTER` operator](Object_group_operator.md).

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
