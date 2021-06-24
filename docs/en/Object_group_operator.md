---
title: 'Object group operator'
---

[Object group](Form_structure.md) operators are used for creating [properties](Properties.md) working with the [current state](Object_group_operators.md) of the object group on the [form](Forms.md).

### Syntax

    FILTER groupObjectId
    VIEW groupObjectId
    ORDER groupObjectId

### Description

Object group operators create properties that accept the same number of parameters as the number of objects in the object group. These operators cannot be used inside [expressions](Expression.md).

The `FILTER` operator creates a property which value is `TRUE` when the object collection passed as parameters meets all the [filtering](Form_structure.md#filters) conditions on the form, otherwise the property value will be `NULL`.

The `VIEW` operator creates a property which value is `TRUE` if the object collection passed as parameters is currently displayed on the form, otherwise, the property value will be `NULL`.

The `ORDER` operator creates a property which value determines the relative order of the object collection on the form passed as a parameter. The value of this property is usually used in `ORDER` blocks of the other properties, for example, [`PARTITION`](PARTITION_operator.md), [`FOR`](FOR_operator.md), etc.

### Parameters

- `groupObjectId`

    Global [object group ID](IDs.md#groupobjectid).

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
