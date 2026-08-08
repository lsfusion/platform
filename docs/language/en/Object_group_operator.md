---
slug: "/Object_group_operator"
title: 'Object group operator'
---

[Object group](../paradigm/Form_structure.md) operators are used for creating [properties](../paradigm/Properties.md) working with the [current state](../paradigm/Object_group_operators.md) of the object group and its properties on the [form](../paradigm/Forms.md).

### Syntax

```
FILTER groupObjectId
VIEW groupObjectId
ORDER groupObjectId
SELECT groupObjectId
SELECT ACTIVE groupObjectId
VIEWTYPE groupObjectId
SELECT PROPERTY formPropertyId
```

### Description

The `FILTER`, `VIEW`, `ORDER`, and `SELECT` operators create properties that accept the same number of parameters as the number of objects in the object group. Object group operators cannot be used inside [expressions](Expression.md).

The `FILTER` operator creates a property which value is `TRUE` when the object collection passed as parameters meets all the [filtering](../paradigm/Form_structure.md#filters) conditions on the form, otherwise the property value will be `NULL`.

The `VIEW` operator creates a property which value is `TRUE` if the object collection passed as parameters is currently displayed on the form, otherwise, the property value will be `NULL`.

The `ORDER` operator creates a property which value determines the relative order of the object collection on the form passed as a parameter. The value of this property is usually used in `ORDER` blocks of the other properties, for example, [`PARTITION`](PARTITION_operator.md), [`FOR`](FOR_operator.md), etc.

The `SELECT` operator creates a property which value is `TRUE` if the object collection passed as parameters is currently selected (checked) by the user in the object group, otherwise the property value will be `NULL`.

The `SELECT ACTIVE` operator creates a parameterless property which value is `TRUE` if rows are selected in the object group, otherwise `NULL`. The selection counts from the moment the user starts it, even if a single row is selected. While this property is `NULL`, the property created by the `SELECT` operator returns the current row.

The `VIEWTYPE` operator creates a parameterless property whose value is the current view type of the object group — an object of the `ListViewType` system class (`grid`, `pivot`, `map`, `custom`, or `calendar`).

The `SELECT PROPERTY` operator creates a property whose value is `TRUE` if the specified form property is currently selected by the user, otherwise `NULL`. It accepts as parameters the objects by which the property is shown across columns; a property shown as a single column has no parameters.

### Parameters

- `groupObjectId`

    Global [object group ID](IDs.md#groupobjectid).

- `formPropertyId`

    Global [ID of a property or action on a form](IDs.md#formpropertyid).

### Examples

```lsf
CLASS Store;
name = DATA STRING[100] (Store);

FORM stores
    OBJECTS s = Store
    PROPERTIES(s) name
;
countF 'Number of filtered warehouses' = GROUP SUM 1 IF [ VIEW stores.s](Store s);
orderF 'Order in an object group' (Store s) = PARTITION SUM 1 IF [ FILTER stores.s](s) ORDER [ ORDER stores.s](s), s;
isPivot 'Stores shown as pivot' () = [ VIEWTYPE stores.s]() == ListViewType.pivot;
selectedCount 'Number of selected stores' () = GROUP SUM 1 IF [ SELECT stores.s](Store s);
selectActive 'The selection is set' () = [ SELECT ACTIVE stores.s]();
nameSelected 'Name property is selected' () = [ SELECT PROPERTY stores.name(s)]();
setNameX 'Add X to name'()  {
    LOCAL k = INTEGER ();
    k() <- 0;
    FOR [ FILTER stores.s](Store s) ORDER [ ORDER stores.s](s) DO {
        k() <- k() + 1;
        name(s) <- 'X' + k() + name(s);
    }
}
```
