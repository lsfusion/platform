---
title: 'Order (ORDER)'
---

The *order* operator creates a [property](Properties.md) that returns the sequence number of an object collection in the specified group of objects, in accordance with the current [order](Form_structure.md#sort) of this group.

### Language

To declare a property that determines the order in a group of objects, use the [`ORDER` operator](Object_group_operator.md).

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
