---
slug: "/Selection_SELECT"
title: 'Selection (SELECT)'
---

The selection operators create [properties](Properties.md) that report the user's row and column selection in an [object group](Form_structure.md#objects):

-   the property of an object collection returns `TRUE` if that collection is currently selected (checked) by the user in the group, and `NULL` otherwise;
-   the property of the group as a whole returns `TRUE` if multiple-row selection is currently active for the group, and `NULL` otherwise;
-   the property of a column returns `TRUE` if that column is currently selected by the user, and `NULL` otherwise.

### Language

To declare these properties, use the [`SELECT`, `SELECT ACTIVE`, and `SELECT PROPERTY` operators](../language/Object_group_operator.md).

### Examples

```lsf
CLASS Store;
name = DATA STRING[100] (Store);

FORM stores
    OBJECTS s = Store
    PROPERTIES(s) name
;
selectedCount 'Number of selected stores' () = GROUP SUM 1 IF [ SELECT stores.s](Store s);
multiSelectActive 'Multiple-row selection is on' () = [ SELECT ACTIVE stores.s]();
nameSelected 'Name property is selected' () = [ SELECT PROPERTY stores.name]();
```
