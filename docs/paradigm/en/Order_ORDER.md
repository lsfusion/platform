---
slug: "/Order_ORDER"
title: 'Order (ORDER)'
---

The *order* operator creates a [property](Properties.md), defined on the objects of an object group, that returns a value reflecting the relative order of their object collection within the group's current [order](Form_structure.md#sort). This value has no standalone meaning: only comparing it with the same property for the group's other object collections is meaningful, and that comparison reproduces the group's current order.

### Language

To declare a property that determines the order in a group of objects, use the [`ORDER` operator](../language/Object_group_operator.md).

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
