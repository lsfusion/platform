---
slug: "/View_type_VIEWTYPE"
title: 'View type (VIEWTYPE)'
---

The view type operator creates a [property](Properties.md) whose value is the current *view type* in which an [object group](Form_structure.md#objects) is displayed to the user: as a table, a pivot table, a map, a calendar, or a custom view.

### Language

To declare a property that returns the current view type of an object group, use the [`VIEWTYPE` operator](../language/Object_group_operator.md).

### Examples

```lsf
CLASS Store;
name = DATA STRING[100] (Store);

FORM stores
    OBJECTS s = Store
    PROPERTIES(s) name
;
isPivot 'Stores shown as pivot' () = [ VIEWTYPE stores.s]() == ListViewType.pivot;
```
