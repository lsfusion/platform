---
slug: "/Container_visibility_EXPAND_COLLAPSE"
title: 'Container visibility (EXPAND, COLLAPSE)'
---

Container *expansion* and *collapse* operators control whether the contents of a *collapsible* [container](Form_design.md#containers) are shown on a [form](Forms.md). A collapsible container can be collapsed to hide its contents, and expanded to show them again; this state is part of the user's [interactive](Interactive_view.md) view of the form.

As input, these operators take a single container of a form. The container must be collapsible. The resulting [action](Actions.md) expands or collapses that container for the user working with the form, and expects a form context.

### Language

To declare actions that expand or collapse a container, use the [`EXPAND`](../language/EXPAND_operator.md) and [`COLLAPSE`](../language/COLLAPSE_operator.md) operators.

### Examples

```lsf
CLASS Store;
name = DATA ISTRING[100] (Store);

FORM dashboard
    OBJECTS s = Store
    PROPERTIES(s) name
;

DESIGN dashboard {
    NEW detailsBox {
        collapsible = TRUE;
        caption = 'Details';
        MOVE BOX(s);
    }
}

expandDetails {
    EXPAND CONTAINER dashboard.detailsBox;
}

collapseDetails {
    COLLAPSE CONTAINER dashboard.detailsBox;
}

EXTEND FORM dashboard
    PROPERTIES() expandDetails, collapseDetails
;
```
