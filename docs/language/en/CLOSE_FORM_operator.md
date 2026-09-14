---
slug: "/CLOSE_FORM_operator"
title: 'CLOSE FORM operator'
---

The `CLOSE FORM` operator creates an [action](../paradigm/Actions.md) that closes the open forms a given address names in the [interactive view](../paradigm/In_an_interactive_view_SHOW_DIALOG.md).

### Syntax

```
CLOSE FORM formAddress
```

Where `formAddress` is one of:

```
[formLabel =] formName [WINDOW windowName]
formLabel [WINDOW windowName]
WINDOW windowName
```

### Description

The `CLOSE FORM` operator creates an action that closes, for the user, every open form the address names (sent to the client as a request). The action has no parameters and uses no [context](Action_operators.md#contextdependent). Closing is a request: a form with unsaved changes asks the user and may stay open. If the address names no open form, the action has no effect.

The address names a form by any combination of the label it was opened with, its name, and the window it was opened into; at least one of the three has to be specified, and a part that is not specified does not narrow the address.

### Parameters

- `formLabel`

    A [string literal](Literals.md#strliteral) — the label the form was given when it was opened by the [`SHOW` operator](SHOW_operator.md). Several open forms may carry the same label, and all of them are closed. If it is not specified, forms are closed whatever label they carry.

- `formName`

    Form name. [Composite ID](IDs.md#cid). If it is not specified, forms of any name are closed.

- `windowName`

    Name of the [`FORMS` window](WINDOW_statement.md) the form was opened into. [Composite ID](IDs.md#cid). If it is not specified, the form is looked for in every window. On the mobile web client and in the desktop client, which draw `System.forms` alone, this part is ignored.

### Examples

```lsf
FORM orders
    OBJECTS o = Order
    PROPERTIES(o) number, customer
;

// open the form with an instance identifier
openOrders ()  {
    SHOW 'ordersInstance' = orders NOWAIT;
}

// close the previously opened form by its label
closeOrders ()  {
    CLOSE FORM 'ordersInstance';
}

WINDOW workspace FORMS;

// every open orders, whatever label it carries
closeAllOrders ()  {
    CLOSE FORM orders;
}

// everything open in a window, which is how a window is emptied
clearWorkspace ()  {
    CLOSE FORM WINDOW workspace;
}
```