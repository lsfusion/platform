---
title: 'Open form'
---

The *open form* operator creates an [action](Actions.md) that opens the specified [form](Forms.md).

### Form selection {#form}

In addition to explicitly specifying the form to be opened, the platform also allows to open the [list/edit](Interactive_view.md#edtClass) form for objects of the specified class. In this case, you only need to specify the corresponding option and specify the custom class whose list/edit form needs to be opened instead of a form.

### View type

When a form is opened, the [view](Form_views.md) type which will be used to display the form needs to be specified:

-   [In an interactive view (`SHOW`, `DIALOG`)](In_an_interactive_view_SHOW_DIALOG.md)
-   In a static view:
    -   [In a print view (`PRINT`)](In_a_print_view_PRINT.md)
    -   [In a structured view (`EXPORT`, `IMPORT`)](In_a_structured_view_EXPORT_IMPORT.md)

### Passing objects {#params}

When you open a form, you can pass a value for each of its objects from the calling context, which will be used as follows depending on the view:

-   In interactive view: the passed value is set as the [current](Form_structure.md#currentObject) object.
-   In a static view: an additional [filter](Form_structure.md#filters) will be set so that the object must be [equal](Comparison_operators_=_etc.md) to the passed value.

By default, all passed values of objects must be defined (not `NULL`); otherwise the action will not be executed and will simply transfer control to the next action. However, in the interactive view, the developer can change this behavior using the corresponding option (that is, allowing `NULL` values). In this case (as in the case when no object is passed at all), the [default object](Interactive_view.md#defaultobject) will be selected as the current object. 


:::info
It is worth noting that passing objects in the interactive view is basically the same as the [object seek](Search_SEEK.md) operation after the form is opened. In this case, the passed objects are the seek objects, and the [seek direction](Search_SEEK.md#direction) is determined by the default object type (`PREV` here is equivalent to `FIRST`).
:::
