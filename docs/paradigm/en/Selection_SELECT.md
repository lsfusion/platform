---
slug: "/Selection_SELECT"
title: 'Selection (SELECT)'
---

The selection operators create [properties](Properties.md) that report the user's row and column selection in an [object group](Form_structure.md#objects):

-   the property of an object collection returns `TRUE` if that collection is currently selected (checked) by the user in the group, and `NULL` otherwise;
-   the property of the group as a whole returns `TRUE` if the user selected rows in the group, and `NULL` otherwise;
-   the property of a column returns `TRUE` if that column is currently selected by the user, and `NULL` otherwise.

As long as the user has not selected any row, the property of an object collection returns `TRUE` for the current row of the object group; once there is a selection, it returns `TRUE` for the selected rows that pass all the form's filter conditions. The selected rows are kept as a range in the current order of the object group, so changing the order recomputes the set of selected rows. The column the current cell belongs to counts as selected.

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
selectActive 'The selection is set' () = [ SELECT ACTIVE stores.s]();
nameSelected 'Name property is selected' () = [ SELECT PROPERTY stores.name(s)]();
```
