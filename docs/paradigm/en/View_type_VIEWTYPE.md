---
slug: "/View_type_VIEWTYPE"
title: 'View type (VIEWTYPE)'
---

The view type operator creates a [property](Properties.md) whose value is the current *view type* in which an [object group](Form_structure.md#objects) is displayed to the user: as a table, a pivot table, a map, a calendar, or a custom view.

### Pivot table {#pivot}

When a pivot table is opened with no measure (value) columns pre-selected, the platform picks a default set: the [working parameter](Working_parameters.md) `pivotOnlySelectedColumn` set to `false` (the default) offers every numeric-typed property as a measure; set to `true`, only numeric-typed `COUNT(...)`-derived properties, and the property the user was on when switching to pivot view if that property is itself numeric, are pre-populated. The working parameter `useClusterizeInPivot` turns on virtualized row rendering (only the currently visible rows are kept in the DOM) once the user clicks *Show all* — where a very large result set would otherwise be rendered in full; the ordinary paginated view is never virtualized either way. Off by default, when *Show all* also renders every row directly.

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
