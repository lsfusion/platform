---
title: 'In an interactive view (SHOW, DIALOG)'
---

This operator creates an action that [opens a form](Open_form.md) in the [interactive](Interactive_view.md) view.

### Flow management {#flow}

The operator works in two modes:

-   *Synchronous* (`WAIT`) - waits for the user to **close** the form, then saves the execution results and passes control to the next action.
-   *Asynchronous* (`NOWAIT`) - passes control to the next action right after **opening** a form on the client end.

By default, the operator works in the synchronous mode.

### Form location {#location}

A form being opened can be shown in two ways:

-   As a *window* (`FLOAT`)- the form is shown as a floating window.
-   As a *tab* (`DOCKED`) - the form is opened as a tab in the system [window](Navigator_design.md) `System.forms`.

By default, forms in the synchronous mode are shown as windows, in the asynchronous mode – as tabs.


:::info
In the current implementation of the platform, a form shown as a window is always modal, which means that the "asynchronous window" mode is not supported.
:::

### System action management

By default, when the interactive view is enabled, the platform [automatically determines](Interactive_view.md#sysactions) which system actions of the form/session lifecycle management should be shown and which not. But since these conditions are somewhat heuristic, the developer can use a corresponding set of options to specify the actions that need to be shown when the form is opened. Similarly, the corresponding option can be used to specify whether the form is the [owner of the session](Interactive_view.md#owner) in which it is opened or not.

### Dialog form {#dialog}

This operator also allows to return the latest [current value](Form_structure.md#currentObject) of a specified object (or, if necessary, of several objects), thus, essentially, performing [value input](Value_input.md). In this case, [input is considered to be canceled](Value_input.md#result) if the user closes the form using `System.formClose` (not `System.formOk`).

While inputting a value, it is sometimes necessary to not just choose one from a list, but also allow the user to return a `NULL` value. For this purpose, the dialog form has a special option that, if enabled, shows the drop action (`System.formDrop`) on the form. Calling this action closes the form and returns a `NULL` value as a result.

Similar to the other value input operators, the dialog form of this operator allows to:

-   specify [initial object values](Value_input.md#initial) (through the [passing objects](Open_form.md) mechanism).
-   specify [main and alternative](Value_input.md#result) actions. The first is called if the input was successfully completed, the second if not (i.e. if the input was canceled).
-   [change](Value_input.md#initial) a specified property.

In addition, if a value is entered to input to change the specified property, it is often necessary (at the value selection stage) to show the user not all object collections, but only those ones, whose selection (and subsequent change with these objects) will not violate existing [constraints](Constraints.md). Accordingly, so that the developer does not have to copy the form and manually add the necessary filter, the platform automatically generates and adds corresponding filters to the form (this behavior, however, can be disabled using a corresponding option).

The operator dialog form is available in the synchronous mode only.

### Extra features {#extra}

When opening a form, you can specify that all of its properties should be available in the "read-only"mode. In this case, the behavior will be identical to the behavior when during form creation, the "read-only" mode is specified for each [property view](Interactive_view.md#property).

Also, when calling the form, you can specify that it will be opened in a [new](New_session_NEWSESSION_NESTEDSESSION.md) (nested) session. In this case, [passing objects](Open_form.md#params) and value input will be performed in the current session (so it makes sense to use this option only if you need to pass objects and/or input a value; otherwise, it makes more sense to use a [new session](New_session_NEWSESSION_NESTEDSESSION.md) operator).

### Language

To open a form in the interactive view, use the [`SHOW` operator](SHOW_operator.md). To show the form in the dialog mode, use the [`DIALOG` operator](DIALOG_operator.md).

### Examples

```lsf
date = DATA DATE (Order);
FORM showForm
    OBJECTS dateFrom = DATE, dateTo = DATE PANEL
    PROPERTIES VALUE(dateFrom), VALUE(dateTo)

    OBJECTS o = Order
    FILTERS date(o) >= dateFrom, date(o) <= dateTo
;

testShow ()  {
    SHOW showForm OBJECTS dateFrom = 2010_01_01, dateTo = 2010_12_31;

    NEWSESSION {
        NEW s = Sku {
            SHOW sku OBJECTS s = s FLOAT;
        }
    }
}
```


```lsf
FORM selectSku
    OBJECTS s = Sku
    PROPERTIES(s) id
;

testDialog  {
    DIALOG selectSku OBJECTS s INPUT DO {
        MESSAGE 'Selected sku : ' + id(s);
    }
}

sku = DATA Sku (OrderDetail);
idSku (OrderDetail d) = id(sku(d));

changeSku (OrderDetail d)  {
    DIALOG selectSku OBJECTS s = sku(d) CHANGE;

    //equivalent to the first option
    DIALOG selectSku OBJECTS s = sku(d) INPUT NULL CONSTRAINTFILTER DO {
        sku(d) <- s;
    }
}
```

  
