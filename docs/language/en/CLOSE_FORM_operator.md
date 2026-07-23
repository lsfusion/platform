---
slug: "/CLOSE_FORM_operator"
title: 'CLOSE FORM operator'
---

The `CLOSE FORM` operator creates an [action](../paradigm/Actions.md) that closes an open form instance with the given identifier in the [interactive view](../paradigm/In_an_interactive_view_SHOW_DIALOG.md).

### Syntax

```
CLOSE FORM formId
```

### Description

The `CLOSE FORM` operator creates an action that closes, for the user, the open form instance with the identifier `formId` (sent to the client as a request). The identifier is assigned when the form is opened by the [`SHOW` operator](SHOW_operator.md). The action has no parameters and uses no [context](Action_operators.md#contextdependent). If several instances with this identifier are open, all of them are closed; if no such instance is open, the action has no effect.

### Parameters

- `formId`

    A [string literal](Literals.md#strliteral) that identifies the form instance to close. This is the same identifier assigned when the form is opened by the `SHOW` operator.

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

// close the previously opened instance by its identifier
closeOrders ()  {
    CLOSE FORM 'ordersInstance';
}
```