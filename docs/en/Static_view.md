---
title: 'Static view'
sidebar_label: Overview
---

In *static* view, all form data is read at the time the form is [opened](Open_form.md), after which the obtained data is converted:

-   either to [print](Print_view.md) view - image or excel. Print view is also often referred to as a *report*
-   or to [structured](Structured_view.md) view - one of the corresponding data formats (e.g. **XML**, **JSON**, **CSV**, **DBF**)

As a rule, structured view is used to load data into other information systems, while print view is used to view data by a user and print to a printer.

### Empty object group {#empty}

In static view, any form has a predefined *empty* object group, which is considered to be the first object group on the form, does not contain any objects and is considered to be the [display group](Form_structure.md#drawgroup) for properties/filters without parameters. Accordingly, in this display group there is always a maximum of one empty object collection (exactly one, if there are no filters without parameters).

### Object group hierarchy {#hierarchy}

To display information in static view , the form's [object groups](Form_structure.md) must be organized in a *hierarchy* in which data for object groups will be a kind of "nested" in each other. For example, if we have object groups `A` and `B`, and `A` is the *parent* of `B`, then in static view all properties of `A` will be displayed first for the first object collection from `A`, then all the properties of `B` and of the pair (`A`, `B`) for all object collections from `B`, then similar information will be displayed for the second object collection from `A` and all sets of objects from `B` and so on.

### Building object group hierarchy

The platform builds object group hierarchy based on the [form structure](Form_structure.md) as follows:

-   First, relations between object groups are built according to the following rules:
    -   object group `A` *depends* on object group `B` if `A` appears in the list of object groups later than `B` and the form has a property or filter that takes objects from `A` and `B` as input arguments (`B` should not be a [group-in-column](Form_structure.md#groupcolumns) of this property).
    -   any object group `A` depends on the empty object group
    -   group `A` *indirectly depends* on group `B` if, again, `A` appears later than `B` and there is an object group `C` which depends on both `A` and `B`

-   After the relations are built, the hierarchy is constructed in such a way that the parent of object group `A` is the group `B` that is latest in the object group list on which `A` depends (directly or indirectly).


:::info
As follows from the algorithm, the empty object group is always the only root group of the constructed hierarchy
:::

### An example of constructing object group hierarchy {#hierarchysample}

```lsf

FORM myForm 'myForm'
    OBJECTS A, B SUBREPORT, C, D, E
    PROPERTIES f(B, C), g(A, C)
    FILTERS c(E) = C, h(B, D)
;
```

The hierarchy of groups of objects for this form will be constructed as follows:

import GroupHierarchyEnSvg from './images/GroupHierarchyEn.svg';

<GroupHierarchyEnSvg />

  
